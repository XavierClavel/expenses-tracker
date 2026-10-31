package com.xavierclavel.bankable.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.xavierclavel.bankable.expenses.ExpensesViewModel
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.cat_default_subcategory
import com.xavierclavel.bankable.resources.cd_edit_subcategory
import com.xavierclavel.bankable.resources.no_expenses_in_subcategory
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.theme.NightBackground
import org.jetbrains.compose.resources.stringResource

@Composable
fun SubcategoryViewScreen(
    viewModel: CategoriesViewModel,
    expensesViewModel: ExpensesViewModel,
    tagsViewModel: TagsViewModel,
    navController: NavController,
) {
    val subcategory = viewModel.selectedSubcategory ?: return
    val expenses by viewModel.subcategoryExpenses.collectAsState()
    val tags by tagsViewModel.tags.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val parent = remember(categories, subcategory.id) {
        categories.find { cat -> cat.subcategories.any { it.id == subcategory.id } }
    }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = if (subcategory.isDefault && parent != null) parent.name else subcategory.name,
                subtitle = when {
                    parent == null -> null
                    subcategory.isDefault -> stringResource(Res.string.cat_default_subcategory, parent.name)
                    else -> parent.name
                },
                navigationIcon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
                actions = {
                    CircleIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = stringResource(Res.string.cd_edit_subcategory),
                        onClick = {
                            viewModel.prepareEditSubcategory(subcategory)
                            navController.navigate("subcategory/edit")
                        },
                    )
                },
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
                emptyText = stringResource(Res.string.no_expenses_in_subcategory),
                onLoadMore = { viewModel.loadMoreSubcategoryExpenses() },
                subcategoryFor = { subcategory },
                onExpenseClick = { expense ->
                    expensesViewModel.prepareEditExpense(expense, subcategory)
                    navController.navigate("expense/edit")
                },
                tags = tags,
                onBatchTag = { ids, tagId, add -> viewModel.batchTagSubcategoryExpenses(ids, tagId, add) },
                onBatchDelete = { ids -> viewModel.batchDeleteSubcategoryExpenses(ids) },
            )
        }
    }
}
