package com.weberhsu.wcalculator.extensions

import android.view.*

/**
 * author : weber
 * e-mail : weber0207@gmail.com
 * time : 2023/09/14 11:31 PM * version: 1.0
 * desc : Extension functions for view
 */
fun View.performHapticFeedback() =
    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
