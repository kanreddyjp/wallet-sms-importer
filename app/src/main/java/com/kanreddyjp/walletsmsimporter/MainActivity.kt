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
import com.kanreddyjp.walletsmsimporter.ui.WalletSmsImporterApp
import com.kanreddyjp.walletsmsimporter.wallet.WalletConnectionState
import com.kanreddyjp.walletsmsimporter.wallet.WalletLocalStore
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

        // Restore Wallet connection state from local cache.
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
                accounts = walletRepository.getCachedAccounts(),
                categories = walletRepository.getCachedCategories()
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

        val messages = SmsReader(this).readInbox()

        smsMessages = messages
        scanStatus = "Scan complete — ${messages.size} messages"
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
                        exception.message
                            ?: "Unable to connect to Wallet"
                    )
            }
        }
    }
}