package com.weberhsu.wcalculator

/**
 * author : weber
 * e-mail : weber0207@gmail.com
 * time : 2023/09/19 5:23 PM * version: 1.0
 * desc :
 */

const val decimalSeparatorSymbol = "."
const val groupingSeparatorSymbol = ","

const val NUMBER_PRECISION = 10

// TODO: There are few issues need to fix, disable scientistic mode first
const val SCIENTISTIC_MODE_OPEN = false
const val THRESHOLD_SCIENTIFIC_NOTATION = 999999999999
const val THRESHOLD_MIN_SCIENTIFIC_NOTATION = -9999999999

const val MINUS = "-"
const val MULTIPLE = "×"
const val PLUS = "+"
const val DIVIDE = "÷"
const val PERCENT = "%"

const val RESULT = "result"
const val CURRENT_INPUT = "currentInput"
const val IS_EQUAL_LAST = "isEqualLast"
const val CALCULATION_RESULT = "calculationResult"
const val CALCULATOR_IS_LEFT = "calculatorIsLeft"
const val CALCULATOR_STATE = "calculatorState"
const val CALCULATOR_STATE_RIGHT = "calculatorStateRight"