package com.apenasangelo.bridgefit.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apenasangelo.bridgefit.ui.devices.DevicesScreen
import com.apenasangelo.bridgefit.ui.glass.GlassHost
import com.apenasangelo.bridgefit.ui.glass.GlassSettings
import com.apenasangelo.bridgefit.ui.glass.GlassTabBar
import com.apenasangelo.bridgefit.ui.glass.ScrollEdge
import com.apenasangelo.bridgefit.ui.glass.TabItem
import com.apenasangelo.bridgefit.ui.glass.tabBarClearance
import com.apenasangelo.bridgefit.ui.metric.MetricDetailScreen
import com.apenasangelo.bridgefit.ui.settings.SettingsScreen
import com.apenasangelo.bridgefit.ui.summary.SummaryScreen
import com.apenasangelo.bridgefit.ui.theme.BridgeFitTheme
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.R
import java.time.LocalDate

private enum class Tab(val label: Int, val icon: ImageVector) {
    SUMMARY(R.string.bf_tab_summary, Icons.Rounded.DonutLarge),
    DEVICES(R.string.bf_tab_devices, Icons.Rounded.Watch),
    SETTINGS(R.string.bf_tab_settings, Icons.Rounded.Settings),
}

@Composable
fun BridgeFitApp(vm: FitViewModel = viewModel()) {
    LaunchedEffect(Unit) { GlassSettings.load() }
    BridgeFitTheme {
        val nav = rememberNavController()
        NavHost(
            nav,
            startDestination = "tabs",
            modifier = Modifier.fillMaxSize().background(fit.background),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) },
            exitTransition = { fadeOut(tween(320)) },
            popEnterTransition = { fadeIn(tween(320)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(320)) },
        ) {
            composable("tabs") {
                Tabs(vm) { metric -> nav.navigate("metric/${metric.route}") }
            }
            composable("metric/{metric}") { entry ->
                MetricDetailScreen(Metric.fromRoute(entry.arguments?.getString("metric")), vm) { nav.popBackStack() }
            }
        }
    }
}

@Composable
private fun Tabs(vm: FitViewModel, onOpenMetric: (Metric) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val padding = PaddingValues(top = top, bottom = tabBarClearance())
    val items = Tab.entries.map { TabItem(stringResource(it.label), it.icon) }
    GlassHost(
        Modifier.fillMaxSize(),
        content = {
            when (Tab.entries[selected]) {
                Tab.SUMMARY -> SummaryScreen(vm, padding, onOpenMetric)
                Tab.DEVICES -> DevicesScreen(vm, padding)
                Tab.SETTINGS -> SettingsScreen(padding)
            }
        },
        overlay = {
            ScrollEdge(top + 12.dp)
            GlassTabBar(items, selected) { index ->
                if (selected == index && Tab.entries[index] == Tab.SUMMARY) vm.selectDay(LocalDate.now())
                selected = index
            }
        },
    )
}
