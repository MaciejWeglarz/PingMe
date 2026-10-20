package com.maciek.pingme.ui.alltasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciek.pingme.data.TaskRepository
import com.maciek.pingme.domain.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllTasksViewModel @Inject constructor(private val repository: TaskRepository) : ViewModel() {

    /** null, dopóki dane nie zostały wczytane z bazy. */
    val tasks: StateFlow<List<Task>?> = repository.observeActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(taskId: Long) {
        viewModelScope.launch { repository.archiveTask(taskId) }
    }
}
