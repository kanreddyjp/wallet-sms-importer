package com.kanreddyjp.walletsmsimporter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanreddyjp.walletsmsimporter.sms.SmsMessage
import com.kanreddyjp.walletsmsimporter.wallet.WalletAccount
import com.kanreddyjp.walletsmsimporter.wallet.WalletCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDraftScreen(
    sms: SmsMessage,
    accounts: List<WalletAccount>,
    categories: List<WalletCategory>,
    onReview: (
        account: WalletAccount,
        amount: Double,
        category: WalletCategory?,
        counterParty: String,
        note: String
    ) -> Unit
) {
    var selectedAccount by remember {
        mutableStateOf(accounts.firstOrNull())
    }

    var selectedCategory by remember {
        mutableStateOf<WalletCategory?>(null)
    }

    var amount by remember {
        mutableStateOf("")
    }

    var counterParty by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    var accountMenuExpanded by remember {
        mutableStateOf(false)
    }

    var categoryMenuExpanded by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create Transaction",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Original SMS",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = sms.body ?: "",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = formatDate(sms.timestamp),
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "Wallet Transaction",
            style = MaterialTheme.typography.titleMedium
        )

        ExposedDropdownMenuBox(
            expanded = accountMenuExpanded,
            onExpandedChange = {
                accountMenuExpanded = !accountMenuExpanded
            }
        ) {
            OutlinedTextField(
                value = selectedAccount?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = {
                    Text("Account")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = accountMenuExpanded
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = accountMenuExpanded,
                onDismissRequest = {
                    accountMenuExpanded = false
                }
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Text(account.name)
                        },
                        onClick = {
                            selectedAccount = account
                            accountMenuExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = amount,
            onValueChange = {
                amount = it
            },
            label = {
                Text("Amount")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(
            expanded = categoryMenuExpanded,
            onExpandedChange = {
                categoryMenuExpanded = !categoryMenuExpanded
            }
        ) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = {
                    Text("Category")
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = categoryMenuExpanded
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = categoryMenuExpanded,
                onDismissRequest = {
                    categoryMenuExpanded = false
                }
            ) {
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (category.groupName != null) {
                                    "${category.name} — ${category.groupName}"
                                } else {
                                    category.name
                                }
                            )
                        },
                        onClick = {
                            selectedCategory = category
                            categoryMenuExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = counterParty,
            onValueChange = {
                counterParty = it
            },
            label = {
                Text("Counterparty")
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = note,
            onValueChange = {
                note = it
            },
            label = {
                Text("Note")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val account = selectedAccount ?: return@Button
                val parsedAmount = amount.toDoubleOrNull()
                    ?: return@Button

                onReview(
                    account,
                    parsedAmount,
                    selectedCategory,
                    counterParty,
                    note
                )
            },
            enabled =
                selectedAccount != null &&
                    amount.toDoubleOrNull() != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Review Transaction")
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    ).format(Date(timestamp))
}