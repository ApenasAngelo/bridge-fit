package com.apenasangelo.bridgefit.ui.prefs

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.apenasangelo.bridgefit.ui.components.GroupCaption
import com.apenasangelo.bridgefit.ui.components.GroupedList
import com.apenasangelo.bridgefit.ui.components.ListRow
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A native (Compose) row bound to an upstream SharedPreferences key.
 *
 * Values are stored exactly as the upstream androidx.preference widgets store them (EditTextPreference and
 * ListPreference as strings, SwitchPreference as booleans, XDatePreference as "yyyy-MM-dd"), so upstream code reads
 * them unchanged. [onChanged] runs the side effects the upstream fragment would run (send to device, broadcasts).
 */
sealed interface Pref {
    val key: String
    val title: Int
    val icon: ImageVector?
    val iconColor: Color

    data class Number(
        override val key: String,
        @StringRes override val title: Int,
        override val icon: ImageVector?,
        override val iconColor: Color,
        val default: Int,
        val unit: String? = null,
        val maxLength: Int = 5,
    ) : Pref

    data class Text(
        override val key: String,
        @StringRes override val title: Int,
        override val icon: ImageVector?,
        override val iconColor: Color,
        val default: String = "",
        val maxLength: Int = 64,
    ) : Pref

    data class Choice(
        override val key: String,
        @StringRes override val title: Int,
        override val icon: ImageVector?,
        override val iconColor: Color,
        val entries: Int,
        val values: Int,
        val default: String,
    ) : Pref

    data class Date(
        override val key: String,
        @StringRes override val title: Int,
        override val icon: ImageVector?,
        override val iconColor: Color,
        val default: String,
    ) : Pref

    data class Toggle(
        override val key: String,
        @StringRes override val title: Int,
        override val icon: ImageVector?,
        override val iconColor: Color,
        val default: Boolean,
        @StringRes val summary: Int? = null,
    ) : Pref

    data class Link(
        override val key: String,
        @StringRes override val title: Int,
        override val icon: ImageVector?,
        override val iconColor: Color,
        @StringRes val summary: Int? = null,
        val onClick: () -> Unit,
    ) : Pref
}

data class PrefSection(@StringRes val title: Int?, val prefs: List<Pref>)

private val prefs: SharedPreferences get() = GBApplication.getPrefs().preferences

/**
 * A counter that changes whenever any preference changes, from anywhere (including upstream screens),
 * so reading it makes a composable recompose with fresh values.
 */
@Composable
fun rememberPrefVersion(): Int {
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return version
}

private fun readString(key: String, default: String): String =
    try {
        prefs.getString(key, default) ?: default
    } catch (_: ClassCastException) {
        prefs.all[key]?.toString() ?: default
    }

/**
 * Adds [sections] as grouped lists to a LazyColumn.
 */
