package com.weberhsu.wcalculator.helpers

import com.weberhsu.wcalculator.*
import org.json.*
import java.math.*
import java.util.*

/**
 * author : weber
 * e-mail : weber0207@gmail.com
 * time : 2023/09/19 5:28 PM * version: 1.0
 * desc :
 */

class CalculatorImpl(
    val isLeft: Boolean,
    calculatorState: String = ""
) {
    var errorType: ErrorType = ErrorType.None
    var errorStatusOld = false

    var currentInput = ""
    var currentResult = ""
    var isEqualLastAction = false
    var calculationResult: BigDecimal = BigDecimal.ZERO
    private var stateInstance = calculatorState

    init {
        if (stateInstance != "") {
            setFromSaveInstanceState(stateInstance)
        }
    }

    fun getCalculatorStateJson(): JSONObject {
        val jsonObj = JSONObject()
        jsonObj.put(RESULT, currentResult)
        jsonObj.put(CURRENT_INPUT, currentInput)
        jsonObj.put(IS_EQUAL_LAST, isEqualLastAction)
        jsonObj.put(CALCULATION_RESULT, calculationResult)
        return jsonObj
    }

    fun setFromSaveInstanceState(json: String) {
        val jsonObject = JSONTokener(json).nextValue() as JSONObject
        currentResult = jsonObject.getString(RESULT)
        currentInput = jsonObject.getString(CURRENT_INPUT)
        isEqualLastAction = jsonObject.getBoolean(IS_EQUAL_LAST)
        calculationResult = jsonObject.getString(CALCULATION_RESULT).toBigDecimal()
    }

    fun resetValues() {
        resetErrorTypes()
        currentResult = ""
        currentInput = ""
        errorStatusOld = false
        isEqualLastAction = false
        calculationResult = BigDecimal.ZERO
    }

    fun resetErrorTypes() {
        errorType = ErrorType.None
    }

    /**
     * Remove zeros at the end of the results (after point)
     */
    fun getResultWithZeroHandled(value: String): String? {
        val resultSplited = value.split('.')
        if (resultSplited.size > 1) {
            val resultPartAfterDecimalSeparator = resultSplited[1].trimEnd('0')
            var resultWithoutZeros = resultSplited[0]
            if (resultPartAfterDecimalSeparator != "") {
                resultWithoutZeros =
                    resultSplited[0] + "." + resultPartAfterDecimalSeparator
            }
            return getNumberFormatted(resultWithoutZeros.replace(".", decimalSeparatorSymbol))
        }

        return null
    }

    fun getNumberFormatted(value: String): String {
        return NumberFormatter.format(
            value,
            decimalSeparatorSymbol,
            groupingSeparatorSymbol
        )
    }

    fun roundResult() {
        var newResult = calculationResult.setScale(NUMBER_PRECISION, RoundingMode.HALF_EVEN)
        if (SCIENTISTIC_MODE_OPEN && (newResult >= BigDecimal(THRESHOLD_SCIENTIFIC_NOTATION) || newResult <= BigDecimal(
                0.1
            ))
        ) {
            val scientificString = String.format(Locale.US, "%.4g", calculationResult)
            newResult = BigDecimal(scientificString)
        }
        val tempResult = newResult.toString().replace("E-", "").replace("E", "")
        val allCharsEqualToZero = tempResult.all { it == '0' }

        calculationResult = if (allCharsEqualToZero || newResult.toString().startsWith("0E")) {
            BigDecimal.ZERO
        } else {
            newResult
        }
    }

    // If result is a number and it is finite
    fun isResultValid(): Boolean = (errorType == ErrorType.None)

    fun getResultAfterPositiveOrNegative(): String? {
        if (currentInput.isBlank()) return null

        val result = if (!currentInput.trimStart('-')
                .any { it.toString() in listOf(PLUS, MINUS, MULTIPLE, DIVIDE, PERCENT) } && currentInput.replace(
                groupingSeparatorSymbol,
                ""
            ).toDouble() != 0.0
        ) {
            if (currentInput.first() == '-') {
                currentInput.substring(1)
            } else {
                "-${currentInput}"
            }
        } else {
            null
        }
        result?.let {
            currentInput = it
            return it
        }

        return null
    }

    fun evaluate(equation: String) {
        println("Equation BigDecimal : $equation")
        calculationResult = object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() {
                ch = if (++pos < equation.length) equation[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): BigDecimal {
                nextChar()
                val x = parseExpression()
                if (pos < equation.length) println("Unexpected: " + ch.toChar() + "Expression: " + equation)
                return x
            }

            fun parseExpression(): BigDecimal {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x = x.add(parseTerm()) // addition
                    else if (eat('-'.code)) x = x.subtract(parseTerm()) // subtraction
                    else return x
                }
            }

            fun parseTerm(): BigDecimal {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x = x.multiply(parseFactor()) // Multiplication
                    else if (eat('#'.code)) { // Modulo
                        val fractionDenominator = parseFactor()
                        if (fractionDenominator == BigDecimal.ZERO) {
                            errorType = ErrorType.DivisionBy0
                            x = BigDecimal.ZERO
                        } else {
                            x = x.rem(fractionDenominator)
                        }
                    } else if (eat('/'.code)) { // Division
                        val fractionDenominator = parseFactor()
                        if (fractionDenominator.toFloat() == 0f) {
                            errorType = ErrorType.DivisionBy0
                            x = BigDecimal.ZERO
                        } else {
                            try {
                                x = x.divide(fractionDenominator)
                            } catch (e: ArithmeticException) { // if the result is a non-terminating decimal expansion
                                x = x.divide(fractionDenominator, NUMBER_PRECISION, RoundingMode.HALF_DOWN)
                                println(x)
                            }
                        }
                    } // division
                    else return x
                }
            }

            fun parseFactor(): BigDecimal {
                if (eat('+'.code)) return parseFactor().plus() // unary plus
                if (eat('-'.code)) return parseFactor().unaryMinus() // unary minus
                var x: BigDecimal
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    if (!eat(')'.code)) {
                        println("Missing ')'")
                        x = BigDecimal.ZERO
                        errorType = ErrorType.SyntaxError
                    }
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    val string = equation.substring(startPos, pos)
                    if (string.count { it == '.' } > 1) {
                        x = BigDecimal.ZERO
                        errorType = ErrorType.SyntaxError
                    } else {
                        if ((string.length == 1) && (string[0] == '.')) {
                            x = BigDecimal.ZERO
                            errorType = ErrorType.SyntaxError
                        } else {
                            x = BigDecimal(string)
                        }
                    }
                } else if (ch >= 'a'.code && ch <= 'z'.code) { // functions
                    while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                    if (eat('('.code)) {
                        x = parseExpression()
                        if (!eat(')'.code)) x = parseFactor()
                    } else {
                        x = parseFactor()
                    }
                    println(x)
                } else {
                    x = BigDecimal.ZERO
                    errorType = ErrorType.SyntaxError
                }
                return x
            }
        }.parse()
    }
}