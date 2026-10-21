package com.maciek.pingme.ui.alltasks

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maciek.pingme.R
import com.maciek.pingme.data.BackupManager
import com.maciek.pingme.data.TaskRepository
import com.maciek.pingme.domain.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllTasksViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    /** null, dopóki dane nie zostały wczytane z bazy. */
    val tasks: StateFlow<List<Task>?> = repository.observeActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Komunikat do pokazania w Snackbarze; null, gdy nie ma żadnego. */
    val message = MutableStateFlow<Int?>(null)

    fun delete(taskId: Long) {
        viewModelScope.launch { repository.archiveTask(taskId) }
    }

    fun export(uri: Uri) = runBackup(R.string.backup_exported, R.string.backup_export_failed) { backupManager.export(uri) }

    fun import(uri: Uri) = runBackup(R.string.backup_imported, R.string.backup_import_failed) { backupManager.import(uri) }

    fun messageShown() {
        message.value = null
    }

    private fun runBackup(@StringRes success: Int, @StringRes failure: Int, action: suspend () -> Unit) {
        viewModelScope.launch {
            message.value = try {
                action()
                success
            } catch (e: Exception) {
                failure
            }
        }
    }
}
