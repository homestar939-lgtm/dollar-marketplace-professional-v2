package com.dollar.marketplace.v3.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.v3.theme.ErrorRed
import com.dollar.marketplace.v3.theme.RoyalBlack
import com.dollar.marketplace.v3.theme.RoyalBlackLight
import com.dollar.marketplace.v3.theme.RoyalGold
import com.dollar.marketplace.v3.theme.RoyalGoldLight
import com.dollar.marketplace.v3.theme.SuccessGreen
import com.dollar.marketplace.v3.theme.TextPrimary
import com.dollar.marketplace.v3.theme.TextSecondary
import kotlinx.coroutines.launch

/** مراحل الدخول: إدخال الرقم ← رمز التحقق ← انتظار الخادم. */
enum class LoginStep { PHONE, OTP, LOADING }

/**
 * ألوان حقول الإدخال. `OutlinedTextFieldDefaults.colors` دالة @Composable،
 * لذا يجب وسم الغلاف بها وإلا فشل التصريف:
 * "Functions which invoke @Composable functions must be marked with the @Composable annotation".
 */
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RoyalGold,
    unfocusedBorderColor = Color(0xFF3A3A41),
    focusedLabelColor = RoyalGold,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = RoyalGold
)

/**
 * شاشة الدخول بالجوال V3.
 * `requestOtp` و`verifyOtp` عمليتان معلّقتان تُنفَّذان على الخادم؛ النتيجة من نوع kotlin.Result.
 *
 * ملاحظة: لا نستخدم مكتبة تأثير اللمعان الانزلاقي (غير موجودة في المشروع) — حالة الانتظار
 * نبض شفافية على CircularProgressIndicator عبر rememberInfiniteTransition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneLoginScreenV3(
    onLoginSuccess: (String) -> Unit,
    requestOtp: suspend (String) -> Result<Unit>,
    verifyOtp: suspend (String, String) -> Result<String>
) {
    var step by remember { mutableStateOf(LoginStep.PHONE) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val pulse = rememberInfiniteTransition(label = "loading-pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading-alpha"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1C1608), RoyalBlack, RoyalBlack))
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(RoyalGoldLight, RoyalGold))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$", color = RoyalBlack, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text("دخول دولار", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text = when (step) {
                    LoginStep.PHONE -> "أدخل رقم جوالك لإرسال رمز التحقق"
                    LoginStep.OTP -> "أدخل الرمز المُرسَل إلى $phone"
                    LoginStep.LOADING -> "جارٍ التحقق من الرمز…"
                },
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(18.dp))

            when (step) {
                LoginStep.PHONE -> {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { raw -> phone = raw.filter { it.isDigit() }.take(15) },
                        label = { Text("رقم الجوال") },
                        placeholder = { Text("9xxxxxxxx") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val value = phone.trim()
                            if (value.length < 8) {
                                error = "أدخل رقم جوال صحيحاً (٨ أرقام على الأقل)"
                            } else {
                                error = null
                                step = LoginStep.LOADING
                                scope.launch {
                                    requestOtp(value)
                                        .onSuccess { step = LoginStep.OTP }
                                        .onFailure {
                                            error = it.message ?: "تعذّر إرسال الرمز، حاول مجدداً"
                                            step = LoginStep.PHONE
                                        }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = RoyalBlack
                        )
                    ) {
                        Text("أرسل رمز التحقق", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                LoginStep.OTP -> {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { raw -> code = raw.filter { it.isDigit() }.take(6) },
                        label = { Text("رمز التحقق") },
                        placeholder = { Text("000000") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val value = code.trim()
                            if (value.length < 4) {
                                error = "الرمز قصير جداً"
                            } else {
                                error = null
                                step = LoginStep.LOADING
                                scope.launch {
                                    verifyOtp(phone.trim(), value)
                                        .onSuccess { token -> onLoginSuccess(token) }
                                        .onFailure {
                                            attempts += 1
                                            error = it.message ?: "رمز غير صحيح، حاول مجدداً"
                                            step = LoginStep.OTP
                                        }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalGold,
                            contentColor = RoyalBlack
                        )
                    ) {
                        Text("تحقّق ودخول", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            error = null
                            code = ""
                            step = LoginStep.PHONE
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("تعديل الرقم", color = TextSecondary, fontSize = 13.sp)
                    }
                }

                LoginStep.LOADING -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(46.dp)
                                .alpha(pulseAlpha),
                            color = RoyalGold,
                            strokeWidth = 3.dp
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "جارٍ التحقق…",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(0.4f + pulseAlpha * 0.6f)
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error ?: "",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RoyalBlackLight)
                        .padding(10.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = if (attempts == 0) {
                    "لن نشارك رقمك مع أي طرف ثالث. الرمز صالح لخمس دقائق."
                } else {
                    "محاولات التحقق: $attempts — بعد خمس محاولات يُقفل الرقم مؤقتاً."
                },
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "مؤشر الاتصال آمن",
                color = SuccessGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
