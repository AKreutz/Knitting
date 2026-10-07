package com.akreutz.knitting.ui.projects

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.akreutz.knitting.R
import java.util.Locale

/**
 * Common yarn color names, in English and German, by lower-case name. The shades are muted and
 * warm to sit with the app's wool palette (terracotta, sage, cream); the red, yellow and green
 * are the ones of the pattern grid's colors.
 */
private val YarnColors: Map<String, Color> = buildMap {
    fun add(argb: Long, vararg names: String) = names.forEach { put(it, Color(argb)) }
    add(0xFFFAF6EE, "white", "weiß", "weiss")
    add(0xFFEFE3C8, "cream", "creme", "ecru")
    add(0xFFD8C6A5, "beige", "natural", "natur")
    add(0xFFE0A526, "yellow", "gelb")
    add(0xFFC9A24A, "mustard", "senf", "gold")
    add(0xFFD2783C, "orange")
    add(0xFFDDA0A0, "pink", "rosa")
    add(0xFFB5493B, "red", "rot")
    add(0xFFA8482B, "terracotta", "rost", "rust")
    add(0xFF7A2E34, "burgundy", "bordeaux", "maroon", "weinrot")
    add(0xFF7D5A82, "purple", "violet", "lila", "violett")
    add(0xFFB5A6C9, "lavender", "lavendel", "flieder")
    add(0xFF466A8F, "blue", "blau")
    add(0xFF2C3E57, "navy", "dark blue", "marine", "dunkelblau")
    add(0xFF94B3CC, "light blue", "sky blue", "hellblau")
    add(0xFF4FA39B, "turquoise", "türkis")
    add(0xFF3F7D7A, "teal", "petrol")
    add(0xFF6E8B5B, "green", "grün")
    add(0xFF3F5A3A, "dark green", "forest green", "dunkelgrün", "tannengrün")
    add(0xFF7F8148, "olive", "oliv")
    add(0xFFA9C7A8, "mint", "light green", "hellgrün")
    add(0xFF55664A, "sage", "salbei")
    add(0xFF7A5338, "brown", "braun")
    add(0xFFC4A27A, "tan", "camel", "kamel", "sand")
    add(0xFF8D857B, "grey", "gray", "grau")
    add(0xFFC9C2B8, "light grey", "light gray", "silver", "hellgrau", "silber")
    add(0xFF5C554E, "dark grey", "dark gray", "dunkelgrau")
    add(0xFF3A3631, "charcoal", "anthracite", "anthrazit")
    add(0xFF2B2018, "black", "schwarz")
}

/** The shade for a yarn color the user typed, or null when it is not a name this app knows. */
internal fun yarnColorOrNull(name: String): Color? =
    YarnColors[name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")]

/** Colors darker than this get light strands, since darker strands would hardly show on them. */
private const val DARK_YARN_LUMINANCE = 0.2f

/**
 * The launcher icon's yarn ball in [color], with its knitting needles: a disc with the ball's
 * outline in a darker shade and its strands in the same shade, or in a light one when the yarn
 * itself is dark. The needles keep the launcher icon's own colors.
 */
@Composable
internal fun YarnBall(color: Color, description: String, modifier: Modifier = Modifier) {
    val outline = lerp(color, Color.Black, 0.4f)
    // On a dark yarn the strands take the darker cream of the cards.
    val strands = if (color.luminance() < DARK_YARN_LUMINANCE) MaterialTheme.colorScheme.surfaceContainerHighest else outline
    Box(modifier = modifier.size(30.dp).semantics { contentDescription = description }) {
        Image(
            painter = painterResource(R.drawable.ic_yarn_needles_back),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            painter = painterResource(R.drawable.ic_yarn_ball_fill),
            contentDescription = null,
            tint = color,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            painter = painterResource(R.drawable.ic_yarn_ball),
            contentDescription = null,
            tint = strands,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            painter = painterResource(R.drawable.ic_yarn_ball_ring),
            contentDescription = null,
            tint = outline,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.ic_yarn_needle_front),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
