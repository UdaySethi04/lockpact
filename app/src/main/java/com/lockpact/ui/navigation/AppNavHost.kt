package com.lockpact.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lockpact.alerts.AlertsScreen
import com.lockpact.apps.MyAppsScreen
import com.lockpact.auth.LoginScreen
import com.lockpact.auth.SignupScreen
import com.lockpact.core.session.SessionManager
import com.lockpact.home.HomeScreen
import com.lockpact.locks.LocksScreen
import com.lockpact.pacts.CreatePactScreen
import com.lockpact.pacts.JoinPactScreen
import com.lockpact.pacts.PactDetailScreen
import com.lockpact.pacts.PactListScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object Pacts : Screen("pacts")
    object CreatePact : Screen("create_pact")
    object JoinPact : Screen("join_pact")
    object PactDetail : Screen("pact_detail/{pactId}") {
        fun createRoute(pactId: String) = "pact_detail/$pactId"
    }
    object MyApps : Screen("my_apps")
    object ActiveLocks : Screen("active_locks")
    object Alerts : Screen("alerts")
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    val scope = rememberCoroutineScope()
    fun navigateMain(route: String) {
        navController.navigate(route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPacts = { navController.navigate(Screen.Pacts.route) },
                onNavigateToMyApps = { navController.navigate(Screen.MyApps.route) },
                onNavigateToActiveLocks = { navController.navigate(Screen.ActiveLocks.route) },
                onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                onNavigateToPactDetail = { pactId ->
                    navController.navigate(Screen.PactDetail.createRoute(pactId))
                },
                currentRoute = Screen.Home.route,
                onBottomNavigate = { route -> navigateMain(route) },
                onLogout = {
                    scope.launch {
                        SessionManager.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.Pacts.route) {
            PactListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPactDetail = { pactId ->
                    navController.navigate(Screen.PactDetail.createRoute(pactId))
                },
                onNavigateToCreate = { navController.navigate(Screen.CreatePact.route) },
                onNavigateToJoin = { navController.navigate(Screen.JoinPact.route) },
                currentRoute = Screen.Pacts.route,
                onBottomNavigate = { route -> navigateMain(route) }
            )
        }

        composable(Screen.CreatePact.route) {
            CreatePactScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }
            )
        }

        composable(Screen.JoinPact.route) {
            JoinPactScreen(
                onBack = { navController.popBackStack() },
                onJoined = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PactDetail.route,
            arguments = listOf(navArgument("pactId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pactId = backStackEntry.arguments?.getString("pactId").orEmpty()
            PactDetailScreen(
                pactId = pactId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MyApps.route) {
            MyAppsScreen(
                currentRoute = Screen.MyApps.route,
                onNavigate = { route -> navigateMain(route) }
            )
        }

        composable(Screen.ActiveLocks.route) {
            LocksScreen(
                currentRoute = Screen.ActiveLocks.route,
                onNavigate = { route -> navigateMain(route) }
            )
        }

        composable(Screen.Alerts.route) {
            AlertsScreen(
                currentRoute = Screen.Alerts.route,
                onNavigate = { route -> navigateMain(route) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderMainScreen(
    title: String,
    body: String,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    androidx.compose.material3.Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleMedium) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            MainBottomBar(currentRoute = currentRoute, onNavigate = onNavigate)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            androidx.compose.material3.Surface(
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (title == "Alerts") "ALERTS" else title.uppercase(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (title == "Alerts") "No alerts yet." else body,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (title == "Alerts") FontWeight.Normal else FontWeight.Normal
                    )
                }
            }
        }
    }
}
