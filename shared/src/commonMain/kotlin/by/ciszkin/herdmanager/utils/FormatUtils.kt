package by.ciszkin.herdmanager.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.2f GB".format(gb)
        mb >= 1 -> "%.2f MB".format(mb)
        kb >= 1 -> "%.2f KB".format(kb)
        else -> "$bytes B"
    }
}

/**
 * Formats an ISO-8601 instant for display in the local time zone.
 * Tolerates the variable precision Ollama can emit (with/without millis and
 * with `Z` or a numeric offset); falls back to the raw string on failure.
 */
fun formatDate(isoString: String): String {
    val inputFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    outputFormat.timeZone = TimeZone.getDefault()

    for (pattern in inputFormats) {
        try {
            val inputFormat = SimpleDateFormat(pattern, Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoString) ?: continue
            return outputFormat.format(date)
        } catch (_: Exception) {
            // try the next pattern
        }
    }
    return isoString
}