package com.dollar.marketplace

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  الشاشات الأساسية — الرئيسية، السوق، الأقسام، السلة، الحساب
 * ========================================================================== */

/* ------------------------------- الرئيسية ------------------------------- */

@Composable
fun HomeScreen(onOpenMarket: () -> Unit, onOpenCategories: () -> Unit) {
    val app = dollarApp()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GradientHero {
                Column(Modifier.padding(22.dp)) {
                    Text(
                        "سوقك العالمي يبدأ هنا",
                        color = Pal.Gold,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 30.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "تجارة متعددة البائعين • جملة وتجزئة • عروض أسعار • تفاوض • دفع عند الاستلام",
                        color = Pal.Text,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoldButton(text = "استكشف السوق", icon = Icons.Default.Storefront, onClick = onOpenMarket)
                        OutlineGoldButton(text = "الأقسام", icon = Icons.Default.Category, onClick = onOpenCategories)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "منتجات معروضة",
                    value = "${app.products.size}",
                    icon = Icons.Default.Inventory,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "موردون وأسواق",
                    value = "${app.categories.size}",
                    icon = Icons.Default.Storefront,
                    modifier = Modifier.weight(1f),
                    tone = StatusTone.Info
                )
                StatTile(
                    label = "في سلتك",
                    value = "${app.cart.count}",
                    icon = Icons.Default.ShoppingBag,
                    modifier = Modifier.weight(1f),
                    tone = StatusTone.Success
                )
            }
        }

        item {
            SectionHeader(
                title = "الأقسام الرئيسية",
                subtitle = "تصفّح ${app.categories.size} قطاعاً تجارياً",
                actionLabel = "الكل",
                onAction = onOpenCategories
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(app.categories) { category ->
                    DollarChip(
                        label = category.displayName,
                        selected = app.categorySlug == category.slug,
                        icon = Icons.Default.Category,
                        onClick = {
                            app.categorySlug = category.slug
                            app.applyFilters()
                            onOpenMarket()
                        }
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = "الأكثر رواجاً",
                subtitle = "اختيارات المشترين هذا الأسبوع",
                actionLabel = "عرض المزيد",
                onAction = onOpenMarket
            )
        }
        if (app.featured.isEmpty() && app.busy) {
            item { ShimmerStrip(height = 96.dp) }
            item { ShimmerStrip(height = 96.dp) }
        }
        items(app.featured.take(8)) { product -> ProductTile(product) }

        item {
            SectionHeader(
                title = "أحدث المنتجات",
                subtitle = "أُضيفت حديثاً من موردين موثوقين"
            )
        }
        if (app.products.isEmpty() && !app.busy) {
            item {
                GoldCard {
                    EmptyState(
                        icon = Icons.Default.CloudOff,
                        title = "لا توجد منتجات بعد",
                        message = "تأكد من تشغيل الخادم ثم أعد المحاولة، أو اسحب للأسفل للتحديث."
                    )
                }
            }
        }
        items(app.products.take(12)) { product -> ProductTile(product) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** بطاقة منتج قابلة للضغط مع مفضلة وتقييم وسعر. */
@Composable
fun ProductTile(product: Product) {
    val app = dollarApp()
    val favorited = app.favoriteIds.contains(product.id)
    GoldCard(onClick = { app.openProduct(product.id) }) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumb(url = product.cover, modifier = Modifier.size(88.dp))
            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    product.displayTitle,
                    color = Pal.Text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storefront,
                        null,
                        tint = Pal.Muted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        product.store?.name ?: "متجر مورد",
                        color = Pal.Muted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (product.store?.verified == true) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Verified,
                            null,
                            tint = Pal.Info,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    money(product.price, product.currency),
                    color = Pal.Gold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        status = if (product.inStock) "متوفر ${product.stock}" else "غير متوفر",
                        tone = if (product.inStock) StatusTone.Success else StatusTone.Danger
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "أدنى ${product.minOrderQty}",
                        color = Pal.Muted,
                        fontSize = 10.sp
                    )
                }
            }
            IconBadge(
                icon = if (favorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                tint = if (favorited) Pal.Danger else Pal.Gold,
                onClick = { app.toggleFavorite(product.id) }
            )
        }
    }
}

/* -------------------------------- السوق -------------------------------- */

