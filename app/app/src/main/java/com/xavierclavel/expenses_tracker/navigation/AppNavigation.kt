package com.xavierclavel.expenses_tracker.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xavierclavel.expenses_tracker.R
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xavierclavel.expenses_tracker.auth.AuthState
import com.xavierclavel.expenses_tracker.auth.AuthViewModel
import com.xavierclavel.expenses_tracker.auth.LoginScreen
import com.xavierclavel.expenses_tracker.auth.SignupScreen
import com.xavierclavel.expenses_tracker.categories.CategoriesViewModel
import com.xavierclavel.expenses_tracker.categories.CategoryEditScreen
import com.xavierclavel.expenses_tracker.categories.CategoryListScreen
import com.xavierclavel.expenses_tracker.categories.ColorPickerScreen
import com.xavierclavel.expenses_tracker.categories.IconPickerScreen
import com.xavierclavel.expenses_tracker.categories.SubcategoryEditScreen
import com.xavierclavel.expenses_tracker.accounts.AccountEditScreen
import com.xavierclavel.expenses_tracker.accounts.AccountListScreen
import com.xavierclavel.expenses_tracker.accounts.AccountReportEditScreen
import com.xavierclavel.expenses_tracker.accounts.AccountViewScreen
import com.xavierclavel.expenses_tracker.accounts.AccountsViewModel
import com.xavierclavel.expenses_tracker.expenses.ExpenseEditScreen
import com.xavierclavel.expenses_tracker.expenses.ExpenseListScreen
import com.xavierclavel.expenses_tracker.expenses.ExpensesViewModel
import com.xavierclavel.expenses_tracker.expenses.SubcategoryPickerScreen
import com.xavierclavel.expenses_tracker.summary.SummaryScreen
import com.xavierclavel.expenses_tracker.summary.SummaryViewModel
import com.xavierclavel.expenses_tracker.trends.TrendsScreen
import com.xavierclavel.expenses_tracker.trends.TrendsViewModel

private val TOP_LEVEL_ROUTES = setOf("home", "categories", "summary", "trends", "accounts")

@Composable
fun AppNavigation(authViewModel: AuthViewModel = viewModel()) {
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        AuthState.Authenticated -> MainNavGraph(authViewModel)
        is AuthState.Unauthenticated -> AuthNavGraph(
            authViewModel,
            authState as AuthState.Unauthenticated,
        )
    }
}

@Composable
private fun AuthNavGraph(authViewModel: AuthViewModel, authState: AuthState.Unauthenticated) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                authState = authState,
                onLogin = { email, password -> authViewModel.login(email, password) },
                onGoogleSignIn = { idToken -> authViewModel.loginWithGoogle(idToken) },
                onNavigateToSignup = { navController.navigate("signup") },
            )
        }
        composable("signup") {
            SignupScreen(
                onSignup = { email, password, onSuccess, onError ->
                    authViewModel.signup(email, password, onSuccess, onError)
                },
                onGoogleSignIn = { idToken -> authViewModel.loginWithGoogle(idToken) },
                onNavigateToLogin = {
                    navController.navigate("login") { popUpTo("login") { inclusive = true } }
                },
            )
        }
    }
}

@Composable
private fun MainNavGraph(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val categoriesViewModel: CategoriesViewModel = viewModel()
    val expensesViewModel: ExpensesViewModel = viewModel()
    val accountsViewModel: AccountsViewModel = viewModel()
    val summaryViewModel: SummaryViewModel = viewModel()
    val trendsViewModel: TrendsViewModel = viewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in TOP_LEVEL_ROUTES) {
                BottomNavBar(navController, currentRoute)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable("home") {
                ExpenseListScreen(
                    expensesViewModel,
                    categoriesViewModel,
                    navController,
                    onLogout = { authViewModel.logout() },
                )
            }
            composable("expense/edit") {
                ExpenseEditScreen(expensesViewModel, navController)
            }
            composable("expense/subcategory-picker") {
                SubcategoryPickerScreen(expensesViewModel, categoriesViewModel, navController)
            }
            composable("categories") {
                CategoryListScreen(categoriesViewModel, navController)
            }
            composable("category/edit") {
                CategoryEditScreen(categoriesViewModel, navController)
            }
            composable("subcategory/edit") {
                SubcategoryEditScreen(categoriesViewModel, navController)
            }
            composable("color/picker") {
                ColorPickerScreen(categoriesViewModel, navController)
            }
            composable("icon/picker") {
                IconPickerScreen(categoriesViewModel, navController)
            }
            composable("summary") {
                SummaryScreen(summaryViewModel, categoriesViewModel, expensesViewModel, navController)
            }
            composable("trends") {
                TrendsScreen(trendsViewModel, categoriesViewModel)
            }
            composable("accounts") {
                AccountListScreen(accountsViewModel, navController)
            }
            composable("account/view") {
                AccountViewScreen(accountsViewModel, navController)
            }
            composable("account/edit") {
                AccountEditScreen(accountsViewModel, navController)
            }
            composable("account/report/edit") {
                AccountReportEditScreen(accountsViewModel, navController)
            }
        }
    }
}

@Composable
private fun BottomNavBar(navController: NavController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick = { navController.navigateTopLevel("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
        )
        NavigationBarItem(
            selected = currentRoute == "categories",
            onClick = { navController.navigateTopLevel("categories") },
            icon = { Icon(Icons.Default.Category, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_categories)) },
        )
        NavigationBarItem(
            selected = currentRoute == "summary",
            onClick = { navController.navigateTopLevel("summary") },
            icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_summary)) },
        )
        NavigationBarItem(
            selected = currentRoute == "trends",
            onClick = { navController.navigateTopLevel("trends") },
            icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_trends)) },
        )
        NavigationBarItem(
            selected = currentRoute == "accounts",
            onClick = { navController.navigateTopLevel("accounts") },
            icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_accounts)) },
        )
    }
}

private fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
