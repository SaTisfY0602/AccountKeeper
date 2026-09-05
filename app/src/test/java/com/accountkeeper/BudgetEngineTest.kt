package com.accountkeeper

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BudgetEngineTest {
    @Test
    fun monthlyBudgetCarriesUnusedMoneyIntoRemainingDays() {
        val plan = BudgetPlan(
            id = "plan-month",
            name = "月度省钱目标",
            period = BudgetPeriod.MONTHLY,
            amountFen = 200_000,
            startsOn = LocalDate.of(2026, 9, 1),
            endsOn = LocalDate.of(2026, 9, 30)
        )
        val transactions = listOf(
            budgetTransaction("2026-09-01 12:00", 4_000),
            budgetTransaction("2026-09-02 12:00", 3_000)
        )

        val state = BudgetEngine.evaluate(plan, transactions, LocalDate.of(2026, 9, 3))

        assertEquals(193_000, state.remainingFen)
        assertEquals(6_893, state.recommendedTodayFen)
        assertEquals(0, state.spentTodayFen)
        assertEquals(false, state.isOverToday)
    }

    @Test
    fun monthlyBudgetReportsOverspendingForToday() {
        val plan = BudgetPlan(
            id = "plan-month",
            name = "月度省钱目标",
            period = BudgetPeriod.MONTHLY,
            amountFen = 2_000,
            startsOn = LocalDate.of(2026, 9, 1),
            endsOn = LocalDate.of(2026, 9, 2)
        )
        val transactions = listOf(
            budgetTransaction("2026-09-02 10:00", 1_500)
        )

        val state = BudgetEngine.evaluate(plan, transactions, LocalDate.of(2026, 9, 2))

        assertEquals(500, state.remainingFen)
        assertEquals(500, state.recommendedTodayFen)
        assertEquals(1_500, state.spentTodayFen)
        assertEquals(true, state.isOverToday)
        assertEquals(1_000, state.overTodayFen)
    }

    private fun budgetTransaction(time: String, amountFen: Long): LedgerTransaction {
        return LedgerTransaction(
            id = time,
            amountFen = amountFen,
            type = "支出",
            occurredAt = time,
            platform = "微信",
            accountId = "wechat",
            merchant = "测试商户",
            categoryId = "shopping",
            note = "",
            source = "test"
        )
    }
}
