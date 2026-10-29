package com.dollar.marketplace

import kotlinx.serialization.Serializable

/* ============================================================
 *  عقود البيانات — مطابقة تماماً لاستجابات Prisma/Express API
 *  كل الحقول لها قيم افتراضية حتى يبقى التحليل آمناً عند أي تغيير.
 * ============================================================ */

@Serializable
data class ProductImage(
    val id: String = "",
    val url: String = "",
    val alt: String? = null,
    val sortOrder: Int = 0
)

@Serializable
data class ProductVideo(val id: String = "", val url: String = "")

@Serializable
data class ProductVariant(
    val id: String = "",
    val sku: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val stock: Int = 0
)

@Serializable
data class AttributeRef(
    val id: String = "",
    val key: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val type: String = "",
    val unit: String? = null
)

@Serializable
data class ProductAttribute(
    val id: String = "",
    val attributeId: String = "",
    val value: String = "",
    val attribute: AttributeRef? = null
)

@Serializable
data class CategoryAttribute(
    val id: String = "",
    val categoryId: String = "",
    val attributeId: String = "",
    val required: Boolean = false,
    val filterable: Boolean = true,
    val sortOrder: Int = 0,
    val attribute: AttributeRef? = null
)

@Serializable
data class Category(
    val id: String = "",
    val nameAr: String = "",
    val nameEn: String = "",
    val slug: String = "",
    val parentId: String? = null,
    val children: List<Category> = emptyList(),
    val attributes: List<CategoryAttribute> = emptyList()
) {
    val displayName: String get() = if (nameAr.isNotBlank()) nameAr else nameEn
}

@Serializable
data class Store(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String? = null,
    val logoUrl: String? = null,
    val coverUrl: String? = null,
    val location: String? = null,
    val country: String? = null,
    val city: String? = null,
    val rating: Double = 0.0,
    val followers: Int = 0,
    val verified: Boolean = false,
    val phone: String? = null
)

@Serializable
data class SellerRef(val id: String = "", val name: String = "", val verified: Boolean = false)

@Serializable
data class Product(
    val id: String = "",
    val sellerId: String = "",
    val storeId: String? = null,
    val categoryId: String = "",
    val titleAr: String = "",
    val titleEn: String = "",
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val price: Double = 0.0,
    val currency: String = "USD",
    val stock: Int = 0,
    val minOrderQty: Int = 1,
    val status: String = "ACTIVE",
    val location: String? = null,
    val views: Int = 0,
    val featured: Boolean = false,
    val sponsored: Boolean = false,
    val shippingText: String? = null,
    val brand: String? = null,
    val images: List<ProductImage> = emptyList(),
    val videos: List<ProductVideo> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val attributes: List<ProductAttribute> = emptyList(),
    val store: Store? = null,
    val category: Category? = null,
    val seller: SellerRef? = null
) {
    val displayTitle: String get() = titleAr.ifBlank { titleEn }
    val cover: String? get() = images.firstOrNull()?.url
    val inStock: Boolean get() = stock > 0
}

@Serializable
data class Facets(
    val categories: List<Category> = emptyList(),
    val locations: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val minPrice: Double = 0.0,
    val maxPrice: Double = 0.0
)

/* ---------------------------- الحساب ---------------------------- */

@Serializable
data class AuthUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "BUYER"
) {
    val isSeller: Boolean get() = role == "SELLER" || role == "ADMIN"
    val isAdmin: Boolean get() = role == "ADMIN"
}

@Serializable
data class AuthResult(val token: String = "", val user: AuthUser = AuthUser())

@Serializable
data class LoginBody(val email: String, val password: String)

@Serializable
data class RegisterBody(val email: String, val password: String, val name: String, val role: String)

@Serializable
data class ErrorBody(val error: String? = null)

/* ---------------------------- السلة ---------------------------- */

@Serializable
data class CartItem(
    val id: String = "",
    val cartId: String = "",
    val productId: String = "",
    val variantId: String? = null,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val product: Product? = null,
    val variant: ProductVariant? = null
) {
    val lineTotal: Double get() = unitPrice * quantity
}

@Serializable
data class Cart(
    val id: String = "",
    val userId: String = "",
    val items: List<CartItem> = emptyList()
) {
    val subtotal: Double get() = items.sumOf { it.lineTotal }
    val count: Int get() = items.sumOf { it.quantity }
    val storeCount: Int get() = items.mapNotNull { it.product?.store?.id }.distinct().size
}

@Serializable
data class AddToCartBody(val productId: String, val quantity: Int, val variantId: String? = null)

@Serializable
data class QtyBody(val quantity: Int)

/* ---------------------------- الطلبات ---------------------------- */

@Serializable
data class OrderItem(
    val id: String = "",
    val productId: String = "",
    val sellerId: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val product: Product? = null
) {
    val lineTotal: Double get() = unitPrice * quantity
}

@Serializable
data class Shipment(val id: String = "", val status: String = "PENDING", val trackingNo: String? = null, val carrier: String? = null)

@Serializable
data class Payment(val id: String = "", val provider: String = "COD", val amount: Double = 0.0, val currency: String = "USD", val status: String = "PENDING")

