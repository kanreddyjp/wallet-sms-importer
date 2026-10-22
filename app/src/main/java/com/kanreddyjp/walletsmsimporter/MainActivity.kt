
package com.kanreddyjp.walletsmsimporter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kanreddyjp.walletsmsimporter.sms.SmsMessage
import com.kanreddyjp.walletsmsimporter.sms.SmsReader
import com.kanreddyjp.walletsmsimporter.transaction.TransactionDraft
import com.kanreddyjp.walletsmsimporter.ui.WalletSmsImporterApp
import com.kanreddyjp.walletsmsimporter.wallet.WalletConnectionState
import com.kanreddyjp.walletsmsimporter.wallet.WalletLocalStore
import com.kanreddyjp.walletsmsimporter.wallet.WalletRecordSubmissionState
import com.kanreddyjp.walletsmsimporter.wallet.WalletRepository
import com.kanreddyjp.walletsmsimporter.wallet.WalletTokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var smsMessages by mutableStateOf<List<SmsMessage>>(emptyList())
    private var scanStatus by mutableStateOf("Never scanned")

    private var walletConnectionState by mutableStateOf<WalletConnectionState>(
        WalletConnectionState.Disconnected
    )

    private var submissionState by mutableStateOf<WalletRecordSubmissionState>(
        WalletRecordSubmissionState.Idle
    )

    private var savedSenderHeaders by mutableStateOf<List<String>>(emptyList())
    private var isRefreshingWalletAccounts by mutableStateOf(false)

    private lateinit var walletTokenStore: WalletTokenStore
    private lateinit var walletLocalStore: WalletLocalStore
    private lateinit var walletRepository: WalletRepository

    private val smsPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                scanSms()
            } else {
                scanStatus = "SMS permission denied"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        walletTokenStore = WalletTokenStore(this)
        walletLocalStore = WalletLocalStore(this)

        walletRepository = WalletRepository(
            tokenStore = walletTokenStore,
            localStore = walletLocalStore
        )

        savedSenderHeaders = walletRepository.getSavedSenderHeaders()

        if (
            walletTokenStore.hasToken() &&
            walletLocalStore.hasWalletData()
        ) {
            val accounts = walletRepository.getCachedAccounts()
            val categories = walletRepository.getCachedCategories()

            walletConnectionState =
                WalletConnectionState.Connected(
                    accountCount = accounts.size,
                    categoryCount = categories.size
                )
        }

        setContent {
            WalletSmsImporterApp(
                smsMessages = smsMessages,
                scanStatus = scanStatus,
                onScanSms = ::requestSmsPermission,
                walletConnectionState = walletConnectionState,
                onConnectWallet = ::connectWallet,
                onRefreshWalletAccounts = ::refreshWalletAccounts,
                isRefreshingWalletAccounts = isRefreshingWalletAccounts,
                accounts = walletRepository.getCachedAccounts(),
                categories = walletRepository.getCachedCategories(),
                submissionState = submissionState,
                onSubmitTransaction = ::submitTransaction,
                onResetSubmission = {
                    submissionState = WalletRecordSubmissionState.Idle
                },
                onIgnoreSms = ::ignoreSms,
                onRefreshSms = ::refreshPendingSms,
                savedSenderHeaders = savedSenderHeaders,
                onRemoveSenderHeader = ::removeSenderHeader
            )
        }
    }

    private fun requestSmsPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED -> {
                scanSms()
            }

            else -> {
                smsPermissionLauncher.launch(
                    Manifest.permission.READ_SMS
                )
            }
        }
    }

    private fun scanSms() {
        scanStatus = "Scanning..."

        lifecycleScope.launch {
            try {
                val pending = withContext(Dispatchers.IO) {
                    walletRepository.getPendingSms(
                        SmsReader(this@MainActivity).readInbox()
                    )
                }

                smsMessages = pending
                scanStatus = "Scan complete — ${pending.size} pending messages"
            } catch (exception: Exception) {
                scanStatus = "Unable to scan SMS: ${
                    exception.message ?: "Unknown error"
                }"
            }
        }
    }

    private fun refreshPendingSms() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scanSms()
        }
    }

    private fun ignoreSms(message: SmsMessage) {
        walletRepository.markSmsIgnored(
            smsId = message.id,
            timestamp = message.timestamp
        )

        smsMessages = smsMessages.filterNot { it.id == message.id }
        scanStatus = "${smsMessages.size} pending messages"
    }

    private fun submitTransaction(
        message: SmsMessage,
        draft: TransactionDraft,
        rememberSenderHeader: Boolean
    ) {
        if (submissionState !is WalletRecordSubmissionState.Idle) {
            return
        }

        submissionState = WalletRecordSubmissionState.Submitting

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val creationResult = walletRepository.createRecord(draft)

                    // Only mark the SMS handled after Wallet confirms creation.
                    walletRepository.markSmsProcessed(
                        smsId = message.id,
                        timestamp = message.timestamp
                    )

                    if (rememberSenderHeader) {
                        message.sender
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let(walletRepository::addSenderHeader)
                    }

                    creationResult
                }

                savedSenderHeaders = walletRepository.getSavedSenderHeaders()
                submissionState =
                    WalletRecordSubmissionState.Success(result.message)
            } catch (exception: Exception) {
                submissionState =
                    WalletRecordSubmissionState.Unconfirmed(
                        "Wallet did not confirm successful creation. " +
                            "Check your Wallet records before trying again. " +
                            "Details: ${exception.message ?: "No additional details"}"
                    )
            }
        }
    }

    private fun removeSenderHeader(sender: String) {
        walletRepository.removeSenderHeader(sender)
        savedSenderHeaders = walletRepository.getSavedSenderHeaders()
    }

    private fun refreshWalletAccounts() {
        if (isRefreshingWalletAccounts) return
        if (!walletTokenStore.hasToken()) {
            walletConnectionState = WalletConnectionState.Disconnected
            return
        }

        isRefreshingWalletAccounts = true
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    walletRepository.sync()
                }
                walletConnectionState = WalletConnectionState.Connected(
                    accountCount = result.accountCount,
                    categoryCount = result.categoryCount
                )
            } catch (exception: Exception) {
                walletConnectionState = WalletConnectionState.Error(
                    exception.message ?: "Unable to refresh Wallet accounts"
                )
            } finally {
                isRefreshingWalletAccounts = false
            }
        }
    }

    private fun connectWallet(token: String) {
        walletConnectionState = WalletConnectionState.Connecting

        lifecycleScope.launch {
            try {
                walletTokenStore.saveToken(token)

                val result = withContext(Dispatchers.IO) {
                    walletRepository.sync()
                }

                walletConnectionState =
                    WalletConnectionState.Connected(
                        accountCount = result.accountCount,
                        categoryCount = result.categoryCount
                    )
            } catch (exception: Exception) {
                walletTokenStore.clearToken()

                walletConnectionState =
                    WalletConnectionState.Error(
                        exception.message ?: "Unable to connect to Wallet"
                    )
            }
        }
    }
}
