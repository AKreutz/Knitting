package com.akreutz.knitting.ui.projects

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An icon split along its diagonal like the app icon: the upper right in the primary color,
 * the lower left in the secondary color.
 */
@Composable
fun DuotoneIcon(imageVector: ImageVector, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = Color.Black,
        modifier = modifier
            .size(iconSize)
            // Drawn offscreen so the colors below only land on the icon's own pixels.
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.linearGradient(
                        0f to primary,
                        0.5f to primary,
                        0.5f to secondary,
                        1f to secondary,
                        start = Offset(size.width, 0f),
                        end = Offset(0f, size.height),
                    ),
                    blendMode = BlendMode.SrcAtop,
                )
            },
    )
}
