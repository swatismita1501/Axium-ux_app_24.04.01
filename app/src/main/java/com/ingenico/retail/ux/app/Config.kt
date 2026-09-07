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

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.Keep
import androidx.annotation.RawRes
import com.google.gson.Gson
import com.ingenico.retail.arc.trace.ArcTraceCategory
import com.ingenico.retail.arc.trace.ArcTraceLevel
import com.ingenico.retail.ui.x.server.UxAccessibilityManager.DEFAULT_ACCESSIBILITY_MODE_ENABLE_CLICK_SOUND
import com.ingenico.retail.ui.x.server.UxAccessibilityManager.DEFAULT_ACCESSIBILITY_MODE_ENABLE_SWIPE_ACTION
import com.ingenico.retail.ui.x.server.UxAccessibilityManager.DEFAULT_ACCESSIBILITY_MODE_IDENTIFY_TIME_MS
import com.ingenico.retail.ui.x.server.UxAccessibilityManager.DEFAULT_ACCESSIBILITY_MODE_ACTIVATION_TIME_MS
import com.ingenico.retail.ui.x.server.UxAccessibilityManager.DEFAULT_ACCESSIBILITY_MODE_ENABLE_ACTIVATION_BY_HEADPHONE_PLUGIN
import java.io.File

/**
 * Reads config from res/raw/config.json file once per application life.
 */
class Config(context: Context) {

    val autoStartOnReboot: Boolean
        get() = configValues.autoStartOnReboot ?: false

    val startArcService: Boolean
        get() = configValues.startArcService ?: false

    val traceCategories: ULong
        get() = try {
            configValues.trace?.categories?.toULong(radix = 16) ?: throw Exception()
        } catch (_: Exception) {
            ArcTraceCategory.Ui.bitmask
        }

    val traceLevel: Int
        get() = try {
            configValues.trace?.level?.toInt() ?: throw Exception()
        } catch (_: Exception) {
            ArcTraceLevel.Error.value
        }

    val traceFolder: String
        get() = configValues.trace?.folder ?: DEFAULT_TRACE_FILE_FOLDER

    val accessibilityModeActivationTimeMs: Long
        get() = configValues.accessibilityMode?.activationTimeMs ?: DEFAULT_ACCESSIBILITY_MODE_ACTIVATION_TIME_MS

    val accessibilityModeIdentifyTimeMs: Long
        get() = configValues.accessibilityMode?.identifyTimeMs ?: DEFAULT_ACCESSIBILITY_MODE_IDENTIFY_TIME_MS

    val accessibilityModeEnableClickSound: Boolean
        get() = configValues.accessibilityMode?.enableClickSound ?: DEFAULT_ACCESSIBILITY_MODE_ENABLE_CLICK_SOUND

    val accessibilityModeEnableSwipeAction: Boolean
        get() = configValues.accessibilityMode?.enableSwipeAction ?: DEFAULT_ACCESSIBILITY_MODE_ENABLE_SWIPE_ACTION

    val accessibilityModeEnableActivationByHeadphonePlugIn: Boolean
        get() = configValues.accessibilityMode?.enableActivationByHeadphonePlugIn
            ?: DEFAULT_ACCESSIBILITY_MODE_ENABLE_ACTIVATION_BY_HEADPHONE_PLUGIN

    // Json object structure
    @Keep
    private data class ConfigValues(
        var autoStartOnReboot: Boolean? = null,
        var startArcService: Boolean? = null,
        var trace: Trace? = null,
        var accessibilityMode: AccessibilityMode? = null
    )

    @Keep
    private data class Trace(
        var categories: String? = null,
        var level: String? = null,
        var folder: String? = null
    )

    @Keep
    private data class AccessibilityMode(
        var activationTimeMs: Long? = null,
        var identifyTimeMs: Long? = null,
        var enableClickSound: Boolean? = null,
        var enableSwipeAction: Boolean? = null,
        var enableActivationByHeadphonePlugIn: Boolean? = null,
    )

    // Reads json data from raw resource, and parse it to the Object [T].
    private inline fun <reified T> Context.jsonToClass(@RawRes resourceId: Int): T =
        Gson().fromJson(resources.openRawResource(resourceId).bufferedReader().use { it.readText() }, T::class.java)

    init {
        // run it once for all instances
        if (!hasConfigBeenRead) {
            hasConfigBeenRead = true
            try {
                configValues = context.jsonToClass(R.raw.config)
                File(CONFIG_FILE_FOLDER).mkdirs()
                readConfigFrom(CONFIG_FILE_PATH)?.let { other ->
                    configValues.merge(other)
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Reads config from file and returns result as ConfigValues or null if file not present.
     * @throws Exception if JSON file has wrong format.
     */
    private fun readConfigFrom(fileName: String): ConfigValues? =
        File(fileName).let {
            if (it.exists())
                Gson().fromJson(it.readLines().joinToString(""), ConfigValues::class.java)
            else
                null
        }


    /** Merges [other] config into this config. */
    private fun ConfigValues.merge(other: ConfigValues) {
        other.startArcService?.also { this.startArcService = it }
        other.autoStartOnReboot?.also { this.autoStartOnReboot = it }
        other.trace?.also { _ ->
            if (this.trace == null) {
                this.trace = Trace()
            }
            this.trace?.also { currentTraceConfig ->
                other.trace?.categories?.also {
                    try {
                        it.toInt(radix = 16) // Validate that string value is Int.
                        currentTraceConfig.categories = it
                    } catch (_: Exception) {
                    }
                }
                other.trace?.level?.also {
                    try {
                        it.toInt() // Validate that string value is Int.
                        currentTraceConfig.level = it
                    } catch (_: Exception) {
                    }
                }
                other.trace?.folder?.also {
                    try {
                        File(it).also { folder ->
                            folder.mkdirs()
                            if (folder.exists())
                                currentTraceConfig.folder = it
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
        other.accessibilityMode?.also { _ ->
            if (this.accessibilityMode == null) {
                this.accessibilityMode = AccessibilityMode()
            }
            this.accessibilityMode?.also { currentAccessibilityConfig ->
                other.accessibilityMode?.activationTimeMs?.also {
                    currentAccessibilityConfig.activationTimeMs = it
                }
                other.accessibilityMode?.identifyTimeMs?.also {
                    currentAccessibilityConfig.identifyTimeMs = it
                }
                other.accessibilityMode?.enableClickSound?.also {
                    currentAccessibilityConfig.enableClickSound = it
                }
                other.accessibilityMode?.enableSwipeAction?.also {
                    currentAccessibilityConfig.enableSwipeAction = it
                }
                other.accessibilityMode?.enableActivationByHeadphonePlugIn?.also {
                    currentAccessibilityConfig.enableActivationByHeadphonePlugIn = it
                }
            }
        }
    }

    private companion object {
        var hasConfigBeenRead = false
        var configValues = ConfigValues()

        @SuppressLint("SdCardPath")
        const val CONFIG_FILE_FOLDER = "/sdcard/UxResources"
        const val CONFIG_FILE_PATH = "${CONFIG_FILE_FOLDER}/config.json"
        const val DEFAULT_TRACE_FILE_FOLDER = "${CONFIG_FILE_FOLDER}/log"
    }
}