package com.balance.monthly.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.monthly.data.EntryRepository
import com.balance.monthly.data.Settings
import com.balance.monthly.domain.EntryWithBalance
import com.balance.monthly.domain.computeTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

class HomeViewModel(
    repository: () -> EntryRepository,
    settings: Settings,
    private val clock: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val today = MutableStateFlow(clock())
    private val startDay = settings.monthStartDayFlow
    private val offset = MutableStateFlow(0)

    init {
        // 달 시작일이 바뀌면 기간 경계가 달라지므로 이번 기간으로 돌아간다
        viewModelScope.launch { startDay.drop(1).collect { offset.value = 0 } }
    }

    // Room은 여기서 처음 열린다. 메인 스레드를 막지 않도록 Default에서 접근한다.
    private val timeline = flow { emitAll(repository().entries) }
        .map<_, List<EntryWithBalance>?> { computeTimeline(it) }
        .onStart { emit(null) }

    val uiState: StateFlow<HomeUiState> = combine(timeline, today, startDay, offset, ::homeState)
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            homeState(null, today.value, startDay.value, offset.value),
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
    }
}
