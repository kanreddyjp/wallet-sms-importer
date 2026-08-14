package com.kanreddyjp.walletsmsimporter.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kanreddyjp.walletsmsimporter.ui.screens.HomeScreen

@Composable
fun WalletSmsImporterApp(
    smsCount: Int,
    scanStatus: String,
    onScanSms: () -> Unit
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            HomeScreen(
                smsCount = smsCount,
                scanStatus = scanStatus,
                onScanSms = onScanSms
            )
        }
    }
}