
package com.kanreddyjp.walletsmsimporter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanreddyjp.walletsmsimporter.transaction.TransactionDraft
import com.kanreddyjp.walletsmsimporter.transaction.TransactionType
import com.kanreddyjp.walletsmsimporter.wallet.WalletRecordSubmissionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionReviewScreen(
    draft: TransactionDraft,
    submissionState: WalletRecordSubmissionState,
    onEdit: () -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit
) {
    val submissionAttempted =
        submissionState !is WalletRecordSubmissionState.Idle

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

        if (!submissionAttempted) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Transaction")
            }

            Button(
                onClick = onSubmit,
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
