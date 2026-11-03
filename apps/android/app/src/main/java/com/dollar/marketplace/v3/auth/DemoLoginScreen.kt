package com.dollar.marketplace.v3.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.b2b.B2bApi
import com.dollar.marketplace.v3.theme.DollarMarketplaceTheme
import com.dollar.marketplace.v3.theme.ErrorRed
import com.dollar.marketplace.v3.theme.RoyalBlack
import com.dollar.marketplace.v3.theme.RoyalBlackLight
import com.dollar.marketplace.v3.theme.RoyalGold
import com.dollar.marketplace.v3.theme.SuccessGreen
import com.dollar.marketplace.v3.theme.TextPrimary
import com.dollar.marketplace.v3.theme.TextSecondary
import kotlinx.coroutines.launch

/** شريحة ترحيب واحدة في تدفّق الدخول التجريبي. */
private data class DemoWelcomeSlide(val badge: String, val title: String, val body: String)

/** دور تجريبي معروض كبطاقة قابلة للضغط. */
private data class DemoRole(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

/** الشرائح الثلاث كما تنصّ المواصفة: عالم تجاري متكامل / طلباتك تحت السيطرة / حساباتك بكل شفافية. */
private val DEMO_SLIDES = listOf(
    DemoWelcomeSlide(
        badge = "١",
        title = "عالم تجاري متكامل",
        body = "منصّة دولار تجمع المشترين والوكالات والمخازن والحسابات في مكان واحد: كتالوج موحّد، وطلبات، وفواتير."
    ),
    DemoWelcomeSlide(
        badge = "٢",
        title = "طلباتك تحت السيطرة",
        body = "تابع كل طلب من الإنشاء حتى التسليم: تجهيز في المخزن، ثم شحن، ثم تأكيد استلام موثّق بلمسة واحدة."
    ),
    DemoWelcomeSlide(
        badge = "٣",
        title = "حساباتك بكل شفافية",
        body = "فاتورة لكل طلب، وسند قبض مُرحَّل لكل دفعة، وكشف حساب دقيق لا يقبل التعديل بعد الاعتماد."
    )
)

/** الأدوار التجريبية الخمسة — القيم مطابقة لما يقبله الخادم في `/api/v3/demo/login`. */
private val DEMO_ROLES = listOf(
    DemoRole("ADMIN", "مدير المنصة", "المستخدمون والوكالات والتقارير المالية", Icons.Default.Shield),
    DemoRole("WAREHOUSE", "مسؤول المخزن", "تجهيز الطلبات وشحنها للعملاء", Icons.Default.Inventory),
    DemoRole("COLLECTOR", "محصل ميداني", "تحصيل الدفعات وإصدار سندات القبض", Icons.Default.Payments),
    DemoRole("ACCOUNTANT", "المحاسب", "اعتماد الفواتير والسندات وكشوف الحساب", Icons.Default.Calculate),
    DemoRole("BUYER", "مؤسسة تجارية", "تصفّح الكتالوج وإنشاء الطلبات ومتابعتها", Icons.Default.ShoppingCart)
)

/*
 * نفس مخزن جلسة B2bState (اسم الملف ومفتاح الرمز) حتى تُستعاد الجلسة التجريبية
 * عند إعادة فتح التطبيق تماماً كما تُستعاد جلسة الدخول العادي.
 */
private const val SESSION_PREFS = "dollar_b2b_session"
private const val SESSION_TOKEN_KEY = "session_token"

private fun saveDemoSession(context: Context, token: String) {
    context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(SESSION_TOKEN_KEY, token)
        .apply()
}

/**
 * الدخول التجريبي: مرحلتان —
 *  1) ثلاث شرائح ترحيب (متابعة / تخطٍّ / رجوع).
 *  2) خمس بطاقات أدوار؛ الضغط يستدعي `demoLogin(role)` ثم يحفظ الجلسة وينادي `onAuthenticated`.
 * لا يستورد مكتبات غير موجودة، وكل الألوان من حزمة `v3.theme`.
 */
@Composable
fun DemoLoginScreen(onAuthenticated: (role: String) -> Unit) {
    val context = LocalContext.current
    val api = remember { B2bApi() }
    val scope = rememberCoroutineScope()

    var showRoles by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var loadingRole by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val busy = loadingRole != null

    fun openRoles() {
        error = null
        showRoles = true
    }

    fun chooseRole(role: DemoRole) {
        if (busy) return
        error = null
        loadingRole = role.id
        scope.launch {
            try {
                val result = api.demoLogin(role.id)
                val token = result.accessToken
                if (token.isBlank()) {
                    error = "لم يُعد الخادم رمز جلسة صالحاً"
                    loadingRole = null
                } else {
                    saveDemoSession(context, token)
                    onAuthenticated(result.user.role.ifBlank { role.id })
                }
            } catch (e: Exception) {
                error = e.message ?: "تعذّر الدخول التجريبي، حاول مجدداً"
                loadingRole = null
            }
        }
    }

    DollarMarketplaceTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF20190A), RoyalBlack, RoyalBlack))
                )
        ) {
            if (showRoles) {
                RoleStage(
                    busy = busy,
                    loadingRole = loadingRole,
                    error = error,
                    onRole = { chooseRole(it) },
                    onBack = {
                        if (!busy) {
                            error = null
                            showRoles = false
                        }
                    }
                )
            } else {
                WelcomeStage(
                    index = index,
                    onIndex = { index = it },
                    onFinish = { openRoles() },
                    onSkip = { openRoles() }
                )
            }
        }
    }
}

