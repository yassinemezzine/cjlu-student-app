package com.cjlu.studentapp.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/** Navigate to a main tab while preserving back-stack state (matches bottom bar + widget deep links). */
fun NavHostController.navigateToMainTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
