package com.balance.monthly.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.balance.monthly.R
import com.balance.monthly.domain.EntryType
import com.balance.monthly.domain.EntryWithBalance
import com.balance.monthly.domain.LedgerEntry
import com.balance.monthly.ui.formatAmount
import com.balance.monthly.ui.formatSigned
import com.balance.monthly.ui.formatWon
import com.balance.monthly.ui.theme.ExpenseRed
import com.balance.monthly.ui.theme.TabularNumbers
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormat = DateTimeFormatter.ofPattern("M/d (E)", Locale.KOREAN)

@Composable
fun RecordsScreen(
    viewModel: RecordsViewModel,
    onEdit: (LedgerEntry) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<LedgerEntry?>(null) }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::showPrevious, enabled = state.canGoBack) {
                Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = "이전 기간")
            }
            Text(
                text = state.periodLabel,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::showNext, enabled = state.canGoForward) {
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = "다음 기간")
            }
        }

        val items = state.items
        when {
            items == null -> Unit
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("이 기간에는 기록이 없습니다", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                items(items, key = { it.entry.id }) { item ->
                    SwipeToDelete(onSwiped = { pendingDelete = item.entry }) {
                        RecordRow(item, onClick = { onEdit(item.entry) })
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("기록을 삭제할까요?") },
            text = { Text("이후 잔고와 차액이 다시 계산됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(entry.id)
                    pendingDelete = null
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("취소") } },
        )
    }
}

/** 왼쪽으로 밀면 삭제를 요청한다. 항목은 제자리로 돌아오고, 실제 삭제는 확인 후에 한다. */
@Composable
private fun SwipeToDelete(onSwiped: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onSwiped()
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("삭제", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        content()
    }
}

@Composable
private fun RecordRow(item: EntryWithBalance, onClick: () -> Unit) {
    val entry = item.entry
    val (symbol, color, label) = when (entry.type) {
        EntryType.INCOME -> Triple("+", MaterialTheme.colorScheme.primary, "수입")
        EntryType.EXPENSE -> Triple("−", ExpenseRed, "지출")
        EntryType.BALANCE -> Triple("=", MaterialTheme.colorScheme.onSurfaceVariant, "실제 잔고")
    }
    val amountText = when (entry.type) {
        EntryType.INCOME -> formatSigned(entry.amount)
        EntryType.EXPENSE -> formatSigned(-entry.amount)
        EntryType.BALANCE -> formatAmount(entry.amount)
    }
    val detail = buildString {
        append(entry.date.format(dateFormat))
        if (entry.type == EntryType.BALANCE) {
            append(" · ")
            append(
                when {
                    item.isOpening -> "시작 잔고"
                    item.diff > 0 -> "차액 ${formatSigned(item.diff)} (미기록 수입)"
                    item.diff < 0 -> "차액 ${formatSigned(item.diff)} (미기록 지출)"
                    else -> "차액 없음"
                },
            )
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeBadge(symbol, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.memo ?: label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            Text(amountText, style = MaterialTheme.typography.bodyLarge.merge(TabularNumbers), color = if (entry.type == EntryType.BALANCE) Color.Unspecified else color)
            Text(
                "잔고 ${formatWon(item.balance)}",
                style = MaterialTheme.typography.bodySmall.merge(TabularNumbers),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TypeBadge(symbol: String, color: Color) {
    Box(
        Modifier.size(36.dp).background(color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = color, style = MaterialTheme.typography.titleMedium)
    }
}
