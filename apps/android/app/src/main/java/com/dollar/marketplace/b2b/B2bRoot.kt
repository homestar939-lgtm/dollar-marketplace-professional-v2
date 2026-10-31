package com.dollar.marketplace.b2b

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.DollarTheme
import com.dollar.marketplace.Pal

/**
 * نقطة إقلاع تطبيق B2B:
 *   الترحيب (مرة واحدة) ← الدخول ← لوحة الدور.
 * الاتجاه RTL مثبّت على مستوى الجذر، واللوحة تُختار من دور المستخدم القادم من الخادم.
 */
@Composable
fun B2bRoot() {
    val context = LocalContext.current
    val state = remember { B2bState(context.applicationContext) }
    LaunchedEffect(Unit) { state.bootstrap() }

    DollarTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val user = state.user
            when {
                state.bootstrapping -> B2bSplash()
                !state.onboardingSeen -> B2bOnboarding(onDone = { state.completeOnboarding() })
                user == null -> B2bLogin(state)
                else -> B2bShell(state)
            }
        }
    }
}

@Composable
private fun B2bSplash() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Pal.goldBrush),
                contentAlignment = Alignment.Center
            ) {
                Text("$", color = Pal.Black, fontWeight = FontWeight.Black, fontSize = 26.sp)
            }
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = Pal.Gold, strokeWidth = 2.dp)
        }
    }
}

private fun roleLabel(role: String): String = when (role) {
    "ADMIN" -> "لوحة المدير"
    "WAREHOUSE" -> "لوحة المخزن"
    "COLLECTOR" -> "لوحة المحصل"
    "ACCOUNTANT" -> "لوحة المحاسب"
    "BUYER" -> "لوحة المشتري"
    "SELLER" -> "لوحة البائع"
    "SUPPORT" -> "لوحة الدعم"
    else -> role
}

/** القشرة: شريط علوي ثابت + محتوى لوحة الدور. */
@Composable
private fun B2bShell(state: B2bState) {
    val user = state.user ?: return
    Column(
        Modifier
            .fillMaxSize()
            .background(Pal.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Pal.Onyx)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Pal.goldBrush),
                contentAlignment = Alignment.Center
            ) {
                Text("$", color = Pal.Black, fontWeight = FontWeight.Black, fontSize = 17.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(roleLabel(user.role), color = Pal.GoldSoft, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(user.name, color = Pal.Muted, fontSize = 11.sp)
            }
            TextButton(onClick = { state.logout() }) {
                Icon(Icons.Default.Logout, contentDescription = "خروج", tint = Pal.Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("خروج", color = Pal.Muted, fontSize = 12.sp)
            }
        }

        Box(Modifier.weight(1f)) {
            when (user.role) {
                "BUYER" -> B2bBuyerScreen(state)
                "WAREHOUSE" -> B2bWarehouseScreen(state)
                "COLLECTOR" -> B2bCollectorScreen(state)
                "ACCOUNTANT" -> B2bAccountantScreen(state)
                "ADMIN" -> B2bAdminScreen(state)
                else -> NoDashboard(state)
            }
        }
    }
}

@Composable
private fun NoDashboard(state: B2bState) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("لا توجد لوحة مخصّصة لدورك بعد.", color = Pal.Muted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        B2bGhostButton(text = "تسجيل الخروج") { state.logout() }
    }
}
