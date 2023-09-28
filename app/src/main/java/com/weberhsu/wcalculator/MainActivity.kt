package com.weberhsu.wcalculator

import android.annotation.*
import android.os.*
import android.text.*
import android.util.*
import android.view.*
import android.view.accessibility.*
import android.widget.*
import androidx.core.content.*
import androidx.lifecycle.*
import com.gyf.immersionbar.*
import com.weberhsu.wcalculator.base.*
import com.weberhsu.wcalculator.databinding.*
import com.weberhsu.wcalculator.extensions.*
import com.weberhsu.wcalculator.helpers.*
import com.weberhsu.wcalculator.utils.*
import kotlinx.coroutines.*
import java.math.*
import java.text.*
import java.util.*
import kotlin.math.*

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var calc: CalculatorImpl // Left calculator
    private lateinit var calcRight: CalculatorImpl // Right calculator
    private var saveCalculatorState: String = ""
    private var saveCalculatorRightState: String = ""
    private var isCurrentCalculatorLeft: Boolean = true // Check for portrait mode because of two calculators
    private var isRestore = false

    override fun createBinding(layoutInflater: LayoutInflater): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            isRestore = true
            saveCalculatorState = savedInstanceState.getCharSequence(CALCULATOR_STATE) as String
            saveCalculatorRightState =
                savedInstanceState.getCharSequence(CALCULATOR_STATE_RIGHT) as String
            isCurrentCalculatorLeft = savedInstanceState.getBoolean(CALCULATOR_IS_LEFT)
        }

        calc = CalculatorImpl(true, saveCalculatorState)
        // For right calculator in landscape mode
        calcRight = CalculatorImpl(false, saveCalculatorRightState)

        // Need to confirm calculator is left or right calculator in portrait mode (because of only one calculator)
        if (isPortrait()) {
            initCalculatorUiComponents(binding.calculator, getCalculator())
            // Focus by default
            binding.calculator.input.requestFocus()
        } else {
            initCalculatorUiComponents(binding.calculator, calc)
            initCalculatorUiComponents(binding.calculatorRight, calcRight)
            // Focus by default
            (if (isCurrentCalculatorLeft) binding.calculator.input else binding.calculatorRight?.input)?.requestFocus()
        }

        binding.btnDelete?.setVibratingOnClickListener {
            backspaceButton()
        }
        binding.btnToLeft?.setVibratingOnClickListener {
            pushInputResult(isToLeft = true)
        }
        binding.btnToRight?.setVibratingOnClickListener {
            pushInputResult(isToLeft = false)
        }

        // For left calculator
        getNumberRelatedButtonIds()?.forEach {
            it.setOnClickListener { view ->
                // May execute with calc or calcRight
                updateDisplay((view as TextView).text.toString(), getCalculator())
            }
        }

        // For right calculator
        getNumberRelatedButtonIds(false)?.forEach {
            it.setOnClickListener { view ->
                updateDisplay((view as TextView).text.toString(), calcRight)
            }
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putString(CALCULATOR_STATE, calc.getCalculatorStateJson().toString())
        bundle.putString(CALCULATOR_STATE_RIGHT, calcRight.getCalculatorStateJson().toString())
        bundle.putBoolean(CALCULATOR_IS_LEFT, isCurrentCalculatorLeft)
    }

    // In portrait mode, we need to check we use which (calc, calcRight) calculator now
    private fun getCalculator() = if (isCurrentCalculatorLeft) calc else calcRight

    override fun onResume() {
        super.onResume()

        if (isPortrait()) {
            // landscape -> portrait
            restoreCalculationResults(if (isCurrentCalculatorLeft) calc else calcRight)
        } else {
            // portrait -> landscape
            restoreCalculationResults(calc)
            restoreCalculationResults(calcRight)
        }

        isRestore = false
    }

    private fun getNumberRelatedButtonIds(isLeftCalculator: Boolean = true) =
        (if (isLeftCalculator) binding.calculator else binding.calculatorRight)?.run {
            arrayOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9)
        }

    @SuppressLint("ClickableViewAccessibility")
    private fun initCalculatorUiComponents(
        view: ViewCalculatorBinding?,
        calculator: CalculatorImpl
    ) {
        view?.input?.let {
            try {
                // Autosize edittext
                EditTextAutoSizeUtils.setupAutoResize(it, this)
            } catch (_: Exception) {
            }
            // Disable the keyboard on display EditText
            it.showSoftInputOnFocus = false
        }

        view?.btnX?.setOnClickOperation(MULTIPLE, calculator)
        view?.btnPlus?.setOnClickOperation(PLUS, calculator)
        view?.btnMinus?.setOnClickOperation(MINUS, calculator)
        view?.btnDivide?.setOnClickOperation(DIVIDE, calculator)
        view?.btnPercent?.setOnClickOperation(PERCENT, calculator)

        view?.btnDot?.setVibratingOnClickListener {
            updateDisplay(decimalSeparatorSymbol, calculator)
        }
        view?.btnAc?.setVibratingOnClickListener {
            clearButton(calculator)
        }
        view?.btnEqual?.setVibratingOnClickListener {
            equalsButton(calculator)
        }
        view?.btnPlusMinus?.setVibratingOnClickListener {
            calculator.getResultAfterPositiveOrNegative()?.let {
                showNewInput(it, calculator)
                view.input.setSelection(it.length)
            }
        }
        view?.input?.setOnLongClickListener { copyToClipboard(view.input.text.toString()) }
        view?.tvResult?.setOnLongClickListener { copyToClipboard(view.tvResult.text.toString()) }

        // Do not clear after equal button if you move the cursor
        view?.input?.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                super.sendAccessibilityEvent(host, eventType)
                if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                    calculator.isEqualLastAction = false
                }
                if (view?.input?.isCursorVisible == false) {
                    view.input.isCursorVisible = true
                }
            }
        }

        // Handle changes into input to update resultDisplay
        view?.input?.addTextChangedListener(object : TextWatcher {
            private var beforeTextLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeTextLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!isRestore) {
                    // Pass first call when rotation
                    updateResultDisplay(calculator)
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })

        view?.input?.setOnTouchListener { _, _ ->
            actionAfterClick(calculator)
            false
        }

        // Button AC action
        handleAcAction(view, calculator)
    }

    private fun copyToClipboard(value: String): Boolean {
        return if (value.isEmpty()) {
            false
        } else {
            copyClipboard(value)
            true
        }
    }

    private fun pushInputResult(isToLeft: Boolean) {
        val source = (if (isToLeft) calcRight else calc)
        val target = (if (isToLeft) calc else calcRight)
        if (source.isResultValid()) {
            isCurrentCalculatorLeft = isToLeft
            target.run {
                // If source's last operation is equals, just pass current input text
                resetValues()
                currentInput = source.currentInput
            }
            showNewInput(target.currentInput, target)
            (if (isToLeft) {
                binding.calculator
            } else {
                binding.calculatorRight
            })?.run {
                input.setSelection(target.currentInput.length)
                input.requestFocus()
            }
            updateResultDisplay(target)
        } else {
            Toast.makeText(this, getString(R.string.can_not_do_action_because_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAcAction(view: ViewCalculatorBinding?, calculator: CalculatorImpl) {
        if (isPortrait()) {
            view?.btnAc?.text = "DEL"
            view?.btnAc?.setVibratingOnClickListener {
                backspaceButton()
            }
            view?.btnAc?.setOnLongClickListener {
                clearButton(calculator)
                true
            }
        } else {
            view?.btnAc?.text = "AC"
            view?.btnAc?.setVibratingOnClickListener {
                checkCurrentCalculatorIndicator(calculator)
                clearButton(calculator)
            }
        }
    }

    private fun restoreCalculationResults(calculator: CalculatorImpl) {
        if (!isRestore) return

        getUpdatingView(calculator)?.input?.let {
            showNewInput(calculator.currentInput, calculator)
            it.setSelection(calculator.currentInput.length)
            showNewResult(calculator.currentResult, calculator)
        }

        // Focus
        if (isPortrait()) {
            binding.calculator.input.requestFocus()
        } else {
            if (isCurrentCalculatorLeft) {
                binding.calculator.input.requestFocus()
            } else {
                binding.calculatorRight?.input?.requestFocus()
            }
        }
    }

    private fun checkCurrentCalculatorIndicator(calculator: CalculatorImpl) {
        // Only need to change in landscape mode
        if (!isPortrait()) {
            isCurrentCalculatorLeft = calculator.isLeft
        }
    }

    private fun View.setOnClickOperation(symbol: String, calculator: CalculatorImpl) {
        setVibratingOnClickListener {
            checkCurrentCalculatorIndicator(calculator)
            addSymbol(symbol, calculator)
        }
    }

    private fun View.setVibratingOnClickListener(callback: (view: View) -> Unit) {
        setOnClickListener {
            callback(it)
            it.performHapticFeedback()
        }
    }

    private fun setErrorColor(errorStatus: Boolean, calculator: CalculatorImpl) {
        // Only run if the color needs to be updated
        if (errorStatus != calculator.errorStatusOld) {
            // Set error color
            getUpdatingView(calculator)?.let {
                it.input.setTextColor(
                    ContextCompat.getColor(this, if (errorStatus) R.color.calculation_error_color else R.color.number)
                )
                it.tvResult.setTextColor(
                    ContextCompat.getColor(this, if (errorStatus) R.color.calculation_error_color else R.color.number)
                )
                calculator.errorStatusOld = errorStatus
            }
        }
    }

    private fun updateDisplay(value: String, calculator: CalculatorImpl) {
        getUpdatingView(calculator)?.input?.let { it ->
            // Reset input with current number if following "equal"
            if (calculator.isEqualLastAction) {
                val anyNumber = "0123456789$decimalSeparatorSymbol".toCharArray().map {
                    it.toString()
                }
                if (anyNumber.contains(value)) {
                    showNewInput("", calculator)
                } else {
                    it.setSelection(it.text.length)
                }
                calculator.isEqualLastAction = false
            }

            if (!it.isCursorVisible) {
                it.isCursorVisible = true
            }

            lifecycleScope.launch(Dispatchers.Default) {
                val formerValue = it.text.toString()
                val cursorPosition = it.selectionStart ?: 0
                val leftValue = formerValue.subSequence(0, cursorPosition).toString()
                val rightValue =
                    formerValue.subSequence(cursorPosition, formerValue.length).toString()

                val newValue = leftValue + value + rightValue

                var newValueFormatted = calculator.getNumberFormatted(newValue)

                withContext(Dispatchers.Main) {
                    // Avoid two decimalSeparator in the same number
                    // When you click on the decimalSeparator button
                    if (value == decimalSeparatorSymbol && decimalSeparatorSymbol in it.text.toString()) {
                        if (it.text.toString().isNotEmpty()) {
                            var lastNumberBefore = ""
                            if (cursorPosition > 0 && it.text.toString()
                                    .substring(0, cursorPosition)
                                    .last() in "0123456789\\$decimalSeparatorSymbol"
                            ) {
                                lastNumberBefore = NumberFormatter.extractNumbers(
                                    it.text.toString().substring(0, cursorPosition),
                                    decimalSeparatorSymbol
                                ).last()
                            }
                            var firstNumberAfter = ""
                            if (cursorPosition < (it.text?.length ?: 0) - 1) {
                                firstNumberAfter = NumberFormatter.extractNumbers(
                                    it.text.toString()
                                        .substring(cursorPosition, it.text?.length ?: 0),
                                    decimalSeparatorSymbol
                                ).first()
                            }
                            if (decimalSeparatorSymbol in lastNumberBefore || decimalSeparatorSymbol in firstNumberAfter) {
                                return@withContext
                            }
                        }
                    }

                    // Update Display
                    showNewInput(newValueFormatted, calculator)
                    actionAfterClick(calculator)

                    // Increase cursor position
                    val cursorOffset = newValueFormatted.length - newValue.length
                    it.setSelection(cursorPosition + value.length + cursorOffset)
                }
            }
        }
    }

    private fun actionAfterClick(calculator: CalculatorImpl) {
        if (!isPortrait()) {
            isCurrentCalculatorLeft = calculator.isLeft
        }
        getUpdatingView(calculator)?.input?.requestFocus()
    }

    @SuppressLint("SetTextI18n")
    private fun updateResultDisplay(calculator: CalculatorImpl) {
        getUpdatingView(calculator)?.let {
            lifecycleScope.launch(Dispatchers.Default) {
                // Reset text color
                setErrorColor(false, calculator)

                val calculation = it.input.text?.toString()

                if (calculation != "") {
                    calculator.resetErrorTypes()

                    calculator.evaluate(
                        Expression().getCleanExpression(it.input.text.toString(), decimalSeparatorSymbol, groupingSeparatorSymbol)
                    )

                    // If result is a number and it is finite
                    if (calculator.isResultValid()) {
                        // Round
                        calculator.roundResult()
                        var formattedResult = calculator.getNumberFormatted(
                            calculator.calculationResult.toString().replace(".", decimalSeparatorSymbol)
                        )

                        // Remove zeros at the end of the results (after point)
                        if (!SCIENTISTIC_MODE_OPEN || !(calculator.calculationResult >= BigDecimal(THRESHOLD_SCIENTIFIC_NOTATION) || calculator.calculationResult <= BigDecimal(
                                0.1
                            ))
                        ) {
                            calculator.getResultWithZeroHandled(calculator.calculationResult.toString())?.let {
                                formattedResult = it
                            }
                        }

                        withContext(Dispatchers.Main) {
                            if (formattedResult != calculation) {
                                showNewResult(formattedResult, calculator)
                            } else {
                                // If last operation is equal, don't clear result value
                                if (!calculator.isEqualLastAction) {
                                    showNewResult("", calculator)
                                }
                            }
                        }

                    } else withContext(Dispatchers.Main) {
                        showNewResult("", calculator)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showNewResult("", calculator)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun equalsButton(calculator: CalculatorImpl) {
        getUpdatingView(calculator)?.let {
            actionAfterClick(calculator)

            lifecycleScope.launch(Dispatchers.Default) {
                val calculation = it.input.text.toString()

                if (calculation != "") {
                    val resultString = calculator.calculationResult.toString()
                    var formattedResult = calculator.getNumberFormatted(resultString.replace(".", decimalSeparatorSymbol))

                    // If result is a number and it is finite
                    if (calculator.isResultValid()) {
                        calculator.getResultWithZeroHandled(resultString)?.let {
                            formattedResult = it
                        }

                        // Hide the cursor before updating binding.input to avoid weird cursor movement
                        withContext(Dispatchers.Main) {
                            it.input.isCursorVisible = false
                        }

                        // Display result
                        withContext(Dispatchers.Main) {
                            showNewInput(formattedResult, calculator)
                        }

                        // Set cursor
                        withContext(Dispatchers.Main) {
                            // Scroll to the end
                            it.input.setSelection(it.input.length())

                            // Hide the cursor (do not remove this, it's not a duplicate)
                            it.input.isCursorVisible = false

                            if (calculation == formattedResult) {
                                showNewResult("", calculator)
                            } else {
                                showNewResult("$calculation=$formattedResult", calculator)
                            }

                            // Avoid out of sync
                            calculator.isEqualLastAction = true
                        }

                        calculator.isEqualLastAction = true
                    } else {
                        withContext(Dispatchers.Main) {
                            when (calculator.errorType) {
                                ErrorType.SyntaxError -> {
                                    setErrorColor(true, calculator)
                                    showNewResult(getString(R.string.syntax_error), calculator)
                                }

                                ErrorType.DivisionBy0 -> {
                                    setErrorColor(true, calculator)
                                    showNewResult(getString(R.string.division_by_0), calculator)
                                }

                                else -> {
                                    showNewResult(formattedResult, calculator)
                                    // Do not clear the calculation (if you click into a number) if there is an error
                                    calculator.isEqualLastAction = true
                                }
                            }
                        }
                    }

                } else {
                    withContext(Dispatchers.Main) { showNewResult("", calculator) }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun addSymbol(currentSymbol: String, calculator: CalculatorImpl) {
        getUpdatingView(calculator)?.input?.let {
            // Get input text length
            val textLength = it.text.length

            // If the input is not empty
            if (textLength > 0) {
                // Get cursor's current position
                val cursorPosition = it.selectionStart

                // Get next / previous characters relative to the cursor
                val nextChar =
                    if (textLength - cursorPosition > 0) it.text[cursorPosition].toString() else "0" // use "0" as default like it's not a symbol
                val previousChar =
                    if (cursorPosition > 0) it.text[cursorPosition - 1].toString() else "0"

                if (currentSymbol != previousChar // Ignore multiple presses of the same button
                    && currentSymbol != nextChar
                    && previousChar != decimalSeparatorSymbol // Ensure that the previous character is not a comma
                    && nextChar != decimalSeparatorSymbol // Ensure that the next character is not a comma
                ) { // Minus symbol is an override
                    // If previous character is a symbol, replace it
                    if (previousChar.matches("[+\\-÷×^]".toRegex())) {

                        val leftString = it.text.subSequence(0, cursorPosition - 1).toString()
                        val rightString = it.text.subSequence(cursorPosition, textLength).toString()

                        // Add a parenthesis if there is another symbol before minus
                        if (currentSymbol == MINUS) {
                            if (previousChar in "+-") {
                                showNewInput(leftString + currentSymbol + rightString, calculator)
                                it.setSelection(cursorPosition)
                            } else {
                                showNewInput(leftString + previousChar + currentSymbol + rightString, calculator)
                                it.setSelection(cursorPosition + 1)
                            }
                        } else if (currentSymbol == PLUS) {
                            showNewInput(leftString + rightString, calculator)
                            it.setSelection(cursorPosition - 1)
                        }
                    } else if (nextChar.matches("[+\\-÷×^%!]".toRegex()) && currentSymbol != PERCENT) {
                        // If next character is a symbol, replace it
                        // Make sure that percent symbol doesn't replace succeeding symbols

                        val leftString = it.text.subSequence(0, cursorPosition).toString()
                        val rightString =
                            it.text.subSequence(cursorPosition + 1, textLength).toString()

                        if (cursorPosition > 0 && previousChar != "(") {
                            showNewInput(leftString + currentSymbol + rightString, calculator)
                            it.setSelection(cursorPosition + 1)
                        } else if (currentSymbol == PLUS) {
                            showNewInput(leftString + rightString, calculator)
                        }
                    } else if (cursorPosition > 0 || nextChar != "0" && currentSymbol == MINUS) {
                        // Otherwise just update the display
                        updateDisplay(currentSymbol, calculator)
                    }
                }
            } else { // Allow minus symbol, even if the input is empty
                if (currentSymbol == MINUS) updateDisplay(currentSymbol, calculator)
            }
        }
    }

    private fun clearButton(calculator: CalculatorImpl) {
        calculator.resetValues()
        showNewInput("", calculator)
        showNewResult("", calculator)
        actionAfterClick(calculator)
    }

    private fun backspaceButton() {
        getUpdatingView(getCalculator())?.input?.let {
            var cursorPosition = it.selectionStart
            val textLength = it.text.length
            var newValue = ""
            var functionLength = 0

            if (getCalculator().isEqualLastAction) {
                cursorPosition = textLength
            }

            if (cursorPosition != 0 && textLength != 0) {
                // remove the grouping separator
                val leftPart = it.text.subSequence(0, cursorPosition).toString()
                val leftPartWithoutSpaces = leftPart.replace(groupingSeparatorSymbol, "")
                functionLength = leftPart.length - leftPartWithoutSpaces.length

                newValue = leftPartWithoutSpaces.subSequence(0, leftPartWithoutSpaces.length - 1)
                    .toString() +
                        it.text.subSequence(cursorPosition, textLength).toString()

                val newValueFormatted = getCalculator().getNumberFormatted(newValue)
                var cursorOffset = newValueFormatted.length - newValue.length
                if (cursorOffset < 0) cursorOffset = 0

                showNewInput(newValueFormatted, getCalculator())
                it.setSelection((cursorPosition - 1 + cursorOffset - functionLength).takeIf { it > 0 }
                    ?: 0)
            }
        }
    }

    private fun showNewInput(value: String, calculator: CalculatorImpl) {
        getUpdatingView(calculator)?.let {
            it.input.setText(value)
            calculator.currentInput = value
        }
    }

    private fun showNewResult(value: String, calculator: CalculatorImpl) {
        getUpdatingView(calculator)?.let {
            it.tvResult.text = value
            calculator.currentResult = value
        }
    }

    private fun getUpdatingView(calculator: CalculatorImpl): ViewCalculatorBinding? {
        // Normal operations
        if (!isRestore) return if (isPortrait()) binding.calculator else (if (calculator.isLeft) binding.calculator else binding.calculatorRight)

        /**
         *  Restore cases :
         */
        return if (!isPortrait()) {
            // portrait -> landscape
            if (calculator.isLeft) {
                binding.calculator
            } else {
                binding.calculatorRight
            }
        } else {
            // landscape -> portrait
            if (calculator.isLeft && isCurrentCalculatorLeft || !calculator.isLeft && !isCurrentCalculatorLeft) {
                binding.calculator // calculator of portrait mode is always left update
            } else {
                null
            }
        }
    }
}
