package com.kanreddyjp.walletsmsimporter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kanreddyjp.walletsmsimporter.sms.SmsMessage
import com.kanreddyjp.walletsmsimporter.wallet.WalletConnectionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    smsMessages: List<SmsMessage>,
    scanStatus: String,
    onScanSms: () -> Unit,
    walletConnectionState: WalletConnectionState,
    onConnectWallet: (String) -> Unit
) {
    var showConnectDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Wallet SMS Importer",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            WalletConnectionSection(
                state = walletConnectionState,
                onConnect = {
                    showConnectDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = scanStatus,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onScanSms
            ) {
                Text("Scan SMS")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = smsMessages,
                key = { message ->
                    "${message.timestamp}-${message.sender}-${message.body}"
                }
            ) { message ->
                SmsCard(message)
            }
        }
    }

    if (showConnectDialog) {
        WalletTokenDialog(
            state = walletConnectionState,
            onDismiss = {
                if (walletConnectionState !is WalletConnectionState.Connecting) {
                    showConnectDialog = false
                }
            },
            onConnect = onConnectWallet
        )
    }
}

@Composable
private fun WalletConnectionSection(
    state: WalletConnectionState,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Wallet",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (state) {
                WalletConnectionState.Disconnected -> {
                    Text("Not connected")

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = onConnect) {
                        Text("Connect Wallet")
                    }
                }

                WalletConnectionState.Connecting -> {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Connecting to Wallet...")
                }

                is WalletConnectionState.Connected -> {
                    Text(
                        text = "✓ Connected",
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text("Accounts: ${state.accountCount}")
                    Text("Categories: ${state.categoryCount}")
                }

                is WalletConnectionState.Error -> {
                    Text(
                        text = "Connection failed",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(state.message)

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = onConnect) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletTokenDialog(
    state: WalletConnectionState,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit
) {
    var token by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Connect Wallet")
        },
        text = {
            Column {
                Text(
                    text = "Enter your Wallet API token. " +
                        "It will be stored securely on this device."
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = {
                        Text("API Token")
                    },
                    singleLine = true,
                    visualTransformation =
                        PasswordVisualTransformation(),
                    enabled =
                        state !is WalletConnectionState.Connecting
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConnect(token.trim())
                },
                enabled =
                    token.isNotBlank() &&
                        state !is WalletConnectionState.Connecting
            ) {
                Text(
                    if (state is WalletConnectionState.Connecting) {
                        "Connecting..."
                    } else {
                        "Connect"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled =
                    state !is WalletConnectionState.Connecting
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SmsCard(message: SmsMessage) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = message.sender ?: "Unknown sender",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatDate(message.timestamp),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message.body ?: "",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(Date(timestamp))
}