@Serializable
data class SellerOrder(
    val id: String = "",
    val status: String = "PENDING",
    val subtotal: Double = 0.0,
    val sellerTotal: Double = 0.0,
    val store: Store? = null,
    val items: List<OrderItem> = emptyList(),
    val shipment: Shipment? = null
)

@Serializable
data class Order(
    val id: String = "",
    val status: String = "PENDING",
    val currency: String = "USD",
    val subtotal: Double = 0.0,
    val shippingCost: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val createdAt: String = "",
    val items: List<OrderItem> = emptyList(),
    val sellerOrders: List<SellerOrder> = emptyList(),
    val payment: Payment? = null,
    val shipment: Shipment? = null
) {
    val shortId: String get() = if (id.length > 8) id.take(8) else id
    val dateLabel: String get() = createdAt.take(10)
}

@Serializable
data class AddressInput(
    val label: String = "المنزل",
    val recipient: String = "",
    val phone: String = "",
    val country: String = "",
    val city: String = "",
    val region: String = "",
    val addressLine: String = "",
    val postalCode: String = ""
)

@Serializable
data class CheckoutBody(val shippingAddress: AddressInput)

/* ------------------------ العروض و RFQ ------------------------ */

@Serializable
data class Offer(
    val id: String = "",
    val productId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val shipping: Double = 0.0,
    val paymentTerms: String? = null,
    val status: String = "PENDING",
    val parentId: String? = null,
    val createdAt: String = "",
    val product: Product? = null,
    val seller: SellerRef? = null,
    val buyer: SellerRef? = null
)

@Serializable
data class OfferBody(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double,
    val shipping: Double = 0.0,
    val paymentTerms: String? = null
)

@Serializable
data class RfqQuote(
    val id: String = "",
    val unitPrice: Double = 0.0,
    val shipping: Double = 0.0,
    val leadTimeDays: Int? = null,
    val terms: String? = null,
    val status: String = "SUBMITTED",
    val seller: SellerRef? = null
)

@Serializable
data class Rfq(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val quantity: Int = 0,
    val budget: Double? = null,
    val currency: String = "USD",
    val location: String? = null,
    val status: String = "OPEN",
    val createdAt: String = "",
    val product: Product? = null,
    val quotes: List<RfqQuote> = emptyList()
)

@Serializable
data class RfqBody(
    val title: String,
    val description: String,
    val quantity: Int,
    val budget: Double? = null,
    val currency: String = "USD",
    val location: String? = null
)

/* -------------------- المراجعات والمفضلة -------------------- */

@Serializable
data class Review(
    val id: String = "",
    val productId: String = "",
    val productRating: Double = 0.0,
    val sellerRating: Double = 0.0,
    val shippingRating: Double = 0.0,
    val communicationRating: Double = 0.0,
    val text: String? = null,
    val createdAt: String = "",
    val author: SellerRef? = null
)

@Serializable
data class ReviewBody(
    val productId: String,
    val productRating: Int,
    val sellerRating: Int,
    val shippingRating: Int,
    val communicationRating: Int,
    val text: String = ""
)

@Serializable
data class Favorite(val id: String = "", val productId: String = "", val product: Product? = null)

/** POST /commerce/favorites/:productId يعيد {favorite: true|false}، وبعض المسارات تعيد {ok:true}. */
@Serializable
data class ToggleResult(val favorite: Boolean = false, val ok: Boolean = false) {
    val isOn: Boolean get() = favorite || ok
}

@Serializable
data class ComparisonItem(val id: String = "", val productId: String = "", val product: Product? = null)

@Serializable
data class Comparison(val id: String = "", val name: String? = null, val items: List<ComparisonItem> = emptyList())

@Serializable
data class CompareBody(val productIds: List<String>, val name: String? = null)

/* -------------------------- المحادثات -------------------------- */

@Serializable
data class MessageBody(val body: String, val type: String = "TEXT")

@Serializable
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val body: String = "",
    val type: String = "TEXT",
    val createdAt: String = "",
    val sender: SellerRef? = null
)

@Serializable
data class Conversation(
    val id: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val productId: String? = null,
    val createdAt: String = "",
    val product: Product? = null,
    val messages: List<Message> = emptyList()
)

@Serializable
data class NewConversationBody(val sellerId: String, val productId: String? = null)

/* ------------------------ مركز البائع ------------------------ */

@Serializable
data class InventoryRow(
    val id: String = "",
    val quantity: Int = 0,
    val reserved: Int = 0,
    val reorderPoint: Int = 0,
    val product: Product? = null
)

@Serializable
data class SellerDashboard(
    val products: Int = 0,
    val orders: Int = 0,
    val revenue: Double = 0.0,
    val rfqs: Int = 0,
    val offers: Int = 0,
    val store: Store? = null,
    val inventory: List<InventoryRow> = emptyList(),
    val orderItems: List<SellerOrder> = emptyList()
)

@Serializable
data class NewProductBody(
    val categoryId: String,
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String = "",
    val descriptionEn: String = "",
    val price: Double,
    val currency: String = "USD",
    val stock: Int,
    val minOrderQty: Int = 1,
    val location: String? = null,
    val shippingText: String? = null,
    val images: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap()
)

@Serializable
data class StoreUpdateBody(
    val name: String,
    val description: String? = null,
    val location: String? = null,
    val city: String? = null,
    val country: String? = null,
    val phone: String? = null
)

@Serializable
data class StatusBody(val status: String)
