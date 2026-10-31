package com.dollar.marketplace.b2b

import kotlinx.serialization.Serializable

/* ============================================================================
 *  دولار B2B — عقود البيانات كما يعيدها الخادم فعلياً.
 *  كل الحقول لها قيم افتراضية حتى لا يتعطّل التحليل عند أي توسعة لاحقة.
 * ========================================================================== */

@Serializable
data class B2bUser(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val status: String = "",
    val phone: String? = null,
    val commissionRate: String? = null,
    val emailVerifiedAt: String? = null
)

@Serializable
data class B2bAuthResponse(
    val user: B2bUser = B2bUser(),
    val accessToken: String = "",
    val refreshToken: String? = null
)

@Serializable
data class B2bMeResponse(val user: B2bUser = B2bUser())

@Serializable
data class B2bLoginBody(val email: String, val password: String)

@Serializable
data class B2bRegisterBody(val name: String, val email: String, val password: String)

@Serializable
data class B2bGoogleBody(val idToken: String)

@Serializable
data class B2bMeta(val page: Int = 1, val limit: Int = 20, val total: Int = 0, val pages: Int = 1)

@Serializable
data class B2bAgency(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val status: String = "",
    val description: String? = null
)

@Serializable
data class B2bProduct(
    val id: String = "",
    val name: String = "",
    val nameEn: String? = null,
    val sku: String = "",
    val price: String = "0",
    val currency: String = "USD",
    val unit: String = "",
    val minimumOrderQuantity: Int = 1,
    val availability: String = "",
    val description: String? = null,
    val countryOfOrigin: String? = null,
    val expiryDate: String? = null,
    val agency: B2bAgency? = null,
    val imageUrl: String? = null
)

@Serializable
data class B2bProductPage(val data: List<B2bProduct> = emptyList(), val meta: B2bMeta = B2bMeta())

@Serializable
data class B2bOrderItem(
    val id: String = "",
    val productName: String = "",
    val productSku: String = "",
    val unit: String = "",
    val unitPrice: String = "0",
    val quantity: Int = 0,
    val preparedQuantity: Int = 0,
    val lineTotal: String = "0",
    val currency: String = "USD"
)

@Serializable
data class B2bInvoiceRef(val id: String = "", val invoiceNumber: String = "", val status: String = "")

@Serializable
data class B2bOrder(
    val id: String = "",
    val orderNumber: String = "",
    val status: String = "",
    val currency: String = "USD",
    val totalAmount: String = "0",
    val subtotal: String? = null,
    val notes: String? = null,
    val createdAt: String = "",
    val shippedAt: String? = null,
    val courierPhone: String? = null,
    val courierName: String? = null,
    val buyer: B2bUser? = null,
    val items: List<B2bOrderItem> = emptyList(),
    val invoices: List<B2bInvoiceRef> = emptyList()
)

@Serializable
data class B2bOrderPage(val data: List<B2bOrder> = emptyList(), val meta: B2bMeta = B2bMeta())

@Serializable
data class B2bInvoice(
    val id: String = "",
    val invoiceNumber: String = "",
    val status: String = "",
    val currency: String = "USD",
    val subtotal: String = "0",
    val totalAmount: String = "0",
    val createdAt: String = "",
    val rejectionReason: String? = null,
    val buyerId: String = "",
    val buyer: B2bUser? = null
)

@Serializable
data class B2bInvoicePage(val data: List<B2bInvoice> = emptyList(), val meta: B2bMeta = B2bMeta())

@Serializable
data class B2bReceipt(
    val id: String = "",
    val receiptNumber: String = "",
    val status: String = "",
    val amount: String = "0",
    val currency: String = "USD",
    val commissionRate: String? = null,
    val commissionAmount: String? = null,
    val paymentMethod: String = "",
    val createdAt: String = "",
    val buyer: B2bUser? = null,
    val collector: B2bUser? = null
)

@Serializable
data class B2bReceiptPage(val data: List<B2bReceipt> = emptyList(), val meta: B2bMeta = B2bMeta())

