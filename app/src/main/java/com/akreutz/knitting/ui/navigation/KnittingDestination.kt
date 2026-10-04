package com.akreutz.knitting.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.ui.graphics.vector.ImageVector
import com.akreutz.knitting.R

enum class KnittingDestination(
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Projects(R.string.nav_projects, Icons.Filled.Checkroom),
    InProgress(R.string.nav_in_progress, Icons.Filled.FormatListNumbered),
}
