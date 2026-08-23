package by.ciszkin.herdmanager.presentation.registry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import by.ciszkin.herdmanager.domain.model.RegistrySort
import herdmanager.shared.generated.resources.Res
import herdmanager.shared.generated.resources.registry_all
import herdmanager.shared.generated.resources.registry_sort_newest
import herdmanager.shared.generated.resources.registry_sort_popular
import org.jetbrains.compose.resources.stringResource

/**
 * Quick-filter controls for the registry: capability filter chips (server-side
 * `c=` filter) and a popular/newest sort toggle (server-side `o=` sort).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistryFilterBar(
    selectedCategory: String?,
    sort: RegistrySort,
    onCategorySelected: (String?) -> Unit,
    onSortSelected: (RegistrySort) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "all") {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text(stringResource(Res.string.registry_all)) }
                )
            }
            items(REGISTRY_FILTER_CATEGORIES, key = { it }) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) }
                )
            }
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
        ) {
            SingleChoiceSegmentedButtonRow {
                RegistrySort.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = sort == option,
                        onClick = { onSortSelected(option) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = RegistrySort.entries.size)
                    ) {
                        Text(
                            stringResource(
                                when (option) {
                                    RegistrySort.POPULAR -> Res.string.registry_sort_popular
                                    RegistrySort.NEWEST -> Res.string.registry_sort_newest
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}