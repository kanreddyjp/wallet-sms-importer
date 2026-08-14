package com.kanreddyjp.walletsmsimporter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kanreddyjp.walletsmsimporter.ui.WalletSmsImporterApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WalletSmsImporterApp()
        }
    }
}