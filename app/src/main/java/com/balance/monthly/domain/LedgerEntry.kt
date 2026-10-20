package com.balance.monthly.domain

import java.time.LocalDate

enum class EntryType { INCOME, EXPENSE, BALANCE }

/**
 * 계산에 쓰는 기록 한 건. Room 엔티티와 분리해 domain이 Android에 의존하지 않게 한다.
 * amount는 원 단위. INCOME/EXPENSE는 양수, BALANCE는 0이나 음수도 허용한다.
 */
data class LedgerEntry(
    val id: Long,
    val type: EntryType,
    val amount: Long,
    val date: LocalDate,
    val createdAt: Long,
    val memo: String? = null,
)
