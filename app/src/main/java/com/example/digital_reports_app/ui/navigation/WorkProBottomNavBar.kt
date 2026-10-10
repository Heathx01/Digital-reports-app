package com.example.digital_reports_app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.digital_reports_app.ui.theme.liquidGlass

data class BottomNavItem(
    val route: String,
    val title: String,
    val iconText: String,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.HOME, "Inicio", "🏠"),
    BottomNavItem(Screen.BITACORA, "Reportes", "📋"),
    BottomNavItem(Screen.CREATE_REPORT, "Plantillas", "📄"),
    BottomNavItem(Screen.BITACORA, "Bitácora", "📓"),
    BottomNavItem(Screen.PROFILE, "Ajustes", "⚙️")
)

@Composable
fun WorkProBottomNavBar(
    currentRoute: String,
    onTabSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(cornerRadius = 0.dp, prominent = true)
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(item.route) },
                    icon = {
                        Text(
                            text = item.iconText,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}
