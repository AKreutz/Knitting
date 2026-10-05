package com.akreutz.knitting.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.akreutz.knitting.ui.navigation.KnittingDestination
import com.akreutz.knitting.ui.projects.InProgressProjectsScreen
import com.akreutz.knitting.ui.projects.ProjectsScreen
import com.akreutz.knitting.ui.theme.KnittingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnittingApp() {
    var current by rememberSaveable { mutableStateOf(KnittingDestination.Projects) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(current.label), style = MaterialTheme.typography.headlineSmall) },
                actions = { LanguageButton() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        bottomBar = {
            NavigationBar {
                KnittingDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == current,
                        onClick = { current = destination },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        AnimatedContent(
            targetState = current,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "destination",
        ) { destination ->
            when (destination) {
                KnittingDestination.Projects -> ProjectsScreen(Modifier.padding(innerPadding))
                KnittingDestination.InProgress -> InProgressProjectsScreen(Modifier.padding(innerPadding))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KnittingAppPreview() {
    KnittingTheme {
        KnittingApp()
    }
}
