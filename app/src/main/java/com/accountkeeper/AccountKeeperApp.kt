package com.accountkeeper

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.math.abs

@Composable
fun AccountKeeperApp(store: LedgerStore) {
    var tab by remember { mutableStateOf(0) }
    val items = listOf("首页", "流水", "记一笔", "账户", "导入")

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF1967D2),
            secondary = Color(0xFF0F8B6F),
            tertiary = Color(0xFFB45309),
            surface = Color(0xFFF8FAFC),
            background = Color(0xFFF3F6FA)
        )
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    items.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Home
                                        1 -> Icons.Default.ReceiptLong
                                        2 -> Icons.Default.Add
                                        3 -> Icons.Default.AccountBalance
                                        else -> Icons.Default.UploadFile
                                    },
                                    contentDescription = label
                                )
                            },
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF3F6FA))
                    .padding(padding),
                color = Color(0xFFF3F6FA)
            ) {
                when (tab) {
                    0 -> HomeScreen(store)
                    1 -> TransactionsScreen(store)
                    2 -> AddTransactionScreen(store, onSaved = { tab = 1 })
                    3 -> AccountsScreen(store)
                    4 -> ImportScreen(store)
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(store: LedgerStore) {
    val state = store.state
    val active = state.transactions.filterNot { it.excludedFromStats }
    val month = YearMonth.now()
    val monthTx = active.filter {
        runCatching { YearMonth.from(LocalDateTime.parse(it.occurredAt, TimeFormatter)) == month }.getOrDefault(false)
    }
    val expense = monthTx.filter { it.type == "支出" }.sumOf { it.amountFen }
    val income = monthTx.filter { it.type == "收入" }.sumOf { it.amountFen }
    val candidates = findDuplicateCandidates(state)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("AccountKeeper", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("统一看清微信、支付宝和银行卡支出", color = Color(0xFF526070))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("本月支出", expense.money(), Color(0xFFB42318), Modifier.weight(1f))
                MetricCard("本月收入", income.money(), Color(0xFF0F8B6F), Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("账户")
            state.accounts.forEach { account ->
                val spending = active.filter { it.accountId == account.id && it.type == "支出" }.sumOf { it.amountFen }
                CompactRow(account.name, "${account.kind}  已记支出 ${spending.money()}", account.balanceFen.money())
            }
        }
        item {
            SectionTitle("疑似重复")
            if (candidates.isEmpty()) {
                EmptyHint("暂无疑似重复记录。微信扣银行卡这类情况，导入后会出现在这里。")
            } else {
                candidates.take(5).forEach { candidate ->
                    DuplicateCard(candidate, store::markDuplicate)
                }
            }
        }
        item {
            SectionTitle("分类统计")
            val byCategory = monthTx.filter { it.type == "支出" }
                .groupBy { it.categoryId }
                .mapValues { it.value.sumOf { tx -> tx.amountFen } }
                .toList()
                .sortedByDescending { it.second }
            if (byCategory.isEmpty()) EmptyHint("本月还没有支出。")
            byCategory.forEach { (categoryId, amount) ->
                val name = state.categories.firstOrNull { it.id == categoryId }?.name ?: "未分类"
                CompactRow(name, "本月支出", amount.money())
            }
        }
    }
}

@Composable
private fun TransactionsScreen(store: LedgerStore) {
    var filter by remember { mutableStateOf("全部") }
    val state = store.state
    val filtered = state.transactions.filter { filter == "全部" || it.platform == filter }
    val platforms = listOf("全部", "微信", "支付宝", "银行卡", "手动")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("流水", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                platforms.forEach {
                    AssistChip(onClick = { filter = it }, label = { Text(if (filter == it) "✓ $it" else it) })
                }
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyHint("还没有流水，先记一笔或者导入几行 CSV。") }
        }
        items(filtered, key = { it.id }) { tx ->
            TransactionCard(tx, state, onDelete = { store.deleteTransaction(tx.id) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionScreen(store: LedgerStore, onSaved: () -> Unit) {
    val state = store.state
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("支出") }
    var platform by remember { mutableStateOf("微信") }
    var account by remember { mutableStateOf(state.accounts.firstOrNull()) }
    var category by remember { mutableStateOf(state.categories.firstOrNull()) }
    var merchant by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var time by remember { mutableStateOf(LocalDateTime.now().format(TimeFormatter)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("记一笔", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("支出", "收入", "转账").forEach {
                    OutlinedButton(onClick = { type = it }, border = if (type == it) BorderStroke(2.dp, Color(0xFF1967D2)) else null) {
                        Text(it)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { ChoiceField("发生平台", platform, listOf("微信", "支付宝", "银行卡", "手动")) { platform = it } }
        item { ChoiceField("实际账户", account?.name ?: "", state.accounts.map { it.name }) { name -> account = state.accounts.first { it.name == name } } }
        item { ChoiceField("分类", category?.name ?: "", state.categories.map { it.name }) { name -> category = state.categories.first { it.name == name } } }
        item {
            OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("商户/对象") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("时间 yyyy-MM-dd HH:mm") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Button(
                onClick = {
                    val amountFen = amount.toFenOrNull()
                    val selectedAccount = account
                    val selectedCategory = category
                    if (amountFen != null && selectedAccount != null && selectedCategory != null) {
                        store.addTransaction(
                            LedgerTransaction(
                                id = UUID.randomUUID().toString(),
                                amountFen = abs(amountFen),
                                type = type,
                                occurredAt = normalizeTime(time),
                                platform = platform,
                                accountId = selectedAccount.id,
                                merchant = merchant.ifBlank { "未填写" },
                                categoryId = selectedCategory.id,
                                note = note,
                                source = "手动"
                            )
                        )
                        amount = ""
                        merchant = ""
                        note = ""
                        onSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("保存")
            }
        }
    }
}

@Composable
private fun AccountsScreen(store: LedgerStore) {
    var name by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("银行卡") }
    var balance by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("账户", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        items(store.state.accounts, key = { it.id }) {
            CompactRow(it.name, it.kind, it.balanceFen.money())
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            Text("新增账户", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        item { OutlinedTextField(name, { name = it }, label = { Text("账户名称") }, modifier = Modifier.fillMaxWidth()) }
        item { ChoiceField("账户类型", kind, listOf("微信零钱", "支付宝余额", "银行卡", "信用卡", "现金")) { kind = it } }
        item {
            OutlinedTextField(
                balance,
                { balance = it },
                label = { Text("当前余额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = {
                    store.addAccount(name, kind, balance.toFenOrNull() ?: 0)
                    name = ""
                    balance = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("添加账户")
            }
        }
    }
}

@Composable
private fun ImportScreen(store: LedgerStore) {
    var csv by remember { mutableStateOf(sampleCsv()) }
    var message by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("导入", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("每行格式：时间,金额,平台,实际账户,分类,商户,备注", color = Color(0xFF526070))
        }
        item {
            OutlinedTextField(
                value = csv,
                onValueChange = { csv = it },
                minLines = 8,
                label = { Text("CSV 内容") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = {
                    val count = store.addImportedCsv(csv)
                    message = "已导入 $count 条记录"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("导入")
            }
        }
        if (message.isNotBlank()) {
            item { EmptyHint(message) }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFF526070))
            Text(value, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun CompactRow(title: String, subtitle: String, trailing: String) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = Color(0xFF526070), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(trailing, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Text(text, color = Color(0xFF526070))
    }
}

@Composable
private fun TransactionCard(tx: LedgerTransaction, state: LedgerState, onDelete: () -> Unit) {
    val account = state.accounts.firstOrNull { it.id == tx.accountId }?.name ?: "未知账户"
    val category = state.categories.firstOrNull { it.id == tx.categoryId }?.name ?: "未分类"
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tx.merchant, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (tx.excludedFromStats) {
                        Text("已合并", color = Color(0xFF0F8B6F), style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("${tx.occurredAt}  ${tx.platform} / $account  $category", color = Color(0xFF526070), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (tx.note.isNotBlank()) Text(tx.note, color = Color(0xFF526070), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (tx.type == "支出") "-" else "+"}${tx.amountFen.money()}",
                    color = if (tx.type == "支出") Color(0xFFB42318) else Color(0xFF0F8B6F),
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = Color(0xFF697586))
                }
            }
        }
    }
}

@Composable
private fun DuplicateCard(candidate: DuplicateCandidate, onMerge: (String, String) -> Unit) {
    OutlinedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(candidate.reason, fontWeight = FontWeight.SemiBold)
            Text("${candidate.visible.platform}: ${candidate.visible.merchant} ${candidate.visible.amountFen.money()}", color = Color(0xFF526070))
            Text("${candidate.duplicate.platform}: ${candidate.duplicate.merchant} ${candidate.duplicate.amountFen.money()}", color = Color(0xFF526070))
            TextButton(onClick = { onMerge(candidate.visible.id, candidate.duplicate.id) }) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("合并为同一笔支出")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceField(label: String, value: String, choices: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            readOnly = true,
            value = value,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onChange(it)
                        expanded = false
                    }
                )
            }
        }
    }
}
