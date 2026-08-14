package com.kanreddyjp.walletsmsimporter.wallet

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WalletApiClient(
    private val token: String
) {

    private val client = OkHttpClient()

    fun getAccounts(): List<WalletAccount> {
        val json = executeGet("/accounts")
        val accounts = json.getJSONArray("accounts")

        return buildList {
            for (index in 0 until accounts.length()) {
                val account = accounts.getJSONObject(index)

                add(
                    WalletAccount(
                        id = account.getString("id"),
                        name = account.getString("name"),
                        accountType = account.getString("accountType"),
                        currencyCode = account.getString("currencyCode"),
                        archived = account.getBoolean("archived"),
                        isBankSync = account.getBoolean("isBankSync"),
                        isInvestmentAccount =
                            account.getBoolean("isInvestmentAccount")
                    )
                )
            }
        }
    }

    fun getCategories(): List<WalletCategory> {
        val json = executeGet("/categories")
        val categories = json.getJSONArray("categories")

        return buildList {
            for (index in 0 until categories.length()) {
                val category = categories.getJSONObject(index)
                val group = category.optJSONObject("group")

                add(
                    WalletCategory(
                        id = category.getString("id"),
                        name = category.getString("name"),
                        groupId = group?.optString("id"),
                        groupName = group?.optString("name")
                    )
                )
            }
        }
    }

    private fun executeGet(path: String): JSONObject {
        val request = Request.Builder()
            .url(BASE_URL + path)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Wallet API returned HTTP ${response.code}"
                )
            }

            val body = response.body?.string()
                ?: throw IllegalStateException(
                    "Wallet API returned an empty response"
                )

            return JSONObject(body)
        }
    }

    companion object {
        private const val BASE_URL =
            "https://rest.budgetbakers.com/wallet/v1/api"
    }
}