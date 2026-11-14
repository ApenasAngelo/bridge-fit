package com.apenasangelo.bridgefit

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceFragmentCompat
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2

/**
 * Process-start hooks of Bridge Fit, kept out of upstream files.
 *
 * 1. Sends every launch of the upstream main screen ([ControlCenterv2]) to [BridgeFitActivity], unless it was opened
 *    on purpose as the "Classic interface" ([EXTRA_CLASSIC]). Upstream opens ControlCenterv2 explicitly from many
 *    places (the ongoing notification, Up navigation, the end of some pairing flows, launcher widgets).
 * 2. Re-skins the upstream screens that Bridge Fit does not replace: an iOS-style circular back button, and
 *    preference screens drawn as grouped lists. Their colors come from the overridden leaf colors in
 *    `src/bridgefit/res/values/colors.xml`.
 *
 * This is a ContentProvider only so that it runs at process start without editing GBApplication; it serves no data.
 */
class BridgeFitBootstrap : ContentProvider() {
    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? Application ?: return true
        app.registerActivityLifecycleCallbacks(Callbacks)
        return true
    }

    private object Callbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity is ControlCenterv2 && !activity.intent.getBooleanExtra(EXTRA_CLASSIC, false)) {
                redirect(activity)
                return
            }
            if (activity is BridgeFitActivity) return
            (activity as? AppCompatActivity)?.supportActionBar?.setHomeAsUpIndicator(R.drawable.bf_back_circle)
            (activity as? FragmentActivity)?.supportFragmentManager
                ?.registerFragmentLifecycleCallbacks(FragmentHooks, true)
        }

        private fun redirect(activity: Activity) {
            val intent = Intent(activity, BridgeFitActivity::class.java).apply {
                action = activity.intent.action
                activity.intent.extras?.let { putExtras(it) }
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            activity.startActivity(intent)
            activity.finish()
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
        }

        override fun onActivityStarted(activity: Activity) {
            // Some screens set their action bar after onCreate.
            if (activity !is BridgeFitActivity) {
                (activity as? AppCompatActivity)?.supportActionBar?.setHomeAsUpIndicator(R.drawable.bf_back_circle)
            }
        }

        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    private object FragmentHooks : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            if (f is PreferenceFragmentCompat) {
                val list = f.listView ?: return
                if (list.itemDecorationCount == 0 || (0 until list.itemDecorationCount).none { list.getItemDecorationAt(it) is GroupedPreferenceDecoration }) {
                    list.addItemDecoration(GroupedPreferenceDecoration(f.requireContext()))
                }
            }
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        /** Set on intents that open ControlCenterv2 on purpose, as the classic interface. */
        const val EXTRA_CLASSIC = "com.apenasangelo.bridgefit.EXTRA_CLASSIC"
    }
}
