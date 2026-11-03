package com.dollar.marketplace.b2b

import com.dollar.marketplace.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/** خطأ يحمل رسالة الخادم العربية الجاهزة للعرض. */
class B2bException(message: String) : Exception(message)

/**
 * عميل B2B: نقطة واحدة تعرف كيف تتحدث مع خادم دولار.
 * - يمرّر رمز الجلسة تلقائياً.
 * - يترجم {error:{message}} إلى B2bException مفهومة.
 */
class B2bApi(private val tokenProvider: () -> String? = { null }) {

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
            requestTimeoutMillis = 25_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 25_000
        }
    }

    private val base: String get() = BuildConfig.API_BASE_URL.trimEnd('/')

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun io.ktor.client.request.HttpRequestBuilder.b2bAuth() {
        tokenProvider()?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
    }

    private suspend inline fun <reified T> HttpResponse.decode(): T {
        val text = bodyAsText()
        if (status.value >= 400) {
            val message = runCatching {
                json.decodeFromString<B2bErrorEnvelope>(text).error?.message
            }.getOrNull()
            throw B2bException(message ?: "فشل الطلب (${status.value})")
        }
        if (text.isBlank()) throw B2bException("استجابة فارغة من الخادم")
        return json.decodeFromString<T>(text)
    }

    /* -------------------------------- المصادقة -------------------------------- */

    suspend fun login(email: String, password: String): B2bAuthResponse =
        client.post("$base/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(B2bLoginBody(email, password))
        }.decode()

    suspend fun register(name: String, email: String, password: String): B2bAuthResponse =
        client.post("$base/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(B2bRegisterBody(name, email, password))
        }.decode()

    suspend fun google(idToken: String): B2bAuthResponse =
        client.post("$base/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(B2bGoogleBody(idToken))
        }.decode()

    /**
     * الدخول التجريبي: يختار الخادم مستخدماً جاهزاً لدور الطلب ويُصدر جلسة حقيقية.
     * المسار `POST /v3/demo/login` بجسم `{ role }`، ونفس فك الترميز ومعالجة الأخطاء كبقية المصادقة.
     */
    suspend fun demoLogin(role: String): B2bAuthResponse =
        client.post("$base/v3/demo/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("role" to role))
        }.decode()

    suspend fun me(): B2bMeResponse = client.get("$base/auth/me") { b2bAuth() }.decode()

    suspend fun logout() {
        runCatching { client.post("$base/auth/logout") { b2bAuth() } }
    }

    /* --------------------------------- منتجات --------------------------------- */

    suspend fun products(q: String = "", page: Int = 1, limit: Int = 30): B2bProductPage =
        client.get("$base/products") {
            b2bAuth()
            if (q.isNotBlank()) parameter("q", q)
            parameter("page", page)
            parameter("limit", limit)
        }.decode()

    /* ---------------------------------- طلبات ---------------------------------- */

    suspend fun orders(status: String = "", page: Int = 1, limit: Int = 40): B2bOrderPage =
        client.get("$base/orders") {
            b2bAuth()
            if (status.isNotBlank()) parameter("status", status)
            parameter("page", page)
            parameter("limit", limit)
        }.decode()

    suspend fun order(id: String): B2bOrderEnvelope =
        client.get("$base/orders/${enc(id)}") { b2bAuth() }.decode()

    suspend fun createOrder(items: List<B2bOrderLineBody>, notes: String? = null): B2bOrderEnvelope =
        client.post("$base/orders") {
            b2bAuth()
            contentType(ContentType.Application.Json)
            setBody(B2bCreateOrderBody(items, notes))
        }.decode()

    suspend fun prepare(orderId: String, items: List<B2bPrepareLineBody>): B2bOrderEnvelope =
        client.post("$base/orders/${enc(orderId)}/prepare") {
            b2bAuth()
            contentType(ContentType.Application.Json)
            setBody(B2bPrepareBody(items))
        }.decode()

    suspend fun ship(orderId: String, body: B2bShipBody): B2bOrderEnvelope =
        client.post("$base/orders/${enc(orderId)}/ship") {
            b2bAuth()
            contentType(ContentType.Application.Json)
            setBody(body)
        }.decode()

    /* --------------------------------- فواتير --------------------------------- */

    suspend fun invoices(status: String = "", page: Int = 1, limit: Int = 40): B2bInvoicePage =
        client.get("$base/invoices") {
            b2bAuth()
            if (status.isNotBlank()) parameter("status", status)
            parameter("page", page)
            parameter("limit", limit)
        }.decode()

    suspend fun createInvoice(orderId: String): B2bInvoiceEnvelope =
        client.post("$base/invoices") {
            b2bAuth()
            contentType(ContentType.Application.Json)
            setBody(B2bInvoiceBody(orderId))
        }.decode()

    suspend fun sendInvoice(id: String): B2bInvoiceEnvelope =
        client.post("$base/invoices/${enc(id)}/send") { b2bAuth() }.decode()

    suspend fun approveInvoice(id: String): B2bInvoiceEnvelope =
        client.post("$base/invoices/${enc(id)}/approve") { b2bAuth() }.decode()

    suspend fun rejectInvoice(id: String, reason: String): B2bInvoiceEnvelope =
        client.post("$base/invoices/${enc(id)}/reject") {
            b2bAuth()
            contentType(ContentType.Application.Json)
            setBody(B2bReasonBody(reason))
        }.decode()

    /* -------------------------------- سندات قبض -------------------------------- */

    suspend fun receipts(status: String = "", page: Int = 1, limit: Int = 40): B2bReceiptPage =
        client.get("$base/receipts") {
            b2bAuth()
            if (status.isNotBlank()) parameter("status", status)
            parameter("page", page)
            parameter("limit", limit)
        }.decode()

    suspend fun createReceipt(body: B2bReceiptBody): B2bReceiptEnvelope =
        client.post("$base/receipts") {
            b2bAuth()
            contentType(ContentType.Application.Json)
            setBody(body)
        }.decode()

    suspend fun sendReceipt(id: String): B2bReceiptEnvelope =
        client.post("$base/receipts/${enc(id)}/send") { b2bAuth() }.decode()

    suspend fun approveReceipt(id: String): B2bReceiptEnvelope =
        client.post("$base/receipts/${enc(id)}/approve") { b2bAuth() }.decode()

    suspend fun rejectReceipt(id: String, reason: String): B2bReceiptEnvelope =
        client.post("$base/receipts/${enc(id)}/reject") {
            b2bAuth()
            contentType(ContentType.Application.Json)
            setBody(B2bReasonBody(reason))
        }.decode()

    suspend fun myCommission(): B2bCommissionResponse =
        client.get("$base/receipts/commission/me") { b2bAuth() }.decode()

    /* -------------------------------- محاسبة -------------------------------- */

    suspend fun buyers(page: Int = 1, limit: Int = 40): B2bBuyerPage =
        client.get("$base/accounting/buyers") {
            b2bAuth()
            parameter("page", page)
            parameter("limit", limit)
        }.decode()

    suspend fun myStatement(): B2bStatement =
        client.get("$base/accounting/me/statement") { b2bAuth() }.decode()

    suspend fun statement(buyerId: String): B2bStatement =
        client.get("$base/accounting/buyers/${enc(buyerId)}/statement") { b2bAuth() }.decode()

    suspend fun financialReport(): B2bFinancialReport =
        client.get("$base/reports/financial") { b2bAuth() }.decode()

    /* --------------------------------- إدارة --------------------------------- */

    suspend fun users(page: Int = 1, limit: Int = 40): B2bUserPage =
        client.get("$base/users") {
            b2bAuth()
            parameter("page", page)
            parameter("limit", limit)
        }.decode()

    suspend fun createUser(body: B2bCreateUserBody): B2bUserEnvelope =
        client.post("$base/users") {
            b2bAuth()
            contentType(ContentType.Application.Json)
            setBody(body)
        }.decode()

    suspend fun agencies(page: Int = 1, limit: Int = 40): B2bAgencyPage =
        client.get("$base/agencies") {
            b2bAuth()
            parameter("page", page)
            parameter("limit", limit)
        }.decode()

    suspend fun createAgency(body: B2bCreateAgencyBody): B2bAgencyEnvelope =
        client.post("$base/agencies") {
            b2bAuth()
            contentType(ContentType.Application.Json)
            setBody(body)
        }.decode()
}
