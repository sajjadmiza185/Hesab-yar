package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button as MaterialButton
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.FilledTonalButton as MaterialFilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Barbarg3DStyle
import com.example.ui.theme.LocalBarbarg3DStyle

/**
 * Applies realistic multi-layered 3D surface treatment based on the active Barbarg3DStyle:
 * - Natural soft ambient drop shadow
 * - Gradient specular highlight on upper edge
 * - Directional material bevel border
 */
fun Modifier.barberg3DCard(
    shape: Shape,
    style: Barbarg3DStyle,
    customBorder: BorderStroke? = null,
    outerShadow: Dp? = null
): Modifier {
    val shadowElevation = outerShadow ?: style.cardElevation
    val shineBrush = style.cardSpecularShine
    val shineH = style.cardSpecularHeight

    return this
        .then(
            if (shadowElevation > 0.dp) {
                Modifier.shadow(
                    elevation = shadowElevation,
                    shape = shape,
                    clip = false,
                    ambientColor = style.cardAmbientShadow,
                    spotColor = style.cardAmbientShadow
                )
            } else Modifier
        )
        .then(
            if (customBorder != null) {
                Modifier.border(customBorder, shape)
            } else {
                Modifier.border(BorderStroke(style.cardBorderWidth, style.cardBorder), shape)
            }
        )
        .clip(shape)
        .background(style.cardBackground, shape)
        .drawWithContent {
            drawContent()
            if (shineBrush != null && shineH > 0.dp) {
                val h = minOf(size.height, shineH.toPx())
                if (h > 0f) {
                    drawRect(
                        brush = shineBrush,
                        topLeft = Offset.Zero,
                        size = Size(size.width, h)
                    )
                }
            }
        }
}

/**
 * Standard Barberg 3D Card for grouping content.
 */
@Composable
fun BarbergCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val style = LocalBarbarg3DStyle.current

    MaterialCard(
        modifier = modifier.barberg3DCard(
            shape = shape,
            style = style,
            customBorder = border
        ),
        shape = shape,
        // When using 3D treatment, the background gradient is painted by the modifier
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null
    ) {
        Column {
            content()
        }
    }
}

/**
 * Clickable Barberg 3D Card with interactive ripple and elevation feedback.
 */
@Composable
fun BarbergCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val style = LocalBarbarg3DStyle.current

    MaterialCard(
        onClick = onClick,
        modifier = modifier.barberg3DCard(
            shape = shape,
            style = style,
            customBorder = border
        ),
        enabled = enabled,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = null,
        interactionSource = interactionSource
    ) {
        Column {
            content()
        }
    }
}

/**
 * 3D Button with specular sheen, bevel edge, and natural elevation.
 */
@Composable
fun BarbergButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val style = LocalBarbarg3DStyle.current
    val resolvedBorder = border ?: style.buttonBorder?.let { BorderStroke(1.dp, it) }

    MaterialButton(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = if (enabled) style.buttonElevation else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = style.cardAmbientShadow,
                spotColor = style.cardAmbientShadow
            )
            .then(if (resolvedBorder != null && enabled) Modifier.border(resolvedBorder, shape) else Modifier)
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier.background(style.buttonBackground, shape).drawWithContent {
                        drawContent()
                        val shine = style.buttonSpecularShine
                        if (shine != null) {
                            val h = minOf(size.height, style.buttonSpecularHeight.toPx())
                            if (h > 0f) {
                                drawRect(
                                    brush = shine,
                                    topLeft = Offset.Zero,
                                    size = Size(size.width, h)
                                )
                            }
                        }
                    }
                } else Modifier
            ),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            contentColor = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        border = null,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun BarbergOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val style = LocalBarbarg3DStyle.current
    val resolvedBorder = border ?: BorderStroke(1.25.dp, style.cardBorder)

    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = if (enabled) 2.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = style.cardAmbientShadow
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    )
                ),
                shape = shape
            ),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = null,
        border = resolvedBorder,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun BarbergFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    val style = LocalBarbarg3DStyle.current
    val resolvedBorder = border ?: BorderStroke(1.dp, style.cardBorder)

    MaterialFilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = if (enabled) 3.dp else 0.dp,
                shape = shape,
                clip = false,
                ambientColor = style.cardAmbientShadow
            )
            .then(Modifier.border(resolvedBorder, shape))
            .clip(shape),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = null,
        border = null,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}
