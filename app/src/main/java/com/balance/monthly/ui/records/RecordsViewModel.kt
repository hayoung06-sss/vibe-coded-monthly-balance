package com.balance.monthly.ui.records

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
import kotlinx.coroutines.launch
import java.time.LocalDate

class RecordsViewModel(
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

    private val timeline = flow { emitAll(repository().entries) }
        .map<_, List<EntryWithBalance>?> { computeTimeline(it) }
        .onStart { emit(null) }

    val uiState: StateFlow<RecordsUiState> = combine(timeline, today, startDay, offset, ::recordsState)
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            recordsState(null, today.value, startDay.value, offset.value),
        )

    fun showPrevious() {
        if (uiState.value.canGoBack) offset.value -= 1
    }

    fun showNext() {
        if (uiState.value.canGoForward) offset.value += 1
    }

    fun refreshToday() {
        today.value = clock()
    }
}
