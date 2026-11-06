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
 * لوحة المدير: منح الأدوار (المشتري الوحيد الذي يسجّل نفسه)، إنشاء الوكالات،
 * ومتابعة المستخدمين. إنشاء الحساب هنا هو الطريق الوحيد لمنح دور موظف.
 */
@Composable
fun B2bAdminScreen(state: B2bState) {
    var users by remember { mutableStateOf<List<B2bUser>>(emptyList()) }
    var agencies by remember { mutableStateOf<List<B2bAgency>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var roleIndex by remember { mutableIntStateOf(0) }
    var agencyName by remember { mutableStateOf("") }
    var agencySlug by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val roles = listOf("WAREHOUSE", "COLLECTOR", "ACCOUNTANT", "SUPPORT")

    LaunchedEffect(refresh, state.realtimeRefreshVersion) {
        loading = true
        runCatching { state.api.users() }.onSuccess { users = it.data }.onFailure { message = it.message }
        runCatching { state.api.agencies() }.onSuccess { agencies = it.data }
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

        B2bMessage(message, isError = true)

        B2bCard(title = "إنشاء حساب موظف") {
            field("الاسم", name) { name = it }
            field("البريد الإلكتروني", email) { email = it }
            field("كلمة المرور المؤقتة", password) { password = it }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEachIndexed { index, role ->
                    TextButton(onClick = { roleIndex = index }) {
                        Text(
                            roleLabelShort(role),
                            color = if (roleIndex == index) Pal.Success else Pal.Muted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            B2bPrimaryButton(
                text = "إنشاء الحساب",
                enabled = !state.busy && name.isNotBlank() && email.isNotBlank() && password.length >= 8
            ) {
                scope.launch {
                    runCatching {
                        state.api.createUser(
                            B2bCreateUserBody(
                                name = name.trim(),
                                email = email.trim(),
                                password = password,
                                role = roles[roleIndex]
                            )
                        )
                    }
                        .onSuccess {
                            message = "أُنشئ حساب ${it.user.email} بدور ${it.user.role}"
                            name = ""; email = ""; password = ""
                            refresh += 1
                        }
                        .onFailure { message = it.message }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("التسجيل الذاتي لا يمنح إلا دور مشترٍ؛ أدوار الموظفين تُمنح من هنا فقط.", color = Pal.Muted, fontSize = 11.sp)
        }

        B2bCard(title = "إنشاء وكالة") {
            field("اسم الوكالة", agencyName) { agencyName = it }
            field("المعرّف (slug)", agencySlug) { agencySlug = it }
            Spacer(Modifier.height(8.dp))
            B2bPrimaryButton(
                text = "إنشاء",
                enabled = !state.busy && agencyName.isNotBlank() && agencySlug.isNotBlank()
            ) {
                scope.launch {
                    runCatching { state.api.createAgency(B2bCreateAgencyBody(agencyName.trim(), agencySlug.trim())) }
                        .onSuccess {
                            message = "أُنشئت الوكالة ${it.agency.name}"
                            agencyName = ""; agencySlug = ""
                            refresh += 1
                        }
                        .onFailure { message = it.message }
                }
            }
        }

        B2bCard(title = "المستخدمون (${users.size})") {
            users.forEach { user ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(user.name, color = Pal.Text, fontSize = 13.sp)
                        Text("${user.email} · ${user.role}", color = Pal.Muted, fontSize = 11.sp)
                    }
                    B2bStatus(user.status)
                }
            }
        }

        B2bCard(title = "الوكالات (${agencies.size})") {
            agencies.forEach { agency ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(agency.name, color = Pal.Text, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(agency.slug, color = Pal.Muted, fontSize = 11.sp)
                    }
                    B2bStatus(agency.status)
                }
            }
        }
    }
}

private fun roleLabelShort(role: String): String = when (role) {
    "WAREHOUSE" -> "مخزن"
    "COLLECTOR" -> "محصل"
    "ACCOUNTANT" -> "محاسب"
    "SUPPORT" -> "دعم"
    else -> role
}

@Composable
private fun field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
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
}
