
package com.kanreddyjp.walletsmsimporter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanreddyjp.walletsmsimporter.transaction.TransactionDraft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionReviewScreen(
    draft: TransactionDraft,
    onEdit: () -> Unit
) {
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
            text = "Please verify the details before proceeding.",
            style = MaterialTheme.typography.bodyMedium
        )

        ReviewField("Account", draft.accountName)
        ReviewField("Amount", draft.amount.toString())
        ReviewField("Category", draft.categoryName ?: "Not selected")
        ReviewField("Counterparty", draft.counterParty.ifBlank { "Not entered" })
        ReviewField("Note", draft.note.ifBlank { "Not entered" })
        ReviewField("Transaction date", formatDate(draft.recordDate))

        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Edit Transaction")
        }

        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Submit to Wallet (coming later)")
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
