package com.example.digital_reports_app.ui.navigation

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
import com.example.digital_reports_app.ui.auth.BiometricConfirmScreen
import com.example.digital_reports_app.ui.auth.LoginScreen
import com.example.digital_reports_app.ui.home.HomeScreen
import com.example.digital_reports_app.ui.profile.ProfileScreen
import com.example.digital_reports_app.ui.report.BitacoraScreen
import com.example.digital_reports_app.ui.report.ExportShareScreen
import com.example.digital_reports_app.ui.report.FormTemplateScreen
import com.example.digital_reports_app.ui.report.ReportDetailScreen

object Screen {
    const val LOGIN = "login"
    const val BIOMETRIC = "biometric"
    const val HOME = "home"
    const val REPORTS_LIST = "reports_list"
    const val CREATE_REPORT = "create_report"
    const val BITACORA = "bitacora"
    const val REPORT_DETAIL = "report_detail/{reportId}"
    const val EXPORT_SHARE = "export_share/{reportId}"
    const val PROFILE = "profile"

    fun reportDetailRoute(reportId: String) = "report_detail/$reportId"
    fun exportShareRoute(reportId: String) = "export_share/$reportId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { 700 }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -700 }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -700 }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { 700 }) + fadeOut() }
    ) {
        composable(Screen.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.BIOMETRIC)
                }
            )
        }
        composable(Screen.BIOMETRIC) {
            BiometricConfirmScreen(
                onConfirmSuccess = {
                    navController.navigate(Screen.HOME) {
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
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
                    navController.navigate(Screen.reportDetailRoute(reportId))
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
                    navController.navigate(Screen.reportDetailRoute(reportId))
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
                }
            )
        }
        composable(
            route = Screen.REPORT_DETAIL,
            arguments = listOf(navArgument("reportId") { type = NavType.StringType })
        ) {
            ReportDetailScreen(
                onBack = {
                    navController.popBackStack()
                },
                onExportClick = { reportId ->
                    navController.navigate(Screen.exportShareRoute(reportId))
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
                }
            )
        }
    }
}
