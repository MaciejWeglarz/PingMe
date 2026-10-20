package com.maciek.pingme.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciek.pingme.data.TaskRepository
import com.maciek.pingme.domain.TaskStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

data class StatsState(val month: YearMonth, val stats: List<TaskStats>) {
    /** W przyszłe miesiące nie przechodzimy. */
    val canGoNext: Boolean get() = month < YearMonth.now()
}

@HiltViewModel
class StatsViewModel @Inject constructor(private val repository: TaskRepository) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    /** null, dopóki dane nie zostały wczytane z bazy. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<StatsState?> = month
        .flatMapLatest { m -> repository.observeMonthlyStats(m).map { StatsState(m, it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { if (it < YearMonth.now()) it.plusMonths(1) else it }
}
