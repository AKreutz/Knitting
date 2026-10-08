package com.akreutz.knitting.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.akreutz.knitting.ui.navigation.KnittingDestination
import com.akreutz.knitting.ui.projects.InProgressProjectsScreen
import com.akreutz.knitting.ui.projects.ProjectsScreen
import com.akreutz.knitting.ui.projects.ProjectsViewModel
import com.akreutz.knitting.ui.theme.KnittingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnittingApp() {
    var current by rememberSaveable { mutableStateOf(KnittingDestination.Projects) }

    // Only on a fresh launch: open on In Progress when a project is being knitted.
    // The flag survives rotation and process death so later tab picks are never overridden.
    var startTabChosen by rememberSaveable { mutableStateOf(false) }
    val projectsViewModel: ProjectsViewModel = viewModel()
    LaunchedEffect(Unit) {
        if (startTabChosen) return@LaunchedEffect
        if (projectsViewModel.hasProjectInProgress()) current = KnittingDestination.InProgress
        startTabChosen = true
    }
    if (!startTabChosen) {
        // Show nothing but the background until the start tab is known, so there is no flash of the wrong tab.
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    // The top bar stays pinned in the darker oatmeal surface while content scrolls under it.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(current.label), style = MaterialTheme.typography.headlineSmall) },
                actions = { LanguageButton() },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
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
            transitionSpec = {
                // Slide in the direction of the tab that was picked, like moving along the navigation bar.
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(300)) { direction * it / 6 } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(tween(300)) { -direction * it / 6 } + fadeOut(tween(150)))
            },
            label = "destination",
        ) { destination ->
            when (destination) {
                KnittingDestination.Projects -> ProjectsScreen(
                    modifier = Modifier.padding(innerPadding),
                    onProjectStarted = { current = KnittingDestination.InProgress },
                )
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
