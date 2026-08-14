package com.kanreddyjp.walletsmsimporter.sms

import android.content.Context
import android.net.Uri

data class SmsMessage(
    val sender: String?,
    val body: String?,
    val timestamp: Long
)

class SmsReader(private val context: Context) {

    fun readInbox(): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()

        val cursor = context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf(
                "address",
                "body",
                "date"
            ),
            null,
            null,
            "date DESC"
        )

        cursor?.use {
            val addressIndex = it.getColumnIndex("address")
            val bodyIndex = it.getColumnIndex("body")
            val dateIndex = it.getColumnIndex("date")

            while (it.moveToNext()) {
                messages.add(
                    SmsMessage(
                        sender = if (addressIndex >= 0) {
                            it.getString(addressIndex)
                        } else {
                            null
                        },
                        body = if (bodyIndex >= 0) {
                            it.getString(bodyIndex)
                        } else {
                            null
                        },
                        timestamp = if (dateIndex >= 0) {
                            it.getLong(dateIndex)
                        } else {
                            0L
                        }
                    )
                )
            }
        }

        return messages
    }
}