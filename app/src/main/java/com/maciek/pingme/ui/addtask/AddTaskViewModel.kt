package com.maciek.pingme.ui.addtask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciek.pingme.data.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddTaskViewModel @Inject constructor(private val repository: TaskRepository) : ViewModel() {

    private val _form = MutableStateFlow(TaskForm(startDate = LocalDate.now()))
    val form: StateFlow<TaskForm> = _form.asStateFlow()

    /** true po zapisaniu zadania: ekran wtedy się zamyka. */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private var saving = false

    fun update(change: (TaskForm) -> TaskForm) = _form.update(change)

    fun save() {
        val form = _form.value
        if (saving || form.validate(LocalDate.now()).isNotEmpty()) return
        saving = true
        viewModelScope.launch {
            repository.addTask(form.toTask())
            _saved.value = true
        }
    }
}
