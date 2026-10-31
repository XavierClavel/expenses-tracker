package com.xavierclavel.bankable.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.action_back
import com.xavierclavel.bankable.resources.expenses_done
import com.xavierclavel.bankable.resources.expenses_tag_picker_helper
import com.xavierclavel.bankable.resources.expenses_tag_picker_subtitle
import com.xavierclavel.bankable.resources.screen_select_tags
import com.xavierclavel.bankable.resources.tags_empty
import com.xavierclavel.bankable.tags.TagsViewModel
import com.xavierclavel.bankable.ui.CheckDot
import com.xavierclavel.bankable.ui.NightCard
import com.xavierclavel.bankable.ui.PrimaryButton
import com.xavierclavel.bankable.ui.ScreenGutter
import com.xavierclavel.bankable.ui.ScreenHeader
import com.xavierclavel.bankable.ui.theme.Mint
import com.xavierclavel.bankable.ui.theme.NightBackground
import com.xavierclavel.bankable.ui.theme.NightTextPrimary
import com.xavierclavel.bankable.ui.theme.NightTextSecondary
import org.jetbrains.compose.resources.stringResource

@Composable
fun TagPickerScreen(
    selectedIds: Set<Int>,
    onToggle: (Int) -> Unit,
    tagsViewModel: TagsViewModel,
    navController: NavController,
) {
    val tags by tagsViewModel.tags.collectAsState()
    val selectedCount = tags.count { selectedIds.contains(it.id) }

    Scaffold(
        containerColor = NightBackground,
        topBar = {
            ScreenHeader(
                title = stringResource(Res.string.screen_select_tags),
                subtitle = stringResource(Res.string.expenses_tag_picker_subtitle, selectedCount),
                navigationIcon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                navigationContentDescription = stringResource(Res.string.action_back),
                onNavigate = { navController.popBackStack() },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(NightBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = ScreenGutter, vertical = 16.dp),
            ) {
                PrimaryButton(
                    text = stringResource(Res.string.expenses_done),
                    onClick = { navController.popBackStack() },
                )
            }
        },
    ) { padding ->
        if (tags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(Res.string.tags_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NightTextSecondary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(tags, key = { it.id }) { tag ->
                    val assigned = selectedIds.contains(tag.id)
                    NightCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onToggle(tag.id) },
                        selected = assigned,
                        contentPadding = PaddingValues(horizontal = 18.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 62.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sell,
                                contentDescription = null,
                                tint = if (assigned) Mint else NightTextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = tag.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NightTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            CheckDot(checked = assigned)
                        }
                    }
                }
                item(key = "helper") {
                    Text(
                        text = stringResource(Res.string.expenses_tag_picker_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = NightTextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp),
                    )
                }
            }
        }
    }
}
