package com.accountkeeper

data class Account(
    val id: String,
    val name: String,
    val kind: String,
    val balanceFen: Long
)

data class Category(
    val id: String,
    val name: String
)

data class LedgerTransaction(
    val id: String,
    val amountFen: Long,
    val type: String,
    val occurredAt: String,
    val platform: String,
    val accountId: String,
    val merchant: String,
    val categoryId: String,
    val note: String,
    val source: String,
    val linkedTo: String? = null,
    val excludedFromStats: Boolean = false
)

data class LedgerState(
    val accounts: List<Account>,
    val categories: List<Category>,
    val transactions: List<LedgerTransaction>
)

data class DuplicateCandidate(
    val visible: LedgerTransaction,
    val duplicate: LedgerTransaction,
    val reason: String
)
