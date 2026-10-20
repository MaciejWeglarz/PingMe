package com.maciek.pingme.ui.taskform

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciek.pingme.data.TaskRepository
import com.maciek.pingme.domain.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TaskFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TaskRepository,
) : ViewModel() {

    /** ID edytowanego zadania z trasy edit_task/{taskId}; null przy dodawaniu. */
    private val taskId: Long? = savedStateHandle[ARG_TASK_ID]
    val isEdit = taskId != null

    private var existing: Task? = null

    /** null, dopóki edytowane zadanie nie zostanie wczytane z bazy. */
    private val _form = MutableStateFlow(if (taskId == null) TaskForm(startDate = LocalDate.now()) else null)
    val form: StateFlow<TaskForm?> = _form.asStateFlow()

    /** true po zapisaniu zadania: ekran wtedy się zamyka. */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private var saving = false

    init {
        if (taskId != null) {
            viewModelScope.launch {
                existing = repository.getTask(taskId)
                _form.value = existing?.toForm()
            }
        }
    }

    fun update(change: (TaskForm) -> TaskForm) = _form.update { it?.let(change) }

    fun save() {
        val form = _form.value ?: return
        val today = LocalDate.now()
        if (saving || form.validate(today).isNotEmpty()) return
        saving = true
        viewModelScope.launch {
            val task = form.toTask(existing, today)
            if (existing == null) repository.addTask(task) else repository.updateTask(task)
            _saved.value = true
        }
    }

    companion object {
        const val ARG_TASK_ID = "taskId"
    }
}