@Serializable
data class B2bBuyerRow(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String? = null,
    val debit: String = "0",
    val credit: String = "0",
    val balance: String = "0",
    val outstanding: String = "0",
    val openInvoices: Int = 0
)

@Serializable
data class B2bBuyerPage(val data: List<B2bBuyerRow> = emptyList(), val meta: B2bMeta = B2bMeta())

@Serializable
data class B2bStatementTotals(
    val debit: String = "0",
    val credit: String = "0",
    val balance: String = "0"
)

@Serializable
data class B2bOutstanding(
    val invoiceId: String = "",
    val invoiceNumber: String = "",
    val total: String = "0",
    val allocated: String = "0",
    val remaining: String = "0"
)

@Serializable
data class B2bStatementLine(
    val id: String = "",
    val type: String = "",
    val amount: String = "0",
    val currency: String = "USD",
    val referenceType: String = "",
    val description: String? = null,
    val createdAt: String = "",
    val runningBalance: String = "0"
)

@Serializable
data class B2bStatement(
    val buyer: B2bUser = B2bUser(),
    val totals: B2bStatementTotals = B2bStatementTotals(),
    val outstanding: List<B2bOutstanding> = emptyList(),
    val statement: List<B2bStatementLine> = emptyList(),
    val closingBalance: String = "0"
)

@Serializable
data class B2bCommission(
    val collected: String = "0",
    val commission: String = "0",
    val receipts: Int = 0,
    val effectiveRate: String = "0"
)

@Serializable
data class B2bCommissionResponse(val commission: B2bCommission = B2bCommission())

@Serializable
data class B2bStatusBucket(val count: Int = 0, val total: String = "0")

@Serializable
data class B2bLedger(val debit: String = "0", val credit: String = "0", val balance: String = "0")

@Serializable
data class B2bFinancialReport(
    val invoices: Map<String, B2bStatusBucket> = emptyMap(),
    val receipts: Map<String, B2bStatusBucket> = emptyMap(),
    val ledger: B2bLedger = B2bLedger(),
    val outstanding: String = "0",
    val buyers: Int = 0
)

@Serializable
data class B2bUserPage(val data: List<B2bUser> = emptyList(), val meta: B2bMeta = B2bMeta())

@Serializable
data class B2bAgencyPage(val data: List<B2bAgency> = emptyList(), val meta: B2bMeta = B2bMeta())

/* ------------------------------ أجسام الطلبات ------------------------------ */

@Serializable
data class B2bOrderLineBody(val productId: String, val quantity: Int)

@Serializable
data class B2bCreateOrderBody(val items: List<B2bOrderLineBody>, val notes: String? = null)

@Serializable
data class B2bPrepareLineBody(val orderItemId: String, val preparedQuantity: Int)

@Serializable
data class B2bPrepareBody(val items: List<B2bPrepareLineBody>, val note: String? = null)

@Serializable
data class B2bShipBody(val courierPhone: String, val courierName: String? = null, val courierChannel: String? = null)

@Serializable
data class B2bInvoiceBody(val orderId: String)

@Serializable
data class B2bReasonBody(val reason: String)

@Serializable
data class B2bReceiptBody(
    val buyerId: String,
    val amount: String,
    val currency: String = "USD",
    val paymentMethod: String = "CASH",
    val notes: String? = null
)

@Serializable
data class B2bOrderEnvelope(val order: B2bOrder = B2bOrder())
@Serializable
data class B2bInvoiceEnvelope(val invoice: B2bInvoice = B2bInvoice())
@Serializable
data class B2bReceiptEnvelope(val receipt: B2bReceipt = B2bReceipt())
@Serializable
data class B2bUserEnvelope(val user: B2bUser = B2bUser())
@Serializable
data class B2bAgencyEnvelope(val agency: B2bAgency = B2bAgency())

@Serializable
data class B2bCreateUserBody(
    val name: String,
    val email: String,
    val password: String,
    val role: String,
    val commissionRate: Double? = null
)

@Serializable
data class B2bCreateAgencyBody(val name: String, val slug: String, val description: String? = null)

@Serializable
data class B2bErrorEnvelope(val error: B2bError? = null)
@Serializable
data class B2bError(val code: String = "", val message: String = "")
