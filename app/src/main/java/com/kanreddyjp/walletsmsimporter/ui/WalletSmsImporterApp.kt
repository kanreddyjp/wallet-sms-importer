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
import com.kanreddyjp.walletsmsimporter.ui.screens.HomeScreen
import com.kanreddyjp.walletsmsimporter.ui.screens.TransactionDraftScreen
import com.kanreddyjp.walletsmsimporter.wallet.WalletAccount
import com.kanreddyjp.walletsmsimporter.wallet.WalletCategory
import com.kanreddyjp.walletsmsimporter.wallet.WalletConnectionState

@Composable
fun WalletSmsImporterApp(
    smsMessages: List<SmsMessage>,
    scanStatus: String,
    onScanSms: () -> Unit,
    walletConnectionState: WalletConnectionState,
    onConnectWallet: (String) -> Unit,
    accounts: List<WalletAccount>,
    categories: List<WalletCategory>
) {
    var selectedSms by remember {
        mutableStateOf<SmsMessage?>(null)
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            selectedSms?.let { sms ->
                TransactionDraftScreen(
                    sms = sms,
                    accounts = accounts,
                    categories = categories,
                    onReview = { account, amount, category, counterParty, note ->
                        // Review screen will be implemented next.
                    }
                )
            } ?: HomeScreen(
                smsMessages = smsMessages,
                scanStatus = scanStatus,
                onScanSms = onScanSms,
                walletConnectionState = walletConnectionState,
                onConnectWallet = onConnectWallet,
                onCreateTransaction = { sms ->
                    selectedSms = sms
                }
            )
        }
    }
}