package com.accountkeeper

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.abs

class LedgerStore(context: Context) {
    private val prefs = context.getSharedPreferences("ledger", Context.MODE_PRIVATE)
    var state by mutableStateOf(load())
        private set

    fun addAccount(name: String, kind: String, balanceFen: Long) {
        if (name.isBlank()) return
        state = state.copy(
            accounts = state.accounts + Account(UUID.randomUUID().toString(), name.trim(), kind, balanceFen)
        )
        persist()
    }

    fun addTransaction(transaction: LedgerTransaction) {
        state = state.copy(transactions = (state.transactions + transaction).sortedByDescending { it.occurredAt })
        persist()
    }

    fun addImportedCsv(raw: String): Int {
        var imported = 0
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val cells = line.split(",", "，").map { it.trim() }
                if (cells.size >= 5) {
                    val amount = cells[1].toFenOrNull()
                    val account = state.accounts.firstOrNull { it.name == cells.getOrNull(3) }
                        ?: state.accounts.firstOrNull()
                    val category = state.categories.firstOrNull { it.name == cells.getOrNull(4) }
                        ?: state.categories.firstOrNull()
                    if (amount != null && account != null && category != null) {
                        addTransaction(
                            LedgerTransaction(
                                id = UUID.randomUUID().toString(),
                                amountFen = abs(amount),
                                type = if (amount < 0) "支出" else "收入",
                                occurredAt = normalizeTime(cells[0]),
                                platform = cells.getOrNull(2).orEmpty().ifBlank { "手动" },
                                accountId = account.id,
                                merchant = cells.getOrNull(5).orEmpty().ifBlank { "导入记录" },
                                categoryId = category.id,
                                note = cells.drop(6).joinToString(" "),
                                source = "CSV"
                            )
                        )
                        imported += 1
                    }
                }
            }
        return imported
    }

    fun markDuplicate(visibleId: String, duplicateId: String) {
        state = state.copy(
            transactions = state.transactions.map {
                when (it.id) {
                    duplicateId -> it.copy(linkedTo = visibleId, excludedFromStats = true)
                    else -> it
                }
            }
        )
        persist()
    }

    fun deleteTransaction(id: String) {
        state = state.copy(transactions = state.transactions.filterNot { it.id == id })
        persist()
    }

    private fun load(): LedgerState {
        val raw = prefs.getString("state", null) ?: return seedState()
        return runCatching {
            val json = JSONObject(raw)
            LedgerState(
                accounts = json.getJSONArray("accounts").objects().map {
                    Account(
                        id = it.getString("id"),
                        name = it.getString("name"),
                        kind = it.getString("kind"),
                        balanceFen = it.getLong("balanceFen")
                    )
                },
                categories = json.getJSONArray("categories").objects().map {
                    Category(it.getString("id"), it.getString("name"))
                },
                transactions = json.getJSONArray("transactions").objects().map {
                    LedgerTransaction(
                        id = it.getString("id"),
                        amountFen = it.getLong("amountFen"),
                        type = it.getString("type"),
                        occurredAt = it.getString("occurredAt"),
                        platform = it.getString("platform"),
                        accountId = it.getString("accountId"),
                        merchant = it.getString("merchant"),
                        categoryId = it.getString("categoryId"),
                        note = it.optString("note"),
                        source = it.optString("source", "手动"),
                        linkedTo = it.optString("linkedTo").ifBlank { null },
                        excludedFromStats = it.optBoolean("excludedFromStats", false)
                    )
                }
            )
        }.getOrElse { seedState() }
    }

    private fun persist() {
        val json = JSONObject()
            .put("accounts", JSONArray(state.accounts.map {
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("kind", it.kind)
                    .put("balanceFen", it.balanceFen)
            }))
            .put("categories", JSONArray(state.categories.map {
                JSONObject().put("id", it.id).put("name", it.name)
            }))
            .put("transactions", JSONArray(state.transactions.map {
                JSONObject()
                    .put("id", it.id)
                    .put("amountFen", it.amountFen)
                    .put("type", it.type)
                    .put("occurredAt", it.occurredAt)
                    .put("platform", it.platform)
                    .put("accountId", it.accountId)
                    .put("merchant", it.merchant)
                    .put("categoryId", it.categoryId)
                    .put("note", it.note)
                    .put("source", it.source)
                    .put("linkedTo", it.linkedTo ?: "")
                    .put("excludedFromStats", it.excludedFromStats)
            }))
        prefs.edit().putString("state", json.toString()).apply()
    }
}

private fun seedState(): LedgerState {
    val wechat = Account(UUID.randomUUID().toString(), "微信零钱", "微信零钱", 0)
    val alipay = Account(UUID.randomUUID().toString(), "支付宝余额", "支付宝余额", 0)
    val bank = Account(UUID.randomUUID().toString(), "默认银行卡", "银行卡", 0)
    val categories = listOf("餐饮", "交通", "购物", "居家", "医疗", "娱乐", "工资", "转账")
        .map { Category(UUID.randomUUID().toString(), it) }
    return LedgerState(listOf(wechat, alipay, bank), categories, emptyList())
}
