package com.balance.monthly.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balance.monthly.data.EntryRepository
import com.balance.monthly.domain.EntryType
import com.balance.monthly.domain.LedgerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** 입력 바텀시트가 저장할 값. id가 null이면 새 기록. */
data class EntryDraft(
    val id: Long?,
    val type: EntryType,
    val amount: Long,
    val date: LocalDate,
    val memo: String?,
)

class EntryViewModel(repositoryProvider: () -> EntryRepository) : ViewModel() {

    // Room을 메인 스레드에서 열지 않도록 Default에서 처음 접근한다
    private val repository by lazy(repositoryProvider)

    /** 실제 잔고 힌트 계산용. 아직 읽지 못했으면 null. */
    val entries: StateFlow<List<LedgerEntry>?> = flow { emitAll(repository.entries) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun save(draft: EntryDraft) {
        viewModelScope.launch(Dispatchers.Default) {
            if (draft.id == null) {
                repository.add(draft.type, draft.amount, draft.date, draft.memo)
            } else {
                repository.update(draft.id, draft.type, draft.amount, draft.date, draft.memo)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch(Dispatchers.Default) { repository.delete(id) }
    }
}
