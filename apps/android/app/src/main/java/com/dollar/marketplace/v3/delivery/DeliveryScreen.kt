package com.dollar.marketplace.v3.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.v3.theme.RoyalBlack
import com.dollar.marketplace.v3.theme.RoyalBlackLight
import com.dollar.marketplace.v3.theme.RoyalGold
import com.dollar.marketplace.v3.theme.RoyalGoldLight
import com.dollar.marketplace.v3.theme.SuccessGreen
import com.dollar.marketplace.v3.theme.TextPrimary
import com.dollar.marketplace.v3.theme.TextSecondary

/**
 * شاشة تأكيد التسليم V3.
 * يُنادَى عليها بالوسائط المسمّاة: `orderNumber`, `onConfirm`, `onBack`.
 */
@Composable
fun DeliveryConfirmationScreen(
    orderNumber: String,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    var received by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1B1507), RoyalBlack, RoyalBlack))
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 30.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(RoyalGoldLight, RoyalGold))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (done) "تم تسجيل التسليم" else "تأكيد التسليم",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "طلب رقم $orderNumber",
                        color = RoyalGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(RoyalBlackLight)
                    .padding(18.dp)
            ) {
                Text(
                    text = "ملخّص التسليم",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                DeliveryRow("رقم الطلب", orderNumber)
                DeliveryRow("حالة الطلب", if (done) "DELIVERED" else "IN_TRANSIT")
                DeliveryRow("المستلم", "العميل — تأكيد ذاتي")
                DeliveryRow("الدفع", "مسدَّد بالكامل")
            }

            Spacer(Modifier.height(18.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RoyalBlackLight)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = received,
                    onCheckedChange = { received = it },
                    enabled = !done,
                    colors = CheckboxDefaults.colors(
                        checkedColor = RoyalGold,
                        uncheckedColor = TextSecondary,
                        checkmarkColor = RoyalBlack
                    )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "أُقرّ بأنني استلمت الطلب كاملاً وبحالة سليمة.",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            if (done) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF11301F))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "سُجّل التسليم باسم العميل، ووُثِّق في سجل الطلب.",
                        color = SuccessGreen,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    received = true
                    done = true
                    onConfirm()
                },
                enabled = received && !done,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalGold,
                    contentColor = RoyalBlack
                )
            ) {
                Text("تأكيد الاستلام", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("رجوع إلى الطلبات", color = TextPrimary, fontSize = 14.sp)
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "التأكيد نهائي ولا يمكن التراجع عنه من التطبيق.",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DeliveryRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
