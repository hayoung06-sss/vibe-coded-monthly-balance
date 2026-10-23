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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.balance.monthly.domain.PeriodRemaining
import com.balance.monthly.domain.REMAINING_GUIDE_PERCENTS
import com.balance.monthly.ui.formatWon
import com.balance.monthly.ui.shortDate
import java.time.LocalDate
import kotlin.math.abs

/**
 * 한 기간의 그래프 데이터.
 * balances는 첫날부터 오늘(지난 기간이면 마지막 날)까지의 일별 잔고이고, 나머지 날은 여백으로 남는다.
 * firstIndex 앞은 첫 기록 이전이라 그리지 않는다. todayIndex가 null이면 지난 기간이다.
 * forecast는 오늘(todayIndex)부터 기간 마지막 날까지의 예측 잔고이고, 없으면 그리지 않는다.
 * lineBalances는 선을 그리는 값으로, 기록한 날 사이를 직선으로 이은 잔고다(누르면 보이는 금액은 balances).
 * remaining이 있으면 잔량 75·50·25% 기준선과 현재 잔량 선분을 그린다.
 */
@Immutable
class BalanceChartData(
    val start: LocalDate,
    val dayCount: Int,
    val balances: LongArray,
    val markers: IntArray,
    val todayIndex: Int?,
    val firstIndex: Int = 0,
    val forecast: LongArray? = null,
    val remaining: PeriodRemaining? = null,
    val lineBalances: LongArray = balances,
) {
    /** 실제로 선을 그리는 날들. */
    val drawnIndices: IntRange get() = firstIndex until balances.size

    fun withForecast(forecast: LongArray?) =
        BalanceChartData(start, dayCount, balances, markers, todayIndex, firstIndex, forecast, remaining, lineBalances)
}

@Composable
fun BalanceChart(data: BalanceChartData, modifier: Modifier = Modifier) {
    var selected by remember(data) { mutableStateOf<Int?>(null) }
    val colors = ChartColors(
        line = MaterialTheme.colorScheme.primary,
        guide = MaterialTheme.colorScheme.outline,
        background = MaterialTheme.colorScheme.background,
        highlight = MaterialTheme.colorScheme.onSurface,
        remaining = MaterialTheme.colorScheme.tertiary,
    )
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall

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
                        selected = if (index in data.drawnIndices && index != selected) index else null
                    }
                },
        ) {
            drawChart(data, selected, colors, textMeasurer, labelStyle)
        }
    }
}

private class ChartColors(
    val line: Color,
    val guide: Color,
    val background: Color,
    val highlight: Color,
    val remaining: Color,
)

private val HorizontalInset = 10.dp
private val VerticalInset = 12.dp

/** 기준선과 잔량 선이 이 거리(px)보다 가까우면 같은 선으로 보고 겹치는 구간을 한 번만 그린다. */
private const val OVERLAP_PX = 1.5f