fun LazyListScope.prefSections(sections: List<PrefSection>, onChanged: (String) -> Unit) {
    sections.forEach { section ->
        item(key = "caption-${section.title}-${section.prefs.firstOrNull()?.key}") {
            section.title?.let { GroupCaption(stringResource(it)) }
        }
        item(key = "section-${section.prefs.firstOrNull()?.key}") {
            GroupedList(rows = section.prefs.map { pref -> { PrefRow(pref, onChanged) } })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrefRow(pref: Pref, onChanged: (String) -> Unit) {
    rememberPrefVersion()
    var editing by remember { mutableStateOf(false) }
    val title = stringResource(pref.title)
    val valueText: String? = when (pref) {
        is Pref.Number -> readString(pref.key, pref.default.toString()).let { v -> pref.unit?.let { "$v $it" } ?: v }
        is Pref.Text -> readString(pref.key, pref.default).ifBlank { null }
        is Pref.Choice -> {
            val entries = stringArrayResource(pref.entries)
            val values = stringArrayResource(pref.values)
            val current = readString(pref.key, pref.default)
            entries.getOrNull(values.indexOf(current))
        }
        is Pref.Date -> readString(pref.key, pref.default)
        is Pref.Toggle, is Pref.Link -> null
    }
    val summary = when (pref) {
        is Pref.Toggle -> pref.summary?.let { stringResource(it) }
        is Pref.Link -> pref.summary?.let { stringResource(it) }
        else -> null
    }
    ListRow(
        title = title,
        subtitle = summary,
        icon = pref.icon,
        iconBackground = pref.iconColor,
        showChevron = pref is Pref.Link || pref is Pref.Choice,
        trailing = {
            when (pref) {
                is Pref.Toggle -> {
                    val checked = prefs.getBoolean(pref.key, pref.default)
                    Switch(
                        checked = checked,
                        onCheckedChange = {
                            prefs.edit().putBoolean(pref.key, it).apply()
                            onChanged(pref.key)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = FitColors.battery, checkedThumbColor = Color.White),
                    )
                }
                else -> valueText?.let { Text(it, style = FitType.body, color = fit.textSecondary, maxLines = 1) }
            }
        },
        onClick = when (pref) {
            is Pref.Link -> pref.onClick
            is Pref.Toggle -> ({
                prefs.edit().putBoolean(pref.key, !prefs.getBoolean(pref.key, pref.default)).apply()
                onChanged(pref.key)
            })
            else -> ({ editing = true })
        },
    )
    if (!editing) return
    val close = { editing = false }
    val save = { value: String ->
        prefs.edit().putString(pref.key, value).apply()
        onChanged(pref.key)
        editing = false
    }
    when (pref) {
        is Pref.Number, is Pref.Text -> {
            val isNumber = pref is Pref.Number
            val initial = if (pref is Pref.Number) readString(pref.key, pref.default.toString()) else readString(pref.key, (pref as Pref.Text).default)
            val maxLength = if (pref is Pref.Number) pref.maxLength else (pref as Pref.Text).maxLength
            var text by remember { mutableStateOf(initial) }
            AlertDialog(
                onDismissRequest = close,
                title = { Text(title) },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v ->
                            val clean = if (isNumber) v.filter { it.isDigit() } else v
                            text = clean.take(maxLength)
                        },
                        singleLine = true,
                        suffix = (pref as? Pref.Number)?.unit?.let { u -> { Text(u) } },
                        keyboardOptions = KeyboardOptions(keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(onClick = { save(text) }, enabled = !isNumber || text.isNotEmpty()) {
                        Text(stringResource(android.R.string.ok), color = FitColors.accent)
                    }
                },
                dismissButton = { TextButton(onClick = close) { Text(stringResource(android.R.string.cancel), color = FitColors.accent) } },
                containerColor = fit.card,
            )
        }

        is Pref.Choice -> {
            val entries = stringArrayResource(pref.entries)
            val values = stringArrayResource(pref.values)
            val current = readString(pref.key, pref.default)
            AlertDialog(
                onDismissRequest = close,
                title = { Text(title) },
                text = {
                    Column {
                        entries.forEachIndexed { i, entry ->
                            ListRow(
                                entry, showChevron = false,
                                trailing = { if (values[i] == current) Icon(Icons.Rounded.Check, null, tint = FitColors.accent) },
                                onClick = { save(values[i]) },
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = close) { Text(stringResource(android.R.string.cancel), color = FitColors.accent) } },
                containerColor = fit.card,
            )
        }

        is Pref.Date -> {
            val initial = runCatching { LocalDate.parse(readString(pref.key, pref.default)) }.getOrNull() ?: LocalDate.of(2000, 1, 1)
            val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            DatePickerDialog(
                onDismissRequest = close,
                confirmButton = {
                    TextButton(onClick = {
                        state.selectedDateMillis?.let { millis ->
                            save(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                        } ?: close()
                    }) { Text(stringResource(android.R.string.ok), color = FitColors.accent) }
                },
                dismissButton = { TextButton(onClick = close) { Text(stringResource(android.R.string.cancel), color = FitColors.accent) } },
            ) { DatePicker(state) }
        }

        else -> close()
    }
}
