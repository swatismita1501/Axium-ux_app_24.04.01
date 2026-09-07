/*---------------------------------------------------------------------------------*
 * INGENICO Technical Software Department                                          *
 *---------------------------------------------------------------------------------*
 * Copyright (c) 2021 - 2023 Ingenico Inc.                                         *
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent("smartpos.intent.action.BOOT_COMPLETED").action) {
            if (Config(context).autoStartOnReboot) {
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        }
    }
}