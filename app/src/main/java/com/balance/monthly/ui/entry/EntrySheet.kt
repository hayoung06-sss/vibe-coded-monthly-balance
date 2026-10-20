package com.balance.monthly.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.balance.monthly.domain.EntryType
import com.balance.monthly.domain.LedgerEntry
import com.balance.monthly.domain.priorBalance
import com.balance.monthly.ui.formatAmount
import com.balance.monthly.ui.formatSigned
import com.balance.monthly.ui.formatWon
import com.balance.monthly.ui.theme.TabularNumbers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 바텀시트를 여는 요청. 새 기록은 처음 선택할 종류를, 수정은 대상 기록을 담는다. */
sealed interface EntrySheetRequest {
    data class New(val type: EntryType) : EntrySheetRequest
    data class Edit(val entry: LedgerEntry) : EntrySheetRequest
}

private val typeLabels = listOf(EntryType.INCOME to "수입", EntryType.EXPENSE to "지출", EntryType.BALANCE to "실제 잔고")
private val dateFormat = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntrySheet(
    request: EntrySheetRequest,
    entries: List<LedgerEntry>?,
    onSave: (EntryDraft) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val editing = (request as? EntrySheetRequest.Edit)?.entry
    val today = remember { LocalDate.now() }

    var type by rememberSaveable { mutableStateOf(editing?.type ?: (request as EntrySheetRequest.New).type) }
    var digits by rememberSaveable { mutableStateOf(editing?.amount?.let { Math.abs(it).toString() }.orEmpty()) }
    var negative by rememberSaveable { mutableStateOf((editing?.amount ?: 0) < 0) }
    var date by rememberSaveable { mutableStateOf(editing?.date ?: today) }
    var memo by rememberSaveable { mutableStateOf(editing?.memo.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val amount = draftAmount(digits, negative, type)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val amountFocus = remember { FocusRequester() }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .imePadding(),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                typeLabels.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = type == value,
                        onClick = { type = value },
                        shape = SegmentedButtonDefaults.itemShape(index, typeLabels.size),
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (type == EntryType.BALANCE) {
                    FilterChip(
                        selected = negative,
                        onClick = { negative = !negative },
                        label = { Text("−") },
                    )
                    Spacer(Modifier.width(8.dp))
                }
                AmountField(
                    digits = digits,
                    negative = negative && type == EntryType.BALANCE,
                    onDigitsChange = { digits = it },
                    onMinus = { if (type == EntryType.BALANCE) negative = !negative },
                    modifier = Modifier.weight(1f).focusRequester(amountFocus),
                )
            }

            if (type == EntryType.BALANCE && entries != null) {
                BalanceHint(entries, date, editing, amount)
            }
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("날짜", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showDatePicker = true }) {
                    Text(date.format(dateFormat) + if (date == today) " · 오늘" else "")
                }
            }

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모 (선택)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (editing != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("삭제", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    enabled = canSave(amount, type),
                    onClick = {
                        onSave(EntryDraft(editing?.id, type, amount!!, date, memo))
                        close()
                    },
                ) { Text("저장") }
            }
        }
    }

    LaunchedEffect(Unit) { amountFocus.requestFocus() }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = LocalDate.ofEpochDay(it / MILLIS_PER_DAY) }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } },
        ) {
            DatePicker(pickerState)
        }
    }

    if (confirmDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("기록을 삭제할까요?") },
            text = { Text("이후 잔고와 차액이 다시 계산됩니다.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete(editing.id)
                    close()
                }) { Text("삭제", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
        )
    }
}

/**
 * 숫자 키패드를 띄우고 입력 중 천 단위 쉼표를 보여준다. 커서는 항상 끝에 둔다.
 * 표시 문자열에는 '-'가 없으므로, 들어온 '-'는 키패드의 마이너스 키로 보고 부호를 바꾼다.
 */
@Composable
private fun AmountField(
    digits: String,
    negative: Boolean,
    onDigitsChange: (String) -> Unit,
    onMinus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = digits.toLongOrNull()?.let(::formatAmount).orEmpty()
    OutlinedTextField(
        value = TextFieldValue(shown, selection = TextRange(shown.length)),
        onValueChange = {
            if ('-' in it.text) onMinus()
            onDigitsChange(sanitizeDigits(it.text))
        },
        label = { Text("금액") },
        prefix = if (negative) ({ Text("−") }) else null,
        suffix = { Text("원") },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.merge(TabularNumbers),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

/** 실제 잔고 탭: 그 자리의 계산 잔고와 입력에 따른 예상 차액. */
@Composable
private fun BalanceHint(entries: List<LedgerEntry>, date: LocalDate, editing: LedgerEntry?, amount: Long?) {
    val prior = remember(entries, date, editing) { priorBalance(entries, date, editing) }
    val diff = amount?.let { it - prior.balance }
    val diffText = when {
        diff == null -> null
        !prior.hasCheckpoint -> "시작 잔고로 기록됩니다"
        diff > 0 -> "예상 차액: ${formatSigned(diff)}원 (미기록 수입)"
        diff < 0 -> "예상 차액: ${formatSigned(diff)}원 (미기록 지출)"
        else -> "차액 없음"
    }
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            "현재 계산 잔고: ${formatWon(prior.balance)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (diffText != null) {
            Text(diffText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