/* ------------------------------ المرحلة الأولى ------------------------------ */

@Composable
private fun WelcomeStage(
    index: Int,
    onIndex: (Int) -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val slide = DEMO_SLIDES[index.coerceIn(0, DEMO_SLIDES.lastIndex)]
    val isLast = index >= DEMO_SLIDES.lastIndex

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(RoyalGold, Color(0xFFB8912A)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$", color = RoyalBlack, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("دولار", color = RoyalGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("نسخة تجريبية", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            TextButton(onClick = onSkip) {
                Text("تخطي", color = TextSecondary, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(30.dp))

        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(RoyalBlackLight),
            contentAlignment = Alignment.Center
        ) {
            Text(slide.badge, color = RoyalGold, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(22.dp))

        Text(slide.title, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)

        Spacer(Modifier.height(12.dp))

        Text(slide.body, color = TextSecondary, fontSize = 14.sp, lineHeight = 24.sp)

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DEMO_SLIDES.indices.forEach { dot ->
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (dot == index) 26.dp else 8.dp, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (dot == index) RoyalGold else Color(0xFF2E2E34))
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { if (isLast) onFinish() else onIndex(index + 1) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoyalGold, contentColor = RoyalBlack)
        ) {
            Text("متابعة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { if (index > 0) onIndex(index - 1) else onSkip() },
            enabled = index > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("رجوع", color = TextPrimary, fontSize = 14.sp)
        }
    }
}

/* ------------------------------ المرحلة الثانية ------------------------------ */

@Composable
private fun RoleStage(
    busy: Boolean,
    loadingRole: String?,
    error: String?,
    onRole: (DemoRole) -> Unit,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Text("اختر دورك التجريبي", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text("دخول فوري بلا كلمة مرور — خمسة أدوار جاهزة على الخادم.", color = TextSecondary, fontSize = 13.sp)

        Spacer(Modifier.height(20.dp))

        if (error != null) {
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(RoyalBlackLight)
                    .padding(10.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        DEMO_ROLES.forEach { role ->
            RoleCard(
                role = role,
                loading = loadingRole == role.id,
                enabled = !busy,
                onClick = { onRole(role) }
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            onClick = onBack,
            enabled = !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("رجوع", color = TextPrimary, fontSize = 14.sp)
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "الوضع التجريبي للعرض فقط؛ بيانات كل دور تُنشأ على الخادم ولا تؤثر على الحسابات الحقيقية.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun RoleCard(
    role: DemoRole,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(RoyalBlackLight)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (enabled) RoyalGold else Color(0xFF3A3A41)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = role.icon,
                contentDescription = role.title,
                tint = RoyalBlack,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(role.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(role.subtitle, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }

        Spacer(Modifier.width(10.dp))

        if (loading) {
            CircularProgressIndicator(color = RoyalGold, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text("دخول", color = RoyalGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
