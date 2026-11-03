package com.dollar.marketplace.v3.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.v3.theme.RoyalBlack
import com.dollar.marketplace.v3.theme.RoyalGold
import com.dollar.marketplace.v3.theme.RoyalGoldLight
import com.dollar.marketplace.v3.theme.TextPrimary
import com.dollar.marketplace.v3.theme.TextSecondary

/** شريحة تعريفية واحدة. */
private data class OnboardingPage(
    val badge: String,
    val title: String,
    val body: String,
    val points: List<String>
)

private val PAGES = listOf(
    OnboardingPage(
        badge = "١",
        title = "تجارة الجملة بثقة",
        body = "منصّة تربط المشترين بالوكالات مباشرة: كتالوج موحّد، أسعار معلنة، ولا وسطاء.",
        points = listOf("كتالوج وأرصدة لحظية", "أسعار جملة واضحة", "طلبات بكميات محددة")
    ),
    OnboardingPage(
        badge = "٢",
        title = "فاتورة لكل طلب",
        body = "يتحوّل كل طلب إلى فاتورة معتمدة، وكل دفعة إلى سند قبض مُرحَّل لا يقبل التعديل بعد الاعتماد.",
        points = listOf("لقطة سعر ثابتة", "سندات قبض باعتماد المحاسب", "كشف حساب دقيق")
    ),
    OnboardingPage(
        badge = "٣",
        title = "تسليم موثَّق",
        body = "يُؤكَّد التسليم من العميل نفسه، ويُختم الطلب بتاريخ التسليم واسم المستلم في السجل.",
        points = listOf("تأكيد استلام بلمسة", "سجل زمني كامل", "إشعار فوري للفريق")
    )
)

/**
 * شاشات الترحيب V3: ثلاث شرائح مع AnimatedContent، مؤشر تقدّم، وأزرار متابعة/رجوع/تخطٍّ.
 * الاهتزاز اللمسي (haptics) يُؤكَّد عند كل انتقال.
 */
@Composable
fun OnboardingScreenV3(onComplete: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val isLast = index == PAGES.lastIndex

    fun goTo(next: Int) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        index = next
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF20190A), RoyalBlack, RoyalBlack)
                )
            )
    ) {
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
                            .background(Brush.linearGradient(listOf(RoyalGoldLight, RoyalGold))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$", color = RoyalBlack, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("دولار", color = RoyalGoldLight, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                TextButton(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onComplete()
                }) {
                    Text("تخطٍّ", color = TextSecondary, fontSize = 13.sp)
                }
            }

            AnimatedContent(
                targetState = index,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { full -> full } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally { full -> -full } + fadeOut(tween(180)))
                    } else {
                        (slideInHorizontally { full -> -full } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally { full -> full } + fadeOut(tween(180)))
                    }
                },
                label = "onboarding-page"
            ) { page ->
                OnboardingPageBody(PAGES[page])
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PAGES.indices.forEach { dot ->
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
                onClick = {
                    if (isLast) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onComplete()
                    } else {
                        goTo(index + 1)
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
                Text(
                    text = if (isLast) "ابدأ الآن" else "متابعة",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { if (index > 0) goTo(index - 1) else onComplete() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (index > 0) "رجوع" else "لدي حساب — دخول",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageBody(page: OnboardingPage) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(28.dp))

        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF23232A)),
            contentAlignment = Alignment.Center
        ) {
            Text(page.badge, color = RoyalGold, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = page.title,
            color = TextPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.body,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(22.dp))

        page.points.forEach { point ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = RoyalGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(point, color = TextPrimary, fontSize = 13.sp)
            }
        }
    }
}
