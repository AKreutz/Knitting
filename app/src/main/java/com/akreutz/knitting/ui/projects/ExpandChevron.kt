package com.akreutz.knitting.ui.projects

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import com.akreutz.knitting.R

/** The chevron of an expandable row; it turns upside down while [expanded]. */
@Composable
fun ExpandChevron(expanded: Boolean, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Icon(
        imageVector = Icons.Filled.ExpandMore,
        contentDescription = stringResource(if (expanded) R.string.collapse_step else R.string.expand_step),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.rotate(rotation),
    )
}
