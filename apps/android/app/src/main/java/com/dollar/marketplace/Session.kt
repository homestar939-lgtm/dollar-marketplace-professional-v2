package com.dollar.marketplace

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val Context.sessionStore by preferencesDataStore(name = "dollar_session")
private val KEY_TOKEN = stringPreferencesKey("auth_token")
private val KEY_USER = stringPreferencesKey("auth_user")

/**
 * حالة التطبيق المركزية: الجلسة، الكتالوج، الفلاتر، السلة، الطلبات، العروض، الرسائل ومركز البائع.
 * كل الشاشات تقرأ من هنا فتبقى البيانات متزامنة لحظياً بين التبويبات.
 */
class AppState(private val appContext: Context) {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val snackbar = SnackbarHostState()
    private val json = Json { ignoreUnknownKeys = true }

    var token by mutableStateOf<String?>(null)
        private set
    var user by mutableStateOf<AuthUser?>(null)
        private set
    val isLoggedIn: Boolean get() = token != null

    val api = Api { token }

    var busy by mutableStateOf(false)
        private set
    var bootstrapped by mutableStateOf(false)
        private set

    /* ------------------------------ بيانات ------------------------------ */

    var categories by mutableStateOf<List<Category>>(emptyList())
        private set
    var products by mutableStateOf<List<Product>>(emptyList())
        private set
    var trending by mutableStateOf<List<Product>>(emptyList())
        private set
    var featured by mutableStateOf<List<Product>>(emptyList())
        private set
    var facets by mutableStateOf(Facets())
        private set
    var detail by mutableStateOf<Product?>(null)
        private set
    var detailReviews by mutableStateOf<List<Review>>(emptyList())
        private set

    var cart by mutableStateOf(Cart())
        private set
    var orders by mutableStateOf<List<Order>>(emptyList())
        private set
    var offers by mutableStateOf<List<Offer>>(emptyList())
        private set
    var rfqs by mutableStateOf<List<Rfq>>(emptyList())
        private set
    var favorites by mutableStateOf<List<Favorite>>(emptyList())
        private set
    var favoriteIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set
    var messagesByConversation by mutableStateOf<Map<String, List<Message>>>(emptyMap())
        private set
    var seller by mutableStateOf<SellerDashboard?>(null)
        private set

    /* ------------------------------ فلاتر ------------------------------ */

    var query by mutableStateOf("")
    var categorySlug by mutableStateOf("")
    var brand by mutableStateOf("")
    var location by mutableStateOf("")
    var minPriceText by mutableStateOf("")
    var maxPriceText by mutableStateOf("")
    var sort by mutableStateOf("newest")

    val activeFilterCount: Int
        get() = listOf(categorySlug, brand, location, minPriceText, maxPriceText).count { it.isNotBlank() }

    /* ------------------------------ إقلاع ------------------------------ */

    fun bootstrap() {
        if (bootstrapped) return
        bootstrapped = true
        scope.launch {
            restoreSession()
            loadCatalog()
        }
    }

    private suspend fun restoreSession() {
        try {
            val prefs = appContext.sessionStore.data.first()
            token = prefs[KEY_TOKEN]
            user = prefs[KEY_USER]?.let { runCatching { json.decodeFromString<AuthUser>(it) }.getOrNull() }
            if (token != null) {
                loadCart()
                loadFavorites()
            }
        } catch (t: Throwable) {
            notice("تعذر استرجاع الجلسة: ${t.message}")
        }
    }

    private suspend fun persistSession(newToken: String?, newUser: AuthUser?) {
        appContext.sessionStore.edit { prefs ->
            if (newToken == null) {
                prefs.remove(KEY_TOKEN); prefs.remove(KEY_USER)
            } else {
                prefs[KEY_TOKEN] = newToken
                prefs[KEY_USER] = json.encodeToString(AuthUser.serializer(), newUser ?: AuthUser())
            }
        }
    }

