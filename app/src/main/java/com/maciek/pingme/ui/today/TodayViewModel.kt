package com.maciek.pingme.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciek.pingme.data.TaskRepository
import com.maciek.pingme.domain.DayTask
import com.maciek.pingme.domain.Mood
import com.maciek.pingme.domain.dayMood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** tasks = null, dopóki dane nie zostały wczytane z bazy. */
data class TodayState(val date: LocalDate, val tasks: List<DayTask>?, val mood: Mood = Mood.NEUTRAL)

@HiltViewModel
class TodayViewModel @Inject constructor(private val repository: TaskRepository) : ViewModel() {

    private val today = MutableStateFlow(LocalDate.now())

    /** Bieżąca godzina co minutę: maskotka posmutnieje, gdy minie deadline, także przy otwartej aplikacji. */
    private val clock = flow {
        while (true) {
            emit(LocalTime.now())
            delay(60_000)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TodayState> = today
        .flatMapLatest { date ->
            combine(repository.observeDay(date), repository.observeAnySkipped(date.minusDays(1)), clock) { tasks, skippedYesterday, now ->
                TodayState(date, tasks, dayMood(tasks, skippedYesterday, now))
            }
        }
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
