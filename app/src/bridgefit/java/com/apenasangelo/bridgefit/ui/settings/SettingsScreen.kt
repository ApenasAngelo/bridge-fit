package com.apenasangelo.bridgefit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.Upstream
import com.apenasangelo.bridgefit.ui.components.GroupCaption
import com.apenasangelo.bridgefit.ui.components.GroupedList
import com.apenasangelo.bridgefit.ui.components.ListRow
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R

private val Gray = Color(0xFF8E8E93)

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize().background(fit.background),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item {
            Text(
                stringResource(R.string.bf_settings_title), style = FitType.largeTitle, color = fit.textPrimary,
                modifier = Modifier.padding(start = 4.dp, top = 28.dp, bottom = 4.dp),
            )
        }
        item { GroupCaption(stringResource(R.string.bf_section_general)) }
        item {
            GroupedList(
                rows = listOf(
                    {
                        ListRow(
                            stringResource(R.string.bf_settings_goals), subtitle = stringResource(R.string.bf_settings_goals_subtitle),
                            icon = Icons.Rounded.Person, iconBackground = FitColors.move,
                            onClick = { Upstream.userPreferences(context) },
                        )
                    },
                    {
                        ListRow(
                            stringResource(R.string.bf_settings_app), subtitle = stringResource(R.string.bf_settings_app_subtitle),
                            icon = Icons.Rounded.Settings, iconBackground = Gray,
                            onClick = { Upstream.settings(context) },
                        )
                    },
                    {
                        ListRow(
                            stringResource(R.string.bf_settings_permissions), icon = Icons.Rounded.Lock,
                            iconBackground = FitColors.accent, onClick = { Upstream.permissions(context) },
                        )
                    },
                ),
            )
        }
        item { GroupCaption(stringResource(R.string.bf_section_data)) }
        item {
            GroupedList(
                rows = listOf {
                    ListRow(
                        stringResource(R.string.bf_settings_data), subtitle = stringResource(R.string.bf_settings_data_subtitle),
                        icon = Icons.Rounded.Storage, iconBackground = FitColors.battery,
                        onClick = { Upstream.dataManagement(context) },
                    )
                },
            )
        }
        item { GroupCaption(stringResource(R.string.bf_section_advanced)) }
        item {
            GroupedList(
                rows = listOf(
                    {
                        ListRow(
                            stringResource(R.string.bf_settings_classic), subtitle = stringResource(R.string.bf_settings_classic_subtitle),
                            icon = Icons.Rounded.Dashboard, iconBackground = Gray,
                            onClick = { Upstream.classicInterface(context) },
                        )
                    },
                    {
                        ListRow(
                            stringResource(R.string.bf_settings_debug), icon = Icons.Rounded.BugReport, iconBackground = Gray,
                            onClick = { Upstream.debug(context) },
                        )
                    },
                    {
                        ListRow(
                            stringResource(R.string.bf_settings_about), subtitle = stringResource(R.string.bf_settings_about_subtitle),
                            icon = Icons.Rounded.Info, iconBackground = FitColors.accent,
                            onClick = { Upstream.about(context) },
                        )
                    },
                    {
                        ListRow(
                            stringResource(R.string.bf_settings_quit), icon = Icons.Rounded.PowerSettingsNew,
                            iconBackground = FitColors.batteryLow, titleColor = FitColors.batteryLow, showChevron = false,
                            onClick = { GBApplication.quit() },
                        )
                    },
                ),
            )
        }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Text(
                    stringResource(R.string.bf_version, BuildConfig.VERSION_NAME), style = FitType.footnote,
                    color = fit.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
