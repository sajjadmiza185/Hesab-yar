package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Barbarg3DStyle

data class ThemeOptionInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val style: Barbarg3DStyle,
    val previewPrimary: Color,
    val previewAccent: Color
)

@Composable
fun ThemePreviewCard(
    option: ThemeOptionInfo,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = option.style
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 14.dp else 5.dp,
                shape = cardShape,
                clip = false,
                ambientColor = style.cardAmbientShadow,
                spotColor = style.cardAmbientShadow
            )
            .border(
                width = if (isSelected) 2.5.dp else 1.25.dp,
                brush = if (isSelected) style.cardBorder else Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.5f),
                        option.previewAccent.copy(alpha = 0.35f)
                    )
                ),
                shape = cardShape
            )
            .clip(cardShape)
            .clickable(onClick = onSelect)
            .background(style.backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title, Description, and Active Selection Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .shadow(4.dp, CircleShape)
                            .background(style.buttonBackground, CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    )
                    Column {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (style.isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                        )
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (style.isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }
                }

                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = option.previewPrimary,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "فعال",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Real 3D Preview Surface Simulation (demonstrating actual Card, Button, and Input appearance)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = style.cardElevation,
                        shape = RoundedCornerShape(14.dp),
                        clip = false,
                        ambientColor = style.cardAmbientShadow
                    )
                    .border(1.25.dp, style.cardBorder, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(style.cardBackground)
                    .drawWithContent {
                        drawContent()
                        val shine = style.cardSpecularShine
                        if (shine != null) {
                            val h = minOf(size.height, style.cardSpecularHeight.toPx())
                            drawRect(
                                brush = shine,
                                topLeft = Offset.Zero,
                                size = Size(size.width, h)
                            )
                        }
                    }
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Mini Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "مانده حساب مشتری (نمونه)",
                                fontSize = 11.sp,
                                color = if (style.isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                            Text(
                                text = "۱,۲۵۰,۰۰۰ تومان",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (style.isDark) Color(0xFF60A5FA) else Color(0xFF0369A1)
                            )
                        }

                        // Simulated 3D Button
                        Box(
                            modifier = Modifier
                                .shadow(style.buttonElevation, RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .background(style.buttonBackground)
                            .drawWithContent {
                                drawContent()
                                val shine = style.buttonSpecularShine
                                if (shine != null) {
                                    drawRect(
                                        brush = shine,
                                        topLeft = Offset.Zero,
                                        size = Size(size.width, 10.dp.toPx())
                                    )
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ثبت پرداخت",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Mini Input/Search field simulator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (style.isDark) Color(0xFF0F172A).copy(alpha = 0.70f)
                                else Color.White.copy(alpha = 0.85f)
                            )
                            .border(
                                1.dp,
                                if (style.isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "جستجوی مشتری یا تراکنش…",
                            fontSize = 10.sp,
                            color = if (style.isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
