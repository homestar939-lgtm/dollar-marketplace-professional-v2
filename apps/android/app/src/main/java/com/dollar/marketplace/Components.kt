package com.dollar.marketplace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

/* ============================================================================
 *  تفاعلات الضغط — نابضة بحركة سبرينغ + توهّج ذهبي عند الضغط
 * ========================================================================== */

enum class StatusTone { Neutral, Gold, Success, Danger, Info }

fun StatusTone.color(): Color = when (this) {
    StatusTone.Neutral -> Pal.Muted
    StatusTone.Gold -> Pal.Gold
    StatusTone.Success -> Pal.Success
    StatusTone.Danger -> Pal.Danger
    StatusTone.Info -> Pal.Info
}

/** يحوّل أي Composable إلى عنصر تفاعلي مع تصغير/توهّج عند الضغط. */
@Composable
fun Pressable(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    glow: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "press-scale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (pressed && glow) 0.55f else 0f,
        animationSpec = tween(160),
        label = "press-glow"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .then(
                if (glowAlpha > 0f) Modifier.background(Pal.Gold.copy(alpha = glowAlpha * 0.18f))
                else Modifier
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
    ) {
        content()
    }
}

/* ============================================================================
 * البطاقات والأسطح
 * ========================================================================== */

@Composable
fun GoldCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    brush: Brush = Pal.cardBrush,
    borderColor: Color = Pal.Stroke,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val base = modifier
        .clip(shape)
        .background(brush)
        .border(BorderStroke(1.dp, borderColor), shape)
    if (onClick == null) {
        Box(base) { content() }
    } else {
        Pressable(modifier = modifier, shape = shape, onClick = onClick) {
            Box(
                Modifier
                    .background(brush)
                    .border(BorderStroke(1.dp, borderColor), shape)
            ) { content() }
        }
    }
}

/** مساعد موحّد لإظهار/إخفاء عناصر القائمة المنسدلة بحركة. */
@Composable
fun AnimatedSlot(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + expandVertically(tween(200)) + scaleIn(initialScale = 0.96f),
        exit = fadeOut(tween(140)) + shrinkVertically(tween(160)) + scaleOut(targetScale = 0.96f)
    ) { content() }
}

/** شريط حالة ملوّن بالعربية. */
@Composable
fun StatusPill(status: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val c = tone.color()
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = c.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, c.copy(alpha = 0.45f))
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FiberManualRecord, null, tint = c, modifier = Modifier.size(8.dp))
            Spacer(Modifier.width(6.dp))
            Text(status, color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/* ============================================================================
 * الأزرار
 * ========================================================================== */

/** الزر الذهبي الرئيسي بتدرّج وتأثير ضغط. */
@Composable
fun GoldButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape = MaterialTheme.shapes.medium,
    onClick: () -> Unit
) {
    Pressable(
        modifier = modifier,
        shape = shape,
        enabled = enabled && !loading,
        pressedScale = 0.95f,
        onClick = onClick
    ) {
        Row(
            Modifier
                .background(if (enabled) Pal.goldBrush else Brush.linearGradient(listOf(Pal.SurfaceHigh, Pal.SurfaceHigh)))
                .alpha(if (enabled) 1f else 0.55f)
                .padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    color = Pal.Black,
                    strokeWidth = 2.dp
                )
            } else if (icon != null) {
                Icon(icon, null, tint = Pal.Black, modifier = Modifier.size(18.dp))
            }
            if (loading || icon != null) Spacer(Modifier.width(8.dp))
            Text(
                text,
                color = Pal.Black,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

/** زر ثانوي بإطار ذهبي فقط. */
@Composable
fun OutlineGoldButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Pal.Gold,
    onClick: () -> Unit
) {
    Pressable(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        pressedScale = 0.95f,
        onClick = onClick
    ) {
        Row(
            Modifier
                .background(tint.copy(alpha = 0.08f))
                .border(BorderStroke(1.dp, tint.copy(alpha = 0.55f)), MaterialTheme.shapes.medium)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
            }
            Text(text, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        }
    }
}

/* ============================================================================
 * الأزرار العائمة (Floating Action Button) + قائمة السرعة
 * ========================================================================== */

/** زر عائم مخصّص بتدرّج ذهبي — لا يعتمد على أي overload قابل للالتباس. */
@Composable
fun DollarFab(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        label = "fab-scale"
    )
    Row(
        modifier
            .scale(scale)
            .clip(CircleShape)
            .background(Pal.goldBrush)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Pal.Black, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Pal.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
    }
}

