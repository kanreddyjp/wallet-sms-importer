
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
    val accountSuffix: String?,
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
            accountSuffix = extractAccountSuffix(body),
            recordDate = extractRecordDate(body, message.timestamp),
            counterParty = extractCounterParty(body),
            note = extractNote(body)
        )
    }

    private fun extractAccountSuffix(body: String): String? {
        // Match four digits only when the SMS explicitly identifies an account/card number.
        val patterns = listOf(
            Regex("""\b(?:a/c|account|acct|card)(?:\s*(?:number|no\.?|ending|ending\s+in))?\s*[:#-]?\s*(?:x{2,}|\*{2,}|[xX*]+)?\s*(\d{4})\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(?:ending|ends)\s+(?:in\s+)?(\d{4})\b""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(body) ?: continue
            return match.groupValues[1]
        }
        return null
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

        val dateParts = Regex(
            """^(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})$"""
        ).matchEntire(dateText)

        val calendar = Calendar.getInstance().apply {
            clear()
            timeZone = java.util.TimeZone.getDefault()
        }

        if (dateParts != null) {
            val day = dateParts.groupValues[1].toInt()
            val month = dateParts.groupValues[2].toInt()
            val yearText = dateParts.groupValues[3]
            val year = if (yearText.length == 2) {
                2000 + yearText.toInt()
            } else {
                yearText.toInt()
            }

            if (month !in 1..12 || day !in 1..31) return smsTimestamp

            calendar.set(year, month - 1, day, 12, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Reject impossible dates such as 31 February.
            if (
                calendar.get(Calendar.YEAR) != year ||
                calendar.get(Calendar.MONTH) != month - 1 ||
                calendar.get(Calendar.DAY_OF_MONTH) != day
            ) {
                return smsTimestamp
            }
        } else {
            val patterns = listOf(
                "d MMM yyyy",
                "dd MMM yyyy",
                "d MMMM yyyy",
                "dd MMMM yyyy",
                "d MMM yy",
                "dd MMM yy"
            )

            var parsed: Date? = null

            for (pattern in patterns) {
                val normalizedText = if (pattern.endsWith("yy")) {
                    dateText.replace(
                        Regex("""(\d{2})$"""),
                        "20$1"
                    )
                } else {
                    dateText
                }

                val actualPattern = pattern.replace("yy", "yyyy")
                val formatter = SimpleDateFormat(actualPattern, Locale.ENGLISH).apply {
                    isLenient = false
                }

                val position = ParsePosition(0)
                val candidate = formatter.parse(normalizedText, position)

                if (candidate != null && position.index == normalizedText.length) {
                    parsed = candidate
                    break
                }
            }

            if (parsed == null) return smsTimestamp

            val parsedCalendar = Calendar.getInstance().apply {
                time = parsed!!
            }

            calendar.set(
                parsedCalendar.get(Calendar.YEAR),
                parsedCalendar.get(Calendar.MONTH),
                parsedCalendar.get(Calendar.DAY_OF_MONTH),
                12, 0, 0
            )
            calendar.set(Calendar.MILLISECOND, 0)
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
