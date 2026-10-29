package com.dollar.marketplace

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/** خطأ API يحمل رسالة الخادم العربية/الإنجليزية للعرض في الـ Snackbar. */
class ApiException(message: String) : Exception(message)

/**
 * عميل واحد مشترك لكل نقاط دولار ماركت.
 * - يحمل رمز JWT تلقائياً إن وُجد.
 * - يترجم أخطاء الخادم {error:"..."} إلى ApiException مفهومة.
 */
class Api(private val tokenProvider: () -> String? = { null }) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 12_000
            socketTimeoutMillis = 20_000
        }
    }

    private val base: String get() = BuildConfig.API_BASE_URL.trimEnd('/')

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun io.ktor.client.request.HttpRequestBuilder.auth() {
        tokenProvider()?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
    }

    private suspend inline fun <reified T> HttpResponse.decode(): T {
        val text = bodyAsText()
        if (status.value >= 400) {
            val msg = runCatching {
                json.decodeFromString<ErrorBody>(text).error
            }.getOrNull()
            throw ApiException(msg ?: "فشل الطلب (${status.value})")
        }
        if (text.isBlank()) throw ApiException("استجابة فارغة من الخادم")
        return json.decodeFromString<T>(text)
    }

    /* ------------------------------ كتالوج ------------------------------ */

    suspend fun categories(): List<Category> =
        client.get("$base/catalog/categories") { auth() }.decode()

    suspend fun category(slug: String): Category =
        client.get("$base/catalog/categories/${enc(slug)}") { auth() }.decode()

    suspend fun products(
        q: String = "",
        category: String = "",
        brand: String = "",
        location: String = "",
        minPrice: Double? = null,
        maxPrice: Double? = null,
        sort: String = "newest"
    ): List<Product> = client.get("$base/catalog/products") {
        auth()
        if (q.isNotBlank()) parameter("q", q)
        if (category.isNotBlank()) parameter("category", category)
        if (brand.isNotBlank()) parameter("brand", brand)
        if (location.isNotBlank()) parameter("location", location)
        minPrice?.let { parameter("minPrice", it) }
        maxPrice?.let { parameter("maxPrice", it) }
        if (sort.isNotBlank()) parameter("sort", sort)
    }.decode()

    suspend fun product(id: String): Product =
        client.get("$base/catalog/products/${enc(id)}") { auth() }.decode()

    suspend fun facets(): Facets =
        client.get("$base/catalog/search/facets") { auth() }.decode()

    /* ------------------------------ حساب ------------------------------ */

    suspend fun login(email: String, password: String): AuthResult =
        client.post("$base/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginBody(email, password))
        }.decode()

    suspend fun register(email: String, password: String, name: String, role: String): AuthResult =
        client.post("$base/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterBody(email, password, name, role))
        }.decode()

    /* ------------------------------- سلة ------------------------------- */

    suspend fun cart(): Cart =
        client.get("$base/commerce/cart") { auth() }.decode()

    suspend fun addToCart(productId: String, quantity: Int, variantId: String? = null): CartItem =
        client.post("$base/commerce/cart/items") {
            auth(); contentType(ContentType.Application.Json)
            setBody(AddToCartBody(productId, quantity, variantId))
        }.decode()

    suspend fun updateCartItem(itemId: String, quantity: Int): CartItem =
        client.patch("$base/commerce/cart/items/${enc(itemId)}") {
            auth(); contentType(ContentType.Application.Json)
            setBody(QtyBody(quantity))
        }.decode()

    suspend fun removeCartItem(itemId: String) {
        client.delete("$base/commerce/cart/items/${enc(itemId)}") { auth() }
    }

    /* ------------------------------ طلبات ------------------------------ */

    suspend fun checkout(address: AddressInput): Order =
        client.post("$base/commerce/orders") {
            auth(); contentType(ContentType.Application.Json)
            setBody(CheckoutBody(address))
        }.decode()

    suspend fun orders(): List<Order> =
        client.get("$base/commerce/orders") { auth() }.decode()

    suspend fun order(id: String): Order =
        client.get("$base/commerce/orders/${enc(id)}") { auth() }.decode()

    /* ------------------------ العروض والتفاوض ------------------------ */

    suspend fun myOffers(): List<Offer> =
        client.get("$base/commerce/offers") { auth() }.decode()

    suspend fun createOffer(body: OfferBody): Offer =
        client.post("$base/commerce/offers") {
            auth(); contentType(ContentType.Application.Json)
            setBody(body)
        }.decode()

    /* ------------------------------ RFQ ------------------------------ */

    suspend fun rfqs(): List<Rfq> =
        client.get("$base/commerce/rfqs") { auth() }.decode()

    suspend fun createRfq(body: RfqBody): Rfq =
        client.post("$base/commerce/rfqs") {
            auth(); contentType(ContentType.Application.Json)
            setBody(body)
        }.decode()

    /* --------------------------- المراجعات --------------------------- */

    suspend fun reviews(productId: String): List<Review> =
        client.get("$base/commerce/products/${enc(productId)}/reviews") { auth() }.decode()

    suspend fun createReview(body: ReviewBody): Review =
        client.post("$base/commerce/reviews") {
            auth(); contentType(ContentType.Application.Json)
            setBody(body)
        }.decode()

    /* -------------------------- المفضلة -------------------------- */

    suspend fun favorites(): List<Favorite> =
        client.get("$base/commerce/favorites") { auth() }.decode()

    suspend fun toggleFavorite(productId: String): ToggleResult =
        client.post("$base/commerce/favorites/${enc(productId)}") { auth() }.decode()

    /* ------------------------- المقارنات ------------------------- */

    suspend fun comparisons(): List<Comparison> =
        client.get("$base/commerce/comparisons") { auth() }.decode()

    suspend fun createComparison(productIds: List<String>): Comparison =
        client.post("$base/commerce/comparisons") {
            auth(); contentType(ContentType.Application.Json)
            setBody(CompareBody(productIds))
        }.decode()

    /* ------------------------- المحادثات ------------------------- */

    suspend fun conversations(): List<Conversation> =
        client.get("$base/messaging/conversations") { auth() }.decode()

    suspend fun startConversation(sellerId: String, productId: String?): Conversation =
        client.post("$base/messaging/conversations") {
            auth(); contentType(ContentType.Application.Json)
            setBody(NewConversationBody(sellerId, productId))
        }.decode()

    suspend fun messages(conversationId: String): List<Message> =
        client.get("$base/messaging/conversations/${enc(conversationId)}/messages") { auth() }.decode()

    suspend fun sendMessage(conversationId: String, text: String): Message =
        client.post("$base/messaging/conversations/${enc(conversationId)}/messages") {
            auth(); contentType(ContentType.Application.Json)
            setBody(MessageBody(text))
        }.decode()

    /* -------------------------- مركز البائع -------------------------- */

    suspend fun sellerDashboard(): SellerDashboard =
        client.get("$base/seller/dashboard") { auth() }.decode()

    suspend fun sellerOrders(): List<SellerOrder> =
        client.get("$base/seller/orders") { auth() }.decode()

    suspend fun updateSellerOrderStatus(orderId: String, status: String): SellerOrder =
        client.patch("$base/seller/orders/${enc(orderId)}/status") {
            auth(); contentType(ContentType.Application.Json)
            setBody(StatusBody(status))
        }.decode()

    suspend fun createProduct(body: NewProductBody): Product =
        client.post("$base/seller/products") {
            auth(); contentType(ContentType.Application.Json)
            setBody(body)
        }.decode()

    suspend fun updateStore(body: StoreUpdateBody): Store =
        client.patch("$base/seller/store") {
            auth(); contentType(ContentType.Application.Json)
            setBody(body)
        }.decode()
}