/** زر عائم واحد مع قائمة إجراءات سريعة تتمدد عمودياً بحركة متتابعة. */
@Composable
fun ExpandedFabMenu(
    items: List<FabAction>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEachIndexed { index, action ->
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(140, delayMillis = index * 25)) +
                        expandVertically(tween(180, delayMillis = index * 25)),
                exit = fadeOut(tween(90)) + shrinkVertically(tween(120))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Pal.SurfaceHigh,
                        border = BorderStroke(1.dp, Pal.Stroke)
                    ) {
                        Text(
                            action.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pal.Text
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Pressable(
                        shape = CircleShape,
                        pressedScale = 0.88f,
                        onClick = { expanded = false; action.onClick() }
                    ) {
                        Box(
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Pal.SurfaceHigh)
                                .border(BorderStroke(1.dp, action.tint.copy(alpha = 0.35f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(action.icon, null, tint = action.tint, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val iconRotation by animateFloatAsState(
            targetValue = if (expanded) 135f else 0f,
            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
            label = "fab-rotation"
        )
        val fabScale by animateFloatAsState(
            targetValue = if (pressed) 0.9f else 1f,
            label = "fab-menu-scale"
        )
        Row(
            Modifier
                .scale(fabScale)
                .clip(CircleShape)
                .background(if (expanded) Pal.SurfaceHigh else Pal.Gold)
                .border(
                    BorderStroke(1.dp, if (expanded) Pal.Gold.copy(alpha = 0.5f) else Pal.GoldDeep),
                    CircleShape
                )
                .clickable(interactionSource = interaction, indication = null) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Add,
                null,
                tint = if (expanded) Pal.Gold else Pal.Black,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(iconRotation)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (expanded) "إغلاق" else "إجراءات سريعة",
                color = if (expanded) Pal.Gold else Pal.Black,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

data class FabAction(val label: String, val icon: ImageVector, val tint: Color = Pal.Gold, val onClick: () -> Unit)

/* ============================================================================
 * القوائم المنسدلة (Dropdown)
 * ========================================================================== */

data class DropdownOption(val value: String, val label: String, val hint: String? = null)

/**
 * قائمة منسدلة (Dropdown) بتصميم دولار:
 * رأس ثابت يُفتح عند الضغط + قائمة عائمة بحركة وتحقّق على العنصر المختار.
 */
@Composable
fun DollarDropdown(
    label: String,
    selected: String,
    options: List<DropdownOption>,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.value == selected }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "chevron")

    Box(modifier) {
        Pressable(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            pressedScale = 0.97f,
            onClick = { expanded = true }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Pal.Surface)
                    .border(
                        BorderStroke(1.dp, if (expanded) Pal.Gold else Pal.Stroke),
                        MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(label, color = if (expanded) Pal.Gold else Pal.Muted, fontSize = 11.sp)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, null, tint = Pal.Gold, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        current?.label ?: "الكل",
                        color = Pal.Text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        null,
                        tint = if (expanded) Pal.Gold else Pal.Muted,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotation)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Pal.SurfaceAlt)
                .heightIn(max = 320.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                option.label,
                                color = if (option.value == selected) Pal.Gold else Pal.Text,
                                fontWeight = if (option.value == selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            option.hint?.let { Text(it, color = Pal.Muted, fontSize = 11.sp) }
                        }
                    },
                    trailingIcon = {
                        if (option.value == selected) {
                            Icon(Icons.Default.Check, null, tint = Pal.Gold, modifier = Modifier.size(18.dp))
                        }
                    },
                    onClick = {
                        onSelect(option.value)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* ============================================================================
 * الحقول والرقائق
 * ========================================================================== */

@Composable
fun DollarField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    numeric: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    helper: String? = null
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            leadingIcon = leadingIcon?.let {
                { Icon(it, null, tint = Pal.Gold, modifier = Modifier.size(18.dp)) }
            },
            trailingIcon = trailingIcon?.let { icon ->
                {
                    Icon(
                        icon,
                        null,
                        tint = Pal.Gold,
                        modifier = Modifier
                            .size(19.dp)
                            .clickable(enabled = onTrailingClick != null) { onTrailingClick?.invoke() }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text
            ),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Pal.Gold,
                unfocusedBorderColor = Pal.Stroke,
                focusedLabelColor = Pal.Gold,
                unfocusedLabelColor = Pal.Muted,
                focusedContainerColor = Pal.Surface,
                unfocusedContainerColor = Pal.Surface,
                focusedTextColor = Pal.Text,
                unfocusedTextColor = Pal.Text,
                cursorColor = Pal.Gold
            )
        )
        helper?.let {
            Text(it, color = Pal.Muted, fontSize = 11.sp, modifier = Modifier.padding(start = 6.dp, top = 4.dp))
        }
    }
}

/** رقاقة اختيار تفاعلية. */
@Composable
fun DollarChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Pressable(
        modifier = modifier,
        shape = CircleShape,
        pressedScale = 0.92f,
        onClick = onClick
    ) {
        Row(
            Modifier
                .background(if (selected) Pal.Gold.copy(alpha = 0.16f) else Pal.SurfaceAlt)
                .border(
                    BorderStroke(1.dp, if (selected) Pal.Gold else Pal.Stroke),
                    CircleShape
                )
                .padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, null, tint = if (selected) Pal.Gold else Pal.Muted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                color = if (selected) Pal.Gold else Pal.Text,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/** عدّاد كمية. */
@Composable
fun QuantityStepper(
    quantity: Int,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = Int.MAX_VALUE,
    onChange: (Int) -> Unit
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(Pal.SurfaceAlt)
            .border(BorderStroke(1.dp, Pal.Stroke), CircleShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(Icons.Default.Remove, enabled = quantity > min) { onChange((quantity - 1).coerceAtLeast(min)) }
        Text(
            quantity.toString(),
            modifier = Modifier.width(42.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black,
            color = Pal.Text,
            fontSize = 15.sp
        )
        StepperButton(Icons.Default.Add, enabled = quantity < max) { onChange((quantity + 1).coerceAtMost(max)) }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Pressable(
        shape = CircleShape,
        enabled = enabled,
        pressedScale = 0.88f,
        onClick = onClick
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(if (enabled) Pal.Gold.copy(alpha = 0.15f) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (enabled) Pal.Gold else Pal.Stroke, modifier = Modifier.size(17.dp))
        }
    }
}

/* ============================================================================
 * عناصر عرض مساعدة
 * ========================================================================== */

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Pal.goldBrush)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Black, color = Pal.Text)
            subtitle?.let { Text(it, color = Pal.Muted, fontSize = 12.sp) }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = Pal.Gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.Gold
) {
    GoldCard(modifier = modifier) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tone.color().copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = tone.color(), modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text(label, color = Pal.Muted, fontSize = 12.sp, maxLines = 2, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = Pal.Text, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
fun RatingStars(rating: Double, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { index ->
            Icon(
                if (index < rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                null,
                tint = Pal.Gold,
                modifier = Modifier.size(size)
            )
        }
        Spacer(Modifier.width(5.dp))
        Text(
            String.format("%.1f", rating),
            color = Pal.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** صورة منتج مع هيكل تحميل متدرّج. */
@Composable
fun ProductThumb(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium
) {
    Box(
        modifier
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(Pal.SurfaceHigh, Pal.SurfaceAlt, Pal.SurfaceHigh))
            )
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                Icons.Default.Storefront,
                null,
                tint = Pal.Gold.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center).size(34.dp)
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(Pal.goldFade),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Pal.Gold, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = Pal.Text, fontSize = 18.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            color = Pal.Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            GoldButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** شريط تحميل ذهبي رفيع يظهر أعلى الشاشة عند العمل. */
@Composable
fun LoadingBar(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(120)), exit = fadeOut(tween(200)), modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Pal.goldBrush)
        )
    }
}

/** شريط تقدم متحرّك داخل شريط أفقي (مؤشر تمرير ذهبي). */
@Composable
fun ShimmerStrip(modifier: Modifier = Modifier, height: Dp = 96.dp) {
    var shift by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            shift += 0.02f
            if (shift > 1.6f) shift = -0.6f
        }
    }
    Box(
        modifier
            .height(height)
            .clip(MaterialTheme.shapes.medium)
            .background(
                Brush.linearGradient(
                    colors = listOf(Pal.SurfaceAlt, Pal.SurfaceHigh, Pal.SurfaceAlt),
                    start = androidx.compose.ui.geometry.Offset(shift * 300f, 0f),
                    end = androidx.compose.ui.geometry.Offset(shift * 300f + 300f, 300f)
                )
            )
    )
}

/** زر أيقونة دائرية صغير شائع الاستخدام. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Pal.Gold,
    background: Color = Pal.Gold.copy(alpha = 0.12f),
    size: Dp = 38.dp,
    onClick: (() -> Unit)? = null
) {
    val content: @Composable () -> Unit = {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(background)
                .border(BorderStroke(1.dp, tint.copy(alpha = 0.28f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(size / 2.2f))
        }
    }
    if (onClick == null) Box(modifier) { content() }
    else Pressable(modifier = modifier, shape = CircleShape, onClick = onClick) { content() }
}

/** سطر عنوان/قيمة مع إمكانية إبراز القيمة. */
@Composable
fun KeyValueRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Pal.Text,
    bold: Boolean = false
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(key, color = Pal.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = valueColor,
            fontSize = if (bold) 16.sp else 13.sp,
            fontWeight = if (bold) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** فاصل ذهبي رفيع. */
@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Pal.Stroke, Color.Transparent)))
    )
}

/** طبقة تغطّي الشاشة كاملة (تفاصيل منتج، محادثات، مركز بائع) مع ظهور ناعم. */
@Composable
fun DollarOverlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.97f, animationSpec = tween(200)),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.97f, animationSpec = tween(160))
    ) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}

/** بطاقة ترحيب متدرّجة تُستخدم في الرئيسية والحساب. */
@Composable
fun GradientHero(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Pal.heroBrush)
            .border(BorderStroke(1.dp, Pal.Gold.copy(alpha = 0.25f)), MaterialTheme.shapes.extraLarge)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Pal.goldFade)
        )
        content()
    }
}