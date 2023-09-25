package com.weberhsu.wcalculator.utils

import android.content.*
import android.text.*
import android.util.*
import android.view.*
import android.widget.*
import androidx.core.widget.*
import kotlin.math.*

/**
 * author : weber
 * e-mail : weber0207@gmail.com
 * time : 2023/09/19 7:46 PM * version: 1.0
 * desc : EditText support autosize
 */
object EditTextAutoSizeUtils {

    var originalTextSize: Float = 0f

    /**
     * This function adds font autosizing to the provided `[editText]` by utilizing an invisible
     * autosizing `TextView`. During this process, the `EditText` is replaced within the layout
     * hierarchy with `FrameLayout`, which contains both the original `EditText` and the autosizing
     * `TextView`.
     *
     * @param`EditText`... the `EditText` you intend to make autosizing.
     * @param context ... your active context, used to create the `FrameLayout` and the `TextView`.
     * @return ... the newly created `Framelayout`, just in case you need it.
     */
    fun setupAutoResize(editText: EditText, context: Context): FrameLayout {
        // Step 1 — Create `FrameLayout` and put the `EditText` into it.
        val container = FrameLayout(context)
        val orgLayoutParams = editText.layoutParams

        (editText.parent as? ViewGroup)?.let { editParent ->
            editParent.indexOfChild(editText).let { index ->
                editParent.removeViewAt(index)
                container.addView(
                    editText,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                editParent.addView(container, index, orgLayoutParams)
            }
        }

        // Step 2 — Create the invisible autosizing `TextView` and add it to the `FrameLayout`.
        val textView = createAutoSizeHelperTextView(editText, context)
        container.addView(textView, 0, editText.layoutParams)

        // Step 3 — Install a listener to keep `TextView` and `EditText` in sync.
        editText.addTextChangedListener(object : TextWatcher {
            val originalTextSize =
                if (EditTextAutoSizeUtils.originalTextSize > 0) EditTextAutoSizeUtils.originalTextSize else editText.textSize

            // Apply the changed text to the `TextView` and its new calculated `textSize` to the `EditText`.
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textView.setText(s?.toString(), TextView.BufferType.EDITABLE)
//                 `textView` lays itself out again, so delay the query of the new `textSize` by using `post{ ... }`.
                editText.post {
                    val optimalSize =
                        if (s.isNullOrBlank())
                            originalTextSize
                        else {
                            val autosize = textView.textSize
                            autosize
                        }
                    editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, optimalSize)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(editable: Editable?) {}
        })

        return container
    }

    /**
     * Creates the invisible `TextView` we use for the `textSize` calculation. It uses the same
     * padding as the `EditText`, since we need both with matching sizes to yield the best possible
     * `textSize` results.
     */
    private fun createAutoSizeHelperTextView(editText: EditText, context: Context): TextView =
        TextView(context).apply {
            maxLines = 1
            visibility = View.INVISIBLE
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                spToPx(context, AUTOSIZE_EDITTEXT_MINTEXTSIZE_SP),
                // It's a good idea to set the helper's max `textSize` to the initial text size of the `EditText` to avoid excessively inflating the font size.
                editText.textSize.roundToInt(),
                spToPx(context, AUTOSIZE_EDITTEXT_STEPSIZE_GRANULARITY_SP),
                TypedValue.COMPLEX_UNIT_PX
            )
            // Ensure `autosizeHelper` has the same layout parameters as the `EditText`.
            setPadding(
                editText.paddingLeft,
                editText.paddingTop,
                editText.paddingRight,
                editText.paddingBottom
            )
        }

    private const val AUTOSIZE_EDITTEXT_MINTEXTSIZE_SP = 12f
    private const val AUTOSIZE_EDITTEXT_STEPSIZE_GRANULARITY_SP = 1f

    private fun spToPx(context: Context, sp: Float) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics
        ).toInt()
}