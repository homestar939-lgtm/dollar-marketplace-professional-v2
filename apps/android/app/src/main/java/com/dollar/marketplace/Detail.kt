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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================================================================
 *  الطبقات التفاعلية: تفاصيل المنتج، عروض الأسعار، المحادثات، مركز البائع
 * ========================================================================== */

/* ---------------------------- تفاصيل المنتج ---------------------------- */

@Composable
fun ProductDetailScreen(onClose: () -> Unit) {
    val app = dollarApp()
    val product = app.detail ?: return
    var quantity by remember(product.id) { mutableStateOf(product.minOrderQty) }
    var showOffer by remember(product.id) { mutableStateOf(false) }
    var offerPrice by remember(product.id) { mutableStateOf(product.price.toString()) }
    var offerTerms by remember(product.id) { mutableStateOf("") }

    OverlayPanel(title = product.displayTitle, onClose = onClose) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (product.images.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(product.images) { image ->
                            ProductThumb(url = image.url, modifier = Modifier.size(96.dp))
                        }
                    }
                } else {
                    ProductThumb(
                        url = product.cover,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }

            item {
                GoldCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                money(product.price, product.currency),
                                color = Pal.Gold,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(
                                status = if (product.inStock) "متوفر" else "نفد المخزون",
                                tone = if (product.inStock) StatusTone.Success else StatusTone.Danger
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        KeyValueRow(key = "الحد الأدنى للطلب", value = "${product.minOrderQty}")
                        KeyValueRow(key = "المخزون", value = "${product.stock}")
                        KeyValueRow(key = "المشاهدات", value = "${product.views}")
                        KeyValueRow(key = "الموقع", value = product.location ?: "غير محدد")
                        product.brand?.let { KeyValueRow(key = "العلامة", value = it) }
                        product.shippingText?.let { KeyValueRow(key = "الشحن", value = it) }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuantityStepper(
                        quantity = quantity,
                        min = product.minOrderQty.coerceAtLeast(1),
                        max = product.stock.coerceAtLeast(1),
                        onChange = { quantity = it }
                    )
                    Spacer(Modifier.width(4.dp))
                    GoldButton(
                        text = "أضف إلى السلة",
                        icon = Icons.Default.ShoppingCart,
                        enabled = product.inStock,
                        modifier = Modifier.weight(1f),
                        onClick = { app.addToCart(product, quantity) }
                    )
                    IconBadge(
                        icon = if (app.favoriteIds.contains(product.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        tint = if (app.favoriteIds.contains(product.id)) Pal.Danger else Pal.Gold,
                        onClick = { app.toggleFavorite(product.id) }
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlineGoldButton(
                        text = if (showOffer) "إخفاء التفاوض" else "قدّم عرض سعر",
                        icon = Icons.Default.RequestQuote,
                        modifier = Modifier.weight(1f),
                        onClick = { showOffer = !showOffer }
                    )
                    OutlineGoldButton(
                        text = "راسل البائع",
                        icon = Icons.Default.Send,
                        tint = Pal.Info,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            app.startConversation(product.sellerId, product.id)
                            onClose()
                        }
                    )
                }
            }

            item {
                AnimatedSlot(visible = showOffer) {
                    GoldCard {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "تفاوض مباشر مع المورد",
                                color = Pal.Gold,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                "أرسل السعر المقترح والكمية، وسيتواصل معك البائع بعرض مضاد.",
                                color = Pal.Muted,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            DollarField(
                                value = offerPrice,
                                onValueChange = { offerPrice = it },
                                label = "سعر الوحدة المقترح",
                                numeric = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            DollarField(
                                value = offerTerms,
                                onValueChange = { offerTerms = it },
                                label = "شروط الدفع (اختياري)",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            KeyValueRow(
                                key = "إجمالي عرضك",
                                value = money((offerPrice.toDoubleOrNull() ?: 0.0) * quantity),
                                bold = true,
                                valueColor = Pal.Gold
                            )
                            Spacer(Modifier.height(10.dp))
                            GoldButton(
                                text = "أرسل العرض",
                                icon = Icons.Default.Send,
                                loading = app.busy,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    app.makeOffer(
                                        product = product,
                                        quantity = quantity,
                                        unitPrice = offerPrice.toDoubleOrNull() ?: product.price,
                                        shipping = 0.0,
                                        terms = offerTerms
                                    )
                                    showOffer = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                GoldCard {
                    Column(Modifier.padding(14.dp)) {
                        SectionHeader(title = "المتجر", subtitle = product.store?.city ?: "مورد معتمد")
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(icon = Icons.Default.Storefront, size = 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        product.store?.name ?: "متجر مورد",
                                        color = Pal.Text,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (product.store?.verified == true) {
                                        Spacer(Modifier.width(5.dp))
                                        Icon(Icons.Default.Verified, null, tint = Pal.Info, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RatingStars(product.store?.rating ?: 0.0)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${product.store?.followers ?: 0} متابع",
                                        color = Pal.Muted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (product.attributes.isNotEmpty()) {
                item {
                    GoldCard {
                        Column(Modifier.padding(14.dp)) {
                            SectionHeader(title = "المواصفات")
                            Spacer(Modifier.height(6.dp))
                            product.attributes.forEach { attribute ->
                                KeyValueRow(
                                    key = attribute.attribute?.nameAr?.ifBlank { attribute.attribute?.key ?: "" } ?: "مواصفة",
                                    value = attribute.value
                                )
                            }
                        }
                    }
                }
            }

            if (product.descriptionAr.isNotBlank() || product.descriptionEn.isNotBlank()) {
                item {
                    GoldCard {
                        Column(Modifier.padding(14.dp)) {
                            SectionHeader(title = "الوصف")
                            Spacer(Modifier.height(6.dp))
                            Text(
                                product.descriptionAr.ifBlank { product.descriptionEn },
                                color = Pal.Text,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "التقييمات",
                    subtitle = "${app.detailReviews.size} مراجعة"
                )
            }
            if (app.detailReviews.isEmpty()) {
                item {
                    Text(
                        "لا مراجعات بعد — كن أول من يقيّم هذا المنتج.",
                        color = Pal.Muted,
                        fontSize = 12.sp
                    )
                }
            }
            items(app.detailReviews) { review ->
                GoldCard {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                review.author?.name ?: "مشترٍ",
                                color = Pal.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            RatingStars(review.productRating, size = 12.dp)
                        }
                        if (!review.text.isNullOrBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(review.text, color = Pal.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

/* ------------------------------ عروض الأسعار ------------------------------ */

@Composable
fun RfqScreen(onClose: () -> Unit) {
    val app = dollarApp()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var budget by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    LaunchedEffect(app.isLoggedIn) {
        if (app.isLoggedIn) app.loadRfqs()
    }

    OverlayPanel(title = "طلبات عروض الأسعار (RFQ)", onClose = onClose) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!app.isLoggedIn) {
                item {
                    EmptyState(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "سجّل الدخول للنشر",
                        message = "طلبات عروض الأسعار متاحة للحسابات المسجّلة فقط."
                    )
                }
                return@LazyColumn
            }
            item {
                GoldCard {
                    Column(Modifier.padding(14.dp)) {
                        SectionHeader(
                            title = "انشر طلبك",
                            subtitle = "سيصل الطلب لجميع الموردين المطابقين"
                        )
                        Spacer(Modifier.height(10.dp))
                        DollarField(
                            value = title,
                            onValueChange = { title = it },
                            label = "عنوان الطلب",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        DollarField(
                            value = description,
                            onValueChange = { description = it },
                            label = "وصف تفصيلي للمطلوب",
                            singleLine = false,
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DollarField(
                                value = quantity,
                                onValueChange = { quantity = it },
                                label = "الكمية",
                                numeric = true,
                                modifier = Modifier.weight(1f)
                            )
                            DollarField(
                                value = budget,
                                onValueChange = { budget = it },
                                label = "الميزانية",
                                numeric = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        DollarField(
                            value = location,
                            onValueChange = { location = it },
                            label = "بلد/مدينة التسليم",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        GoldButton(
                            text = "نشر طلب عرض السعر",
                            icon = Icons.Default.RequestQuote,
                            loading = app.busy,
                            enabled = title.isNotBlank() && description.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                app.createRfq(
                                    RfqBody(
                                        title = title.trim(),
                                        description = description.trim(),
                                        quantity = quantity.toIntOrNull() ?: 1,
                                        budget = budget.toDoubleOrNull(),
                                        currency = "USD",
                                        location = location.trim().takeIf { it.isNotBlank() }
                                    )
                                )
                                title = ""; description = ""; quantity = "1"; budget = ""; location = ""
                            }
                        )
                    }
                }
            }
            item {
                SectionHeader(
                    title = "طلباتي المنشورة",
                    subtitle = "${app.rfqs.size} طلب"
                )
            }
            if (app.rfqs.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.TrendingUp,
                        title = "لا طلبات بعد",
                        message = "انشر أول طلب وسيبدأ الموردون بإرسال عروضهم."
                    )
                }
            }
            items(app.rfqs) { rfq ->
                GoldCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                rfq.title.ifBlank { "طلب عرض سعر" },
                                color = Pal.Text,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(
                                status = RfqStatus.label(rfq.status),
                                tone = RfqStatus.tone(rfq.status)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(rfq.description, color = Pal.Muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(8.dp))
                        KeyValueRow(key = "الكمية", value = "${rfq.quantity}")
                        KeyValueRow(
                            key = "الميزانية",
                            value = rfq.budget?.let { money(it, rfq.currency) } ?: "غير محددة",
                            valueColor = Pal.Gold
                        )
                        KeyValueRow(key = "عدد العروض", value = "${rfq.quotes.size}")
                        if (rfq.quotes.isNotEmpty()) {
                            GoldDivider(Modifier.padding(vertical = 8.dp))
                            rfq.quotes.take(3).forEach { quote ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        quote.seller?.name ?: "مورد",
                                        color = Pal.Muted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        money(quote.unitPrice),
                                        color = Pal.Gold,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        quote.leadTimeDays?.let { "$it يوم" } ?: "-",
                                        color = Pal.Muted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

/* -------------------------------- المحادثات -------------------------------- */

@Composable
fun MessagesScreen(onClose: () -> Unit) {
    val app = dollarApp()
    var activeId by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(app.isLoggedIn) {
        if (app.isLoggedIn) app.loadConversations()
    }

    OverlayPanel(title = "المحادثات والتفاوض", onClose = onClose) {
        if (!app.isLoggedIn) {
            EmptyState(
                icon = Icons.Default.Send,
                title = "سجّل الدخول للمراسلة",
                message = "المحادثات مع الموردين متاحة للحسابات المسجّلة.",
                modifier = Modifier.fillMaxSize()
            )
            return@OverlayPanel
        }

        if (activeId == null) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SectionHeader(
                        title = "صندوق الرسائل",
                        subtitle = "${app.conversations.size} محادثة"
                    )
                }
                if (app.conversations.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.Send,
                            title = "لا محادثات بعد",
                            message = "افتح أي منتج واضغط «راسل البائع» لبدء التفاوض."
                        )
                    }
                }
                items(app.conversations) { conversation ->
                    GoldCard(
                        onClick = {
                            activeId = conversation.id
                            app.openConversation(conversation.id)
                        }
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBadge(icon = Icons.Default.Storefront, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    conversation.product?.displayTitle ?: "محادثة مع مورد",
                                    color = Pal.Text,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    conversation.messages.lastOrNull()?.body ?: "ابدأ المحادثة...",
                                    color = Pal.Muted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                "${conversation.messages.size}",
                                color = Pal.Gold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            return@OverlayPanel
        }

        val conversationId = activeId ?: return@OverlayPanel
        val messages = app.messagesByConversation[conversationId].orEmpty()

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBadge(icon = Icons.Default.Close, onClick = { activeId = null })
                Spacer(Modifier.width(10.dp))
                Text(
                    "المحادثة",
                    color = Pal.Text,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconBadge(
                    icon = Icons.Default.Dashboard,
                    onClick = { app.openConversation(conversationId) }
                )
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val mine = message.senderId == app.user?.id
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (mine) Pal.Gold.copy(alpha = 0.18f) else Pal.SurfaceAlt)
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Column {
                                Text(
                                    message.body,
                                    color = Pal.Text,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                )
                                Text(
                                    message.createdAt.take(16).replace("T", " "),
                                    color = Pal.Muted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Pal.Onyx)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DollarField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = "اكتب رسالتك...",
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconBadge(
                    icon = Icons.Default.Send,
                    size = 44.dp,
                    onClick = {
                        app.sendMessage(conversationId, draft)
                        draft = ""
                    }
                )
            }
        }
    }
}

/** شكل زوايا محلي — أُبقي للتوثيق فقط. */

/* ------------------------------- مركز البائع ------------------------------- */

@Composable
fun SellerCenterScreen(onClose: () -> Unit) {
    val app = dollarApp()
    var showNewProduct by remember { mutableStateOf(false) }
    var titleAr by remember { mutableStateOf("") }
    var titleEn by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("1") }
    var minQty by remember { mutableStateOf("1") }
    var location by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("") }

    LaunchedEffect(app.isLoggedIn) {
        if (app.isLoggedIn) app.loadSeller()
    }

    OverlayPanel(title = "مركز البائع", onClose = onClose) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!app.isLoggedIn) {
                item {
                    EmptyState(
                        icon = Icons.Default.Dashboard,
                        title = "سجّل الدخول كبائع",
                        message = "أدر متجرك ومنتجاتك وطلباتك من هنا."
                    )
                }
                return@LazyColumn
            }

            val seller = app.seller
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "منتجاتي",
                        value = "${seller?.products ?: 0}",
                        icon = Icons.Default.Inventory,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "طلبات",
                        value = "${seller?.orders ?: 0}",
                        icon = Icons.Default.ReceiptLong,
                        modifier = Modifier.weight(1f),
                        tone = StatusTone.Info
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = "الإيرادات",
                        value = money(seller?.revenue ?: 0.0),
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1f),
                        tone = StatusTone.Success
                    )
                    StatTile(
                        label = "عروض واردة",
                        value = "${seller?.offers ?: 0}",
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f),
                        tone = StatusTone.Gold
                    )
                }
            }

            item {
                GoldCard {
                    Column(Modifier.padding(14.dp)) {
                        SectionHeader(
                            title = "متجري",
                            subtitle = seller?.store?.location ?: "لم يتم ضبط الموقع بعد"
                        )
                        Spacer(Modifier.height(8.dp))
                        KeyValueRow(key = "الاسم", value = seller?.store?.name ?: "—", bold = true)
                        KeyValueRow(key = "التقييم", value = "${seller?.store?.rating ?: 0.0}")
                        KeyValueRow(key = "المتابعون", value = "${seller?.store?.followers ?: 0}")
                        KeyValueRow(
                            key = "موثّق",
                            value = if (seller?.store?.verified == true) "نعم" else "لا",
                            valueColor = if (seller?.store?.verified == true) Pal.Success else Pal.Muted
                        )
                    }
                }
            }

            item {
                GoldButton(
                    text = if (showNewProduct) "إخفاء نموذج الإضافة" else "أضف منتجاً جديداً",
                    icon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showNewProduct = !showNewProduct }
                )
            }

            item {
                AnimatedSlot(visible = showNewProduct) {
                    GoldCard {
                        Column(Modifier.padding(14.dp)) {
                            DollarField(
                                value = titleAr,
                                onValueChange = { titleAr = it },
                                label = "الاسم بالعربية",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            DollarField(
                                value = titleEn,
                                onValueChange = { titleEn = it },
                                label = "الاسم بالإنجليزية",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            val categoryRows =
                                if (app.categories.isNotEmpty()) app.categories else app.facets.categories
                            DollarDropdown(
                                label = "القسم",
                                selected = categoryId,
                                leadingIcon = Icons.Default.Category,
                                options = categoryRows.map { DropdownOption(it.id, it.displayName) },
                                modifier = Modifier.fillMaxWidth(),
                                onSelect = { categoryId = it }
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DollarField(
                                    value = price,
                                    onValueChange = { price = it },
                                    label = "السعر",
                                    numeric = true,
                                    modifier = Modifier.weight(1f)
                                )
                                DollarField(
                                    value = stock,
                                    onValueChange = { stock = it },
                                    label = "المخزون",
                                    numeric = true,
                                    modifier = Modifier.weight(1f)
                                )
                                DollarField(
                                    value = minQty,
                                    onValueChange = { minQty = it },
                                    label = "أدنى طلب",
                                    numeric = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            DollarField(
                                value = location,
                                onValueChange = { location = it },
                                label = "موقع المخزون/الشحن",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            GoldButton(
                                text = "نشر المنتج",
                                icon = Icons.Default.CheckCircle,
                                loading = app.busy,
                                enabled = titleAr.isNotBlank() && categoryId.isNotBlank() && price.toDoubleOrNull() != null,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    app.createProduct(
                                        NewProductBody(
                                            categoryId = categoryId,
                                            titleAr = titleAr.trim(),
                                            titleEn = titleEn.trim().ifBlank { titleAr.trim() },
                                            price = price.toDoubleOrNull() ?: 0.0,
                                            stock = stock.toIntOrNull() ?: 1,
                                            minOrderQty = minQty.toIntOrNull() ?: 1,
                                            location = location.trim().takeIf { it.isNotBlank() }
                                        )
                                    ) {
                                        titleAr = ""; titleEn = ""; price = ""; stock = "1"
                                        minQty = "1"; location = ""; showNewProduct = false
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "طلبات المتجر",
                    subtitle = "حدّث حالة الشحن لكل طلب فرعي"
                )
            }
            if (seller?.orderItems.isNullOrEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.ReceiptLong,
                        title = "لا طلبات واردة",
                        message = "ستظهر هنا طلبات المشترين القادمة لمتجرك."
                    )
                }
            }
            items(seller?.orderItems.orEmpty()) { order ->
                GoldCard {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "طلب #${order.id.take(8)}",
                                color = Pal.Text,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(
                                status = OrderStatus.label(order.status),
                                tone = OrderStatus.tone(order.status)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        KeyValueRow(key = "الإجمالي", value = money(order.sellerTotal), valueColor = Pal.Gold, bold = true)
                        KeyValueRow(key = "عدد الأصناف", value = "${order.items.size}")
                        KeyValueRow(
                            key = "الشحن",
                            value = order.shipment?.status ?: "PENDING"
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrderStatus.flow.forEach { status ->
                                DollarChip(
                                    label = OrderStatus.label(status),
                                    selected = order.status == status,
                                    onClick = { app.updateSellerOrder(order.id, status) }
                                )
                            }
                        }
                    }
                }
            }

            if (!seller?.inventory.isNullOrEmpty()) {
                item {
                    SectionHeader(
                        title = "المخزون",
                        subtitle = "${seller?.inventory?.size ?: 0} صنف"
                    )
                }
                items(seller?.inventory.orEmpty()) { row ->
                    GoldCard {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProductThumb(url = row.product?.cover, modifier = Modifier.size(52.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    row.product?.displayTitle ?: "منتج",
                                    color = Pal.Text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "متاح ${row.quantity} • محجوز ${row.reserved}",
                                    color = Pal.Muted,
                                    fontSize = 11.sp
                                )
                            }
                            StatusPill(
                                status = if (row.quantity > row.reorderPoint) "جيد" else "أعد الطلب",
                                tone = if (row.quantity > row.reorderPoint) StatusTone.Success else StatusTone.Danger
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}