    /* --------------------------- مساعد موحّد --------------------------- */

    fun notice(message: String?) {
        scope.launch { snackbar.showSnackbar(message ?: "حدث خطأ غير متوقع") }
    }

    fun toast(message: String) = notice(message)

    private suspend fun <T> guard(block: suspend () -> T): T? = try {
        block()
    } catch (t: Throwable) {
        notice(t.message ?: "تعذر إكمال العملية")
        null
    }

    private suspend fun <T> loading(block: suspend () -> T): T? {
        busy = true
        val result = guard(block)
        busy = false
        return result
    }

    /* ------------------------------ كتالوج ------------------------------ */

    suspend fun loadCatalog() {
        loading {
            categories = runCatching { api.categories() }.getOrDefault(categories)
            facets = runCatching { api.facets() }.getOrDefault(facets)
            featured = runCatching { api.products(sort = "popular").take(12) }.getOrDefault(emptyList())
            loadProducts()
        }
    }

    /** يقرأ المنتجات حسب الفلاتر النشطة (تُستخدم من الرئيسية والبحث). */
    suspend fun loadProducts() {
        val rows = guard {
            api.products(
                q = query,
                category = categorySlug,
                brand = brand,
                location = location,
                minPrice = minPriceText.toDoubleOrNull(),
                maxPrice = maxPriceText.toDoubleOrNull(),
                sort = sort
            )
        }
        if (rows != null) products = rows
    }

    fun search(q: String) {
        query = q
        scope.launch { loadProducts() }
    }

    fun applyFilters() {
        scope.launch {
            loadProducts()
            notice("تم تحديث النتائج (${products.size} منتج)")
        }
    }

    fun resetFilters() {
        categorySlug = ""; brand = ""; location = ""; minPriceText = ""; maxPriceText = ""; sort = "newest"
        scope.launch { loadProducts() }
    }

    fun openProduct(id: String) {
        scope.launch {
            detail = null
            detailReviews = emptyList()
            val p = loading { api.product(id) } ?: return@launch
            detail = p
            detailReviews = guard { api.reviews(id) } ?: emptyList()
        }
    }

    fun closeProduct() {
        detail = null
        detailReviews = emptyList()
    }

    /* ------------------------------- جلسة ------------------------------- */

    fun login(email: String, password: String, onDone: () -> Unit = {}) = scope.launch {
        val res = loading { api.login(email.trim(), password) } ?: return@launch
        token = res.token
        user = res.user
        persistSession(res.token, res.user)
        notice("مرحباً ${res.user.name} 👋")
        loadCart()
        loadFavorites()
        onDone()
    }

    fun register(name: String, email: String, password: String, role: String, onDone: () -> Unit = {}) = scope.launch {
        val res = loading { api.register(email.trim(), password, name.trim(), role) } ?: return@launch
        token = res.token
        user = res.user
        persistSession(res.token, res.user)
        notice("تم إنشاء الحساب بنجاح")
        loadCart()
        onDone()
    }

    fun logout() = scope.launch {
        token = null
        user = null
        cart = Cart()
        orders = emptyList()
        offers = emptyList()
        rfqs = emptyList()
        favorites = emptyList()
        favoriteIds = emptySet()
        conversations = emptyList()
        messagesByConversation = emptyMap()
        seller = null
        persistSession(null, null)
        notice("تم تسجيل الخروج")
    }

    /* -------------------------------- سلة -------------------------------- */

    suspend fun loadCart() {
        if (!isLoggedIn) return
        val fresh = guard { api.cart() } ?: return
        cart = fresh
    }

    fun addToCart(product: Product, quantity: Int = 1, variantId: String? = null) {
        if (!isLoggedIn) { notice("سجّل الدخول أولاً لإضافة المنتجات للسلة"); return }
        scope.launch {
            val qty = quantity.coerceAtLeast(product.minOrderQty)
            val ok = loading { api.addToCart(product.id, qty, variantId) }
            if (ok != null) {
                notice("أُضيف «${product.displayTitle}» إلى السلة")
                loadCart()
            }
        }
    }

