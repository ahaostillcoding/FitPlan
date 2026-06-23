package com.fitplan.app.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.fitplan.app.navigation.TopLevelDestination

@Composable
fun FitPlanScaffold(
    currentDestination: NavDestination?,
    onNavigateToTopLevel: (TopLevelDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentDestination
                            ?.hierarchy
                            ?.any { it.route == destination.route } == true,
                        onClick = { onNavigateToTopLevel(destination) },
                        icon = { Text(destination.iconLabel) },
                        label = { Text(destination.label) }
                    )
                }
            }
        },
        content = content
    )
}

