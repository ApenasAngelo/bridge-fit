package com.apenasangelo.bridgefit

import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2

/**
 * Sends every launch of the upstream main screen ([ControlCenterv2]) to [BridgeFitActivity], unless it was
 * opened on purpose as the "Classic interface" (see [EXTRA_CLASSIC]).
 *
 * Upstream code opens ControlCenterv2 explicitly from many places (the ongoing notification, Up navigation,
 * the end of some pairing flows, launcher widgets). Redirecting here keeps those upstream files untouched.
 *
 * This is a ContentProvider only so that it runs at process start without editing GBApplication; it serves no data.
 */
class ClassicRedirect : ContentProvider() {
    override fun onCreate(): Boolean {
        val app = context?.applicationContext as? Application ?: return true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity !is ControlCenterv2 || activity.intent.getBooleanExtra(EXTRA_CLASSIC, false)) return
                val redirect = Intent(activity, BridgeFitActivity::class.java).apply {
                    action = activity.intent.action
                    activity.intent.extras?.let { putExtras(it) }
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                activity.startActivity(redirect)
                activity.finish()
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        return true
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
