
package com.kanreddyjp.walletsmsimporter.sms

import com.kanreddyjp.walletsmsimporter.transaction.TransactionType
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ParsedSmsTransaction(
    val amount: Double?,
    val type: TransactionType,
    val recordDate: Long,
    val counterParty: String,
    val note: String
)

object SmsTransactionParser {

    private val currencyAmountRegex = Regex(
        """(?:₹|INR\b|Rs\.?\s*)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val verbAmountRegex = Regex(
        """\b(?:debited|credited|withdrawn|deposited|spent|received|paid)\b(?:\s+(?:by|with|of|for))?\s+(?:₹|INR\b|Rs\.?\s*)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val dateRegex = Regex(
        """\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{1,2}\s+[A-Za-z]{3,9}\s+\d{2,4})\b"""
    )

    private val timeRegex = Regex(
        """\b(\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM)?)\b""",
        RegexOption.IGNORE_CASE
    )

    private val referenceRegex = Regex(
        """\b(?:UPI\s*)?(?:Ref(?:erence)?(?:\s*(?:No|ID))?|Txn(?:\s*ID)?|Transaction\s*ID)\s*[:#-]?\s*([A-Z0-9/-]{4,})""",
        RegexOption.IGNORE_CASE
    )

    private val counterPartyRegex = Regex(
        """\b(?:to|from|at)\s+([A-Z][A-Z0-9 &.'_-]{1,35}?)(?=\s+(?:on|via|for|ref|upi|txn|transaction|at|from|to|avl|available|bal|balance)\b|[,;.]|$)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(message: SmsMessage): ParsedSmsTransaction {
        val body = message.body.orEmpty()

        return ParsedSmsTransaction(
            amount = extractAmount(body),
            type = extractType(body),
            recordDate = extractRecordDate(body, message.timestamp),
            counterParty = extractCounterParty(body),
            note = extractNote(body)
        )
    }

    private fun extractAmount(body: String): Double? {
        val match = currencyAmountRegex.find(body)
            ?: verbAmountRegex.find(body)
            ?: return null

        val number = match.groupValues[1]
            .replace(",", "")
            .toDoubleOrNull()
            ?: return null

        return number.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun extractType(body: String): TransactionType {
        val text = body.lowercase(Locale.ROOT)

        val debitIndicators = listOf(
            "debited",
            "debit",
            "withdrawn",
            "withdrawal",
            "spent",
            "purchase",
            "paid",
            "sent",
            "payment to"
        )

        val creditIndicators = listOf(
            "credited",
            "credit",
            "deposited",
            "deposit",
            "received",
            "refund",
            "cashback"
        )

        val debit = debitIndicators.any { it in text }
        val credit = creditIndicators.any { it in text }

        return when {
            debit && !credit -> TransactionType.EXPENSE
            credit && !debit -> TransactionType.INCOME
            else -> TransactionType.EXPENSE
        }
    }

    private fun extractRecordDate(
        body: String,
        smsTimestamp: Long
    ): Long {
        val dateMatch = dateRegex.find(body) ?: return smsTimestamp
        val dateText = dateMatch.groupValues[1]

        val datePatterns = listOf(
            "dd/MM/yyyy",
            "d/M/yyyy",
            "dd-MM-yyyy",
            "d-M-yyyy",
            "dd/MM/yy",
            "d/M/yy",
            "dd-MM-yy",
            "d-M-yy",
            "dd MMM yyyy",
            "d MMM yyyy",
            "dd MMMM yyyy",
            "d MMMM yyyy",
            "dd MMM yy",
            "d MMM yy"
        )

        var parsedDate: Date? = null

        for (pattern in datePatterns) {
            val formatter = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                isLenient = false
            }

            val position = ParsePosition(0)
            val candidate = formatter.parse(dateText, position)

            if (candidate != null && position.index == dateText.length) {
                parsedDate = candidate
                break
            }
        }

        val date = parsedDate ?: return smsTimestamp
        val calendar = Calendar.getInstance().apply {
            time = date
        }

        val timeMatch = timeRegex.find(
            body.substring(dateMatch.range.last + 1)
        )

        if (timeMatch != null) {
            val timeText = timeMatch.groupValues[1]
                .trim()
                .uppercase(Locale.ROOT)

            val timePatterns = listOf(
                "h:mm:ss a",
                "h:mm a",
                "HH:mm:ss",
                "HH:mm"
            )

            var parsedTime: Date? = null

            for (pattern in timePatterns) {
                val formatter = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    isLenient = false
                }

                val position = ParsePosition(0)
                val candidate = formatter.parse(timeText, position)

                if (candidate != null && position.index == timeText.length) {
                    parsedTime = candidate
                    break
                }
            }

            if (parsedTime != null) {
                val timeCalendar = Calendar.getInstance().apply {
                    time = parsedTime
                }

                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
                calendar.set(Calendar.MILLISECOND, 0)
            }
        }

        return calendar.timeInMillis
    }

    private fun extractCounterParty(body: String): String {
        val match = counterPartyRegex.find(body) ?: return ""

        val candidate = match.groupValues[1]
            .trim()
            .trimEnd('.', ',', ';', ':')

        val lower = candidate.lowercase(Locale.ROOT)

        val genericPhrases = listOf(
            "your account",
            "your a/c",
            "account",
            "a/c",
            "the account"
        )

        if (genericPhrases.any { lower.startsWith(it) }) {
            return ""
        }

        return candidate
    }

    private fun extractNote(body: String): String {
        val match = referenceRegex.find(body) ?: return ""

        return "Reference: ${match.groupValues[1]}"
    }
}
