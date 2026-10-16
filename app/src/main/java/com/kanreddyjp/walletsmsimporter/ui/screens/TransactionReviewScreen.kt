
package com.kanreddyjp.walletsmsimporter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanreddyjp.walletsmsimporter.sms.SmsMessage
import com.kanreddyjp.walletsmsimporter.transaction.TransactionDraft
import com.kanreddyjp.walletsmsimporter.transaction.TransactionType
import com.kanreddyjp.walletsmsimporter.wallet.WalletRecordSubmissionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionReviewScreen(
    sms: SmsMessage,
    draft: TransactionDraft,
    submissionState: WalletRecordSubmissionState,
    savedSenderHeaders: List<String>,
    onRemoveSenderHeader: (String) -> Unit,
    onEdit: () -> Unit,
    onSubmit: (Boolean) -> Unit,
    onBackToSmsList: () -> Unit,
    onDone: () -> Unit
) {
    val submissionAttempted =
        submissionState !is WalletRecordSubmissionState.Idle

    val sender = sms.sender?.trim().orEmpty()
    val senderAlreadySaved = savedSenderHeaders.any {
        it.equals(sender, ignoreCase = true)
    }

    var rememberSenderHeader by remember(sms.id, savedSenderHeaders) {
        mutableStateOf(sender.isNotBlank() && !senderAlreadySaved)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Review Transaction",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Verify the details before submitting to Wallet.",
            style = MaterialTheme.typography.bodyMedium
        )

        ReviewField(
            "Transaction type",
            when (draft.type) {
                TransactionType.EXPENSE -> "Expense"
                TransactionType.INCOME -> "Income"
            }
        )

        ReviewField("Account", draft.accountName)

        ReviewField(
            "Amount",
            String.format(Locale.getDefault(), "%.2f", draft.amount)
        )

        ReviewField("Category", draft.categoryName ?: "Not selected")
        ReviewField("Counterparty", draft.counterParty.ifBlank { "Not entered" })
        ReviewField("Note", draft.note.ifBlank { "Not entered" })
        ReviewField("Transaction date", formatDate(draft.recordDate))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "SMS sender header",
                style = MaterialTheme.typography.titleMedium
            )

            Text(sender.ifBlank { "Unknown sender" })

            if (sender.isNotBlank() && !senderAlreadySaved) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberSenderHeader,
                        onCheckedChange = { rememberSenderHeader = it },
                        enabled = !submissionAttempted
                    )

                    Text("Remember this sender as a bank SMS header")
                }
            } else if (senderAlreadySaved) {
                Text(
                    text = "This sender is already saved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Saved bank SMS headers",
                style = MaterialTheme.typography.titleMedium
            )

            if (savedSenderHeaders.isEmpty()) {
                Text(
                    text = "No headers saved yet.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                savedSenderHeaders.forEach { header ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = header,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        TextButton(
                            onClick = { onRemoveSenderHeader(header) },
                            enabled = !submissionAttempted
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }

        when (val state = submissionState) {
            WalletRecordSubmissionState.Idle -> Unit

            WalletRecordSubmissionState.Submitting -> {
                CircularProgressIndicator()
                Text("Submitting transaction to Wallet...")
            }

            is WalletRecordSubmissionState.Success -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            is WalletRecordSubmissionState.Unconfirmed -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        if (
            submissionState is WalletRecordSubmissionState.Idle ||
            submissionState is WalletRecordSubmissionState.Unconfirmed
        ) {
            OutlinedButton(
                onClick = onBackToSmsList,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to SMS list")
            }
        }
        if (!submissionAttempted) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Transaction")
            }

            Button(
                onClick = { onSubmit(rememberSenderHeader) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm and Submit to Wallet")
            }
        } else if (submissionState !is WalletRecordSubmissionState.Submitting) {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun ReviewField(
    label: String,
    value: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(Date(timestamp))
}
