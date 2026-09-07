/*---------------------------------------------------------------------------------*
 * INGENICO Technical Software Department                                          *
 *---------------------------------------------------------------------------------*
 * Copyright (c) 2025 Ingenico Inc.                                                *
 * 3025 Windward Plaza, Suite 600, Alpharetta, Georgia, 30005, United States       *
 * All rights reserved.                                                            *
 * This source program is the property of the INGENICO Company mentioned above     *
 * and may not be copied in any form or by any means, whether in part or in whole, *
 * except under license expressly granted by such INGENICO company.                *
 * All copies of this source program, whether in part or in whole, and whether     *
 * modified or not, must display this and all other embedded copyright and         *
 * ownership notices in full.                                                      *
 *---------------------------------------------------------------------------------*/

package com.ingenico.retail.ux.app

import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ingenico.retail.ui.x.server.UxAccessibilityManager
import com.ingenico.retail.arc.trace.ArcTrace
import com.ingenico.retail.arc.trace.ArcTraceCategory
import com.ingenico.retail.ui.x.server.UxFormManager
import com.ingenico.retail.ui.x.server.UxIntent
import com.ingenico.retail.ui.x.server.UxScreenManager
import com.ingenico.retail.ui.x.server.UxServer
import com.ingenico.retail.ui.x.server.UxServerBroadcastTransport

class MainActivity : AppCompatActivity() {

    // Variable keep a video play position in case form goes to background.
    private var savedVideoPlayPosition: Int? = null

    private val trace = ArcTrace(ArcTraceCategory.Ui)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        trace.i { "Activity for ux-server is creating." }

        // Uncomment next line to add additional security for broadcast message.
        //UxServer.isAdditionalSecurity = true

        supportActionBar?.hide()
        hideSystemUI(window)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets ->
            // Check if navigation bars are visible in the provided window insets.
            if (windowInsets.isVisible(WindowInsetsCompat.Type.navigationBars())) {
                // If navigation bars are visible, hide system UI.
                hideSystemUI(window)
            }
            windowInsets
        }

        UxScreenManager.currentActivity = this

        UxAccessibilityManager.init(this).also {
            Config(this@MainActivity).also { appConfig ->
                with(UxAccessibilityManager.config) {
                    activationTimeMs = appConfig.accessibilityModeActivationTimeMs
                    identifyTimeMs = appConfig.accessibilityModeIdentifyTimeMs
                    enableClickSound = appConfig.accessibilityModeEnableClickSound
                    enableSwipeAction = appConfig.accessibilityModeEnableSwipeAction
                    enableActivationByHeadphonePlugIn = appConfig.accessibilityModeEnableActivationByHeadphonePlugIn
                }
            }
        }

        // Set default Form orientation.
        // Set Form orientation received from ux-client.
        UxScreenManager.orientationMode.toAndroidOrientation().let {
            UxFormManager.init(this, UxServer(UxServerBroadcastTransport(this.baseContext)), it, null)
            requestedOrientation = it
        }

        // Show default view.
        setContentView(R.layout.activity_main)

        // Handle UI request received via broadcast or saved after destroy.
        UxIntent.getJsonMessage(intent)?.also { jsonRequest ->
            trace.i { "JSON request: $jsonRequest " }
        }
        UxFormManager.handleRequest(this, intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Show view received in request.
        UxFormManager.handleRequest(this, intent)
    }

    override fun onResume() {
        super.onResume()
        UxFormManager.videoPlayPosition = savedVideoPlayPosition
        activityOnTop = true
    }

    override fun onPause() {
        super.onPause()
        savedVideoPlayPosition = UxFormManager.videoPlayPosition
        activityOnTop = false
    }

    override fun onDestroy() {
        super.onDestroy()
        trace.i { "onDestroy Activity." }
        UxFormManager.clear()
        UxAccessibilityManager.clear()
        UxScreenManager.currentActivity = null

    }

    private fun hideSystemUI(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        var activityOnTop = false
    }
}
