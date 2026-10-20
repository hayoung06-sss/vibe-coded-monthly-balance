package com.balance.monthly.ui.entry

import com.balance.monthly.data.EntryRepository
import com.balance.monthly.domain.EntryType

/** 금액 입력 칸에 남길 숫자. 숫자 외 문자는 버리고 앞의 0을 지우며 13자리로 자른다. */
fun sanitizeDigits(input: String): String =
    input.filter { it in '0'..'9' }.trimStart('0').take(MAX_DIGITS).ifEmpty { if (input.any { it == '0' }) "0" else "" }

/** 입력 칸 숫자와 음수 여부로 금액을 만든다. 음수는 실제 잔고에서만 허용한다. 비어 있으면 null. */
fun draftAmount(digits: String, negative: Boolean, type: EntryType): Long? {
    val value = digits.toLongOrNull() ?: return null
    return if (negative && type == EntryType.BALANCE) -value else value
}

/** 수입·지출은 0원과 빈 금액을 막고, 실제 잔고는 0원을 허용한다. */
fun canSave(amount: Long?, type: EntryType): Boolean = when {
    amount == null -> false
    type == EntryType.BALANCE -> true
    else -> amount > 0
}

private val MAX_DIGITS = EntryRepository.MAX_AMOUNT.toString().length
