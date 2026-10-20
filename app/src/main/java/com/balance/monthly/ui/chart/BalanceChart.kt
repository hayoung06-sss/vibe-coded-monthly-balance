package com.balance.monthly.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.balance.monthly.ui.formatWon
import com.balance.monthly.ui.shortDate
import java.time.LocalDate

/**
 * 한 기간의 그래프 데이터.
 * balances는 첫날부터 오늘(지난 기간이면 마지막 날)까지의 일별 잔고이고, 나머지 날은 여백으로 남는다.
 * todayIndex가 null이면 지난 기간이다.
 */
@Immutable
class BalanceChartData(
    val start: LocalDate,
    val dayCount: Int,
    val balances: LongArray,
    val markers: IntArray,
    val todayIndex: Int?,
)

@Composable
fun BalanceChart(data: BalanceChartData, modifier: Modifier = Modifier) {
    var selected by remember(data) { mutableStateOf<Int?>(null) }
    val colors = ChartColors(
        line = MaterialTheme.colorScheme.primary,
        guide = MaterialTheme.colorScheme.outline,
        background = MaterialTheme.colorScheme.background,
        highlight = MaterialTheme.colorScheme.onSurface,
    )

    Column(modifier) {
        Text(
            text = selected?.let { "${shortDate(data.start.plusDays(it.toLong()))}  ${formatWon(data.balances[it])}" }.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = colors.line,
            modifier = Modifier.height(24.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics { contentDescription = "잔고 그래프" }
                .pointerInput(data) {
                    detectTapGestures { position ->
                        val inset = HorizontalInset.toPx()
                        val index = dayIndexAt(position.x, inset, size.width - inset, data.dayCount)
                        selected = if (index in data.balances.indices && index != selected) index else null
                    }
                },
        ) {
            drawChart(data, selected, colors)
        }
    }
}

private class ChartColors(val line: Color, val guide: Color, val background: Color, val highlight: Color)

private val HorizontalInset = 10.dp
private val VerticalInset = 12.dp

private fun DrawScope.drawChart(data: BalanceChartData, selected: Int?, colors: ChartColors) {
    val left = HorizontalInset.toPx()
    val right = size.width - left
    val top = VerticalInset.toPx()
    val bottom = size.height - top
    val step = if (data.dayCount > 1) (right - left) / (data.dayCount - 1) else 0f
    val range = chartRange(data.balances)

    fun x(index: Int) = left + step * index
    fun y(value: Long): Float {
        if (range == null) return (top + bottom) / 2
        val fraction = (range.endInclusive - value) / (range.endInclusive - range.start)
        return (top + (bottom - top) * fraction).toFloat()
    }

    val thin = 1.dp.toPx()
    drawLine(colors.guide.copy(alpha = 0.4f), Offset(left, bottom), Offset(right, bottom), thin)
    if (range != null && 0.0 in range) {
        drawLine(colors.guide.copy(alpha = 0.8f), Offset(left, y(0)), Offset(right, y(0)), thin)
    }
    data.todayIndex?.let { today ->
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        drawLine(colors.guide, Offset(x(today), top), Offset(x(today), bottom), thin, pathEffect = dash)
    }

    val balances = data.balances
    if (balances.size >= 2) {
        val path = Path().apply {
            moveTo(x(0), y(balances[0]))
            for (i in 1 until balances.size) lineTo(x(i), y(balances[i]))
        }
        drawPath(path, colors.line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    for (marker in data.markers) {
        val center = Offset(x(marker), y(balances[marker]))
        drawCircle(colors.background, 5.dp.toPx(), center)
        drawCircle(colors.line, 3.dp.toPx(), center)
    }

    if (data.todayIndex != null && balances.isNotEmpty()) {
        val center = Offset(x(balances.lastIndex), y(balances.last()))
        drawCircle(colors.line.copy(alpha = 0.25f), 10.dp.toPx(), center)
        drawCircle(colors.line, 5.dp.toPx(), center)
    }

    selected?.let { index ->
        val center = Offset(x(index), y(balances[index]))
        drawLine(colors.guide, Offset(center.x, top), Offset(center.x, bottom), thin)
        drawCircle(colors.background, 6.dp.toPx(), center)
        drawCircle(colors.highlight, 4.dp.toPx(), center)
    }
}
