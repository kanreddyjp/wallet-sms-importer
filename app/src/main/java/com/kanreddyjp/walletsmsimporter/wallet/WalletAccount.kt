package com.kanreddyjp.walletsmsimporter.wallet

data class WalletAccount(
    val id: String,
    val name: String,
    val accountType: String,
    val currencyCode: String,
    val archived: Boolean,
    val isBankSync: Boolean,
    val isInvestmentAccount: Boolean
)