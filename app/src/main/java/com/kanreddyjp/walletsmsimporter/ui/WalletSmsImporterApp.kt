
package com.kanreddyjp.walletsmsimporter.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kanreddyjp.walletsmsimporter.sms.SmsMessage
import com.kanreddyjp.walletsmsimporter.transaction.TransactionDraft
import com.kanreddyjp.walletsmsimporter.transaction.TransactionType
import com.kanreddyjp.walletsmsimporter.ui.screens.HomeScreen
import com.kanreddyjp.walletsmsimporter.ui.screens.TransactionDraftScreen
import com.kanreddyjp.walletsmsimporter.ui.screens.TransactionReviewScreen
import com.kanreddyjp.walletsmsimporter.wallet.WalletAccount
import com.kanreddyjp.walletsmsimporter.wallet.WalletCategory
import com.kanreddyjp.walletsmsimporter.wallet.WalletConnectionState
import com.kanreddyjp.walletsmsimporter.wallet.WalletRecordSubmissionState

@Composable
fun WalletSmsImporterApp(
    smsMessages: List<SmsMessage>,
    scanStatus: String,
    onScanSms: () -> Unit,
    walletConnectionState: WalletConnectionState,
    onConnectWallet: (String) -> Unit,
    onRefreshWalletAccounts: () -> Unit,
    isRefreshingWalletAccounts: Boolean,
    accounts: List<WalletAccount>,
    categories: List<WalletCategory>,
    submissionState: WalletRecordSubmissionState,
    onSubmitTransaction: (SmsMessage, TransactionDraft, Boolean) -> Unit,
    onResetSubmission: () -> Unit,
    onIgnoreSms: (SmsMessage) -> Unit,
    onRefreshSms: () -> Unit,
    savedSenderHeaders: List<String>,
    onRemoveSenderHeader: (String) -> Unit
) {
    var selectedSms by remember {
        mutableStateOf<SmsMessage?>(null)
    }

    var currentDraft by remember {
        mutableStateOf<TransactionDraft?>(null)
    }

    var isReviewing by remember {
        mutableStateOf(false)
    }

    fun returnToSmsList() {
        selectedSms = null
        currentDraft = null
        isReviewing = false
        onResetSubmission()
        onRefreshSms()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val sms = selectedSms

            when {
                sms == null -> {
                    HomeScreen(
                        smsMessages = smsMessages,
                        scanStatus = scanStatus,
                        onScanSms = onScanSms,
                        walletConnectionState = walletConnectionState,
                        onConnectWallet = onConnectWallet,
                        onRefreshWalletAccounts = onRefreshWalletAccounts,
                        isRefreshingWalletAccounts = isRefreshingWalletAccounts,
                        onCreateTransaction = { message ->
                            onResetSubmission()
                            selectedSms = message
                            currentDraft = null
                            isReviewing = false
                        },
                        onIgnoreSms = onIgnoreSms
                    )
                }

                isReviewing && currentDraft != null -> {
                    TransactionReviewScreen(
                        sms = sms,
                        draft = currentDraft!!,
                        submissionState = submissionState,
                        savedSenderHeaders = savedSenderHeaders,
                        onRemoveSenderHeader = onRemoveSenderHeader,
                        onEdit = {
                            if (submissionState is WalletRecordSubmissionState.Idle) {
                                isReviewing = false
                            }
                        },
                        onSubmit = { rememberSenderHeader ->
                            if (submissionState is WalletRecordSubmissionState.Idle) {
                                currentDraft?.let { draft ->
                                    onSubmitTransaction(
                                        sms,
                                        draft,
                                        rememberSenderHeader
                                    )
                                }
                            }
                        },
                        onBackToSmsList = {
                            when (submissionState) {
                                WalletRecordSubmissionState.Idle,
                                is WalletRecordSubmissionState.Unconfirmed -> {
                                    returnToSmsList()
                                }

                                WalletRecordSubmissionState.Submitting,
                                is WalletRecordSubmissionState.Success -> Unit
                            }
                        },
                        onDone = {
                            if (
                                submissionState !is WalletRecordSubmissionState.Submitting
                            ) {
                                returnToSmsList()
                            }
                        }
                    )
                }

                else -> {
                    TransactionDraftScreen(
                        sms = sms,
                        accounts = accounts,
                        categories = categories,
                        initialDraft = currentDraft,
                        onBackToSmsList = {
                            returnToSmsList()
                        },
                        onReview = {
                            account,
                            amount,
                            category,
                            counterParty,
                            note,
                            type,
                            recordDate  ->
                            currentDraft = TransactionDraft(
                                accountId = account.id,
                                accountName = account.name,
                                amount = amount,
                                categoryId = category?.id,
                                categoryName = category?.name,
                                recordDate = recordDate,
                                counterParty = counterParty,
                                note = note,
                                type = type
                            )
                            isReviewing = true
                        }
                    )
                }
            }
        }
    }
}
