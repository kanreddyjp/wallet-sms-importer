package com.kanreddyjp.walletsmsimporter.wallet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class WalletLocalStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun saveAccounts(accounts: List<WalletAccount>) {
        val array = JSONArray()

        accounts.forEach { account ->
            array.put(
                JSONObject().apply {
                    put("id", account.id)
                    put("name", account.name)
                    put("accountType", account.accountType)
                    put("currencyCode", account.currencyCode)
                    put("archived", account.archived)
                    put("isBankSync", account.isBankSync)
                    put("isInvestmentAccount", account.isInvestmentAccount)
                }
            )
        }

        preferences.edit()
            .putString(KEY_ACCOUNTS, array.toString())
            .apply()
    }

    fun getAccounts(): List<WalletAccount> {
        val json = preferences.getString(KEY_ACCOUNTS, null)
            ?: return emptyList()

        val array = JSONArray(json)

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                add(
                    WalletAccount(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        accountType = item.getString("accountType"),
                        currencyCode = item.getString("currencyCode"),
                        archived = item.getBoolean("archived"),
                        isBankSync = item.getBoolean("isBankSync"),
                        isInvestmentAccount =
                            item.getBoolean("isInvestmentAccount")
                    )
                )
            }
        }
    }

    fun saveCategories(categories: List<WalletCategory>) {
        val array = JSONArray()

        categories.forEach { category ->
            array.put(
                JSONObject().apply {
                    put("id", category.id)
                    put("name", category.name)

                    if (category.groupId != null) {
                        put("groupId", category.groupId)
                    }

                    if (category.groupName != null) {
                        put("groupName", category.groupName)
                    }
                }
            )
        }

        preferences.edit()
            .putString(KEY_CATEGORIES, array.toString())
            .apply()
    }

    fun getCategories(): List<WalletCategory> {
        val json = preferences.getString(KEY_CATEGORIES, null)
            ?: return emptyList()

        val array = JSONArray(json)

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)

                add(
                    WalletCategory(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        groupId = item.optString(
                            "groupId",
                            null
                        ),
                        groupName = item.optString(
                            "groupName",
                            null
                        )
                    )
                )
            }
        }
    }

    fun saveLastSync(timestamp: Long) {
        preferences.edit()
            .putLong(KEY_LAST_SYNC, timestamp)
            .apply()
    }

    fun getLastSync(): Long? {
        if (!preferences.contains(KEY_LAST_SYNC)) {
            return null
        }

        return preferences.getLong(KEY_LAST_SYNC, 0L)
    }

    fun hasWalletData(): Boolean {
        return preferences.contains(KEY_ACCOUNTS) &&
            preferences.contains(KEY_CATEGORIES)
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_ACCOUNTS)
            .remove(KEY_CATEGORIES)
            .remove(KEY_LAST_SYNC)
            .apply()
    }

    companion object {
        private const val PREFS_NAME =
            "wallet_sms_importer_wallet_data"

        private const val KEY_ACCOUNTS =
            "accounts"

        private const val KEY_CATEGORIES =
            "categories"

        private const val KEY_LAST_SYNC =
            "last_sync"
    }
}