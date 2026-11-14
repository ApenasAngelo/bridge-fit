package com.apenasangelo.bridgefit.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.apenasangelo.bridgefit.ui.metric.PushedScreen
import com.apenasangelo.bridgefit.ui.prefs.Pref
import com.apenasangelo.bridgefit.ui.prefs.PrefSection
import com.apenasangelo.bridgefit.ui.prefs.prefSections
import com.apenasangelo.bridgefit.ui.prefs.rememberPrefVersion
import com.apenasangelo.bridgefit.ui.theme.FitColors
import com.apenasangelo.bridgefit.ui.theme.FitType
import com.apenasangelo.bridgefit.ui.theme.fit
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser

private val Gray = Color(0xFF8E8E93)

/**
 * Keys whose change upstream sends to the devices, and keys that need the device list refreshed, as in
 * AboutUserPreferencesActivity.
 */
private val SEND_TO_DEVICE = setOf(
    ActivityUser.PREF_USER_NAME, ActivityUser.PREF_USER_DATE_OF_BIRTH, ActivityUser.PREF_USER_HEIGHT_CM,
    ActivityUser.PREF_USER_WEIGHT_KG, ActivityUser.PREF_USER_GENDER, ActivityUser.PREF_USER_STEPS_GOAL,
    ActivityUser.PREF_USER_CALORIES_BURNT, ActivityUser.PREF_USER_GOAL_WEIGHT_KG,
    ActivityUser.PREF_USER_GOAL_STANDING_TIME_HOURS, ActivityUser.PREF_USER_GOAL_FAT_BURN_TIME_MINUTES,
    ActivityUser.PREF_USER_GOAL_HYDRATION_ML,
)

/**
 * Native replacement of the upstream "About you" screen: personal data and daily goals.
 */
@Composable
fun ProfileScreen(onBack: () -> Unit, onChangedGoals: () -> Unit) {
    val context = LocalContext.current
    rememberPrefVersion()
    val name = GBApplication.getPrefs().getString(ActivityUser.PREF_USER_NAME, "").orEmpty()
    val onChanged: (String) -> Unit = { key ->
        if (key in SEND_TO_DEVICE) GBApplication.deviceService().onSendConfiguration(key)
        LocalBroadcastManager.getInstance(context.applicationContext)
            .sendBroadcast(Intent(DeviceManager.ACTION_REFRESH_DEVICELIST))
        onChangedGoals()
    }
    val sections = listOf(
        PrefSection(
            R.string.bf_section_personal,
            listOf(
                Pref.Text(ActivityUser.PREF_USER_NAME, R.string.bf_p_name, Icons.Rounded.Badge, Gray, maxLength = 17),
                Pref.Date(ActivityUser.PREF_USER_DATE_OF_BIRTH, R.string.bf_p_birth, Icons.Rounded.Cake, FitColors.move, "2000-01-01"),
                Pref.Choice(ActivityUser.PREF_USER_GENDER, R.string.bf_p_gender, Icons.Rounded.Wc, FitColors.sleep, R.array.gender, R.array.gender_values, "2"),
                Pref.Number(ActivityUser.PREF_USER_HEIGHT_CM, R.string.bf_p_height, Icons.Rounded.Height, FitColors.accent, 175, "cm", 3),
                Pref.Number(ActivityUser.PREF_USER_WEIGHT_KG, R.string.bf_p_weight, Icons.Rounded.Scale, FitColors.battery, 70, "kg", 3),
                Pref.Number(ActivityUser.PREF_USER_STEP_LENGTH_CM, R.string.bf_p_step_length, Icons.Rounded.Straighten, FitColors.steps, 0, "cm", 3),
            ),
        ),
        PrefSection(
            R.string.bf_section_goals,
            listOf(
                Pref.Number(ActivityUser.PREF_USER_CALORIES_BURNT, R.string.bf_g_move, Icons.Rounded.LocalFireDepartment, FitColors.move, 350, "kcal", 4),
                Pref.Number(ActivityUser.PREF_USER_ACTIVETIME_MINUTES, R.string.bf_g_exercise, Icons.Rounded.Timer, FitColors.exercise, 60, "min", 3),
                Pref.Number(ActivityUser.PREF_USER_STEPS_GOAL, R.string.bf_g_steps, Icons.AutoMirrored.Rounded.DirectionsWalk, FitColors.steps, 10000, null, 5),
                Pref.Number(ActivityUser.PREF_USER_DISTANCE_METERS, R.string.bf_g_distance, Icons.Rounded.Route, FitColors.distance, 5000, "m", 5),
                Pref.Number(ActivityUser.PREF_USER_SLEEP_DURATION_MINUTES, R.string.bf_g_sleep, Icons.Rounded.Bedtime, FitColors.sleep, 420, "min", 4),
                Pref.Number(ActivityUser.PREF_USER_GOAL_STANDING_TIME_HOURS, R.string.bf_g_stand, Icons.Rounded.Accessibility, FitColors.steps, 12, "h", 2),
                Pref.Number(ActivityUser.PREF_USER_GOAL_FAT_BURN_TIME_MINUTES, R.string.bf_g_fat_burn, Icons.Rounded.Whatshot, FitColors.temperature, 30, "min", 3),
                Pref.Number(ActivityUser.PREF_USER_GOAL_HYDRATION_ML, R.string.bf_g_hydration, Icons.Rounded.LocalDrink, FitColors.spo2, 2000, "ml", 5),
                Pref.Number(ActivityUser.PREF_USER_GOAL_WEIGHT_KG, R.string.bf_g_weight, Icons.Rounded.MonitorWeight, FitColors.battery, 0, "kg", 3),
            ),
        ),
    )
    PushedScreen(stringResource(R.string.bf_settings_goals), onBack, glow = FitColors.move) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(88.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(FitColors.moveEnd, FitColors.moveStart))),
                    contentAlignment = Alignment.Center,
                ) {
                    if (name.isNotBlank()) {
                        Text(name.take(1).uppercase(), style = FitType.title1, color = Color.White)
                    } else {
                        Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (name.isNotBlank()) Text(name, style = FitType.title2, color = fit.textPrimary)
            }
        }
        prefSections(sections, onChanged)
    }
}
