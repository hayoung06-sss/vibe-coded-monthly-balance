package com.balance.monthly.domain

/** 홈 그래프에 기준선으로 그리는 잔량(%). 위에서부터 75, 50, 25. */
val REMAINING_GUIDE_PERCENTS = listOf(75, 50, 25)

/**
 * 기간 잔량. [base]는 이번 기간에 쓸 수 있었던 돈(기간 시작 잔고 + 시작 잔고 차액 + 기간 수입)이고,
 * [balance]는 지금(지난 기간이면 기간 끝)의 잔고다. 잔량 = balance ÷ base.
 * 미기록 수입이 있으면 100%를 넘고, 잔고가 마이너스면 0% 미만이 될 수 있다.
 */
data class PeriodRemaining(val base: Long, val balance: Long) {
    val ratio: Double get() = balance.toDouble() / base

    val percent: Int get() = Math.round(ratio * 100).toInt()

    /** 잔량이 [percent]%일 때의 잔고. */
    fun balanceAtPercent(percent: Int): Long = Math.round(base * percent / 100.0)
}

/** 쓸 수 있었던 돈이 0 이하라 비율을 낼 수 없으면 null. */
fun periodRemaining(summary: PeriodSummary): PeriodRemaining? {
    val base = summary.startBalance + summary.opening + summary.income
    if (base <= 0) return null
    return PeriodRemaining(base, summary.endBalance)
}
