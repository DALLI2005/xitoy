package com.commander.xitoy.presentation.main

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.commander.xitoy.domain.model.CartManager
import com.commander.xitoy.domain.model.SelectedProductHolder
import com.commander.xitoy.presentation.MainActivity
import com.commander.xitoy.presentation.cart.CartScreen
import com.commander.xitoy.presentation.common.tabEnter
import com.commander.xitoy.presentation.common.tabExit
import com.commander.xitoy.presentation.favorites.FavoritesScreen
import com.commander.xitoy.presentation.home.HomeScreen
import com.commander.xitoy.presentation.home.HomeViewModel
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.DalliSurface
import com.commander.xitoy.ui.theme.DalliText

data class BottomNavItem(val name: String, val route: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    rootNavController: NavHostController,
    homeViewModel: HomeViewModel,
    initialTab: String = "home"
) {
    val bottomNavController = rememberNavController()
    var pendingCategory by remember { mutableStateOf<String?>(null) }

    val cartCount = CartManager.cartItems.collectAsState().value.size

    // Push notification deep link — splash tugagach shu yerda ishlaydi
    LaunchedEffect(Unit) {
        val route = MainActivity.pendingRoute.value ?: return@LaunchedEffect
        MainActivity.pendingRoute.value = null
        rootNavController.navigate(route)
    }

    // Boshqa ekrandan kerakli tab bilan ochilganda (masalan, DetailsScreen → Savatcha)
    LaunchedEffect(initialTab) {
        if (initialTab != "home") {
            bottomNavController.navigate(initialTab) {
                popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val navItems = listOf(
        BottomNavItem("Asosiy", "home", Icons.Default.Home),
        BottomNavItem("Katalog", "catalog", Icons.Default.GridView),
        BottomNavItem("Savatcha", "cart", Icons.Default.ShoppingCart),
        BottomNavItem("Buyurtma", "orders", Icons.Default.Inventory2),
        BottomNavItem("Profil", "profile", Icons.Default.Person)
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = DalliPrimary,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.18f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness    = Spring.StiffnessMediumLow
                            ),
                            label = "icon_scale_${item.route}"
                        )
                        NavigationBarItem(
                            icon = {
                                if (item.route == "cart" && cartCount > 0) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = DalliPrimary) {
                                            Text(
                                                text = if (cartCount > 99) "99+" else "$cartCount",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }) {
                                        Icon(
                                            item.icon,
                                            contentDescription = item.name,
                                            modifier = Modifier.scale(iconScale)
                                        )
                                    }
                                } else {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.name,
                                        modifier = Modifier.scale(iconScale)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = item.name,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            },
                            selected = selected,
                            onClick = {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DalliPrimary,
                                selectedTextColor = DalliPrimary,
                                unselectedIconColor = DalliMuted,
                                unselectedTextColor = DalliMuted,
                                indicatorColor = DalliPrimary.copy(alpha = 0.10f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable("home", enterTransition = tabEnter, exitTransition = tabExit,
                popEnterTransition = tabEnter, popExitTransition = tabExit) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onProductClick = { product ->
                        SelectedProductHolder.product = product
                        rootNavController.navigate("details")
                    },
                    onCartClick = { bottomNavController.navigate("cart") },
                    onFavoritesClick = { bottomNavController.navigate("favorites") },
                    onSalesClick = { rootNavController.navigate("sales") },
                    onOrdersClick = {
                        bottomNavController.navigate("orders") {
                            popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    pendingCategory = pendingCategory,
                    onCategoryConsumed = { pendingCategory = null }
                )
            }

            composable("catalog", enterTransition = tabEnter, exitTransition = tabExit,
                popEnterTransition = tabEnter, popExitTransition = tabExit) {
                com.commander.xitoy.presentation.catalog.CatalogScreen(
                    onProductClick = { product ->
                        SelectedProductHolder.product = product
                        rootNavController.navigate("details")
                    },
                    onCategoryClick = { category ->
                        pendingCategory = category
                        bottomNavController.navigate("home") {
                            popUpTo(bottomNavController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable("cart", enterTransition = tabEnter, exitTransition = tabExit,
                popEnterTransition = tabEnter, popExitTransition = tabExit) {
                CartScreen(
                    onBackClick = { bottomNavController.navigate("home") },
                    onOrderPlaced = {
                        bottomNavController.navigate("orders") {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToPayment = { orderId, total ->
                        rootNavController.navigate("payment/$orderId/$total")
                    }
                )
            }

            composable("orders", enterTransition = tabEnter, exitTransition = tabExit,
                popEnterTransition = tabEnter, popExitTransition = tabExit) {
                com.commander.xitoy.presentation.orders.OrdersScreen()
            }

            composable("profile", enterTransition = tabEnter, exitTransition = tabExit,
                popEnterTransition = tabEnter, popExitTransition = tabExit) {
                com.commander.xitoy.presentation.profile.ProfileScreen(
                    onLoginClick = { rootNavController.navigate("login") },
                    onLogout = {
                        rootNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable("favorites", enterTransition = tabEnter, exitTransition = tabExit,
                popEnterTransition = tabEnter, popExitTransition = tabExit) {
                FavoritesScreen(
                    onProductClick = { product ->
                        SelectedProductHolder.product = product
                        rootNavController.navigate("details")
                    }
                )
            }
        }
    }
}
