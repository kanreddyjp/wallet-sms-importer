
package com.kanreddyjp.walletsmsimporter.wallet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class WalletLocalStore(context: Context) {

    private val preferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // Wallet accounts

    fun saveAccounts(accounts: List<WalletAccount>) {
        val array = JSONArray()

        accounts.forEach { account ->
            array.put(
                JSONObject().apply {
                    put("id", account.id)
                    put("name", account.name)
                    put("bankAccountNumber", account.bankAccountNumber)
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
                        bankAccountNumber = item.optString("bankAccountNumber").takeUnless { it == "null" },
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

    // Wallet categories

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

    // Wallet cache metadata

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

    // SMS processing state

    fun markSmsProcessed(smsId: Long, timestamp: Long) {
        require(smsId > 0L) { "SMS ID must be positive" }
        require(timestamp > 0L) { "SMS timestamp must be positive" }

        val processed = getLongSet(KEY_PROCESSED_SMS_IDS).toMutableSet()
        processed.add(smsId)

        val ignored = getLongSet(KEY_IGNORED_SMS_IDS).toMutableSet()
        ignored.remove(smsId)

        preferences.edit()
            .putString(KEY_PROCESSED_SMS_IDS, JSONArray(processed.toList()).toString())
            .putString(KEY_IGNORED_SMS_IDS, JSONArray(ignored.toList()).toString())
            .putLong(
                KEY_LAST_PROCESSED_SMS_TIMESTAMP,
                maxOf(getLastProcessedSmsTimestamp() ?: 0L, timestamp)
            )
            .apply()
    }

    fun markSmsIgnored(smsId: Long, timestamp: Long) {
        require(smsId > 0L) { "SMS ID must be positive" }
        require(timestamp > 0L) { "SMS timestamp must be positive" }

        val ignored = getLongSet(KEY_IGNORED_SMS_IDS).toMutableSet()
        ignored.add(smsId)

        val processed = getLongSet(KEY_PROCESSED_SMS_IDS).toMutableSet()
        processed.remove(smsId)

        preferences.edit()
            .putString(KEY_IGNORED_SMS_IDS, JSONArray(ignored.toList()).toString())
            .putString(KEY_PROCESSED_SMS_IDS, JSONArray(processed.toList()).toString())
            .putLong(
                KEY_LAST_PROCESSED_SMS_TIMESTAMP,
                maxOf(getLastProcessedSmsTimestamp() ?: 0L, timestamp)
            )
            .apply()
    }

    fun isSmsProcessed(smsId: Long): Boolean {
        return smsId in getLongSet(KEY_PROCESSED_SMS_IDS)
    }

    fun isSmsIgnored(smsId: Long): Boolean {
        return smsId in getLongSet(KEY_IGNORED_SMS_IDS)
    }

    fun isSmsHandled(smsId: Long): Boolean {
        return isSmsProcessed(smsId) || isSmsIgnored(smsId)
    }

    fun getProcessedSmsIds(): Set<Long> {
        return getLongSet(KEY_PROCESSED_SMS_IDS)
    }

    fun getIgnoredSmsIds(): Set<Long> {
        return getLongSet(KEY_IGNORED_SMS_IDS)
    }

    fun getLastProcessedSmsTimestamp(): Long? {
        if (!preferences.contains(KEY_LAST_PROCESSED_SMS_TIMESTAMP)) {
            return null
        }

        return preferences.getLong(KEY_LAST_PROCESSED_SMS_TIMESTAMP, 0L)
    }

    private fun getLongSet(key: String): Set<Long> {
        val json = preferences.getString(key, null)
            ?: return emptySet()

        val array = JSONArray(json)

        return buildSet {
            for (index in 0 until array.length()) {
                val value = array.optLong(index, -1L)
                if (value > 0L) {
                    add(value)
                }
            }
        }
    }

    // Saved SMS sender headers

    fun getSavedSenderHeaders(): List<String> {
        val json = preferences.getString(KEY_SENDER_HEADERS, null)
            ?: return emptyList()

        val array = JSONArray(json)

        return buildList {
            for (index in 0 until array.length()) {
                val header = array.optString(index, "")
                    .trim()

                if (header.isNotEmpty()) {
                    add(header)
                }
            }
        }
    }

    fun addSenderHeader(sender: String): Boolean {
        val header = sender.trim()
            .uppercase(Locale.ROOT)

        if (header.isBlank()) {
            return false
        }

        val headers = getSavedSenderHeaders().toMutableList()

        if (headers.any { it.equals(header, ignoreCase = true) }) {
            return false
        }

        headers.add(header)

        preferences.edit()
            .putString(KEY_SENDER_HEADERS, JSONArray(headers).toString())
            .apply()

        return true
    }

    fun removeSenderHeader(sender: String) {
        val headers = getSavedSenderHeaders()
            .filterNot { it.equals(sender.trim(), ignoreCase = true) }

        preferences.edit()
            .putString(KEY_SENDER_HEADERS, JSONArray(headers).toString())
            .apply()
    }

    // Clear Wallet cache only. Preserve SMS history and saved headers.

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

        private const val KEY_PROCESSED_SMS_IDS =
            "processed_sms_ids"

        private const val KEY_IGNORED_SMS_IDS =
            "ignored_sms_ids"

        private const val KEY_LAST_PROCESSED_SMS_TIMESTAMP =
            "last_processed_sms_timestamp"

        private const val KEY_SENDER_HEADERS =
            "saved_sender_headers"
    }
}
