package com.example.wildliferescuer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType

import com.example.wildliferescuer.screens.SplashScreen
import com.example.wildliferescuer.screens.LoginScreen
import com.example.wildliferescuer.screens.SignupScreen
import com.example.wildliferescuer.screens.HomeScreen
import com.example.wildliferescuer.screens.CreateCaseScreen
import com.example.wildliferescuer.screens.CasesScreen
import com.example.wildliferescuer.screens.RescueTeamListScreen
import com.example.wildliferescuer.screens.CaseChatScreen
import com.example.wildliferescuer.screens.AdminDashboardScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Signup.route) {
            SignupScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        //Home
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToCreateCase = { navController.navigate(Screen.CreateCase.route) },
                onNavigateToTeamMembers = { navController.navigate(Screen.RescueTeamList.route) },
                onNavigateToCases = { navController.navigate(Screen.Cases.route) },
                onNavigateToAdminDashboard = { navController.navigate(Screen.AdminDashboard.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        //Create case screen
        composable(route = Screen.CreateCase.route) {
            CreateCaseScreen(
                onBack = { navController.popBackStack() },
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        //View cases screen
        composable(route = Screen.Cases.route) {
            CasesScreen(
                onBack = { navController.popBackStack() },
                onOpenCase = { caseId ->
                    navController.navigate(Screen.CaseChat.createRoute(caseId))
                }
            )
        }

        //Rescue team members screen
        composable(route = Screen.RescueTeamList.route) {
            RescueTeamListScreen(
                onBack = { navController.popBackStack() }
            )
        }

        //Case chat screen
        composable(
            route = Screen.CaseChat.route,
            arguments = listOf(navArgument("caseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val caseId = backStackEntry.arguments?.getString("caseId") ?: return@composable
            CaseChatScreen(
                caseId = caseId,
                onBack = { navController.popBackStack() }
            )
        }

        //Admin Dashboard screen
        composable(route = Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onBack = { navController.popBackStack() },
                onOpenCase = { caseId ->
                    navController.navigate(Screen.CaseChat.createRoute(caseId))
                }
            )
        }

    }
}
