package com.kanreddyjp.walletsmsimporter.wallet

sealed interface WalletConnectionState {

    data object Disconnected : WalletConnectionState

    data object Connecting : WalletConnectionState

    data class Connected(
        val accountCount: Int,
        val categoryCount: Int
    ) : WalletConnectionState

    data class Error(
        val message: String
    ) : WalletConnectionState
}