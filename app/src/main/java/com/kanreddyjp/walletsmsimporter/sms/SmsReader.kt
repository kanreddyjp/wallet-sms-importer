
package com.kanreddyjp.walletsmsimporter.sms

import android.content.Context
import android.net.Uri

data class SmsMessage(
    val id: Long,
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
                "_id",
                "address",
                "body",
                "date",
                "type"
            ),
            "type = ?",
            arrayOf("1"),
            "date ASC, _id ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex("_id")
            val addressIndex = it.getColumnIndex("address")
            val bodyIndex = it.getColumnIndex("body")
            val dateIndex = it.getColumnIndex("date")

            while (it.moveToNext()) {
                // Skip rows without a usable database ID or timestamp.
                if (idIndex < 0 || dateIndex < 0) {
                    continue
                }

                val id = it.getLong(idIndex)
                val timestamp = it.getLong(dateIndex)

                if (id <= 0L || timestamp <= 0L) {
                    continue
                }

                messages.add(
                    SmsMessage(
                        id = id,
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
                        timestamp = timestamp
                    )
                )
            }
        }

        return messages
    }
}
