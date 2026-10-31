package com.xavierclavel.bankable.tags

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.categories.CategoriesViewModel
import com.xavierclavel.bankable.categories.ExpenseHistoryList
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.expenses.ExpensesViewModel
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.cd_edit_tag
import com.xavierclavel.bankable.resources.no_expenses_with_tag
import com.xavierclavel.bankable.resources.tag_expense_count
import com.xavierclavel.bankable.resources.tag_total
import com.xavierclavel.bankable.ui.CircleIconButton
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource

@Composable
fun TagViewScreen(
    tagsViewModel: TagsViewModel,
    expensesViewModel: ExpensesViewModel,
    categoriesViewModel: CategoriesViewModel,
    navController: NavController,
) {
    val selected = tagsViewModel.selectedTag ?: return
    // Track the up-to-date copy from the list so total/label edits reflect on return.
    val tags by tagsViewModel.tags.collectAsState()
    val tag = remember(tags, selected.id) { tags.find { it.id == selected.id } ?: selected }

    val expenses by tagsViewModel.tagExpenses.collectAsState()
    val categories by categoriesViewModel.categories.collectAsState()
    val subcategoryMap = remember(categories) {
        categories.flatMap { it.subcategories }.associateBy { it.id }
    }
    val locale = LocalAppLocale.current

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = tag.label,
                subtitle = stringResource(Res.string.tag_expense_count, tag.expenseCount),
                navigationIcon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
                leading = { IconTile(icon = Icons.Outlined.Sell, color = Mint, size = 52.dp, iconSize = 26.dp) },
                actions = {
                    CircleIconButton(
                        icon = Icons.Outlined.Edit,
                        contentDescription = stringResource(Res.string.cd_edit_tag),
                        onClick = {
                            tagsViewModel.prepareEditTag(tag)
                            navController.navigate("tag/edit")
                        },
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NightCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenGutter, vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.tag_total),
                        style = MaterialTheme.typography.bodyLarge,
                        color = NightTextSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${formatAmountDisplay(tag.total, locale)} ${currencySymbol("EUR")}",
                        style = MaterialTheme.typography.displaySmall,
                        color = NightTextPrimary,
                        maxLines = 1,
                    )
                }
            }

            ExpenseHistoryList(
                expenses = expenses,
                isLoading = tagsViewModel.isLoadingExpenses,
                emptyText = stringResource(Res.string.no_expenses_with_tag),
                onLoadMore = { tagsViewModel.loadMoreTagExpenses() },
                subcategoryFor = { subcategoryMap[it.categoryId] },
                onExpenseClick = { expense ->
                    expensesViewModel.prepareEditExpense(expense, subcategoryMap[expense.categoryId])
                    navController.navigate("expense/edit")
                },
                tags = tags,
                onBatchTag = { ids, tagId, add -> tagsViewModel.batchTagTagExpenses(ids, tagId, add) {} },
                onBatchDelete = { ids -> tagsViewModel.batchDeleteTagExpenses(ids) {} },
            )
        }
    }
}
