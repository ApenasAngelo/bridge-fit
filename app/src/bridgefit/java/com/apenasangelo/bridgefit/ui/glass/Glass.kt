package com.apenasangelo.bridgefit.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apenasangelo.bridgefit.ui.theme.fit
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import nodomain.freeyourgadget.gadgetbridge.GBApplication

/**
 * How glass surfaces (tab bar, top bar, floating buttons) are rendered.
 *
 * - [LIQUID]: iOS 26 Liquid Glass. Light blur, vibrancy and lens refraction at the edges (refraction needs Android 13).
 * - [ONE_UI]: Samsung One UI style. Strong blur with a soft tint and boosted saturation, no refraction.
 *
 * Below Android 12 there is no backdrop blur at all, and both fall back to a translucent surface.
 */
enum class GlassStyle(val key: String) {
    LIQUID("liquid"),
    ONE_UI("oneui"),
    ;

    companion object {
        const val PREF_KEY = "bridgefit_glass_style"

        fun current(): GlassStyle {
            val value = GBApplication.getPrefs().getString(PREF_KEY, LIQUID.key)
            return entries.firstOrNull { it.key == value } ?: LIQUID
        }
    }
}

/**
 * Process-wide observable glass style, so changing it in Settings restyles the shell at once.
 */
object GlassSettings {
    var style by mutableStateOf(GlassStyle.LIQUID)
        private set

    fun load() {
        style = GlassStyle.current()
    }

    fun set(value: GlassStyle) {
        GBApplication.getPrefs().preferences.edit().putString(GlassStyle.PREF_KEY, value.key).apply()
        style = value
    }
}

val isBackdropSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The backdrop that glass overlays sample. Null outside a [GlassHost].
 */
val LocalBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Records [content] into a backdrop, and draws [overlay] (the glass layer) above it.
 *
 * Glass must never be drawn inside [content] with this backdrop: an element cannot sample a layer it is part of.
 */
@Composable
fun GlassHost(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
) {
    val background = fit.background
    val backdrop = rememberLayerBackdrop {
        drawRect(background)
        drawContent()
    }
    Box(modifier) {
        Box(Modifier.matchParentSize().layerBackdrop(backdrop), content = content)
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            overlay()
        }
    }
}

/**
 * Draws a glass surface of [shape] behind the element, sampling the nearest [GlassHost].
 * [tint] adds a colored glaze (for a selected or prominent control).
 */
@Composable
fun Modifier.glass(
    shape: CornerBasedShape,
    tint: Color? = null,
    refraction: Boolean = true,
    shadow: Boolean = true,
): Modifier {
    val backdrop = LocalBackdrop.current
    val surfaces = fit
    val style = GlassSettings.style
    val surface = when (style) {
        GlassStyle.LIQUID -> if (surfaces.isDark) Color(0xFF1C1C1E).copy(alpha = 0.42f) else Color.White.copy(alpha = 0.5f)
        GlassStyle.ONE_UI -> if (surfaces.isDark) Color(0xFF202024).copy(alpha = 0.62f) else Color(0xFFF7F7FA).copy(alpha = 0.7f)
    }
    if (backdrop == null || !isBackdropSupported) {
        val fallback = if (surfaces.isDark) Color(0xFF1C1C1E).copy(alpha = 0.96f) else Color(0xFFF9F9F9).copy(alpha = 0.97f)
        return this
            .background(tint?.copy(alpha = 0.25f)?.compositeOverOpaque(fallback) ?: fallback, shape)
            .border(0.5.dp, surfaces.separator.copy(alpha = 0.6f), shape)
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            when (style) {
                GlassStyle.LIQUID -> {
                    vibrancy()
                    blur(6.dp.toPx())
                    if (refraction) {
                        // The refraction band must not exceed the corner radius.
                        val radius = shape.topStart.toPx(size, this)
                        val height = minOf(14.dp.toPx(), radius)
                        lens(height, height * 2f)
                    }
                }

                GlassStyle.ONE_UI -> {
                    colorControls(saturation = 1.7f)
                    blur(30.dp.toPx())
                }
            }
        },
        highlight = { if (style == GlassStyle.ONE_UI || !refraction) null else Highlight.Default },
        shadow = { if (shadow) Shadow.Default else null },
        onDrawSurface = {
            drawRect(surface)
            tint?.let { drawRect(it.copy(alpha = 0.22f)) }
        },
    )
}

private fun Color.compositeOverOpaque(base: Color): Color {
    val a = alpha
    return Color(
        red = red * a + base.red * (1 - a),
        green = green * a + base.green * (1 - a),
        blue = blue * a + base.blue * (1 - a),
        alpha = base.alpha,
    )
}
