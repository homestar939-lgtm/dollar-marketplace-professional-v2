package com.dollar.marketplace.b2b

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** سطر سلة محلي — الأسعار تُحسب على الخادم دائماً، وما هنا للعرض فقط. */
data class B2bCartLine(val product: B2bProduct, val quantity: Int)

/**
 * حالة التطبيق: الجلسة، شاشات الترحيب، والسلة.
 * - "شوهدت الشاشات" تُحفظ في التخزين المحلي فلا تعود تظهر بعد الإكمال.
 * - الجلسة تُحفظ لتُستعاد عند فتح التطبيق من جديد.
 */
class B2bState(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dollar_b2b_session", Context.MODE_PRIVATE)

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var onboardingSeen by mutableStateOf(prefs.getBoolean(KEY_ONBOARDING, false))
        private set

    var token by mutableStateOf(prefs.getString(KEY_TOKEN, null))
        private set

    var user by mutableStateOf<B2bUser?>(null)
        private set

    var bootstrapping by mutableStateOf(true)
        private set

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    val cart = mutableStateListOf<B2bCartLine>()

    val api = B2bApi { token }

    val isLoggedIn: Boolean get() = token != null && user != null

    fun clearError() {
        error = null
    }

    /** إتمام شاشات الترحيب: تُحفظ العلامة ثم يُنتقل للدخول. */
    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
        onboardingSeen = true
    }

    /** استعادة الجلسة عند الإقلاع: الرمز موجود؟ تحقق من صحته ثم اطلب المستخدم. */
    fun bootstrap() {
        scope.launch {
            val existing = token
            if (existing.isNullOrBlank()) {
                bootstrapping = false
                return@launch
            }
            try {
                user = api.me().user
            } catch (_: Exception) {
                prefs.edit().remove(KEY_TOKEN).apply()
                token = null
                user = null
            } finally {
                bootstrapping = false
            }
        }
    }

    private fun persist(newToken: String, newUser: B2bUser) {
        prefs.edit().putString(KEY_TOKEN, newToken).apply()
        token = newToken
        user = newUser
        error = null
    }

    fun login(email: String, password: String) {
        scope.launch {
            busy = true
            error = null
            try {
                val result = api.login(email.trim(), password)
                persist(result.accessToken, result.user)
            } catch (e: Exception) {
                error = e.message ?: "تعذّر تسجيل الدخول"
            } finally {
                busy = false
            }
        }
    }

    /** الدخول عبر Google: يُرسَل idToken إلى الخادم ليتحقق منه بنفسه. */
    fun loginWithGoogle(idToken: String) {
        scope.launch {
            busy = true
            error = null
            try {
                val result = api.google(idToken)
                persist(result.accessToken, result.user)
            } catch (e: Exception) {
                error = e.message ?: "تعذّر الدخول عبر Google"
            } finally {
                busy = false
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        scope.launch {
            busy = true
            error = null
            try {
                api.register(name.trim(), email.trim(), password)
                error = null
                info = "تم إنشاء الحساب. تحقق من بريدك لتفعيله قبل الدخول."
            } catch (e: Exception) {
                error = e.message ?: "تعذّر إنشاء الحساب"
            } finally {
                busy = false
            }
        }
    }

    var info by mutableStateOf<String?>(null)
        private set

    fun clearInfo() {
        info = null
    }

    fun logout() {
        scope.launch {
            runCatching { api.logout() }
            prefs.edit().remove(KEY_TOKEN).apply()
            token = null
            user = null
            cart.clear()
        }
    }

    /* --------------------------------- السلة --------------------------------- */

    fun addToCart(product: B2bProduct) {
        val index = cart.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val line = cart[index]
            cart[index] = line.copy(quantity = line.quantity + product.minimumOrderQuantity.coerceAtLeast(1))
        } else {
            cart.add(B2bCartLine(product, product.minimumOrderQuantity.coerceAtLeast(1)))
        }
    }

    fun setQuantity(productId: String, quantity: Int) {
        val index = cart.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (quantity <= 0) cart.removeAt(index) else cart[index] = cart[index].copy(quantity = quantity)
        }
    }

    fun clearCart() = cart.clear()

    fun reportError(message: String) {
        error = message
    }

    private companion object {
        const val KEY_ONBOARDING = "onboarding_seen_v1"
        const val KEY_TOKEN = "session_token"
    }
}
