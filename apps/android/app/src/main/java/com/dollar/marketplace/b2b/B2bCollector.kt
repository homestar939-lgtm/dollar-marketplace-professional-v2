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
 * لوحة المحصل: أرصدة المشترين، إنشاء سند قبض وإرساله للمحاسب،
 * وأرشيف التحصيل. العمولة لا تُحتسب هنا — تُثبَّت في الخادم لحظة اعتماد المحاسب.
 */
@Composable
fun B2bCollectorScreen(state: B2bState) {
    var buyers by remember { mutableStateOf<List<B2bBuyerRow>>(emptyList()) }
    var receipts by remember { mutableStateOf<List<B2bReceipt>>(emptyList()) }
    var commission by remember { mutableStateOf<B2bCommission?>(null) }
    var buyerId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh) {
        loading = true
        runCatching { state.api.buyers() }.onSuccess { buyers = it.data }.onFailure { message = it.message }
        runCatching { state.api.receipts() }.onSuccess { receipts = it.data }
        runCatching { state.api.myCommission() }.onSuccess { commission = it.commission }
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

        commission?.let {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                B2bStat("المحصَّل المعتمد", it.collected, Pal.Success)
                B2bStat("عمولتي", it.commission, Pal.Gold)
                B2bStat("سندات", "${it.receipts}")
            }
            Spacer(Modifier.height(10.dp))
        }

        B2bMessage(message, isError = true)

        B2bCard(title = "اختر المشتري") {
            if (buyers.isEmpty()) {
                Text("لا يوجد مشترون.", color = Pal.Muted, fontSize = 13.sp)
            } else {
                buyers.forEach { buyer ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(buyer.name, color = Pal.Text, fontSize = 13.sp)
                            Text(
                                "مستحق ${buyer.outstanding} · فواتير ${buyer.openInvoices}",
                                color = Pal.Muted,
                                fontSize = 11.sp
                            )
                        }
                        TextButton(onClick = { buyerId = buyer.id }) {
                            Text(
                                if (buyerId == buyer.id) "محدد ✓" else "اختيار",
                                color = if (buyerId == buyer.id) Pal.Success else Pal.Gold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        B2bCard(title = "سند قبض جديد") {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("المبلغ المستلم (مثال 200.0000)") },
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
                text = "تسجيل وإرسال للمحاسب",
                enabled = !state.busy && buyerId.isNotBlank() && amount.isNotBlank()
            ) {
                scope.launch {
                    runCatching {
                        val created = state.api.createReceipt(
                            B2bReceiptBody(buyerId = buyerId, amount = amount.trim())
                        )
                        state.api.sendReceipt(created.receipt.id)
                    }
                        .onSuccess {
                            message = "أُرسل السند ${it.receipt.receiptNumber} للمحاسب"
                            amount = ""
                            refresh += 1
                        }
                        .onFailure { message = it.message }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "لا يحرّك السند الرصيد قبل اعتماد المحاسب، والعمولة تُثبَّت لحظة الاعتماد.",
                color = Pal.Muted,
                fontSize = 11.sp
            )
        }

        B2bCard(title = "أرشيف التحصيل (${receipts.size})") {
            if (receipts.isEmpty()) {
                Text("لا توجد سندات.", color = Pal.Muted, fontSize = 13.sp)
            } else {
                receipts.forEach { receipt ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(receipt.receiptNumber, color = Pal.GoldSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${receipt.buyer?.name ?: "—"} · ${receipt.amount} ${receipt.currency}",
                                color = Pal.Muted,
                                fontSize = 11.sp
                            )
                            receipt.commissionAmount?.let {
                                Text("عمولة $it", color = Pal.Success, fontSize = 11.sp)
                            }
                        }
                        B2bStatus(receipt.status)
                    }
                }
            }
        }
    }
}
