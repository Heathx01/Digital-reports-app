package com.example.digital_reports_app.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.digital_reports_app.ui.auth.LoginScreen
import com.example.digital_reports_app.ui.home.HomeScreen
import com.example.digital_reports_app.ui.profile.ProfileScreen
import com.example.digital_reports_app.ui.profile.UserManagementScreen
import com.example.digital_reports_app.ui.report.BitacoraScreen
import com.example.digital_reports_app.ui.report.ExportShareScreen
import com.example.digital_reports_app.ui.report.FormTemplateScreen
import com.example.digital_reports_app.ui.report.GuidedFormScreen
import com.example.digital_reports_app.ui.report.PdfViewerScreen

object Screen {
    const val LOGIN = "login"
    const val HOME = "home"
    const val REPORTS_LIST = "reports_list"
    const val CREATE_REPORT = "create_report"
    const val GUIDED_FORM = "guided_form?formId={formId}"
    const val BITACORA = "bitacora"
    const val EXPORT_SHARE = "export_share/{reportId}"
    const val PDF_VIEWER = "pdf_viewer/{reportId}"
    const val PROFILE = "profile"
    const val USER_MANAGEMENT = "user_management"

    fun guidedFormRoute(formId: String? = null) = if (!formId.isNullOrBlank()) "guided_form?formId=$formId" else "guided_form"
    fun exportShareRoute(reportId: String) = "export_share/$reportId"
    fun pdfViewerRoute(reportId: String) = "pdf_viewer/$reportId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.LOGIN) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.HOME) {
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.HOME) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(Screen.HOME) { inclusive = true }
                    }
                },
                onCreateReportClick = {
                    navController.navigate(Screen.CREATE_REPORT)
                },
                onReportClick = { reportId ->
                    navController.navigate(Screen.exportShareRoute(reportId))
                },
                onProfileClick = {
                    navController.navigate(Screen.PROFILE)
                },
                onBitacoraClick = {
                    navController.navigate(Screen.BITACORA)
                }
            )
        }
        composable(Screen.BITACORA) {
            BitacoraScreen(
                onReportClick = { reportId ->
                    navController.navigate(Screen.exportShareRoute(reportId))
                },
                onTabSelected = { route ->
                    when (route) {
                        Screen.HOME -> navController.navigate(Screen.HOME)
                        Screen.CREATE_REPORT -> navController.navigate(Screen.CREATE_REPORT)
                        Screen.PROFILE -> navController.navigate(Screen.PROFILE)
                        else -> {}
                    }
                }
            )
        }
        composable(Screen.CREATE_REPORT) {
            FormTemplateScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenGuidedForms = { formId ->
                    navController.navigate(Screen.guidedFormRoute(formId))
                },
                onTabSelected = { route ->
                    when (route) {
                        Screen.HOME -> navController.navigate(Screen.HOME)
                        Screen.BITACORA -> navController.navigate(Screen.BITACORA)
                        Screen.PROFILE -> navController.navigate(Screen.PROFILE)
                        else -> {}
                    }
                }
            )
        }
        composable(
            route = Screen.GUIDED_FORM,
            arguments = listOf(navArgument("formId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) {
            GuidedFormScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSaved = { reportId ->
                    navController.navigate(Screen.exportShareRoute(reportId)) {
                        popUpTo(Screen.CREATE_REPORT) { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = Screen.EXPORT_SHARE,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) {
            ExportShareScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenPdfViewer = { reportId ->
                    navController.navigate(Screen.pdfViewerRoute(reportId))
                }
            )
        }
        composable(
            route = Screen.PDF_VIEWER,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) {
            PdfViewerScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.USER_MANAGEMENT) {
            UserManagementScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.PROFILE) {
            ProfileScreen(
                onBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(Screen.HOME) { inclusive = true }
                    }
                },
                onOpenUserManagement = {
                    navController.navigate(Screen.USER_MANAGEMENT)
                },
                onTabSelected = { route ->
                    when (route) {
                        Screen.HOME -> navController.navigate(Screen.HOME)
                        Screen.CREATE_REPORT -> navController.navigate(Screen.CREATE_REPORT)
                        Screen.BITACORA -> navController.navigate(Screen.BITACORA)
                        else -> {}
                    }
                }
            )
        }
    }
}
