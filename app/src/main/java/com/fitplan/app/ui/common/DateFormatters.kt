package com.fitplan.app.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val shortDateFormatter = DateTimeFormatter.ofPattern("MM月dd日 HH:mm")
private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")

fun formatShortDate(millis: Long?): String {
    if (millis == null || millis <= 0) return "暂无"
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(shortDateFormatter)
}

fun formatFullDate(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(fullDateFormatter)
}

