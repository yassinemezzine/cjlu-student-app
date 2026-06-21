package com.cjlu.studentapp.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import com.cjlu.core.resources.R
import com.cjlu.studentapp.navigation.Screen

data class BottomNavItem(
    val screen: Screen,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    companion object {
        val items = listOf(
            BottomNavItem(
                screen = Screen.Home,
                labelRes = R.string.nav_home,
                icon = Icons.Filled.Home
            ),
            BottomNavItem(
                screen = Screen.Services,
                labelRes = R.string.nav_services,
                icon = Icons.Filled.Widgets
            ),
            BottomNavItem(
                screen = Screen.Messages,
                labelRes = R.string.nav_messages,
                icon = Icons.Filled.Notifications
            ),
            BottomNavItem(
                screen = Screen.Profile,
                labelRes = R.string.nav_profile,
                icon = Icons.Filled.Person
            )
        )
    }
}
