package com.accountkeeper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognitionEngineTest {
    @Test
    fun parsesWechatPaymentNotificationAsExpenseCandidate() {
        val event = RawEvent(
            id = "raw-1",
            sourceType = RawEventSourceType.NOTIFICATION,
            sourceApp = "微信",
            capturedAt = "2026-09-05 12:30",
            rawText = "微信支付 凭证 付款100.00元 给便利店"
        )

        val parsed = RecognitionEngine.parse(event)

        assertEquals("支出", parsed.direction)
        assertEquals(10_000, parsed.amountFen)
        assertEquals("微信", parsed.platform)
        assertEquals("便利店", parsed.merchant)
        assertTrue(parsed.confidence.score >= 80)
    }
}
