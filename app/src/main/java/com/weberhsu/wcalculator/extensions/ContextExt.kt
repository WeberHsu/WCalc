package com.weberhsu.wcalculator.extensions

import android.app.*
import android.content.*
import android.content.res.*
import android.os.*
import android.widget.*
import com.weberhsu.wcalculator.*

/**
 * author : weber
 * e-mail : weber0207@gmail.com
 * time : 2023/09/15 12:21 AM * version: 1.0
 * desc : Extension functions for context
 */

fun Context.isPortrait(): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
}

private fun doToast(context: Context, message: String, length: Int) {
    if (context is Activity) {
        if (!context.isFinishing && !context.isDestroyed) {
            Toast.makeText(context, message, length).show()
        }
    } else {
        Toast.makeText(context, message, length).show()
    }
}


fun Context.toast(msg: String, length: Int = Toast.LENGTH_SHORT) {
    try {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            doToast(this, msg, length)
        } else {
            Handler(Looper.getMainLooper()).post {
                doToast(this, msg, length)
            }
        }
    } catch (e: Exception) {
    }
}

fun Context.copyClipboard(text: String) {
    val clip = ClipData.newPlainText("result", text)
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    val toastText = String.format(getString(R.string.value_copied_to_clipboard_show), text)
    toast(toastText)
}