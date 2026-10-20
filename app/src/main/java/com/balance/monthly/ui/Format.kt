package com.balance.monthly.ui

import com.balance.monthly.domain.Period
import java.time.LocalDate
import java.util.Locale

private const val MINUS = '−'

/** 1234567 → "1,234,567", −30000 → "−30,000" */
fun formatAmount(value: Long): String {
    val digits = "%,d".format(Locale.KOREA, value)
    return if (value < 0) MINUS + digits.removePrefix("-") else digits
}

fun formatWon(value: Long): String = formatAmount(value) + "원"

/** 수입·지출·차액 요약용. 0이 아니면 항상 부호를 붙인다. */
fun formatSigned(value: Long): String = if (value > 0) "+" + formatAmount(value) else formatAmount(value)

fun shortDate(date: LocalDate): String = "${date.monthValue}/${date.dayOfMonth}"

/** 시작일이 1이면 "2026년 10월", 아니면 "10/25 ~ 11/24" */
fun periodLabel(period: Period, startDay: Int): String =
    if (startDay == 1) "${period.start.year}년 ${period.start.monthValue}월"
    else "${shortDate(period.start)} ~ ${shortDate(period.end)}"
