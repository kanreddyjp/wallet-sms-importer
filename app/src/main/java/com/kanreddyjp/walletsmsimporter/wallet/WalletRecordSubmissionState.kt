
package com.kanreddyjp.walletsmsimporter.wallet

sealed interface WalletRecordSubmissionState {
    data object Idle : WalletRecordSubmissionState

    data object Submitting : WalletRecordSubmissionState

    data class Success(
        val message: String
    ) : WalletRecordSubmissionState

    data class Unconfirmed(
        val message: String
    ) : WalletRecordSubmissionState
}
