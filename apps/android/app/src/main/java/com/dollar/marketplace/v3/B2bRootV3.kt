package com.dollar.marketplace.v3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dollar.marketplace.v3.auth.PhoneLoginScreenV3
import com.dollar.marketplace.v3.delivery.DeliveryConfirmationScreen
import com.dollar.marketplace.v3.onboarding.OnboardingScreenV3
import com.dollar.marketplace.v3.theme.DollarMarketplaceTheme

/** مسارات V3 — تُستورد من هنا حتى يبقى الربط في مكان واحد. */
object V3Routes {
    const val ONBOARDING = "onboarding"
    const val PHONE_LOGIN = "phone_login"
    const val DELIVERY_CONFIRMATION = "delivery_confirmation/{orderNumber}"
    const val DASHBOARD_BUYER = "dashboard_buyer"
    const val DASHBOARD_SELLER = "dashboard_seller"
    const val DASHBOARD_WAREHOUSE = "dashboard_warehouse"
    const val DASHBOARD_COLLECTOR = "dashboard_collector"
    const val DASHBOARD_ACCOUNTANT = "dashboard_accountant"
    const val DASHBOARD_ADMIN = "dashboard_admin"

    fun delivery(orderNumber: String): String = "delivery_confirmation/$orderNumber"
}

/**
 * جذر V3: يحوي الـ NavHost ووجهات onboarding / phone_login / delivery_confirmation/{orderNumber}.
 *
 * وجهات `dashboard_*` تُركت فارغة عن قصد — لوحات الأدوار يملكها بوت آخر، فلا نلمس ملفاته.
 * `requestOtp` / `verifyOtp` هنا هما نقطتا وصل مؤقتتان؛ الربط الحقيقي بالخادم مسؤولية طبقة الـ API.
 */
@Composable
fun B2bRootV3() {
    val navController = rememberNavController()

    DollarMarketplaceTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            NavHost(
                navController = navController,
                startDestination = V3Routes.ONBOARDING
            ) {
                composable(V3Routes.ONBOARDING) {
                    OnboardingScreenV3(
                        onComplete = { navController.navigate(V3Routes.PHONE_LOGIN) }
                    )
                }

                composable(V3Routes.PHONE_LOGIN) {
                    PhoneLoginScreenV3(
                        onLoginSuccess = { _ ->
                            navController.navigate(V3Routes.DASHBOARD_BUYER)
                        },
                        requestOtp = { _ -> Result.success(Unit) },
                        verifyOtp = { _, _ -> Result.success("v3-session-placeholder") }
                    )
                }

                composable(V3Routes.DELIVERY_CONFIRMATION) { backStackEntry ->
                    val orderNumber = backStackEntry.arguments?.getString("orderNumber").orEmpty()
                    DeliveryConfirmationScreen(
                        orderNumber = orderNumber,
                        onConfirm = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(V3Routes.DASHBOARD_BUYER) { /* لوحة المشتري — يملكها بوت آخر */ }
                composable(V3Routes.DASHBOARD_SELLER) { /* لوحة البائع — يملكها بوت آخر */ }
                composable(V3Routes.DASHBOARD_WAREHOUSE) { /* لوحة المخزن — يملكها بوت آخر */ }
                composable(V3Routes.DASHBOARD_COLLECTOR) { /* لوحة المحصّل — يملكها بوت آخر */ }
                composable(V3Routes.DASHBOARD_ACCOUNTANT) { /* لوحة المحاسب — يملكها بوت آخر */ }
                composable(V3Routes.DASHBOARD_ADMIN) { /* لوحة المدير — يملكها بوت آخر */ }
            }
        }
    }
}
