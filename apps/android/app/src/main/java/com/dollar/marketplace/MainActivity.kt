package com.dollar.marketplace

import com.dollar.marketplace.b2b.B2bRoot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  دولار ماركت — نقطة تشغيل التطبيق
 *  هيكل: حالة مركزية واحدة (AppState) + قشرة تنقّل + طبقات (Overlays) للتفاصيل.
 * ========================================================================== */

/** وصّاف الحالة المركزية: كل الشاشات تقرأ منه مباشرة بلا تمرير وسائط. */
val LocalApp = staticCompositionLocalOf<AppState> {
    error("AppState غير مهيأ — تأكد من CompositionLocalProvider")
}

/** اختصار للوصول للحالة المركزية داخل أي Composable. */
@Composable
fun dollarApp(): AppState = LocalApp.current

/** تبويبات التنقّل السفلي. */
enum class Tab(val label: String, val icon: ImageVector) {
    Home("الرئيسية", Icons.Default.Home),
    Market("السوق", Icons.Default.Search),
    Categories("الأقسام", Icons.Default.Category),
    Cart("السلة", Icons.Default.ShoppingCart),
    Account("حسابي", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { B2bRoot() }
    }
}

@Composable
private fun DollarRoot() {
    val context = LocalContext.current
    val state = remember { AppState(context.applicationContext) }
    LaunchedEffect(Unit) { state.bootstrap() }
    DollarTheme {
        CompositionLocalProvider(
            LocalApp provides state,
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            DollarShell()
        }
    }
}

@Composable
private fun DollarShell() {
    val app = dollarApp()
    var tab by remember { mutableStateOf(Tab.Home) }
    var showSeller by remember { mutableStateOf(false) }
    var showRfq by remember { mutableStateOf(false) }
    var showChats by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Black)
    ) {
        Scaffold(
            containerColor = Pal.Black,
            snackbarHost = { SnackbarHost(app.snackbar) },
            bottomBar = {
                DollarBottomBar(
                    selected = tab,
                    cartCount = app.cart.count,
                    onSelect = { tab = it }
                )
            },
            floatingActionButton = {
                if (app.detail == null) {
                    ExpandedFabMenu(
                        items = listOf(
                            FabAction("طلب عرض سعر", Icons.Default.RequestQuote) { showRfq = true },
                            FabAction("المحادثات", Icons.Default.Chat) { showChats = true },
                            FabAction("مركز البائع", Icons.Default.Dashboard) { showSeller = true }
                        )
                    )
                }
            }
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {
                DollarTopBar(
                    onOpenCart = { tab = Tab.Cart },
                    onOpenChats = { showChats = true },
                    onOpenAccount = { tab = Tab.Account }
                )
                LoadingBar(app.busy)
                when (tab) {
                    Tab.Home -> HomeScreen(
                        onOpenMarket = { tab = Tab.Market },
                        onOpenCategories = { tab = Tab.Categories }
                    )
                    Tab.Market -> MarketScreen()
                    Tab.Categories -> CategoriesScreen(
                        onOpenCategory = { slug ->
                            app.categorySlug = slug
                            app.applyFilters()
                            tab = Tab.Market
                        }
                    )
                    Tab.Cart -> CartScreen(onLogin = { tab = Tab.Account })
                    Tab.Account -> AccountScreen()
                }
            }
        }

        /* ------------------------- طبقات فوق الشاشة ------------------------- */
        DollarOverlay(visible = app.detail != null) {
            ProductDetailScreen(onClose = { app.closeProduct() })
        }
        DollarOverlay(visible = showRfq) { RfqScreen(onClose = { showRfq = false }) }
        DollarOverlay(visible = showChats) { MessagesScreen(onClose = { showChats = false }) }
        DollarOverlay(visible = showSeller) { SellerCenterScreen(onClose = { showSeller = false }) }
    }
}

@Composable
private fun DollarBottomBar(selected: Tab, cartCount: Int, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = Pal.Onyx, tonalElevation = 0.dp) {
        Tab.entries.forEach { item ->
            val label = if (item == Tab.Cart && cartCount > 0) "${item.label} ($cartCount)" else item.label
            NavigationBarItem(
                selected = selected == item,
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Pal.Black,
                    selectedTextColor = Pal.Gold,
                    indicatorColor = Pal.Gold,
                    unselectedIconColor = Pal.Muted,
                    unselectedTextColor = Pal.Muted
                )
            )
        }
    }
}

@Composable
private fun DollarTopBar(
    onOpenCart: () -> Unit,
    onOpenChats: () -> Unit,
    onOpenAccount: () -> Unit
) {
    val app = dollarApp()
    Column(
        Modifier
            .fillMaxWidth()
            .background(Pal.Onyx)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Pal.goldBrush),
                contentAlignment = Alignment.Center
            ) {
                Text("$", color = Pal.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Column(
                Modifier
                    .padding(start = 10.dp)
                    .weight(1f)
            ) {
                Text(
                    "DOLLAR",
                    color = Pal.Gold,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text("سوق متعدد البائعين • جملة وتجزئة", color = Pal.Muted, fontSize = 9.sp)
            }
            IconBadge(icon = Icons.Default.Chat, onClick = onOpenChats)
            Spacer(Modifier.width(8.dp))
            IconBadge(icon = Icons.Default.ShoppingCart, onClick = onOpenCart)
            Spacer(Modifier.width(8.dp))
            IconBadge(icon = Icons.Default.Person, onClick = onOpenAccount)
        }
        Spacer(Modifier.height(10.dp))
        DollarField(
            value = app.query,
            onValueChange = { app.query = it; app.search(it) },
            label = "ابحث عن منتج، مورد أو شركة",
            leadingIcon = Icons.Default.Search,
            trailingIcon = if (app.query.isNotBlank()) Icons.Default.Close else null,
            onTrailingClick = { app.query = ""; app.applyFilters() }
        )
    }
}
