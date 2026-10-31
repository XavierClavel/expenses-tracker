package com.xavierclavel.bankable.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.constants.currencySymbol
import com.xavierclavel.bankable.constants.formatAmountDisplay
import com.xavierclavel.bankable.model.TagOut
import com.xavierclavel.bankable.platform.LocalAppLocale
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.cd_add_tag
import com.xavierclavel.bankable.resources.nav_tags
import com.xavierclavel.bankable.resources.tag_expense_count
import com.xavierclavel.bankable.resources.tag_list_subtitle
import com.xavierclavel.bankable.resources.tags_empty
import com.xavierclavel.bankable.ui.IconTile
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.NightFab
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource

@Composable
fun TagListScreen(
    viewModel: TagsViewModel,
    navController: NavController,
) {
    val tags by viewModel.tags.collectAsState()

    // Totals depend on expenses, which may have changed elsewhere; refresh on each visit.
    LaunchedEffect(Unit) { viewModel.loadTags() }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.nav_tags),
                subtitle = stringResource(Res.string.tag_list_subtitle, tags.size),
                navigationIcon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
        },
        floatingActionButton = {
            NightFab(
                icon = Icons.Default.Add,
                contentDescription = stringResource(Res.string.cd_add_tag),
                onClick = {
                    viewModel.prepareNewTag()
                    navController.navigate("tag/edit")
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                viewModel.isLoading && tags.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center), color = Mint)

                tags.isEmpty() ->
                    Text(
                        text = stringResource(Res.string.tags_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = NightTextSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(tags, key = { it.id }) { tag ->
                        TagRow(
                            tag = tag,
                            onClick = {
                                viewModel.prepareViewTag(tag)
                                navController.navigate("tag/view")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagRow(
    tag: TagOut,
    onClick: () -> Unit,
) {
    val locale = LocalAppLocale.current
    NightCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = Icons.Outlined.Sell, color = Mint)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tag.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = NightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.tag_expense_count, tag.expenseCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NightTextSecondary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${formatAmountDisplay(tag.total, locale)} ${currencySymbol("EUR")}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NightTextPrimary,
            )
        }
    }
}