    fun setQuantity(item: CartItem, quantity: Int) {
        if (quantity <= 0) { removeCartItem(item); return }
        scope.launch {
            val ok = guard { api.updateCartItem(item.id, quantity) }
            if (ok != null) loadCart()
        }
    }

    fun removeCartItem(item: CartItem) = scope.launch {
        val ok = guard { api.removeCartItem(item.id) }
        if (ok != null) {
            notice("أُزيل المنتج من السلة")
            loadCart()
        }
    }

    fun checkout(address: AddressInput, onDone: (Order) -> Unit = {}) = scope.launch {
        val order = loading { api.checkout(address) }
        if (order != null) {
            notice("تم إنشاء الطلب ${order.shortId} بنجاح ✅")
            loadCart()
            loadOrders()
            onDone(order)
        }
    }

    /* ------------------------------- طلبات ------------------------------- */

    suspend fun loadOrders() {
        if (!isLoggedIn) return
        orders = guard { api.orders() } ?: orders
    }

    /* -------------------------- عروض و RFQ -------------------------- */

    suspend fun loadOffers() {
        if (!isLoggedIn) return
        offers = guard { api.myOffers() } ?: offers
    }

    fun makeOffer(product: Product, quantity: Int, unitPrice: Double, shipping: Double, terms: String?) = scope.launch {
        val created = loading {
            api.createOffer(
                OfferBody(
                    productId = product.id,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    shipping = shipping,
                    paymentTerms = terms?.takeIf { it.isNotBlank() }
                )
            )
        }
        if (created != null) {
            notice("أُرسل عرضك إلى البائع للتفاوض")
            loadOffers()
        }
    }

    suspend fun loadRfqs() {
        if (!isLoggedIn) return
        rfqs = guard { api.rfqs() } ?: rfqs
    }

    fun createRfq(body: RfqBody) = scope.launch {
        val created = loading { api.createRfq(body) }
        if (created != null) {
            notice("نُشر طلب عرض السعر بنجاح")
            loadRfqs()
        }
    }

    /* ---------------------------- مفضلة/مقارنة ---------------------------- */

    suspend fun loadFavorites() {
        if (!isLoggedIn) return
        val rows = guard { api.favorites() } ?: return
        favorites = rows
        favoriteIds = rows.map { it.productId }.toSet()
    }

    fun toggleFavorite(productId: String) {
        if (!isLoggedIn) { notice("سجّل الدخول لحفظ المفضلة"); return }
        scope.launch {
            val res = guard { api.toggleFavorite(productId) }
            if (res != null) {
                val on = res.isOn
                favoriteIds = if (on) favoriteIds + productId else favoriteIds - productId
                notice(if (on) "أُضيف إلى المفضلة ⭐" else "أُزيل من المفضلة")
                loadFavorites()
            }
        }
    }

    fun compare(ids: List<String>) = scope.launch {
        val created = loading { api.createComparison(ids) }
        if (created != null) notice("تم إنشاء مقارنة بين ${ids.size} منتجات")
    }

    /* ---------------------------- محادثات ---------------------------- */

    suspend fun loadConversations() {
        if (!isLoggedIn) return
        conversations = guard { api.conversations() } ?: conversations
    }

    fun openConversation(id: String, onLoaded: () -> Unit = {}) = scope.launch {
        val rows = guard { api.messages(id) } ?: return@launch
        messagesByConversation = messagesByConversation + (id to rows)
        onLoaded()
    }

    fun sendMessage(conversationId: String, text: String) = scope.launch {
        if (text.isBlank()) return@launch
        val sent = guard { api.sendMessage(conversationId, text.trim()) }
        if (sent != null) openConversation(conversationId)
    }

