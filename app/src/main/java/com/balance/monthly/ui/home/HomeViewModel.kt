package com.balance.monthly.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.monthly.data.EntryRepository
import com.balance.monthly.data.ForecastManager
import com.balance.monthly.data.HomeSnapshot
import com.balance.monthly.data.Settings
import com.balance.monthly.data.SnapshotStore
import com.balance.monthly.domain.EntryWithBalance
import com.balance.monthly.domain.computeTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 첫 프레임은 [initialSnapshot]으로 그리고, 백그라운드에서 Room을 열어 계산한 결과로 바꾼다.
 * 실제 데이터로 그린 이번 기간은 다음 실행을 위해 스냅샷으로 저장한다.
 */
class HomeViewModel(
    repository: () -> EntryRepository,
    private val settings: Settings,
    private val snapshotStore: () -> SnapshotStore,
    private val initialSnapshot: HomeSnapshot?,
    private val forecastManager: ForecastManager,
    private val clock: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val today = MutableStateFlow(clock())
    private val startDay = settings.monthStartDayFlow
    private val offset = MutableStateFlow(0)
    private val loaded = MutableStateFlow(false)
    private var lastSaved: HomeSnapshot? = initialSnapshot

    /** 실제 데이터로 한 번 그려졌는지. reportFullyDrawn 시점이다. */
    val isLoaded: StateFlow<Boolean> = loaded.asStateFlow()

    init {
        // 달 시작일이 바뀌면 기간 경계가 달라지므로 이번 기간으로 돌아간다
        viewModelScope.launch { startDay.drop(1).collect { offset.value = 0 } }
    }

    /** 설정에서 켜져 있고 기록이 바뀌지 않았을 때만 예측을 넘긴다. 키의 나머지(날짜, 시작일)는 withForecast가 확인한다. */
    private val visibleForecast = combine(
        forecastManager.forecast,
        settings.showForecastFlow,
        settings.dataVersionFlow,
    ) { forecast, show, dataVersion -> if (show && forecast?.dataVersion == dataVersion) forecast else null }

    // Room은 여기서 처음 열린다. 메인 스레드를 막지 않도록 Default에서 접근한다.
    private val timeline = flow { emitAll(repository().entries) }
        .map<_, List<EntryWithBalance>?> { computeTimeline(it) }
        .onStart { emit(null) }

    val uiState: StateFlow<HomeUiState> = combine(timeline, today, startDay, offset, visibleForecast) { timeline, today, startDay, offset, forecast ->
        val state = if (timeline == null) {
            loadingState(today, startDay, offset)
        } else {
            homeState(timeline, today, startDay, offset).also {
                if (offset == 0) saveSnapshot(it, timeline.isEmpty(), today, startDay)
                if (!loaded.value) {
                    loaded.value = true
                    // 예측은 홈이 실제 데이터로 그려진 뒤에만 시작한다
                    forecastManager.start()
                }
            }
        }
        state.withForecast(forecast, settings.dataVersion, today, startDay)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            loadingState(today.value, startDay.value, offset.value)
                .withForecast(forecastManager.forecast.value.takeIf { settings.showForecast }, settings.dataVersion, today.value, startDay.value),
        )

    fun showPrevious() {
        if (uiState.value.canGoBack) offset.update { it - 1 }
    }

    fun showNext() {
        offset.update { minOf(0, it + 1) }
    }

    /** 앱을 켜둔 채 자정이 지났을 수 있으므로 화면에 돌아올 때마다 확인한다. */
    fun refreshToday() {
        today.value = clock()
        forecastManager.refreshToday()
    }

    private fun loadingState(today: LocalDate, startDay: Int, offset: Int): HomeUiState =
        (if (offset == 0) initialSnapshot?.toUiState(today, startDay) else null)
            ?: homeState(null, today, startDay, offset)

    private fun saveSnapshot(state: HomeUiState, isEmpty: Boolean, today: LocalDate, startDay: Int) {
        val snapshot = state.toSnapshot(settings.dataVersion, today, startDay)
        if (snapshot == lastSaved) return
        lastSaved = snapshot
        viewModelScope.launch(Dispatchers.IO) {
            if (snapshot == null) {
                if (isEmpty) snapshotStore().clearHome()
            } else {
                snapshotStore().writeHome(snapshot)
            }
        }
    }
}
