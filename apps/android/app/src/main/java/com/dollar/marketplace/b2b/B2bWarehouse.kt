package com.dollar.marketplace.b2b

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * لوحة المخزن: طابور الطلبات، تجهيز كميات (جزئي أو كامل)، إصدار فاتورة وإرسالها للمحاسب،
 * ثم تسجيل الشحن برقم مندوب التواصل.
 */
@Composable
fun B2bWarehouseScreen(state: B2bState) {
    var orders by remember { mutableStateOf<List<B2bOrder>>(emptyList()) }
    var selected by remember { mutableStateOf<B2bOrder?>(null) }
    var quantities by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var courierPhone by remember { mutableStateOf("") }
    var courierName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) {
        loading = true
        runCatching { state.api.orders() }
            .onSuccess { page ->
                orders = page.data
                val current = selected
                if (current != null) selected = page.data.firstOrNull { it.id == current.id } ?: current
            }
            .onFailure { message = it.message }
        loading = false
    }

    fun openOrder(order: B2bOrder) {
        selected = order
        quantities = order.items.associate { it.id to it.preparedQuantity }
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

        B2bMessage(message, isError = true)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            B2bStat("قيد التنفيذ", "${orders.count { it.status in listOf("PENDING", "CONFIRMED", "PREPARED", "INVOICED") }}")
            B2bStat("الكل", "${orders.size}", Pal.Success)
        }
        Spacer(Modifier.height(10.dp))

        B2bCard(title = "طابور الطلبات") {
            if (orders.isEmpty()) {
                Text("لا توجد طلبات.", color = Pal.Muted, fontSize = 13.sp)
            } else {
                orders.forEach { order ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(order.orderNumber, color = Pal.GoldSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${order.buyer?.name ?: "—"} · ${order.totalAmount} ${order.currency}",
                                color = Pal.Muted,
                                fontSize = 11.sp
                            )
                        }
                        B2bStatus(order.status)
                        TextButton(onClick = { openOrder(order) }) { Text("فتح", color = Pal.Gold, fontSize = 12.sp) }
                    }
                }
            }
        }

        val order = selected
        if (order != null) {
            B2bCard(title = "تجهيز ${order.orderNumber}") {
                order.items.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.productName, color = Pal.Text, fontSize = 13.sp)
                            Text("المطلوب ${item.quantity}", color = Pal.Muted, fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            val next = ((quantities[item.id] ?: 0) - 1).coerceAtLeast(0)
                            quantities = quantities + (item.id to next)
                        }) { Text("−", color = Pal.Gold, fontSize = 16.sp) }
                        Text("${quantities[item.id] ?: 0}", color = Pal.Text, fontSize = 14.sp)
                        TextButton(onClick = {
                            val next = ((quantities[item.id] ?: 0) + 1).coerceAtMost(item.quantity)
                            quantities = quantities + (item.id to next)
                        }) { Text("+", color = Pal.Gold, fontSize = 16.sp) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                B2bPrimaryButton(text = "حفظ التجهيز", enabled = !state.busy) {
                    val lines = order.items.map {
                        B2bPrepareLineBody(it.id, quantities[it.id] ?: it.preparedQuantity)
                    }
                    scope.launch {
                        runCatching { state.api.prepare(order.id, lines) }
                            .onSuccess { message = "تم تحديث التجهيز"; refresh += 1 }
                            .onFailure { message = it.message }
                    }
                }
                Spacer(Modifier.height(8.dp))
                B2bGhostButton(text = "إنشاء فاتورة وإرسالها ") {
                    scope.launch {
                        runCatching {
                            val envelope = state.api.createInvoice(order.id)
                            state.api.sendInvoice(envelope.invoice.id)
                        }
                            .onSuccess { message = "أُرسلت الفاتورة للمحاسب"; refresh += 1 }
                            .onFailure { message = it.message }
                    }
                }
            }

            B2bCard(title = "تسجيل الشحن") {
                OutlinedTextField(
                    value = courierPhone,
                    onValueChange = { courierPhone = it },
                    label = { Text("رقم مندوب الشحن") },
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
                OutlinedTextField(
                    value = courierName,
                    onValueChange = { courierName = it },
                    label = { Text("اسم المندوب (اختياري)") },
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
                Spacer(Modifier.height(10.dp))
                B2bPrimaryButton(
                    text = "تأكيد الشحن",
                    enabled = !state.busy && courierPhone.length >= 6
                ) {
                    scope.launch {
                        runCatching {
                            state.api.ship(
                                order.id,
                                B2bShipBody(
                                    courierPhone = courierPhone,
                                    courierName = courierName.ifBlank { null }
                                )
                            )
                        }
                            .onSuccess {
                                message = "تم تسجيل الشحن — وقت الشحن من الخادم"
                                courierPhone = ""
                                courierName = ""
                                refresh += 1
                            }
                            .onFailure { message = it.message }
                    }
                }
                order.shippedAt?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("شُحن في: $it", color = Pal.Info, fontSize = 11.sp)
                }
            }
        }
    }
}
