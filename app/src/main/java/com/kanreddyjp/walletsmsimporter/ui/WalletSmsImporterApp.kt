
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
    accounts: List<WalletAccount>,
    categories: List<WalletCategory>,
    submissionState: WalletRecordSubmissionState,
    onSubmitTransaction: (TransactionDraft) -> Unit,
    onResetSubmission: () -> Unit
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
                        onCreateTransaction = { message ->
                            onResetSubmission()
                            selectedSms = message
                            currentDraft = null
                            isReviewing = false
                        }
                    )
                }

                isReviewing && currentDraft != null -> {
                    TransactionReviewScreen(
                        draft = currentDraft!!,
                        submissionState = submissionState,
                        onEdit = {
                            isReviewing = false
                        },
                        onSubmit = {
                            currentDraft?.let(onSubmitTransaction)
                        },
                        onDone = {
                            selectedSms = null
                            currentDraft = null
                            isReviewing = false
                            onResetSubmission()
                        }
                    )
                }

                else -> {
                    TransactionDraftScreen(
                        sms = sms,
                        accounts = accounts,
                        categories = categories,
                        initialDraft = currentDraft,
                        onReview = {
                            account,
                            amount,
                            category,
                            counterParty,
                            note,
                            type ->
                            currentDraft = TransactionDraft(
                                accountId = account.id,
                                accountName = account.name,
                                amount = amount,
                                categoryId = category?.id,
                                categoryName = category?.name,
                                recordDate = sms.timestamp,
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
