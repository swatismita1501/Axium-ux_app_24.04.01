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
import com.ingenico.retail.arc.trace.ArcTrace
import com.ingenico.retail.arc.trace.ArcTraceCategory
import com.ingenico.retail.ui.x.server.*
import com.ingenico.retail.ui.x.server.forms.view.LineDisplayView
import com.ingenico.retail.ui.x.server.util.handleNonActivityRequest

class UxServerBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            context?.also {
                UxIntent.getJsonMessage(intent)?.let { jsonMessage ->
                    trace.i { "-> UX JSON: $jsonMessage" }

                    UxRequest.getRequest(jsonMessage)?.also { request ->

                        // Handle static data for Line display view.
                        LineDisplayView.handleRequest(request)

                        request.nonForm?.also {
                            handleNonActivityRequest(UxServer(UxServerBroadcastTransport(context)), request)
                        }

                        request.form?.also {
                            MainApplication.showMainActivity(context, intent)
                        }

                    } ?: trace.e { "Failed to parse JSON message." }

                } ?: trace.e { "No request detected in received broadcast." }
            } ?: trace.e { "Ignore request, no context available." }
        } catch (e: Exception) {
            trace.e { "Exception during handling ux-client request: ${e.message}" }
        }
        abortBroadcast()
    }

    companion object {
        private val trace = ArcTrace(ArcTraceCategory.Ui)
    }
}