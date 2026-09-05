package com.accountkeeper

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun String.toFenOrNull(): Long? {
    val clean = replace("¥", "").replace("￥", "").trim()
    return clean.toBigDecimalOrNull()?.movePointRight(2)?.toLong()
}

fun Long.money(): String {
    val yuan = this / 100
    val fen = abs(this % 100).toString().padStart(2, '0')
    return "¥$yuan.$fen"
}

fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

fun normalizeTime(value: String): String {
    val trimmed = value.trim()
    val candidates = listOf(
        TimeFormatter,
        DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    )
    for (formatter in candidates) {
        val parsed = runCatching { LocalDateTime.parse(trimmed, formatter) }.getOrNull()
        if (parsed != null) return parsed.format(TimeFormatter)
    }
    return LocalDateTime.now().format(TimeFormatter)
}

fun sampleCsv(): String {
    val now = LocalDateTime.now().format(TimeFormatter)
    return "$now,-35.00,微信,默认银行卡,餐饮,午餐,微信支付但银行卡扣款\n$now,-35.00,银行卡,默认银行卡,餐饮,财付通支付,银行卡流水"
}
