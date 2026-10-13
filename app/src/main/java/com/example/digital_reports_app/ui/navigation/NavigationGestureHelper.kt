package com.example.digital_reports_app.ui.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.horizontalTabSwipeGesture(
    currentRoute: String,
    onNavigateToTab: (String) -> Unit
): Modifier {
    val tabOrder = listOf(Screen.HOME, Screen.CREATE_REPORT, Screen.BITACORA, Screen.PROFILE)
    val currentIndex = tabOrder.indexOf(currentRoute)

    if (currentIndex == -1) return this

    return this.pointerInput(currentRoute) {
        var dragAmountX = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragAmountX = 0f },
            onDragEnd = {
                if (dragAmountX < -120f && currentIndex < tabOrder.size - 1) {
                    onNavigateToTab(tabOrder[currentIndex + 1])
                } else if (dragAmountX > 120f && currentIndex > 0) {
                    onNavigateToTab(tabOrder[currentIndex - 1])
                }
            },
            onHorizontalDrag = { _, dragAmount ->
                dragAmountX += dragAmount
            }
        )
    }
}