    fun startConversation(sellerId: String, productId: String?, onCreated: (Conversation) -> Unit = {}) = scope.launch {
        if (!isLoggedIn) { notice("سجّل الدخول لبدء محادثة مع البائع"); return@launch }
        val conv = loading { api.startConversation(sellerId, productId) }
        if (conv != null) {
            notice("فُتحت محادثة جديدة مع البائع")
            loadConversations()
            onCreated(conv)
        }
    }

    /* --------------------------- مركز البائع --------------------------- */

    suspend fun loadSeller() {
        if (!isLoggedIn) return
        seller = guard { api.sellerDashboard() } ?: seller
    }

    fun createProduct(body: NewProductBody, onDone: () -> Unit = {}) = scope.launch {
        val created = loading { api.createProduct(body) }
        if (created != null) {
            notice("نُشر المنتج «${created.displayTitle}» بنجاح")
            loadSeller()
            loadProducts()
            onDone()
        }
    }

    fun updateSellerOrder(orderId: String, status: String) = scope.launch {
        val ok = guard { api.updateSellerOrderStatus(orderId, status) }
        if (ok != null) {
            notice("حُدّثت حالة الطلب إلى ${OrderStatus.label(status)}")
            loadSeller()
        }
    }

    fun updateStore(body: StoreUpdateBody) = scope.launch {
        val ok = guard { api.updateStore(body) }
        if (ok != null) {
            notice("حُدّثت بيانات المتجر")
            loadSeller()
        }
    }
}

/** تسميات عربية موحّدة لحالات الطلب. */
object OrderStatus {
    val flow = listOf("PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "IN_TRANSIT", "DELIVERED")

    fun label(status: String): String = when (status.uppercase()) {
        "PENDING" -> "قيد الانتظار"
        "CONFIRMED" -> "مؤكد"
        "PROCESSING" -> "قيد التجهيز"
        "SHIPPED" -> "تم الشحن"
        "IN_TRANSIT" -> "في الطريق"
        "DELIVERED" -> "تم التسليم"
        "CANCELLED" -> "ملغى"
        "RETURNED" -> "مُرتجع"
        "REFUNDED" -> "مُسترجع المبلغ"
        else -> status
    }

    fun tone(status: String): StatusTone = when (status.uppercase()) {
        "DELIVERED" -> StatusTone.Success
        "SHIPPED", "IN_TRANSIT" -> StatusTone.Info
        "CANCELLED", "RETURNED", "REFUNDED" -> StatusTone.Danger
        "PROCESSING", "CONFIRMED" -> StatusTone.Gold
        else -> StatusTone.Neutral
    }
}

object OfferStatus {
    fun label(status: String): String = when (status.uppercase()) {
        "PENDING" -> "بانتظار رد البائع"
        "COUNTER_OFFER" -> "عرض مضاد"
        "ACCEPTED" -> "مقبول"
        "REJECTED" -> "مرفوض"
        "EXPIRED" -> "منتهي"
        "CANCELLED" -> "ملغى"
        else -> status
    }

    fun tone(status: String): StatusTone = when (status.uppercase()) {
        "ACCEPTED" -> StatusTone.Success
        "COUNTER_OFFER" -> StatusTone.Info
        "REJECTED", "EXPIRED", "CANCELLED" -> StatusTone.Danger
        else -> StatusTone.Gold
    }
}

object RfqStatus {
    fun label(status: String): String = when (status.uppercase()) {
        "OPEN" -> "مفتوح للعروض"
        "QUOTED" -> "وردت عروض"
        "AWARDED" -> "تم الترسية"
        "CLOSED" -> "مغلق"
        "CANCELLED" -> "ملغى"
        else -> status
    }

    fun tone(status: String): StatusTone = when (status.uppercase()) {
        "OPEN" -> StatusTone.Gold
        "QUOTED" -> StatusTone.Info
        "AWARDED" -> StatusTone.Success
        "CLOSED", "CANCELLED" -> StatusTone.Danger
        else -> StatusTone.Neutral
    }
}