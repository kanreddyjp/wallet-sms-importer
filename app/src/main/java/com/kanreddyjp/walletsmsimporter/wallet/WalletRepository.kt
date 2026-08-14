package com.kanreddyjp.walletsmsimporter.wallet

class WalletRepository(
    private val tokenStore: WalletTokenStore,
    private val localStore: WalletLocalStore
) {

    fun getCachedAccounts(): List<WalletAccount> {
        return localStore.getAccounts()
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

    fun sync(): SyncResult {
        val token = tokenStore.getToken()
            ?: throw IllegalStateException(
                "Wallet is not connected"
            )

        val apiClient = WalletApiClient(token)

        val accounts = apiClient.getAccounts()
        val categories = apiClient.getCategories()

        localStore.saveAccounts(accounts)
        localStore.saveCategories(categories)

        val syncTime = System.currentTimeMillis()

        localStore.saveLastSync(syncTime)

        return SyncResult(
            accountCount = accounts.size,
            categoryCount = categories.size,
            timestamp = syncTime
        )
    }
}

data class SyncResult(
    val accountCount: Int,
    val categoryCount: Int,
    val timestamp: Long
)