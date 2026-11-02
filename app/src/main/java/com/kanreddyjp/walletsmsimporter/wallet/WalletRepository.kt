
package com.kanreddyjp.walletsmsimporter.wallet

import com.kanreddyjp.walletsmsimporter.sms.SmsMessage
import com.kanreddyjp.walletsmsimporter.transaction.TransactionDraft

class WalletRepository(
    private val tokenStore: WalletTokenStore,
    private val localStore: WalletLocalStore
) {

    // Cached Wallet data

    fun getCachedAccounts(): List<WalletAccount> {
    return localStore.getAccounts()
        .filterNot { it.archived }
    }

    fun getCachedCategories(): List<WalletCategory> {
        return localStore.getCategories()
    }

    fun getLastSync(): Long? {
        return localStore.getLastSync()
    }

    fun hasCachedData(): Boolean {
        return localStore.hasWalletData()
    }

    // Wallet synchronization

    fun sync(): SyncResult {
        val token = tokenStore.getToken()
            ?: throw IllegalStateException("Wallet is not connected")

        val apiClient = WalletApiClient(token)

        val accounts = apiClient.getAccounts()
        val categories = apiClient.getCategories()

        localStore.saveAccounts(accounts)
        localStore.saveCategories(categories)

        val syncTime = System.currentTimeMillis()
        localStore.saveLastSync(syncTime)

        return SyncResult(
            accountCount = accounts.count { !it.archived },
            categoryCount = categories.size,
            timestamp = syncTime
        )
    }

    // Wallet transaction creation

    fun createRecord(
        draft: TransactionDraft
    ): WalletRecordCreationResult {
        val token = tokenStore.getToken()
            ?: throw IllegalStateException("Wallet is not connected")

        return WalletApiClient(token).createRecord(draft)
    }

    // SMS processing

    /**
     * Return only bank SMS messages that:
     * 1. Match at least one configured bank identifier.
     * 2. Have not already been imported or ignored.
     *
     * Matching is case-insensitive substring matching.
     * Example: BT-ICICIT-S matches the identifier ICICI.
     */
    fun getPendingSms(messages: List<SmsMessage>): List<SmsMessage> {
        val handledIds = localStore.getProcessedSmsIds() +
            localStore.getIgnoredSmsIds()

        val bankIdentifiers = localStore.getBankSmsIdentifiers()
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }

        return messages
            .filter { message ->
                val sender = message.sender
                    ?.trim()
                    ?.uppercase()
                    ?: return@filter false

                bankIdentifiers.any { identifier ->
                    sender.contains(identifier)
                }
            }
            .filter { it.id !in handledIds }
            .sortedWith(
                compareBy<SmsMessage> { it.timestamp }
                    .thenBy { it.id }
            )
    }

    fun markSmsProcessed(smsId: Long, timestamp: Long) {
        localStore.markSmsProcessed(smsId, timestamp)
    }

    fun markSmsIgnored(smsId: Long, timestamp: Long) {
        localStore.markSmsIgnored(smsId, timestamp)
    }

    fun isSmsProcessed(smsId: Long): Boolean {
        return localStore.isSmsProcessed(smsId)
    }

    fun isSmsIgnored(smsId: Long): Boolean {
        return localStore.isSmsIgnored(smsId)
    }

    fun getLastProcessedSmsTimestamp(): Long? {
        return localStore.getLastProcessedSmsTimestamp()
    }

    // Bank SMS identifiers

    fun getBankSmsIdentifiers(): List<String> {
        return localStore.getBankSmsIdentifiers()
    }

    fun saveBankSmsIdentifiers(identifiers: List<String>) {
        localStore.saveBankSmsIdentifiers(identifiers)
    }

    // Sender-header master list

    fun getSavedSenderHeaders(): List<String> {
        return localStore.getSavedSenderHeaders()
    }

    fun addSenderHeader(sender: String): Boolean {
        return localStore.addSenderHeader(sender)
    }

    fun removeSenderHeader(sender: String) {
        localStore.removeSenderHeader(sender)
    }
}

data class SyncResult(
    val accountCount: Int,
    val categoryCount: Int,
    val timestamp: Long
)
