package com.dollar.marketplace.b2b

import android.app.PendingIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.BuildConfig
import com.dollar.marketplace.Pal
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * شاشة الدخول: بريد وكلمة مرور، أو حساب Google.
 * الرمز المميّز من Google يُرسَل للخادم الذي يتحقق منه بنفسه — لا نثق ببريد يرسله العميل.
 */
@Composable
fun B2bLogin(state: B2bState) {
    var mode by remember { mutableStateOf("login") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                state.reportError("لم يُعد Google رمز هوية صالحاً")
            } else {
                state.loginWithGoogle(idToken)
            }
        } catch (error: Exception) {
            state.reportError("تعذّر إكمال الدخول عبر Google: ${error.message ?: "خطأ غير معروف"}")
        }
    }

    fun launchGoogle() {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (clientId.isBlank()) {
            state.reportError("لم يُضبط GOOGLE_WEB_CLIENT_ID في إعدادات البناء.")
            return
        }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, options)
        client.signInIntentSender
            .addOnSuccessListener { pendingIntent: PendingIntent ->
                googleLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            }
            .addOnFailureListener {
                state.reportError("تعذّر بدء الدخول عبر Google")
            }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF191307), Pal.Black)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("دولار", color = Pal.Gold, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                "منصة التجارة بين الشركات — سجّل الدخول بحسابك الوظيفي.",
                color = Pal.Muted,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(26.dp))

            B2bMessage(state.error, isError = true)
            B2bMessage(state.info, isError = false)

            if (mode == "register") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم الكامل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("البريد الإلكتروني") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            Spacer(Modifier.height(18.dp))

            if (state.busy) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Pal.Gold, strokeWidth = 2.dp)
                }
                Spacer(Modifier.height(12.dp))
            }

            B2bPrimaryButton(
                text = if (mode == "login") "دخول" else "إنشاء حساب مشترٍ",
                enabled = !state.busy && email.isNotBlank() && password.length >= 8
            ) {
                if (mode == "login") state.login(email, password)
                else state.register(name, email, password)
            }

            Spacer(Modifier.height(10.dp))

            B2bGhostButton(text = "الدخول عبر Google", enabled = !state.busy) { launchGoogle() }

            Spacer(Modifier.height(10.dp))

            B2bGhostButton(
                text = if (mode == "login") "ليس لدي حساب — تسجيل" else "لدي حساب — دخول"
            ) {
                mode = if (mode == "login") "register" else "login"
                state.clearError()
                state.clearInfo()
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "الحساب لا يُفعّل قبل التحقق من البريد. التسجيل الذاتي للمشترين فقط، وأدوار الموظفين يمنحها المدير.",
                color = Pal.Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pal.Gold,
    unfocusedBorderColor = Pal.Stroke,
    focusedLabelColor = Pal.Gold,
    unfocusedLabelColor = Pal.Muted,
    focusedTextColor = Pal.Text,
    unfocusedTextColor = Pal.Text,
    cursorColor = Pal.Gold
)
