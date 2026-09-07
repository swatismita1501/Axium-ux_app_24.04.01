/*
 * --------------------------------------------------------------------------------*
 * INGENICO Technical Software Department                                          *
 * --------------------------------------------------------------------------------*
 * Copyright (c) 2025 Ingenico Inc.                                                *
 * 3025 Windward Plaza, Suite 600, Alpharetta, Georgia, 30005, United States       *
 * All rights reserved.                                                            *
 * This source program is the property of the INGENICO Company mentioned above     *
 * and may not be copied in any form or by any means, whether in part or in whole, *
 * except under license expressly granted by such INGENICO company.                *
 * All copies of this source program, whether in part or in whole, and whether     *
 * modified or not, must display this and all other embedded copyright and         *
 * ownership notices in full.                                                      *
 * --------------------------------------------------------------------------------*
 */

package com.ingenico.retail.ux.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.ingenico.retail.arc.trace.FileTraceWriter.Companion.BACKUP_FILE_EXTENSION
import com.ingenico.retail.arc.trace.FileTraceWriter.Companion.DEFAULT_TRACE_FILE_NAME
import com.ingenico.retail.arc.trace.FileTraceWriter.Companion.LOG_FILE_EXTENSION
import java.io.File

/**
 * A custom [ContentProvider] for securely sharing log files with external apps via a content URI.
 *
 * Provides read-only access to the `ArcTrace.log` file located in the app's internal trace folder.
 *
 * ## Usage
 * Access the log file using the content URI:
 * ```
 * content://com.ingenico.retail.ux.app.files/ArcTrace.log
 * ```
 *
 * ## Example
 * ```
 * val uri = Uri.parse("content://com.ingenico.retail.ux.app.files/ArcTrace.log")
 * val inputStream = contentResolver.openInputStream(uri)
 * ```
 */
class UxTraceFileProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? =
        DEFAULT_TRACE_FILE_NAME.let { fileName ->
            context?.let { it ->
                Config(it).traceFolder.let { traceFolder ->
                    val file = when {
                        uri.path?.endsWith("$fileName$LOG_FILE_EXTENSION") == true ->
                            File("$traceFolder/$fileName$LOG_FILE_EXTENSION")

                        uri.path?.endsWith("$fileName$BACKUP_FILE_EXTENSION") == true ->
                            File("$traceFolder/$fileName$BACKUP_FILE_EXTENSION")

                        else -> null
                    }

                    file?.takeIf { it.exists() }
                }
            }
        }?.let { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun getType(uri: Uri): String = "text/plain"
}