package com.xavierclavel.expenses_tracker.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.xavierclavel.expenses_tracker.expenses.ExpenseEditScreen
import com.xavierclavel.expenses_tracker.expenses.ExpenseListScreen
import com.xavierclavel.expenses_tracker.expenses.ExpensesViewModel
import com.xavierclavel.expenses_tracker.expenses.SubcategoryPickerScreen

private val TOP_LEVEL_ROUTES = setOf("home", "categories", "profile")

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
                onNavigateToSignup = { navController.navigate("signup") },
            )
        }
        composable("signup") {
            SignupScreen(
                onSignup = { email, password, onSuccess, onError ->
                    authViewModel.signup(email, password, onSuccess, onError)
                },
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
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                ExpenseListScreen(expensesViewModel, categoriesViewModel, navController)
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
            composable("profile") {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Profile")
                }
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
            label = { Text("Home") },
        )
        NavigationBarItem(
            selected = currentRoute == "categories",
            onClick = { navController.navigateTopLevel("categories") },
            icon = { Icon(Icons.Default.Category, contentDescription = null) },
            label = { Text("Categories") },
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { navController.navigateTopLevel("profile") },
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile") },
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
