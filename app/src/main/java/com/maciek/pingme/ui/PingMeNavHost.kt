package com.maciek.pingme.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.maciek.pingme.R
import com.maciek.pingme.ui.alltasks.AllTasksScreen
import com.maciek.pingme.ui.taskform.TaskFormScreen
import com.maciek.pingme.ui.taskform.TaskFormViewModel
import com.maciek.pingme.ui.today.TodayScreen

private const val TODAY = "today"
private const val ALL_TASKS = "all_tasks"
private const val ADD_TASK = "add_task"
private const val EDIT_TASK = "edit_task"

private enum class Tab(val route: String, @param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    TODAY_TAB(TODAY, R.string.tab_today, R.drawable.ic_today),
    ALL_TASKS_TAB(ALL_TASKS, R.string.tab_all, R.drawable.ic_list),
}

@Composable
fun PingMeNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        // Wcięcia na pasek statusu i systemowy obsługują Scaffoldy poszczególnych ekranów.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Pasek zakładek tylko na głównych ekranach, nie w formularzu.
            if (Tab.entries.any { it.route == currentRoute }) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController,
            startDestination = TODAY,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        ) {
            composable(TODAY) {
                TodayScreen(onAddClick = { navController.navigate(ADD_TASK) })
            }
            composable(ALL_TASKS) {
                AllTasksScreen(
                    onAddClick = { navController.navigate(ADD_TASK) },
                    onTaskClick = { taskId -> navController.navigate("$EDIT_TASK/$taskId") },
                )
            }
            composable(ADD_TASK) {
                TaskFormScreen(onDone = { navController.popBackStack() })
            }
            composable(
                "$EDIT_TASK/{${TaskFormViewModel.ARG_TASK_ID}}",
                arguments = listOf(navArgument(TaskFormViewModel.ARG_TASK_ID) { type = NavType.LongType }),
            ) {
                TaskFormScreen(onDone = { navController.popBackStack() })
            }
        }
    }
}

/** Przełącza zakładkę bez budowania długiego stosu ekranów i z zachowaniem stanu zakładek. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
