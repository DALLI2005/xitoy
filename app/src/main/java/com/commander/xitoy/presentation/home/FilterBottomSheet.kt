package com.commander.xitoy.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentFilter: FilterState,
    availableCategories: List<String>,
    minPrice: Float,
    maxPrice: Float,
    onApply: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf(currentFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DalliSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Filter va saralash",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DalliText,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            SectionTitle("Saralash")
            SortOption.values().forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { draft = draft.copy(sortBy = option) }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = draft.sortBy == option,
                        onClick = { draft = draft.copy(sortBy = option) },
                        colors = RadioButtonDefaults.colors(selectedColor = DalliPrimary)
                    )
                    Text(
                        option.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DalliText
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionTitle("Kategoriya")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                availableCategories.forEach { category ->
                    val selected = category in draft.selectedCategories
                    FilterChip(
                        selected = selected,
                        onClick = {
                            draft = draft.copy(
                                selectedCategories = if (selected)
                                    draft.selectedCategories - category
                                else
                                    draft.selectedCategories + category
                            )
                        },
                        label = { Text(category, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DalliPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            SectionTitle("Narx oralig'i")
            Text(
                "${formatPrice(draft.priceRange.start.toLong())} – ${formatPrice(draft.priceRange.endInclusive.toLong())} so'm",
                fontSize = 14.sp,
                color = DalliMuted,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            RangeSlider(
                value = draft.priceRange,
                onValueChange = { draft = draft.copy(priceRange = it) },
                valueRange = minPrice..maxPrice,
                colors = SliderDefaults.colors(
                    thumbColor = DalliPrimary,
                    activeTrackColor = DalliPrimary,
                    inactiveTrackColor = DalliLine
                )
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { draft = draft.copy(onlyDiscounted = !draft.onlyDiscounted) }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = draft.onlyDiscounted,
                    onCheckedChange = { draft = draft.copy(onlyDiscounted = it) },
                    colors = CheckboxDefaults.colors(checkedColor = DalliPrimary)
                )
                Text(
                    "Faqat chegirmali mahsulotlar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DalliText
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { draft = FilterState() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, DalliLine)
                ) {
                    Text("Tozalash", color = DalliText, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DalliPrimary)
                ) {
                    Text("Qo'llash", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        color = DalliText,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

private fun formatPrice(value: Long): String =
    value.toString().reversed().chunked(3).joinToString(" ").reversed()
