/*  Copyright (C) 2026 Bridge Fit contributors

    This file is part of Bridge Fit, a fork of Gadgetbridge.

    Bridge Fit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Bridge Fit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package com.apenasangelo.bridgefit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.apenasangelo.bridgefit.ui.BridgeFitApp
import com.apenasangelo.bridgefit.ui.FitViewModel
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.activities.GBActivity
import nodomain.freeyourgadget.gadgetbridge.activities.welcome.WelcomeActivity
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper
import java.util.Locale

/**
 * The launcher activity of Bridge Fit: hosts the Compose UI, and does the start-up work that
 * ControlCenterv2 does upstream (first-run welcome flow, device info request, shortcuts, quit).
 */
class BridgeFitActivity : AppCompatActivity(), GBActivity {
    private val vm: FitViewModel by viewModels()
    private var isLanguageInvalid = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                GBApplication.ACTION_QUIT -> finish()
                GBApplication.ACTION_LANGUAGE_CHANGE -> setLanguage(GBApplication.getLanguage(), true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BridgeFitApp(vm) }

        LocalBroadcastManager.getInstance(this).registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(GBApplication.ACTION_QUIT)
                addAction(GBApplication.ACTION_LANGUAGE_CHANGE)
            },
        )

        if (GBApplication.getPrefs().getBoolean("first_run", true)) {
            startActivity(Intent(this, WelcomeActivity::class.java))
        }
        handleShortcut(intent)
        GBApplication.deviceService().requestDeviceInfo()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShortcut(intent)
    }

    override fun onResume() {
        super.onResume()
        if (isLanguageInvalid) {
            isLanguageInvalid = false
            recreate()
            return
        }
        vm.refreshDevices()
        vm.reload()
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun setLanguage(language: Locale, invalidateLanguage: Boolean) {
        if (invalidateLanguage) {
            isLanguageInvalid = true
        }
        AndroidUtils.setLanguage(this, language)
    }

    private fun handleShortcut(intent: Intent?) {
        if (intent?.action != DeviceService.ACTION_CONNECT) return
        val address = intent.getStringExtra("device") ?: return
        val candidate = DeviceHelper.getInstance().findAvailableDevice(address) ?: return
        if (!candidate.isConnected) {
            GBApplication.deviceService(candidate).connect()
        }
    }
}
