package com.maciek.pingme.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.maciek.pingme.ui.addtask.AddTaskScreen
import com.maciek.pingme.ui.today.TodayScreen

private const val TODAY = "today"
private const val ADD_TASK = "add_task"

@Composable
fun PingMeNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = TODAY) {
        composable(TODAY) {
            TodayScreen(onAddClick = { navController.navigate(ADD_TASK) })
        }
        composable(ADD_TASK) {
            AddTaskScreen(onDone = { navController.popBackStack() })
        }
    }
}