@Composable
fun MarketScreen() {
    val app = dollarApp()
    var showFilters by remember { mutableStateOf(false) }
    var compareMode by remember { mutableStateOf(false) }
    val compareIds = remember { mutableStateOf(setOf<String>()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "السوق",
                color = Pal.Text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            if (app.activeFilterCount > 0) {
                Text(
                    "${app.activeFilterCount} فلتر نشط",
                    color = Pal.Gold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            IconBadge(
                icon = Icons.Default.Tune,
                onClick = { showFilters = !showFilters }
            )
            Spacer(Modifier.width(8.dp))
            IconBadge(
                icon = Icons.Default.Refresh,
                onClick = { app.applyFilters() }
            )
        }

        AnimatedSlot(visible = showFilters) {
            GoldCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    val categoryRows =
                        if (app.facets.categories.isNotEmpty()) app.facets.categories else app.categories
                    DollarDropdown(
                        label = "القسم",
                        selected = app.categorySlug,
                        leadingIcon = Icons.Default.Category,
                        options = listOf(DropdownOption("", "كل الأقسام")) +
                            categoryRows.map { DropdownOption(it.slug, it.displayName) },
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { app.categorySlug = it }
                    )
                    Spacer(Modifier.height(10.dp))
                    DollarDropdown(
                        label = "المدينة / الموقع",
                        selected = app.location,
                        leadingIcon = Icons.Default.LocalShipping,
                        options = listOf(DropdownOption("", "كل المواقع")) +
                            app.facets.locations.map { DropdownOption(it, it) },
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { app.location = it }
                    )
                    Spacer(Modifier.height(10.dp))
                    DollarDropdown(
                        label = "العلامة التجارية",
                        selected = app.brand,
                        leadingIcon = Icons.Default.Verified,
                        options = listOf(DropdownOption("", "كل العلامات")) +
                            app.facets.brands.map { DropdownOption(it, it) },
                        modifier = Modifier.fillMaxWidth(),
                        onSelect = { app.brand = it }
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DollarField(
                            value = app.minPriceText,
                            onValueChange = { app.minPriceText = it },
                            label = "أقل سعر",
                            numeric = true,
                            modifier = Modifier.weight(1f)
                        )
                        DollarField(
                            value = app.maxPriceText,
                            onValueChange = { app.maxPriceText = it },
                            label = "أعلى سعر",
                            numeric = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoldButton(
                            text = "تطبيق الفلاتر",
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                app.applyFilters()
                                showFilters = false
                            }
                        )
                        OutlineGoldButton(
                            text = "مسح",
                            icon = Icons.Default.Delete,
                            onClick = { app.resetFilters() }
                        )
                    }
                }
            }
        }

        LazyRow(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sorts = listOf(
                "newest" to "الأحدث",
                "popular" to "الأكثر رواجاً",
                "price_asc" to "الأرخص أولاً",
                "price_desc" to "الأغلى أولاً"
            )
            items(sorts) { pair ->
                DollarChip(
                    label = pair.second,
                    selected = app.sort == pair.first,
                    onClick = {
                        app.sort = pair.first
                        app.applyFilters()
                    }
                )
            }
        }

        if (app.busy && app.products.isEmpty()) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShimmerStrip(height = 92.dp)
                ShimmerStrip(height = 92.dp)
                ShimmerStrip(height = 92.dp)
            }
        }

        if (app.products.isEmpty() && !app.busy) {
            EmptyState(
                icon = Icons.Default.CloudOff,
                title = "لا نتائج مطابقة",
                message = "جرّب تغيير كلمة البحث أو إزالة بعض الفلاتر.",
                actionLabel = "مسح الفلاتر",
                onAction = { app.resetFilters() }
            )
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${app.products.size} منتج",
                        color = Pal.Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    DollarChip(
                        label = if (compareMode) "إنهاء المقارنة" else "مقارنة",
                        selected = compareMode,
                        icon = Icons.Default.Dashboard,
                        onClick = { compareMode = !compareMode }
                    )
                }
            }
            if (compareMode) {
                item {
                    GoldCard {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "اختر منتجين أو أكثر ثم اضغط «أنشئ المقارنة»",
                                color = Pal.Muted,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "المحدد: ${compareIds.value.size}",
                                color = Pal.Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            GoldButton(
                                text = "أنشئ المقارنة",
                                enabled = compareIds.value.size >= 2,
                                onClick = {
                                    app.compare(compareIds.value.toList())
                                    compareIds.value = emptySet()
                                    compareMode = false
                                }
                            )
                        }
                    }
                }
            }
            items(app.products) { product ->
                if (compareMode) {
                    val checked = compareIds.value.contains(product.id)
                    GoldCard(
                        onClick = {
                            compareIds.value = if (checked) {
                                compareIds.value - product.id
                            } else {
                                compareIds.value + product.id
                            }
                        }
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (checked) Icons.Default.CheckCircle else Icons.Default.Add,
                                null,
                                tint = if (checked) Pal.Success else Pal.Gold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            ProductThumb(
                                url = product.cover,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    product.displayTitle,
                                    color = Pal.Text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    money(product.price, product.currency),
                                    color = Pal.Gold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                } else {
                    ProductTile(product)
                }
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
    }
}

/* ------------------------------- الأقسام ------------------------------- */

@Composable
fun CategoriesScreen(onOpenCategory: (String) -> Unit) {
    val app = dollarApp()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "كل الأقسام",
                subtitle = "اختر قطاعاً لعرض منتجاته"
            )
        }
        if (app.categories.isEmpty()) {
            item {
                ShimmerStrip(height = 64.dp)
                Spacer(Modifier.height(10.dp))
                ShimmerStrip(height = 64.dp)
            }
        }
        items(app.categories) { category ->
            GoldCard(onClick = { onOpenCategory(category.slug) }) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(icon = Icons.Default.Category, size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            category.displayName,
                            color = Pal.Text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            category.nameEn.ifBlank { category.slug },
                            color = Pal.Muted,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        "${category.attributes.size} مواصفة",
                        color = Pal.Gold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

/* --------------------------------- السلة --------------------------------- */

@Composable
fun CartScreen(onLogin: () -> Unit) {
    val app = dollarApp()
    var showCheckout by remember { mutableStateOf(false) }
    var orderPlaced by remember { mutableStateOf<Order?>(null) }
    var recipient by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var addressLine by remember { mutableStateOf("") }

    LaunchedEffect(app.isLoggedIn) {
        if (app.isLoggedIn) app.loadCart()
    }

    if (!app.isLoggedIn) {
        EmptyState(
            icon = Icons.Default.ShoppingBag,
            title = "سجّل الدخول لعرض سلتك",
            message = "كل منتج تضيفه من أي متجر يبقى محفوظاً في سلتك حتى إتمام الدفع.",
            modifier = Modifier.fillMaxSize(),
            actionLabel = "تسجيل الدخول",
            onAction = onLogin
        )
        return
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionHeader(
                    title = "سلة التسوق",
                    subtitle = "${app.cart.count} قطعة من ${app.cart.storeCount} متجر"
                )
            }
            if (app.cart.items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.ShoppingBag,
                        title = "سلتك فارغة",
                        message = "ابدأ بإضافة منتجات من السوق، وستُقسَّم تلقائياً إلى طلبات لكل بائع عند الدفع."
                    )
                }
            }
            items(app.cart.items) { item ->
                GoldCard {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductThumb(
                            url = item.product?.cover,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.product?.displayTitle ?: "منتج",
                                color = Pal.Text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                item.product?.store?.name ?: "متجر",
                                color = Pal.Muted,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                money(item.lineTotal, item.product?.currency ?: "USD"),
                                color = Pal.Gold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            QuantityStepper(
                                quantity = item.quantity,
                                min = item.product?.minOrderQty ?: 1,
                                max = (item.product?.stock ?: 99).coerceAtLeast(1),
                                onChange = { app.setQuantity(item, it) }
                            )
                            Spacer(Modifier.height(6.dp))
                            IconBadge(
                                icon = Icons.Default.Delete,
                                tint = Pal.Danger,
                                size = 32.dp,
                                onClick = { app.removeCartItem(item) }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        GoldCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                KeyValueRow(key = "المجموع الفرعي", value = money(app.cart.subtotal), bold = true, valueColor = Pal.Gold)
                KeyValueRow(key = "عدد القطع", value = "${app.cart.count}")
                KeyValueRow(key = "عدد طلبات البائعين", value = "${app.cart.storeCount}")
                GoldDivider(Modifier.padding(vertical = 8.dp))
                GoldButton(
                    text = "إتمام الشراء",
                    icon = Icons.Default.Payments,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = app.cart.items.isNotEmpty(),
                    onClick = { showCheckout = true }
                )
            }
        }
    }

    AnimatedSlot(visible = showCheckout) {
        OverlayPanel(title = "بيانات الشحن", onClose = { showCheckout = false }) {
            Column(Modifier.padding(16.dp)) {
                DollarField(
                    value = recipient,
                    onValueChange = { recipient = it },
                    label = "اسم المستلم",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                DollarField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "رقم الهاتف",
                    numeric = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                DollarField(
                    value = city,
                    onValueChange = { city = it },
                    label = "المدينة",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                DollarField(
                    value = addressLine,
                    onValueChange = { addressLine = it },
                    label = "العنوان التفصيلي",
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                KeyValueRow(key = "المبلغ المستحق", value = money(app.cart.subtotal), bold = true, valueColor = Pal.Gold)
                Spacer(Modifier.height(14.dp))
                GoldButton(
                    text = "تأكيد الطلب (دفع عند الاستلام)",
                    icon = Icons.Default.CheckCircle,
                    loading = app.busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        app.checkout(
                            AddressInput(
                                label = "المنزل",
                                recipient = recipient.ifBlank { app.user?.name ?: "مشتري" },
                                phone = phone,
                                country = "Yemen",
                                city = city,
                                region = city,
                                addressLine = addressLine.ifBlank { "بدون تفاصيل" },
                                postalCode = ""
                            )
                        ) { order ->
                            orderPlaced = order
                            showCheckout = false
                        }
                    }
                )
                AnimatedSlot(visible = orderPlaced != null) {
                    Column(Modifier.padding(top = 12.dp)) {
                        GoldDivider()
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "تم إنشاء الطلب ${orderPlaced?.shortId ?: ""}",
                            color = Pal.Success,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "الإجمالي ${money(orderPlaced?.total ?: 0.0)} • ${orderPlaced?.sellerOrders?.size ?: 0} طلب فرعي للبائعين",
                            color = Pal.Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/* -------------------------------- الحساب -------------------------------- */

@Composable
fun AccountScreen() {
    val app = dollarApp()
    var section by remember { mutableStateOf("summary") }

    LaunchedEffect(app.isLoggedIn) {
        if (app.isLoggedIn) {
            app.loadOrders()
            app.loadFavorites()
            app.loadOffers()
        }
    }

    if (!app.isLoggedIn) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "حسابي",
                    subtitle = "سجّل الدخول للوصول لسلتك وطلباتك وعروضك"
                )
            }
            item { AuthPanel() }
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GoldCard {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Pal.goldBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (app.user?.name ?: "U").take(1).uppercase(),
                            color = Pal.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                    Column(
                        Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        Text(
                            app.user?.name ?: "مستخدم",
                            color = Pal.Text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(app.user?.email ?: "", color = Pal.Muted, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        StatusPill(
                            status = when (app.user?.role) {
                                "ADMIN" -> "مدير المنصة"
                                "SELLER" -> "بائع"
                                else -> "مشتري"
                            },
                            tone = if (app.user?.isSeller == true) StatusTone.Gold else StatusTone.Info
                        )
                    }
                    IconBadge(
                        icon = Icons.Default.Logout,
                        tint = Pal.Danger,
                        onClick = { app.logout() }
                    )
                }
            }
        }

        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DollarChip("ملخّص", section == "summary") { section = "summary" }
                DollarChip("طلباتي", section == "orders") { section = "orders" }
                DollarChip("المفضلة", section == "favorites") { section = "favorites" }
                DollarChip("عروضي", section == "offers") { section = "offers" }
            }
        }

        when (section) {
            "orders" -> {
                if (app.orders.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.ReceiptLong,
                            title = "لا توجد طلبات",
                            message = "أول طلب ستشتريه سيظهر هنا مع حالة الشحن.",
                            actionLabel = "تحديث",
                            onAction = { app.notice("جارٍ التحديث...") }
                        )
                    }
                }
                items(app.orders) { order -> OrderCard(order) }
            }
            "favorites" -> {
                if (app.favorites.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.FavoriteBorder,
                            title = "لا مفضلات بعد",
                            message = "اضغط على القلب في أي منتج لإضافته هنا."
                        )
                    }
                }
                items(app.favorites) { favorite ->
                    favorite.product?.let { ProductTile(it) }
                }
            }
            "offers" -> {
                if (app.offers.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "لا عروض سعر",
                            message = "افتح أي منتج واضغط «قدّم عرض سعر» للتفاوض مع المورد."
                        )
                    }
                }
                items(app.offers) { offer ->
                    GoldCard {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    offer.product?.displayTitle ?: "منتج",
                                    color = Pal.Text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusPill(
                                    status = OfferStatus.label(offer.status),
                                    tone = OfferStatus.tone(offer.status)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            KeyValueRow(key = "الكمية", value = "${offer.quantity}")
                            KeyValueRow(
                                key = "سعر الوحدة",
                                value = money(offer.unitPrice),
                                valueColor = Pal.Gold
                            )
                            KeyValueRow(key = "الشحن", value = money(offer.shipping))
                            KeyValueRow(
                                key = "الإجمالي",
                                value = money(offer.unitPrice * offer.quantity + offer.shipping),
                                bold = true
                            )
                        }
                    }
                }
            }
            else -> {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            label = "طلبات",
                            value = "${app.orders.size}",
                            icon = Icons.Default.ReceiptLong,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "مفضلة",
                            value = "${app.favorites.size}",
                            icon = Icons.Default.Favorite,
                            modifier = Modifier.weight(1f),
                            tone = StatusTone.Danger
                        )
                        StatTile(
                            label = "عروض",
                            value = "${app.offers.size}",
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.weight(1f),
                            tone = StatusTone.Info
                        )
                    }
                }
                item {
                    GoldCard {
                        Column(Modifier.padding(14.dp)) {
                            SectionHeader(
                                title = "أدوات سريعة",
                                subtitle = "شغّل الاختصارات من الزر العائم"
                            )
                            Spacer(Modifier.height(8.dp))
                            KeyValueRow(key = "الجلسة", value = if (app.isLoggedIn) "نشطة" else "غير مسجّل", valueColor = Pal.Success)
                            KeyValueRow(key = "الخادم", value = BuildConfig.API_BASE_URL)
                            KeyValueRow(key = "البريد", value = app.user?.email ?: "-")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
fun OrderCard(order: Order) {
    GoldCard {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "طلب #${order.shortId}",
                    color = Pal.Text,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                StatusPill(status = OrderStatus.label(order.status), tone = OrderStatus.tone(order.status))
            }
            Spacer(Modifier.height(6.dp))
            Text(order.dateLabel, color = Pal.Muted, fontSize = 11.sp)
            GoldDivider(Modifier.padding(vertical = 8.dp))
            order.items.take(3).forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        item.product?.displayTitle ?: "منتج",
                        color = Pal.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${item.quantity} × ${money(item.unitPrice)}",
                        color = Pal.Text,
                        fontSize = 12.sp
                    )
                }
            }
            GoldDivider(Modifier.padding(vertical = 8.dp))
            KeyValueRow(key = "الإجمالي", value = money(order.total), bold = true, valueColor = Pal.Gold)
            KeyValueRow(
                key = "الدفع",
                value = order.payment?.provider ?: "COD"
            )
            KeyValueRow(
                key = "الشحن",
                value = order.shipment?.status ?: "قيد التجهيز"
            )
        }
    }
}

