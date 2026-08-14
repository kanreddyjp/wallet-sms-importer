package com.kanreddyjp.walletsmsimporter.wallet

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WalletApiClient(
    private val token: String
) {

    private val client = OkHttpClient()

    fun getAccounts(): List<WalletAccount> {
        return getAllPages(
            path = "/accounts",
            arrayName = "accounts"
        ) { account ->
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
        }
    }

    fun getCategories(): List<WalletCategory> {
        return getAllPages(
            path = "/categories",
            arrayName = "categories"
        ) { category ->
            val group = category.optJSONObject("group")

            WalletCategory(
                id = category.getString("id"),
                name = category.getString("name"),
                groupId = group?.optString("id"),
                groupName = group?.optString("name")
            )
        }
    }

    private fun <T> getAllPages(
        path: String,
        arrayName: String,
        mapper: (JSONObject) -> T
    ): List<T> {

        val results = mutableListOf<T>()

        var offset = 0

        while (true) {
            val json = executeGet(
                path = path,
                limit = PAGE_SIZE,
                offset = offset
            )

            val items = json.getJSONArray(arrayName)

            for (index in 0 until items.length()) {
                results.add(
                    mapper(items.getJSONObject(index))
                )
            }

            val nextOffset = if (
                json.has("nextOffset") &&
                !json.isNull("nextOffset")
            ) {
                json.getInt("nextOffset")
            } else {
                null
            }

            if (nextOffset == null) {
                break
            }

            if (nextOffset <= offset) {
                throw IllegalStateException(
                    "Wallet API returned an invalid nextOffset: $nextOffset"
                )
            }

            offset = nextOffset
        }

        return results
    }

    private fun executeGet(
        path: String,
        limit: Int? = null,
        offset: Int? = null
    ): JSONObject {

        val urlBuilder = StringBuilder(
            BASE_URL + path
        )

        val queryParameters = mutableListOf<String>()

        if (limit != null) {
            queryParameters.add("limit=$limit")
        }

        if (offset != null) {
            queryParameters.add("offset=$offset")
        }

        if (queryParameters.isNotEmpty()) {
            urlBuilder.append("?")
            urlBuilder.append(
                queryParameters.joinToString("&")
            )
        }

        val request = Request.Builder()
            .url(urlBuilder.toString())
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

        private const val PAGE_SIZE = 100
    }
}