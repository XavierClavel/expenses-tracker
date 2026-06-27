package com.xavierclavel.bankable.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.xavierclavel.bankable.R
import com.xavierclavel.bankable.expenses.ExpensesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubcategoryViewScreen(
    viewModel: CategoriesViewModel,
    expensesViewModel: ExpensesViewModel,
    navController: NavController,
) {
    val subcategory = viewModel.selectedSubcategory ?: return
    val expenses by viewModel.subcategoryExpenses.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subcategory.name) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.prepareEditSubcategory(subcategory)
                        navController.navigate("subcategory/edit")
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit_subcategory))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ExpenseHistoryList(
                expenses = expenses,
                isLoading = viewModel.isLoadingSubcategoryExpenses,
                emptyText = stringResource(R.string.no_expenses_in_subcategory),
                onLoadMore = { viewModel.loadMoreSubcategoryExpenses() },
                subcategoryFor = { subcategory },
                onExpenseClick = { expense ->
                    expensesViewModel.prepareEditExpense(expense, subcategory)
                    navController.navigate("expense/edit")
                },
            )
        }
    }
}
