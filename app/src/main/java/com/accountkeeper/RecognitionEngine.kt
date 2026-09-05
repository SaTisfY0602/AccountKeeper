package com.accountkeeper

object RecognitionEngine {
    private val amountPattern = Regex("""(?:付款|支付|收入|收款|到账)?\s*([0-9]+(?:\.[0-9]{1,2})?)\s*元""")
    private val merchantPattern = Regex("""(?:给|向|商户[:：]?)\s*([\u4e00-\u9fa5A-Za-z0-9 _-]{2,24})""")

    fun parse(event: RawEvent): ParsedTransaction {
        val text = event.rawText
        val amountFen = amountPattern.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.toFenOrNull()
            ?: 0
        val direction = when {
            text.contains("收入") || text.contains("收款") || text.contains("到账") -> "收入"
            else -> "支出"
        }
        val platform = when {
            event.sourceApp.contains("微信") || text.contains("微信") -> "微信"
            event.sourceApp.contains("支付宝") || text.contains("支付宝") -> "支付宝"
            else -> event.sourceApp.ifBlank { "未知平台" }
        }
        val merchant = merchantPattern.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?: "未知商户"
        val score = when {
            amountFen > 0 && merchant != "未知商户" && platform != "未知平台" -> 90
            amountFen > 0 && platform != "未知平台" -> 70
            else -> 30
        }

        return ParsedTransaction(
            rawEventId = event.id,
            direction = direction,
            amountFen = amountFen,
            platform = platform,
            merchant = merchant,
            occurredAt = normalizeTime(event.capturedAt),
            confidence = RecognitionConfidence(score, "基于来源、金额关键词和商户文本解析")
        )
    }
}
