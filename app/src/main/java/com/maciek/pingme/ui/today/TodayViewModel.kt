package com.maciek.pingme.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciek.pingme.data.TaskRepository
import com.maciek.pingme.domain.DayTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** tasks = null, dopóki dane nie zostały wczytane z bazy. */
data class TodayState(val date: LocalDate, val tasks: List<DayTask>?)

@HiltViewModel
class TodayViewModel @Inject constructor(private val repository: TaskRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TodayState> = today
        .flatMapLatest { date -> repository.observeDay(date).map { TodayState(date, it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState(today.value, null))

    /** Wywoływane przy każdym powrocie do aplikacji: po północy przełącza listę na nowy dzień. */
    fun refresh() {
        today.value = LocalDate.now()
    }

    fun setDone(taskId: Long, done: Boolean) {
        val date = today.value
        viewModelScope.launch { repository.setDone(taskId, date, done) }
    }
}
