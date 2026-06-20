package com.commander.xitoy.presentation.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.commander.xitoy.domain.model.FavoritesManager
import com.commander.xitoy.domain.model.Product
import com.commander.xitoy.presentation.home.ProductCard
import com.commander.xitoy.ui.theme.DalliBackground
import com.commander.xitoy.ui.theme.DalliLine
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliPrimarySoft
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText
import com.commander.xitoy.ui.theme.DalliTextSecondary

private val CHIP_CATEGORIES = listOf(
    "Hammasi", "Elektronika", "Kiyim", "Poyabzal", "Aksessuar", "Sport", "Uy uchun"
)

private val SORT_OPTIONS = listOf(
    "hot" to "Ommabop",
    "margin" to "Foyda %",
    "cheap" to "Arzon"
)

@Composable
fun CatalogScreen(
    onProductClick: (Product) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val favorites = FavoritesManager.favorites.collectAsState().value

    var selectedCategory by remember { mutableStateOf("Hammasi") }
    var sort by remember { mutableStateOf("hot") }

    val visibleProducts = remember(products, selectedCategory, sort) {
        val byCat = if (selectedCategory == "Hammasi") products
        else products.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        when (sort) {
            "hot" -> byCat.sortedByDescending { it.soldCount }
            "margin" -> byCat.sortedByDescending { it.discountPercent }
            "cheap" -> byCat.sortedBy { it.price }
            else -> byCat
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DalliBackground)
    ) {
        when {
            isLoading && products.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DalliPrimary, strokeWidth = 3.dp)
                }
            }

            errorMessage != null && products.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = errorMessage ?: "Xatolik yuz berdi",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(text = "Qayta urinish", color = DalliPrimary)
                        }
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Katalog",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DalliText
                            )
                            CatalogCategoryChips(
                                selected = selectedCategory,
                                onSelect = { selectedCategory = it }
                            )
                            CatalogSortRow(
                                count = visibleProducts.size,
                                sort = sort,
                                onSortChange = { sort = it }
                            )
                        }
                    }

                    items(visibleProducts, key = { it.id }) { product ->
                        val isFav = favorites.any { it.name == product.name }
                        ProductCard(
                            product = product,
                            isFavorite = isFav,
                            onClick = { onProductClick(product) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogCategoryChips(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CHIP_CATEGORIES.forEach { cat ->
            val isSelected = selected.equals(cat, ignoreCase = true)
            Card(
                onClick = { onSelect(cat) },
                shape = RoundedCornerShape(999.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DalliText else DalliSurface
                ),
                border = if (!isSelected) BorderStroke(1.dp, DalliLine) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = cat,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) Color.White else DalliTextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogSortRow(count: Int, sort: String, onSortChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$count ta mahsulot",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = DalliMuted
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SORT_OPTIONS.forEach { (key, label) ->
                val isSelected = sort == key
                Card(
                    onClick = { onSortChange(key) },
                    shape = RoundedCornerShape(9.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DalliPrimarySoft else DalliSurface
                    ),
                    border = if (!isSelected) BorderStroke(1.dp, DalliLine) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) DalliPrimary else DalliMuted
                    )
                }
            }
        }
    }
}
