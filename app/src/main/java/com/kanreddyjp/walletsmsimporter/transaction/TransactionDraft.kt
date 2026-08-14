package com.kanreddyjp.walletsmsimporter.transaction

data class TransactionDraft(
    val accountId: String,
    val accountName: String,
    val amount: Double,
    val categoryId: String?,
    val categoryName: String?,
    val recordDate: Long,
    val counterParty: String,
    val note: String
)