private fun DrawScope.drawChart(
    data: BalanceChartData,
    selected: Int?,
    colors: ChartColors,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    val left = HorizontalInset.toPx()
    val right = size.width - left
    val top = VerticalInset.toPx()
    val bottom = size.height - top
    val step = if (data.dayCount > 1) (right - left) / (data.dayCount - 1) else 0f
    val drawn = data.drawnIndices
    val remaining = data.remaining?.takeIf { !drawn.isEmpty() }
    val guides = remaining?.let { r -> REMAINING_GUIDE_PERCENTS.map { it to r.balanceAtPercent(it) } }.orEmpty()
    // y축 범위는 예측과 잔량 기준선까지 포함해 세 줄이 항상 보이게 한다
    val range = chartRange(
        data.lineBalances.copyOfRange(drawn.first, drawn.last + 1) +
            (data.forecast ?: LongArray(0)) +
            guides.map { it.second }.toLongArray(),
    )

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

    val labels = if (remaining != null) {
        drawRemaining(remaining, guides, x(drawn.first), x(drawn.last), left, right, ::y, colors, textMeasurer, labelStyle)
    } else {
        emptyList()
    }

    val balances = data.lineBalances
    val forecast = data.forecast
    val todayIndex = data.todayIndex
    if (forecast != null && todayIndex != null && forecast.size >= 2) {
        val path = Path().apply {
            moveTo(x(todayIndex), y(forecast[0]))
            for (i in 1 until forecast.size) lineTo(x(todayIndex + i), y(forecast[i]))
        }
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
        drawPath(path, colors.line.copy(alpha = 0.35f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash))
    }

    if (drawn.count() >= 2) {
        val path = Path().apply {
            moveTo(x(drawn.first), y(balances[drawn.first]))
            for (i in drawn.first + 1..drawn.last) lineTo(x(i), y(balances[i]))
        }
        drawPath(path, colors.line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    } else if (drawn.count() == 1 && data.todayIndex == null) {
        // 지난 기간의 마지막 날에만 기록이 있으면 점 하나로 보여준다
        drawCircle(colors.line, 3.dp.toPx(), Offset(x(drawn.first), y(balances[drawn.first])))
    }

    for (marker in data.markers) {
        val center = Offset(x(marker), y(balances[marker]))
        drawCircle(colors.background, 5.dp.toPx(), center)
        drawCircle(colors.line, 3.dp.toPx(), center)
    }

    if (data.todayIndex != null && !drawn.isEmpty()) {
        val center = Offset(x(balances.lastIndex), y(balances.last()))
        drawCircle(colors.line.copy(alpha = 0.25f), 10.dp.toPx(), center)
        drawCircle(colors.line, 5.dp.toPx(), center)
    }

    // 라벨은 모든 선 위에 배경을 깔고 그려, 꺾은선이나 예측선이 지나가도 읽히게 한다
    val pad = 2.dp.toPx()
    for (label in labels) {
        drawRect(
            colors.background.copy(alpha = 0.85f),
            topLeft = label.topLeft - Offset(pad, 0f),
            size = label.layout.size.toSize().let { Size(it.width + pad * 2, it.height) },
        )
        drawText(label.layout, topLeft = label.topLeft)
    }

    selected?.let { index ->
        val center = Offset(x(index), y(balances[index]))
        drawLine(colors.guide, Offset(center.x, top), Offset(center.x, bottom), thin)
        drawCircle(colors.background, 6.dp.toPx(), center)
        drawCircle(colors.highlight, 4.dp.toPx(), center)
    }
}

private class ChartLabel(val layout: TextLayoutResult, val topLeft: Offset)

/**
 * 잔량 75·50·25% 기준선(전체 폭, 점선)과 현재 잔량 선분(꺾은선이 있는 구간만, 실선).
 * 잔량이 기준선과 같은 높이면 기준선은 선분과 겹치는 구간을 빼고 그리고, 라벨이 겹치면 기준선 라벨을 숨긴다.
 * 라벨은 다른 선 위에 그리도록 돌려준다.
 */
private fun DrawScope.drawRemaining(
    remaining: PeriodRemaining,
    guides: List<Pair<Int, Long>>,
    segmentStart: Float,
    segmentEnd: Float,
    left: Float,
    right: Float,
    y: (Long) -> Float,
    colors: ChartColors,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
): List<ChartLabel> {
    val thin = 1.dp.toPx()
    val gap = 3.dp.toPx()
    val guideColor = colors.guide.copy(alpha = 0.7f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
    val currentY = y(remaining.balance)

    // 잔량 라벨: 선분 왼쪽 끝, 선 바로 위. 위나 오른쪽이 잘리면 선 아래, 안쪽으로 옮긴다.
    val currentLabel = textMeasurer.measure("잔량 ${remaining.percent}%", labelStyle.copy(color = colors.remaining))
    val labelAboveY = currentY - gap - currentLabel.size.height
    val currentLabelTopLeft = Offset(
        x = minOf(segmentStart, right - currentLabel.size.width),
        y = if (labelAboveY >= 0f) labelAboveY else currentY + gap,
    )
    val currentLabelRect = Rect(currentLabelTopLeft, currentLabel.size.toSize())
    val labels = mutableListOf(ChartLabel(currentLabel, currentLabelTopLeft))

    for ((percent, balance) in guides) {
        val guideY = y(balance)
        if (abs(guideY - currentY) < OVERLAP_PX) {
            if (segmentStart > left) drawLine(guideColor, Offset(left, guideY), Offset(segmentStart, guideY), thin, pathEffect = dash)
            if (segmentEnd < right) drawLine(guideColor, Offset(segmentEnd, guideY), Offset(right, guideY), thin, pathEffect = dash)
        } else {
            drawLine(guideColor, Offset(left, guideY), Offset(right, guideY), thin, pathEffect = dash)
        }

        // 기준선 라벨: 오른쪽 끝, 선 바로 위
        val label = textMeasurer.measure("$percent%", labelStyle.copy(color = colors.guide))
        val topLeft = Offset(right - label.size.width, guideY - gap - label.size.height)
        if (!Rect(topLeft, label.size.toSize()).overlaps(currentLabelRect)) labels += ChartLabel(label, topLeft)
    }

    drawLine(colors.remaining, Offset(segmentStart, currentY), Offset(segmentEnd, currentY), 1.5.dp.toPx(), cap = StrokeCap.Round)
    return labels
}
