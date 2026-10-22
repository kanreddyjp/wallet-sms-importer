
package com.kanreddyjp.walletsmsimporter.wallet

import com.kanreddyjp.walletsmsimporter.transaction.TransactionDraft
import com.kanreddyjp.walletsmsimporter.transaction.TransactionType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class WalletRecordCreationResult(
    val recordId: String?,
    val message: String
)

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

    fun createRecord(
        draft: TransactionDraft
    ): WalletRecordCreationResult {
        require(draft.amount.isFinite() && draft.amount > 0.0) {
            "Amount must be greater than zero"
        }

        val signedAmount = when (draft.type) {
            TransactionType.EXPENSE -> -draft.amount
            TransactionType.INCOME -> draft.amount
        }

        val formattedRecordDate = formatRecordDate(draft.recordDate)

        val record = JSONObject().apply {
            put("accountId", draft.accountId)
            put("amount", JSONObject().put("value", signedAmount))

            draft.categoryId?.let {
                put("categoryId", it)
            }

            if (draft.counterParty.isNotBlank()) {
                put("counterParty", draft.counterParty.trim())
            }

            if (draft.note.isNotBlank()) {
                put("note", draft.note.trim())
            }

            val formattedRecordDate = formatRecordDate(draft.recordDate)
            put("recordDate", formattedRecordDate)
        }

        val requestBody = JSONArray()
            .put(record)
            .toString()
            .toRequestBody(
                "application/json; charset=utf-8".toMediaType()
            )

        val request = Request.Builder()
            .url("$BASE_URL/records")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body
                    ?.string()
                    ?.take(2000)
                    ?.takeIf { it.isNotBlank() }

                val message = buildString {
                    append("Wallet API returned HTTP ${response.code}; recordDate=$formattedRecordDate")
                    if (errorBody != null) {
                        append(": ")
                        append(errorBody)
                    }
                }

                throw IllegalStateException(message)
            }
            val body = response.body?.string()
                ?: throw IllegalStateException(
                    "Wallet API returned an empty response"
                )

            val result = try {
                JSONObject(body)
            } catch (_: Exception) {
                throw IllegalStateException(
                    "Wallet returned an unrecognized creation response"
                )
            }

            val results = result.optJSONArray("results")
                ?: throw IllegalStateException(
                    "Wallet response did not contain record results"
                )

            if (results.length() != 1) {
                throw IllegalStateException(
                    "Wallet returned ${results.length()} results for one submitted record; creation status is unconfirmed"
                )
            }

            val item = results.optJSONObject(0)
                ?: throw IllegalStateException(
                    "Wallet returned an invalid record result"
                )

            if (!item.has("success") || item.isNull("success")) {
                throw IllegalStateException(
                    "Wallet response did not confirm whether the record was created"
                )
            }

            if (!item.getBoolean("success")) {
                val error = item.optString("error")
                    .takeIf { it.isNotBlank() && it != "null" }

                throw IllegalStateException(
                    error ?: "Wallet reported that record creation failed"
                )
            }

            val recordId = item.optString("id")
                .takeIf { it.isNotBlank() && it != "null" }
                ?: item.optJSONObject("record")
                    ?.optString("id")
                    ?.takeIf { it.isNotBlank() && it != "null" }

            return WalletRecordCreationResult(
                recordId = recordId,
                message = if (recordId != null) {
                    "Transaction created successfully (ID: $recordId)"
                } else {
                    "Wallet confirmed transaction creation"
                }
            )
        }
    }

    private fun formatRecordDate(timestamp: Long): String {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(timestamp))
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
                results.add(mapper(items.getJSONObject(index)))
            }

            val nextOffset = if (
                json.has("nextOffset") &&
                !json.isNull("nextOffset")
            ) {
                json.getInt("nextOffset")
            } else {
                null
            }

            if (nextOffset == null) break

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
        val urlBuilder = StringBuilder(BASE_URL + path)
        val queryParameters = mutableListOf<String>()

        if (limit != null) queryParameters.add("limit=$limit")
        if (offset != null) queryParameters.add("offset=$offset")

        if (queryParameters.isNotEmpty()) {
            urlBuilder.append("?")
            urlBuilder.append(queryParameters.joinToString("&"))
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
