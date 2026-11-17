package com.example.util

import java.util.Locale

object FormatUtils {
    fun formatCompactCount(count: Long): String {
        return when {
            count >= 1_000_000 -> {
                val value = count / 1_000_000.0
                if (value >= 10) String.format(Locale.US, "%.0fM", value)
                else String.format(Locale.US, "%.1fM", value).removeSuffix(".0M").let {
                    if (it.endsWith("M")) it else "${it}M"
                }
            }
            count >= 1_000 -> {
                val value = count / 1_000.0
                if (value >= 100) String.format(Locale.US, "%.0fK", value)
                else String.format(Locale.US, "%.1fK", value).replace(".0K", "K")
            }
            else -> count.toString()
        }
    }

    fun formatCompactCount(count: Int): String = formatCompactCount(count.toLong())

    fun formatDuration(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatViews(viewsCount: Long, isLive: Boolean): String {
        return if (isLive) {
            "${formatCompactCount(viewsCount)} watching"
        } else {
            "${formatCompactCount(viewsCount)} views"
        }
    }
}
