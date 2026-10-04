package com.akreutz.knitting.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing scale shared by every screen, so paddings and gaps stay consistent. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp

    /** Bottom padding that keeps the last list item clear of the floating action button. */
    val fabClearance = 88.dp

    /** Width of the status-colored strip on the left edge of a project card. */
    val accentStrip = 6.dp
}
