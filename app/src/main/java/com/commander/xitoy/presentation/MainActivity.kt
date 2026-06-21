package com.commander.xitoy.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.commander.xitoy.domain.model.NotificationPermissionManager
import com.commander.xitoy.domain.model.SelectedProductHolder
import com.commander.xitoy.domain.model.SessionManager
import com.commander.xitoy.presentation.common.slideEnter
import com.commander.xitoy.presentation.common.slideExit
import com.commander.xitoy.presentation.common.slidePopEnter
import com.commander.xitoy.presentation.common.slidePopExit
import com.commander.xitoy.presentation.details.DetailsScreen
import com.commander.xitoy.presentation.home.HomeViewModel
import com.commander.xitoy.presentation.login.LoginScreen
import com.commander.xitoy.presentation.main.MainScreen
import com.commander.xitoy.presentation.notification.NotificationPermissionScreen
import com.commander.xitoy.presentation.onboarding.OnboardingScreen
import com.commander.xitoy.presentation.payment.PaymentScreen
import com.commander.xitoy.presentation.sales.SalesScreen
import com.commander.xitoy.ui.theme.DalliMuted
import com.commander.xitoy.ui.theme.DalliPrimary
import com.commander.xitoy.ui.theme.XitoyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val pendingRoute = MutableStateFlow<String?>(null)
    }

    private val homeViewModel: HomeViewModel by viewModels()

    // Rad etilganda dialog ko'rsatish uchun state
    private val showNotificationDeniedDialog = mutableStateOf(false)

    // Ruxsat natijasi kelgandan keyin chaqiriladigan callback
    private var pendingNotificationNavigation: (() -> Unit)? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                showNotificationDeniedDialog.value = true
            }
            pendingNotificationNavigation?.invoke()
            pendingNotificationNavigation = null
        }

    private fun extractRoute(intent: Intent?): String? {
        intent?.data?.takeIf { it.scheme == "dalli" }?.let { uri ->
            val id = uri.lastPathSegment ?: return@let
            return when (uri.host) {
                "product" -> "product/$id"
                "order"   -> "order/$id"
                else      -> null
            }
        }
        return intent?.getStringExtra("navigate_to_tab")?.let { "main_screen?tab=$it" }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute.value = extractRoute(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingRoute.value = extractRoute(intent)
        setContent {
            XitoyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

                    val rootNavController = rememberNavController()

                    NavHost(navController = rootNavController, startDestination = "splash") {

                        composable("splash") {
                            com.commander.xitoy.presentation.splash.SplashScreen(navController = rootNavController)
                        }

                        composable("onboarding") {
                            OnboardingScreen(onFinish = {
                                rootNavController.navigate("login") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            })
                        }

                        composable(
                            "login",
                            enterTransition = slideEnter,
                            exitTransition = slideExit,
                            popEnterTransition = slidePopEnter,
                            popExitTransition = slidePopExit
                        ) {
                            LoginScreen(
                                onLoginSuccess = {
                                    rootNavController.navigate("notification_permission") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("notification_permission") {
                            NotificationPermissionScreen(
                                onRequestPermission = {
                                    requestNotificationPermission {
                                        rootNavController.navigate("main_screen") {
                                            popUpTo("notification_permission") { inclusive = true }
                                        }
                                    }
                                },
                                onSkip = {
                                    rootNavController.navigate("main_screen") {
                                        popUpTo("notification_permission") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(
                            route = "main_screen?tab={tab}",
                            arguments = listOf(
                                navArgument("tab") {
                                    type = NavType.StringType
                                    defaultValue = "home"
                                }
                            )
                        ) { backStackEntry ->
                            val tab = backStackEntry.arguments?.getString("tab") ?: "home"
                            MainScreen(
                                rootNavController = rootNavController,
                                homeViewModel = homeViewModel,
                                initialTab = tab
                            )
                        }

                        composable(
                            "details",
                            enterTransition = slideEnter,
                            exitTransition = slideExit,
                            popEnterTransition = slidePopEnter,
                            popExitTransition = slidePopExit
                        ) {
                            val product = SelectedProductHolder.product
                            if (product != null) {
                                val viewModel: HomeViewModel = hiltViewModel()
                                val allProducts by viewModel.filteredProducts.collectAsState()
                                DetailsScreen(
                                    product = product,
                                    allProducts = allProducts,
                                    onBackClick = { rootNavController.popBackStack() },
                                    onCartClick = {
                                        rootNavController.navigate("main_screen?tab=cart") {
                                            popUpTo("main_screen") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onProductClick = { sp ->
                                        SelectedProductHolder.product = sp
                                        rootNavController.navigate("details")
                                    }
                                )
                            }
                        }

                        composable(
                            "sales",
                            enterTransition = slideEnter,
                            exitTransition = slideExit,
                            popEnterTransition = slidePopEnter,
                            popExitTransition = slidePopExit
                        ) {
                            SalesScreen(
                                onProductClick = { product ->
                                    SelectedProductHolder.product = product
                                    rootNavController.navigate("details")
                                },
                                onBackClick = { rootNavController.popBackStack() }
                            )
                        }

                        composable("product/{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: ""
                            val products by homeViewModel.filteredProducts.collectAsState()

                            LaunchedEffect(products) {
                                val product = products.find { it.id.toString() == id }
                                if (product != null) {
                                    SelectedProductHolder.product = product
                                    rootNavController.navigate("details") {
                                        popUpTo("product/$id") { inclusive = true }
                                    }
                                }
                            }

                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        composable(
                            "order/{id}",
                            enterTransition = slideEnter,
                            exitTransition = slideExit,
                            popEnterTransition = slidePopEnter,
                            popExitTransition = slidePopExit
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id") ?: ""
                            OrderScreen(
                                id = id,
                                onBackClick = { rootNavController.popBackStack() }
                            )
                        }

                        composable(
                            route = "payment/{orderId}/{total}",
                            enterTransition = slideEnter,
                            exitTransition = slideExit,
                            popEnterTransition = slidePopEnter,
                            popExitTransition = slidePopExit,
                            arguments = listOf(
                                navArgument("orderId") { type = NavType.StringType },
                                navArgument("total")   { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
                            val total   = backStackEntry.arguments?.getString("total")?.toLongOrNull() ?: 0L
                            val tgId    = SessionManager.session.value?.telegramId ?: ""
                            PaymentScreen(
                                orderId       = orderId,
                                totalSomm     = total,
                                telegramId    = tgId,
                                onBack        = { rootNavController.popBackStack() },
                                onPaymentSent = {
                                    rootNavController.navigate("main_screen?tab=orders") {
                                        popUpTo("main_screen") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }

                    // Rad etilganda chiquvchi dialog
                    if (showNotificationDeniedDialog.value) {
                        val context = LocalContext.current
                        AlertDialog(
                            onDismissRequest = { showNotificationDeniedDialog.value = false },
                            icon = {
                                Icon(Icons.Default.NotificationsOff, null, tint = DalliMuted)
                            },
                            title = {
                                Text("Bildirishnomalar o'chiq", fontWeight = FontWeight.ExtraBold)
                            },
                            text = {
                                Text(
                                    "Siz buni o'chirib qo'ydingiz. Buyurtma holati va chegirmalar haqida bilmay qolishingiz mumkin.",
                                    color = DalliMuted
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    NotificationPermissionManager.openNotificationSettings(context)
                                    showNotificationDeniedDialog.value = false
                                }) {
                                    Text("Qayta yoqish", color = DalliPrimary, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNotificationDeniedDialog.value = false }) {
                                    Text("Yopish", color = DalliMuted)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    fun requestNotificationPermission(onResult: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                pendingNotificationNavigation = onResult
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onResult()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderScreen(id: String, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buyurtma #$id") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Buyurtma ID: $id")
        }
    }
}
