package com.accountkeeper

import java.time.LocalDate

enum class BudgetPeriod {
    MONTHLY,
    YEARLY
}

data class BudgetPlan(
    val id: String,
    val name: String,
    val period: BudgetPeriod,
    val amountFen: Long,
    val startsOn: LocalDate,
    val endsOn: LocalDate
)

data class DailyBudgetState(
    val planId: String,
    val date: LocalDate,
    val totalBudgetFen: Long,
    val spentToDateFen: Long,
    val spentTodayFen: Long,
    val remainingFen: Long,
    val recommendedTodayFen: Long,
    val isOverToday: Boolean,
    val overTodayFen: Long
)
