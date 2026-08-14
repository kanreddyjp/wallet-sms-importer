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
import com.kanreddyjp.walletsmsimporter.sms.SmsReader
import com.kanreddyjp.walletsmsimporter.ui.WalletSmsImporterApp

class MainActivity : ComponentActivity() {

    private var smsCount by mutableStateOf(0)
    private var scanStatus by mutableStateOf("Never scanned")

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

        setContent {
            WalletSmsImporterApp(
                smsCount = smsCount,
                scanStatus = scanStatus,
                onScanSms = ::requestSmsPermission
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

        smsCount = messages.size
        scanStatus = "Scan complete"
    }
}