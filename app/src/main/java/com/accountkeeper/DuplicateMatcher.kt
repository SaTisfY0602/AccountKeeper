package com.accountkeeper

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

fun findDuplicateCandidates(state: LedgerState): List<DuplicateCandidate> {
    val active = state.transactions.filterNot { it.excludedFromStats }
    val candidates = mutableListOf<DuplicateCandidate>()
    for (a in active) {
        for (b in active) {
            if (a.id >= b.id || a.type != "支出" || b.type != "支出") continue
            if (a.amountFen != b.amountFen) continue
            if (!isWalletBankPair(a, b)) continue
            val minutes = minutesBetween(a.occurredAt, b.occurredAt)
            if (minutes <= 24 * 60) {
                val visible = if (a.platform == "银行卡") b else a
                val duplicate = if (a.platform == "银行卡") a else b
                candidates += DuplicateCandidate(visible, duplicate, "金额相同、时间接近，可能是平台支付和银行卡扣款重复")
            }
        }
    }
    return candidates.distinctBy { it.visible.id + it.duplicate.id }
}

private fun isWalletBankPair(a: LedgerTransaction, b: LedgerTransaction): Boolean {
    val platforms = setOf(a.platform, b.platform)
    val hasWallet = platforms.contains("微信") || platforms.contains("支付宝")
    return hasWallet && platforms.contains("银行卡")
}

private fun minutesBetween(a: String, b: String): Long {
    val first = runCatching { LocalDateTime.parse(a, TimeFormatter) }.getOrNull() ?: return Long.MAX_VALUE
    val second = runCatching { LocalDateTime.parse(b, TimeFormatter) }.getOrNull() ?: return Long.MAX_VALUE
    return abs(ChronoUnit.MINUTES.between(first, second))
}
