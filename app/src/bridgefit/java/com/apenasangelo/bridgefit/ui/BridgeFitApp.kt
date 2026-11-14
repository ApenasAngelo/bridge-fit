package com.apenasangelo.bridgefit.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apenasangelo.bridgefit.ui.devices.DevicesScreen
import com.apenasangelo.bridgefit.ui.metric.MetricDetailScreen
import com.apenasangelo.bridgefit.ui.settings.SettingsScreen
import com.apenasangelo.bridgefit.ui.summary.SummaryScreen
import com.apenasangelo.bridgefit.ui.theme.BridgeFitTheme
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.R

private enum class Tab(val label: Int, val icon: ImageVector) {
    SUMMARY(R.string.bf_tab_summary, Icons.Rounded.DonutLarge),
    DEVICES(R.string.bf_tab_devices, Icons.Rounded.Watch),
    SETTINGS(R.string.bf_tab_settings, Icons.Rounded.Settings),
}

@Composable
fun BridgeFitApp(vm: FitViewModel = viewModel()) {
    BridgeFitTheme {
        val nav = rememberNavController()
        NavHost(
            nav,
            startDestination = "tabs",
            modifier = Modifier.fillMaxSize().background(fit.background),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) },
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
    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val tabBarHeight = 56.dp
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    val padding = PaddingValues(
        top = statusBars.calculateTopPadding(),
        bottom = navBars.calculateBottomPadding() + tabBarHeight,
    )
    Box(Modifier.fillMaxSize().background(fit.background)) {
        when (Tab.entries[selected]) {
            Tab.SUMMARY -> SummaryScreen(vm, padding, onOpenMetric)
            Tab.DEVICES -> DevicesScreen(vm, padding)
            Tab.SETTINGS -> SettingsScreen(padding)
        }
        // Status bar scrim, so scrolled content does not run under the clock.
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .height(statusBars.calculateTopPadding())
                .background(fit.background.copy(alpha = 0.94f)),
        )
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(fit.tabBar)) {
            HorizontalDivider(thickness = 0.5.dp, color = fit.separator)
            Row(
                Modifier.fillMaxWidth().height(tabBarHeight).windowInsetsPadding(WindowInsets(0)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tab.entries.forEachIndexed { index, tab ->
                    val active = index == selected
                    val color = if (active) FitColors.accent else fit.textSecondary
                    Column(
                        Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) {
                            if (selected == index && tab == Tab.SUMMARY) vm.selectDay(java.time.LocalDate.now())
                            selected = index
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(tab.icon, null, tint = color, modifier = Modifier.size(26.dp))
                        Text(stringResource(tab.label), style = FitType.caption, color = color)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(bottom = navBars.calculateBottomPadding()))
        }
    }
}
