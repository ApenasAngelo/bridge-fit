package com.apenasangelo.bridgefit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit

val CardShape = RoundedCornerShape(14.dp)

/**
 * The rounded card of grouped iOS lists. Dims slightly while pressed, like a list cell.
 */
@Composable
fun FitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val base = Modifier
        .fillMaxWidth()
        .clip(CardShape)
        .background(if (pressed) fit.cardPressed else fit.card)
    val clickable = if (onClick != null) base.clickable(interaction, indication = null, onClick = onClick) else base
    Column(modifier.then(clickable).then(if (contentPadding) Modifier.padding(16.dp) else Modifier), content = content)
}

/**
 * A number followed by its unit in smaller type: "72 BPM".
 */
@Composable
fun ValueText(
    value: String,
    unit: String?,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = FitType.value,
    unitColor: Color = fit.textSecondary,
) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = color)) { append(value) }
            if (!unit.isNullOrEmpty()) {
                withStyle(
                    SpanStyle(
                        color = unitColor,
                        fontSize = FitType.unit.fontSize,
                        fontWeight = FitType.unit.fontWeight,
                    ),
                ) { append(" $unit") }
            }
        },
        style = style,
        modifier = modifier,
        maxLines = 1,
    )
}

/**
 * The title row of a metric card: colored icon and title, an optional time label, and a chevron.
 */
@Composable
fun CardHeader(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    showChevron: Boolean = true,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            title, style = FitType.subheadlineBold, color = color, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        trailing?.let { Text(it, style = FitType.footnote, color = fit.textSecondary) }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null,
                tint = fit.textTertiary, modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * A Summary card for one metric: header, the value block on the left, and an optional chart on the right.
 */
@Composable
fun MetricCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    chart: (@Composable RowScope.() -> Unit)? = null,
    value: @Composable ColumnScope.() -> Unit,
) {
    FitCard(modifier, onClick = onClick) {
        CardHeader(title, icon, color, trailing = trailing, showChevron = onClick != null)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), content = value)
            if (chart != null) {
                Spacer(Modifier.width(12.dp))
                chart()
            }
        }
    }
}

/**
 * A grouped section title: bold, primary color, above a card ("Highlights", "Trends").
 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text, style = FitType.title3, color = fit.textPrimary, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action, style = FitType.subheadline, color = com.apenasangelo.bridgefit.ui.theme.FitColors.accent,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

/**
 * A small uppercase caption above a grouped list ("GENERAL").
 */
@Composable
fun GroupCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), style = FitType.footnote, color = fit.textSecondary,
        modifier = modifier.padding(start = 16.dp, top = 24.dp, bottom = 6.dp),
    )
}

/**
 * One row of a grouped list: optional icon tile, title, subtitle, and a chevron.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = Color.White,
    iconBackground: Color? = null,
    titleColor: Color = fit.textPrimary,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .background(if (pressed) fit.cardPressed else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(interaction, null, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            if (iconBackground != null) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(iconBackground),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp)) }
            } else {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = FitType.body, color = titleColor)
            subtitle?.let { Text(it, style = FitType.footnote, color = fit.textSecondary) }
        }
        trailing?.invoke()
        if (showChevron && onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = fit.textTertiary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * A card holding [ListRow]s separated by inset dividers.
 */
@Composable
fun GroupedList(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    FitCard(modifier, contentPadding = false) {
        rows.forEachIndexed { index, row ->
            row()
            if (index < rows.lastIndex) {
                HorizontalDivider(Modifier.padding(start = 58.dp), thickness = 0.5.dp, color = fit.separator)
            }
        }
    }
}

/**
 * A label above a value, for the statistic rows of detail screens.
 */
@Composable
fun Stat(label: String, value: String, unit: String?, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = FitType.footnote, color = fit.textSecondary, maxLines = 1)
        ValueText(value, unit, color, style = FitType.title2)
    }
}
