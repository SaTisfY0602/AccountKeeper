package com.accountkeeper

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

object BudgetEngine {
    fun evaluate(
        plan: BudgetPlan,
        transactions: List<LedgerTransaction>,
        today: LocalDate
    ): DailyBudgetState {
        val inPeriod = transactions.filter { transaction ->
            val date = transaction.occurredDateOrNull()
            transaction.type == "支出" && date != null && !date.isBefore(plan.startsOn) && !date.isAfter(today)
        }
        val spentToDate = inPeriod.sumOf { it.amountFen }
        val spentToday = inPeriod
            .filter { it.occurredDateOrNull() == today }
            .sumOf { it.amountFen }
        val remaining = max(0, plan.amountFen - spentToDate)
        val daysRemainingIncludingToday = max(1, ChronoUnit.DAYS.between(today, plan.endsOn).toInt() + 1)
        val recommendedToday = ceilDivide(remaining, daysRemainingIncludingToday.toLong())

        return DailyBudgetState(
            planId = plan.id,
            date = today,
            totalBudgetFen = plan.amountFen,
            spentToDateFen = spentToDate,
            spentTodayFen = spentToday,
            remainingFen = remaining,
            recommendedTodayFen = recommendedToday,
            isOverToday = spentToday > recommendedToday,
            overTodayFen = max(0, spentToday - recommendedToday)
        )
    }

    private fun LedgerTransaction.occurredDateOrNull(): LocalDate? {
        return runCatching {
            LocalDateTime.parse(occurredAt, TimeFormatter).toLocalDate()
        }.getOrNull()
    }

    private fun ceilDivide(value: Long, divisor: Long): Long {
        return if (value == 0L) 0 else ((value - 1) / divisor) + 1
    }
}
