package com.dollar.marketplace.b2b

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.Pal

/** لوحة ذهبية على أسود ملكي: الغلاف البصري لكل شاشات B2B. */
@Composable
fun B2bCard(
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Pal.Surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null || trailing != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title ?: "",
                        color = Pal.GoldSoft,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    trailing?.invoke()
                }
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun B2bStat(label: String, value: String, tone: Color = Pal.Gold) {
    Column(
        Modifier
            .border(1.dp, Pal.Stroke, RoundedCornerShape(14.dp))
            .background(Pal.SurfaceAlt, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = Pal.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = tone, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/** شريحة حالة: أخضر للاعتماد، ذهبي للانتظار، أحمر للرفض. */
@Composable
fun B2bStatus(status: String) {
    val color = when (status) {
        "APPROVED", "DELIVERED", "SHIPPED", "ACTIVE" -> Pal.Success
        "REJECTED", "CANCELLED", "SUSPENDED" -> Pal.Danger
        "SENT", "PENDING", "CONFIRMED", "PREPARED", "INVOICED" -> Pal.Gold
        else -> Pal.Muted
    }
    Text(
        text = statusLabel(status),
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "بانتظار التأكيد"
    "CONFIRMED" -> "مؤكد"
    "PREPARED" -> "مجهّز"
    "INVOICED" -> "مفوتر"
    "SHIPPED" -> "مشحون"
    "DELIVERED" -> "مسلَّم"
    "CANCELLED" -> "ملغى"
    "DRAFT" -> "مسودة"
    "SENT" -> "مرسل"
    "APPROVED" -> "معتمد"
    "REJECTED" -> "مرفوض"
    "VOIDED" -> "ملغى"
    "ACTIVE" -> "نشط"
    "PENDING_VERIFICATION" -> "بانتظار التحقق"
    "SUSPENDED" -> "موقوف"
    "DISABLED" -> "معطّل"
    else -> status
}

@Composable
fun B2bFieldValue(label: String, value: String, emphasis: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Pal.Muted, fontSize = 12.sp)
        Text(
            value,
            color = if (emphasis) Pal.Gold else Pal.Text,
            fontSize = 13.sp,
            fontWeight = if (emphasis) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun B2bPrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Pal.Gold,
            contentColor = Pal.Black,
            disabledContainerColor = Pal.SurfaceHigh,
            disabledContentColor = Pal.Muted
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
fun B2bGhostButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Pal.GoldSoft,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Pal.Muted
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Pal.Stroke)
    ) {
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun B2bMessage(text: String?, isError: Boolean) {
    if (text.isNullOrBlank()) return
    Text(
        text = text,
        color = if (isError) Pal.Danger else Pal.Success,
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}
