package com.example.digital_reports_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.digital_reports_app.ui.auth.LoginScreen
import com.example.digital_reports_app.ui.home.HomeScreen
import com.example.digital_reports_app.ui.report.CreateReportScreen
import com.example.digital_reports_app.ui.report.ReportDetailScreen

object Screen {
    const val LOGIN = "login"
    const val HOME = "home"
    const val CREATE_REPORT = "create_report"
    const val REPORT_DETAIL = "report_detail/{reportId}"

    fun reportDetailRoute(reportId: String) = "report_detail/$reportId"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
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
                    navController.navigate(Screen.reportDetailRoute(reportId))
                }
            )
        }
        composable(Screen.CREATE_REPORT) {
            CreateReportScreen(
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
                }
            )
        }
    }
}
