
package com.kanreddyjp.walletsmsimporter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

private const val DEFAULT_BANK_IDENTIFIERS =
    "SBIBNK, SBIINB, SBYONO, ATMSBI, HDFCBK, HDFCCN, " +
    "ICICIB, ICICIC, ICICIP, KOTAKB, KOTAKM, KMBLBK, " +
    "UNIONB, UBIACQ, CNRBK, CANARA, CANMNY, AUBANK, " +
    "AUBMSG, AUDOST, INDUSB, INDUSA, INDUSO"

@Composable
fun BankHeaderManagementScreen(
    identifiers: String = DEFAULT_BANK_IDENTIFIERS,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var headerText by remember(identifiers) {
        mutableStateOf(identifiers)
    }

    val identifierCount = headerText
        .split(',', '\n', '\r')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.uppercase(Locale.ROOT) }
        .size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Bank SMS Identifiers",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Manage the bank identifiers used to recognise " +
                "SMS messages from your banks.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = headerText,
            onValueChange = { headerText = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = {
                Text("Bank identifiers")
            },
            placeholder = {
                Text("SBIBNK, SBIINB, HDFCBK...")
            },
            supportingText = {
                Text(
                    "$identifierCount identifiers"
                )
            },
            minLines = 8,
            maxLines = 20
        )

        Text(
            text = "Separate identifiers using commas or new lines. " +
                "Matching will be case-insensitive.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    val cleanedIdentifiers = headerText
                        .split(',', '\n', '\r')
                        .map { it.trim().uppercase(Locale.ROOT) }
                        .filter { it.isNotEmpty() }
                        .distinct()

                    onSave(cleanedIdentifiers.joinToString(", "))
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}
