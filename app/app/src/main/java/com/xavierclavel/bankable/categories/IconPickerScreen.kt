package com.xavierclavel.bankable.categories

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.xavierclavel.bankable.constants.iconMap
import com.xavierclavel.bankable.ui.theme.MyApplicationTheme

@Composable
fun IconPickerScreen(
    viewModel: CategoriesViewModel,
    navController: NavController,
) {
    val entries = iconMap.entries.toList()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(entries) { (name, vector) ->
            IconItem(
                name = name,
                vector = vector,
                selected = viewModel.selectedIcon == name,
                onClick = {
                    viewModel.setSelectedIcon(name)
                    navController.popBackStack()
                },
            )
        }
    }
}

@Composable
private fun IconItem(
    name: String,
    vector: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(6.dp)
            .then(
                if (selected) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(8.dp),
                )
                else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = vector,
            contentDescription = name,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IconPickerScreenPreview() {
    MyApplicationTheme {
        IconPickerScreen(
            viewModel = CategoriesViewModel(),
            navController = rememberNavController(),
        )
    }
}
