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
 * لوحة المحاسب: اعتماد/رفض الفواتير والسندات، ومؤشرات الدفتر.
 * الاعتماد هو المدخل الوحيد للترحيل المالي، ولا يمكن التراجع عنه.
 */
@Composable
fun B2bAccountantScreen(state: B2bState) {
    var invoices by remember { mutableStateOf<List<B2bInvoice>>(emptyList()) }
    var receipts by remember { mutableStateOf<List<B2bReceipt>>(emptyList()) }
    var report by remember { mutableStateOf<B2bFinancialReport?>(null) }
    var rejectTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var reason by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refresh, state.realtimeRefreshVersion) {
        loading = true
        runCatching { state.api.invoices(status = "SENT") }
            .onSuccess { invoices = it.data }.onFailure { message = it.message }
        runCatching { state.api.receipts(status = "SENT") }.onSuccess { receipts = it.data }
        runCatching { state.api.financialReport() }.onSuccess { report = it }
        loading = false
    }

    fun act(block: suspend () -> Unit, success: String) {
        scope.launch {
            runCatching { block() }
                .onSuccess { message = success; refresh += 1 }
                .onFailure { message = it.message }
        }
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

        report?.let {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                B2bStat("مدين", it.ledger.debit, Pal.Danger)
                B2bStat("دائن", it.ledger.credit, Pal.Success)
                B2bStat("المستحق", it.outstanding, Pal.Gold)
            }
            Spacer(Modifier.height(10.dp))
        }

        B2bMessage(message, isError = true)

        B2bCard(title = "فواتير بانتظار المراجعة (${invoices.size})") {
            if (invoices.isEmpty()) {
                Text("لا توجد فواتير معلقة.", color = Pal.Muted, fontSize = 13.sp)
            } else {
                invoices.forEach { invoice ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(invoice.invoiceNumber, color = Pal.GoldSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("${invoice.totalAmount} ${invoice.currency}", color = Pal.Muted, fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            act({ state.api.approveInvoice(invoice.id) }, "اعتُمدت الفاتورة ${invoice.invoiceNumber}")
                        }) { Text("اعتماد", color = Pal.Success, fontSize = 12.sp) }
                        TextButton(onClick = {
                            rejectTarget = "invoice" to invoice.id
                            reason = ""
                        }) { Text("رفض", color = Pal.Danger, fontSize = 12.sp) }
                    }
                }
            }
        }

        B2bCard(title = "سندات بانتظار المراجعة (${receipts.size})") {
            if (receipts.isEmpty()) {
                Text("لا توجد سندات معلقة.", color = Pal.Muted, fontSize = 13.sp)
            } else {
                receipts.forEach { receipt ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(receipt.receiptNumber, color = Pal.GoldSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${receipt.buyer?.name ?: "—"} · ${receipt.amount} ${receipt.currency}",
                                color = Pal.Muted,
                                fontSize = 11.sp
                            )
                            Text("المحصل: ${receipt.collector?.name ?: "—"}", color = Pal.Muted, fontSize = 11.sp)
                        }
                        TextButton(onClick = {
                            act({ state.api.approveReceipt(receipt.id) }, "اعتُمد السند ${receipt.receiptNumber}")
                        }) { Text("اعتماد", color = Pal.Success, fontSize = 12.sp) }
                        TextButton(onClick = {
                            rejectTarget = "receipt" to receipt.id
                            reason = ""
                        }) { Text("رفض", color = Pal.Danger, fontSize = 12.sp) }
                    }
                }
            }
        }

        val target = rejectTarget
        if (target != null) {
            B2bCard(title = "سبب الرفض") {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("السبب (إعادة للتصحيح)") },
                    singleLine = false,
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
                B2bPrimaryButton(text = "تأكيد الرفض", enabled = reason.length >= 3) {
                    val (kind, id) = target
                    act({
                        if (kind == "invoice") state.api.rejectInvoice(id, reason)
                        else state.api.rejectReceipt(id, reason)
                    }, "تم الرفض وإعادة المستند")
                    rejectTarget = null
                    reason = ""
                }
            }
        }
    }
}
