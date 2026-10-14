package com.example.wildliferescuer.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object CaseChat : Screen("case_chat/{caseId}") {
        fun createRoute(caseId: String) = "case_chat/$caseId"
    }
    object CreateCase : Screen("create_case")
    object Cases : Screen("cases")
    object RescueTeamList : Screen("rescue_team_list")
    object MemberDetail : Screen("member_detail/{memberName}")
    object AdminDashboard : Screen("admin_dashboard")
    object AdminCaseChat : Screen("admin_case_chat/{caseId}") {
        fun createRoute(caseId: String) = "admin_case_chat/$caseId"
    }


}
