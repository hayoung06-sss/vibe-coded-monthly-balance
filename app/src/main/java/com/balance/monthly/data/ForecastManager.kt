package com.balance.monthly.data

import com.balance.monthly.domain.FORECAST_ALGORITHM_VERSION
import com.balance.monthly.domain.LedgerEntry
import com.balance.monthly.domain.computeTimeline
import com.balance.monthly.domain.forecast
import com.balance.monthly.domain.periodOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 잔고 예측을 앱 수명 코루틴에서 계산하고 forecast.bin에 캐시한다.
 * WorkManager 없이, 홈이 실제 데이터로 그려진 뒤에만 [start]로 시작한다.
 * 기록·달 시작일·날짜가 바뀌면 500ms 디바운스 후 다시 계산하고, 설정에서 끄면 계산하지 않는다.
 */
class ForecastManager(
    private val scope: CoroutineScope,
    private val repository: () -> EntryRepository,
    private val settings: Settings,
    private val store: () -> SnapshotStore,
    initial: CachedForecast?,
    private val clock: () -> LocalDate = LocalDate::now,
) {
    private val cached = MutableStateFlow(initial)
    private val today = MutableStateFlow(clock())
    private var job: Job? = null

    /** 마지막으로 계산한 예측. 현재 상태와 맞는지는 쓰는 쪽이 키로 확인한다. */
    val forecast: StateFlow<CachedForecast?> = cached.asStateFlow()

    fun refreshToday() {
        today.value = clock()
    }

    @OptIn(FlowPreview::class)
    @Synchronized
    fun start() {
        if (job != null) return
        job = scope.launch(Dispatchers.Default) {
            combine(
                flow { emitAll(repository().entries) },
                settings.monthStartDayFlow,
                settings.showForecastFlow,
                settings.dataVersionFlow,
                today,
            ) { entries, startDay, show, dataVersion, today -> Input(entries, startDay, show, dataVersion, today) }
                .debounce(DEBOUNCE_MILLIS)
                .collectLatest { input -> if (input.show) compute(input) }
        }
    }

    private fun compute(input: Input) {
        val current = cached.value
        if (current != null && current.matches(input.dataVersion, input.today, input.startDay)) return

        val balances = forecast(computeTimeline(input.entries), input.today, periodOf(input.today, input.startDay))
        if (balances == null) {
            cached.value = null
            store().clearForecast()
            return
        }
        val result = CachedForecast(input.dataVersion, input.today, input.startDay, FORECAST_ALGORITHM_VERSION, balances)
        cached.value = result
        store().writeForecast(result)
    }

    private class Input(
        val entries: List<LedgerEntry>,
        val startDay: Int,
        val show: Boolean,
        val dataVersion: Long,
        val today: LocalDate,
    )

    private companion object {
        const val DEBOUNCE_MILLIS = 500L
    }
}

/** 캐시 키가 현재 상태와 같은지. 다르면 이전 예측도 보여주지 않는다. */
fun CachedForecast.matches(dataVersion: Long, today: LocalDate, startDay: Int): Boolean =
    this.dataVersion == dataVersion &&
        computedOn == today &&
        monthStartDay == startDay &&
        algorithmVersion == FORECAST_ALGORITHM_VERSION
