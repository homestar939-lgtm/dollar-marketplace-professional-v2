package com.dollar.marketplace.b2b

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.Pal
import kotlinx.coroutines.launch

/**
 * لوحة المشتري: بحث في الكتالوج (اسم/وكالة/SKU عبر الخادم)، سلة، تأكيد طلب،
 * متابعة الطلبات وكشف الحساب. الأسعار تأتي من الخادم ولا تُحسب هنا.
 */
@Composable
fun B2bBuyerScreen(state: B2bState) {
    var query by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<B2bProduct>>(emptyList()) }
    var orders by remember { mutableStateOf<List<B2bOrder>>(emptyList()) }
    var statement by remember { mutableStateOf<B2bStatement?>(null) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) {
        loading = true
        runCatching { state.api.products(q = query) }.onSuccess { products = it.data }
            .onFailure { message = it.message }
        runCatching { state.api.orders() }.onSuccess { orders = it.data }
        runCatching { state.api.myStatement() }.onSuccess { statement = it }
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(color = Pal.Gold, strokeWidth = 2.dp)
            return@Column
        }

        statement?.let { snapshot ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                B2bStat("المستحق", "${snapshot.totals.balance}", Pal.Danger)
                B2bStat("مدفوع", "${snapshot.totals.credit}", Pal.Success)
                B2bStat("فواتير مفتوحة", "${snapshot.outstanding.size}", Pal.Gold)
            }
            Spacer(Modifier.height(10.dp))
        }

        B2bMessage(message, isError = true)

        B2bCard(title = "البحث في الكتالوج") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("اسم المنتج أو SKU أو الوكالة") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Pal.Gold,
                    unfocusedBorderColor = Pal.Stroke,
                    focusedTextColor = Pal.Text,
                    unfocusedTextColor = Pal.Text,
                    cursorColor = Pal.Gold
                )
            )
            Spacer(Modifier.height(8.dp))
            B2bGhostButton(text = "بحث") { refresh += 1 }
        }

        if (products.isEmpty()) {
            B2bCard { Text("لا توجد منتجات مطابقة.", color = Pal.Muted, fontSize = 13.sp) }
        } else {
            products.forEach { product ->
                B2bCard(title = product.name) {
                    Text(
                        "${product.agency?.name ?: "—"} · ${product.sku}",
                        color = Pal.Muted,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    B2bFieldValue("السعر", "${product.price} ${product.currency}", emphasis = true)
                    B2bFieldValue("الوحدة", product.unit)
                    B2bFieldValue("أقل كمية", "${product.minimumOrderQuantity}")
                    product.countryOfOrigin?.let { B2bFieldValue("بلد المنشأ", it) }
                    Spacer(Modifier.height(8.dp))
                    B2bGhostButton(text = "أضف إلى السلة") { state.addToCart(product) }
                }
            }
        }

        B2bCard(title = "السلة (${state.cart.size})") {
            if (state.cart.isEmpty()) {
                Text("السلة فارغة.", color = Pal.Muted, fontSize = 13.sp)
            } else {
                state.cart.forEach { line ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(line.product.name, color = Pal.Text, fontSize = 13.sp)
                            Text(
                                "${line.quantity} × ${line.product.price} ${line.product.currency}",
                                color = Pal.Muted,
                                fontSize = 11.sp
                            )
                        }
                        TextButton(onClick = { state.setQuantity(line.product.id, line.quantity + 1) }) {
                            Text("+", color = Pal.Gold, fontSize = 16.sp)
                        }
                        TextButton(onClick = { state.setQuantity(line.product.id, line.quantity - 1) }) {
                            Text("−", color = Pal.Gold, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                B2bPrimaryButton(text = "تأكيد الطلب", enabled = !state.busy) {
                    val lines = state.cart.map { B2bOrderLineBody(it.product.id, it.quantity) }
                    scope.launch {
                        runCatching { state.api.createOrder(lines) }
                            .onSuccess {
                                state.clearCart()
                                message = "تم إنشاء الطلب ${it.order.orderNumber}"
                                refresh += 1
                            }
                            .onFailure { message = it.message }
                    }
                }
            }
        }

        B2bCard(title = "طلباتي (${orders.size})") {
            if (orders.isEmpty()) {
                Text("لا توجد طلبات بعد.", color = Pal.Muted, fontSize = 13.sp)
            } else {
                orders.forEach { order ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(order.orderNumber, color = Pal.GoldSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            B2bStatus(order.status)
                        }
                        Text(
                            "${order.totalAmount} ${order.currency} · ${order.items.sumOf { it.preparedQuantity }}/${order.items.sumOf { it.quantity }} مجهّز",
                            color = Pal.Muted,
                            fontSize = 11.sp
                        )
                        order.shippedAt?.let {
                            Text("شُحن إلى: ${order.courierPhone ?: "—"}", color = Pal.Info, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        statement?.let { snapshot ->
            B2bCard(title = "كشف الحساب") {
                if (snapshot.statement.isEmpty()) {
                    Text("لا توجد حركات مالية.", color = Pal.Muted, fontSize = 13.sp)
                } else {
                    snapshot.statement.takeLast(15).forEach { line ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                line.description ?: line.referenceType,
                                color = Pal.Text,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (line.type == "DEBIT") "-${line.amount}" else "+${line.amount}",
                                color = if (line.type == "DEBIT") Pal.Danger else Pal.Success,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
