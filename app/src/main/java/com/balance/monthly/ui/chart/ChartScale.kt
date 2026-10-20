package com.balance.monthly.ui.chart

import kotlin.math.abs

/**
 * y축 범위. 최소·최대에서 위아래로 10% 여유를 둔다.
 * 모든 값이 같으면 그 크기의 10%(최소 1,000원)만큼 여유를 둔다. 값이 없으면 null.
 */
fun chartRange(values: LongArray): ClosedFloatingPointRange<Double>? {
    if (values.isEmpty()) return null
    val min = values.min().toDouble()
    val max = values.max().toDouble()
    val span = max - min
    val padding = if (span > 0) span * 0.1 else maxOf(abs(max) * 0.1, 1_000.0)
    return (min - padding)..(max + padding)
}

/** 터치한 x 좌표에서 가장 가까운 날짜 인덱스. */
fun dayIndexAt(x: Float, left: Float, right: Float, dayCount: Int): Int {
    if (dayCount <= 1 || right <= left) return 0
    val fraction = ((x - left) / (right - left)).coerceIn(0f, 1f)
    return Math.round(fraction * (dayCount - 1))
}