/* --------------------------- لوحة تسجيل الدخول --------------------------- */

@Composable
fun AuthPanel() {
    val app = dollarApp()
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("BUYER") }

    GoldCard {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (isRegister) "إنشاء حساب جديد" else "تسجيل الدخول",
                color = Pal.Gold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(12.dp))
            if (isRegister) {
                DollarField(
                    value = name,
                    onValueChange = { name = it },
                    label = "الاسم الكامل",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }
            DollarField(
                value = email,
                onValueChange = { email = it },
                label = "البريد الإلكتروني",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            DollarField(
                value = password,
                onValueChange = { password = it },
                label = "كلمة المرور",
                modifier = Modifier.fillMaxWidth()
            )
            if (isRegister) {
                Spacer(Modifier.height(12.dp))
                Text("نوع الحساب", color = Pal.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DollarChip("مشتري", role == "BUYER") { role = "BUYER" }
                    DollarChip("بائع", role == "SELLER") { role = "SELLER" }
                }
            }
            Spacer(Modifier.height(16.dp))
            GoldButton(
                text = if (isRegister) "إنشاء الحساب" else "دخول",
                icon = Icons.Default.CheckCircle,
                loading = app.busy,
                enabled = email.isNotBlank() && password.length >= 6,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (isRegister) {
                        app.register(name, email, password, role)
                    } else {
                        app.login(email, password)
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isRegister) "لدي حساب بالفعل — تسجيل الدخول" else "ليس لدي حساب — إنشاء حساب",
                color = Pal.Gold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            )
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = { isRegister = !isRegister }
            ) {
                Text(
                    if (isRegister) "التبديل إلى تسجيل الدخول" else "التبديل إلى إنشاء حساب",
                    color = Pal.Gold,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "حسابات تجريبية: buyer@dollar.local / Buyer123!  •  seller@dollar.local / Seller123!",
                color = Pal.Muted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/* ---------------------- لوحة علوية عامة للطبقات ---------------------- */

@Composable
fun OverlayPanel(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Black.copy(alpha = 0.94f))
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Pal.Onyx)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    color = Pal.Gold,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                IconBadge(icon = Icons.Default.Close, onClick = onClose)
            }
            content()
        }
    }
}