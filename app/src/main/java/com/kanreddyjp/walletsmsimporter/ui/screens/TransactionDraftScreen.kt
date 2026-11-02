
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanreddyjp.walletsmsimporter.sms.SmsMessage
import com.kanreddyjp.walletsmsimporter.sms.SmsTransactionParser
import com.kanreddyjp.walletsmsimporter.transaction.TransactionDraft
import com.kanreddyjp.walletsmsimporter.transaction.TransactionType
import com.kanreddyjp.walletsmsimporter.wallet.WalletAccount
import com.kanreddyjp.walletsmsimporter.wallet.WalletCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Card

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDraftScreen(
    sms: SmsMessage,
    accounts: List<WalletAccount>,
    categories: List<WalletCategory>,
    initialDraft: TransactionDraft? = null,
    onBackToSmsList: () -> Unit,
    onReview: (
        account: WalletAccount,
        amount: Double,
        category: WalletCategory?,
        counterParty: String,
        note: String,
        type: TransactionType,
        recordDate: Long
    ) -> Unit
) {
    val parsedSms = remember(sms.id, sms.body, sms.timestamp) {
        SmsTransactionParser.parse(sms)
    }

    var selectedAccount by remember(sms, initialDraft, accounts) {
        mutableStateOf(
            accounts.firstOrNull { it.id == initialDraft?.accountId }
                ?: parsedSms.accountSuffix?.let { suffix ->
                    val matches = accounts.filter { account ->
                        account.bankAccountNumber
                            .orEmpty()
                            .filter(Char::isDigit)
                            .takeLast(suffix.length) == suffix
                    }
                    matches.singleOrNull()
                }
                ?: accounts.firstOrNull()
        )
    }

    var selectedCategory by remember(sms, initialDraft, categories) {
        mutableStateOf(
            categories.firstOrNull { it.id == initialDraft?.categoryId }
        )
    }

    var amount by remember(sms, initialDraft) {
        mutableStateOf(
            initialDraft?.amount?.toString()
                ?: parsedSms.amount?.toString()
                ?: ""
        )
    }

    var counterParty by remember(sms, initialDraft) {
        mutableStateOf(
            initialDraft?.counterParty
                ?: parsedSms.counterParty
        )
    }

    var note by remember(sms, initialDraft) {
        mutableStateOf(
            initialDraft?.note
                ?: parsedSms.note
        )
    }

    var transactionType by remember(sms, initialDraft) {
        mutableStateOf(
            initialDraft?.type ?: parsedSms.type
        )
    }

    var accountMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var accountSearchQuery by remember(sms.id) { mutableStateOf("") }
    var categorySearchQuery by remember(sms.id) { mutableStateOf("") }

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
            text = "SMS received: ${formatDate(sms.timestamp)}",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "Detected transaction date: ${formatDate(parsedSms.recordDate)}",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "Detected fields are prefilled where possible. Please verify them before reviewing.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Wallet Transaction",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Transaction type",
            style = MaterialTheme.typography.labelLarge
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = transactionType == TransactionType.EXPENSE,
                    onClick = { transactionType = TransactionType.EXPENSE }
                )
                Text(
                    text = "Expense",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = transactionType == TransactionType.INCOME,
                    onClick = { transactionType = TransactionType.INCOME }
                )
                Text(
                    text = "Income",
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        Column {
            OutlinedTextField(
                value = if (accountMenuExpanded) {
                    accountSearchQuery
                } else {
                    selectedAccount?.name ?: ""
                },
                onValueChange = {
                    accountSearchQuery = it
                    accountMenuExpanded = true
                },
                label = { Text("Account") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (accountMenuExpanded) {
                val filteredAccounts = accounts.filter {
                    it.name.contains(accountSearchQuery, ignoreCase = true)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        if (filteredAccounts.isEmpty()) {
                            Text(
                                text = "No matching accounts",
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            filteredAccounts.forEach { account ->
                                Text(
                                    text = account.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAccount = account
                                            accountSearchQuery = ""
                                            accountMenuExpanded = false
                                        }
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Column {
            OutlinedTextField(
                value = if (categoryMenuExpanded) {
                    categorySearchQuery
                } else {
                    selectedCategory?.name ?: ""
                },
                onValueChange = {
                    categorySearchQuery = it
                    categoryMenuExpanded = true
                },
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (categoryMenuExpanded) {
                val filteredCategories = categories.filter { category ->
                    category.name.contains(categorySearchQuery, ignoreCase = true) ||
                        category.groupName.orEmpty()
                            .contains(categorySearchQuery, ignoreCase = true)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        if (filteredCategories.isEmpty()) {
                            Text(
                                text = "No matching categories",
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            filteredCategories.forEach { category ->
                                Text(
                                    text = if (category.groupName != null) {
                                        "${category.name} — ${category.groupName}"
                                    } else {
                                        category.name
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCategory = category
                                            categorySearchQuery = ""
                                            categoryMenuExpanded = false
                                        }
                                        .padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = counterParty,
            onValueChange = { counterParty = it },
            label = { Text("Counterparty") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Note") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = onBackToSmsList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to SMS list")
        }

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
                    note,
                    transactionType,
                    parsedSms.recordDate
                )
            },
            enabled = selectedAccount != null &&
                amount.toDoubleOrNull()?.let {
                    it.isFinite() && it > 0.0
                } == true,
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
