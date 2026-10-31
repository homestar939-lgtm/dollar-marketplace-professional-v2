package com.dollar.marketplace.b2b

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dollar.marketplace.Pal

/** شريحة تعريفية واحدة: عنوان تسويقي + وصف + مزايا. */
private data class OnboardingSlide(
    val badge: String,
    val title: String,
    val description: String,
    val points: List<String>
)

private val SLIDES = listOf(
    OnboardingSlide(
        badge = "١",
        title = "دولار: تجارة الجملة بثقة",
        description = "منصّة B2B تربط المشترين بالوكالات مباشرة، بأسعار معلنة وفواتير نظامية — لا وسطاء ولا مفاوضات غامضة.",
        points = listOf(
            "كتالوج موحّد لكل وكالة",
            "أسعار وأرصدة لحظية",
            "طلبات جملة بكميات محددة"
        )
    ),
    OnboardingSlide(
        badge = "٢",
        title = "افتِراس مالي مُحكم",
        description = "كل طلب يتحوّل إلى فاتورة معتمدة، وكل دفعة إلى سند قبض يُرحَّل في دفتر لا يقبل التعديل بعد الاعتماد.",
        points = listOf(
            "فاتورة لكل طلب مع لقطة سعر ثابتة",
            "سندات قبض باعتماد المحاسب",
            "كشف حساب دقيق لكل مشترٍ"
        )
    ),
    OnboardingSlide(
        badge = "٣",
        title = "أدوار واضحة، صلاحيات محسوبة",
        description = "المخزن يجهّز ويشحن، المحصل يقبض، المحاسب يعتمد، والمشتري يتابع — كلٌّ يرى ما يخصه فقط.",
        points = listOf(
            "خمس لوحات مخصّصة للأدوار",
            "تجهيز جزئي للطلبات",
            "عمولة محصل محسوبة عند الاعتماد"
        )
    )
)

/**
 * شاشات الترحيب: ثلاث شرائح، مؤشر تقدّم، «متابعة» و«رجوع» و«تخطٍّ إلى الدخول».
 * عند الإكمال تُحفظ العلامة فلا تظهر الشاشات مرة أخرى.
 */
@Composable
fun B2bOnboarding(onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val slide = SLIDES[index]
    val last = index == SLIDES.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1408), Pal.Black, Pal.Black)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                            .background(Pal.goldBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$", color = Pal.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("دولار", color = Pal.GoldSoft, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text(
                    text = "تخطٍّ إلى الدخول",
                    color = Pal.Muted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onDone() }
                        .padding(6.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Pal.SurfaceAlt),
                contentAlignment = Alignment.Center
            ) {
                Text(slide.badge, color = Pal.Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = slide.title,
                color = Pal.Text,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = slide.description,
                color = Pal.Muted,
                fontSize = 14.sp,
                lineHeight = 24.sp
            )

            Spacer(Modifier.height(22.dp))

            slide.points.forEach { point ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Pal.Gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(point, color = Pal.Text, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(26.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SLIDES.indices.forEach { dot ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (dot == index) 26.dp else 8.dp, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (dot == index) Pal.Gold else Pal.Stroke)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            B2bPrimaryButton(
                text = if (last) "ابدأ الآن" else "متابعة",
                enabled = true
            ) {
                if (last) onDone() else index += 1
            }

            Spacer(Modifier.height(10.dp))

            if (index > 0) {
                B2bGhostButton(text = "رجوع") { index -= 1 }
            } else {
                B2bGhostButton(text = "لدي حساب — دخول") { onDone() }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
