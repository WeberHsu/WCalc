package com.weberhsu.wcalculator.helpers

class Expression {

    fun getCleanExpression(calculation: String, decimalSeparatorSymbol: String, groupingSeparatorSymbol: String): String {
        var cleanCalculation = replaceSymbolsFromCalculation(calculation, decimalSeparatorSymbol, groupingSeparatorSymbol)
        cleanCalculation = addMultiply(cleanCalculation)
        if (cleanCalculation.contains('%')) {
            cleanCalculation = getPercentString(cleanCalculation)
            cleanCalculation = cleanCalculation.replace("%", "/100")
        }

        cleanCalculation = addParenthesis(cleanCalculation)

        return cleanCalculation
    }

    private fun replaceSymbolsFromCalculation(
        calculation: String,
        decimalSeparatorSymbol: String,
        groupingSeparatorSymbol: String
    ): String {
        var calculation2 = calculation.replace('×', '*')
        calculation2 = calculation2.replace('÷', '/')
        calculation2 = calculation2.replace("E", "*10^")
        calculation2 = calculation2.replace(groupingSeparatorSymbol, "")
        calculation2 = calculation2.replace(decimalSeparatorSymbol, ".")
        return calculation2
    }

    /**
     * Transform any calculation string containing %
     * to respect the user friend (non-mathematical) way (see issue #35)
     */
    private fun getPercentString(calculation: String): String {
        val percentPos = calculation.lastIndexOf('%')
        if (percentPos < 1) {
            return calculation
        }
        // find the last operator before the last %
        val operatorBeforePercentPos =
            calculation.subSequence(0, percentPos - 1).lastIndexOfAny(charArrayOf('-', '+', '*', '/', '('))

        if (operatorBeforePercentPos < 1) {
            return calculation
        }

        if (calculation[operatorBeforePercentPos] == '*') {
            return calculation
        }

        if (calculation[operatorBeforePercentPos] == '/') {
            // insert brackets into percentage. Fixes 900/10% -> 900/(10/100), not 900/10/100 which evals differently.
            // also prevents it from doing the rest of this function, which screws the calculation up
            val stringFirst = calculation.substring(0, operatorBeforePercentPos + 1)
            val stringMiddle = calculation.substring(operatorBeforePercentPos + 1, percentPos + 1)
            val stringLast = calculation.substring(percentPos + 1, calculation.length)
            return "$stringFirst($stringMiddle)$stringLast"
        }

        // extract the first part of the calculation
        var calculationStringFirst = calculation.subSequence(0, operatorBeforePercentPos).toString()

        // recursively parse it
        if (calculationStringFirst.contains('%')) {
            calculationStringFirst = getPercentString(calculationStringFirst)
        }
        // check if there are already some parenthesis, so we skip formatting
        if (calculation[operatorBeforePercentPos] == '(') {
            return calculationStringFirst + calculation.subSequence(operatorBeforePercentPos, calculation.length)
        }
        calculationStringFirst = "($calculationStringFirst)"

        // modify the calculation to have something like (X)+(Y%*X)
        return calculationStringFirst + calculation[operatorBeforePercentPos] + calculationStringFirst + "*(" + calculation.subSequence(
            operatorBeforePercentPos + 1,
            percentPos
        ) + ")" + calculation.subSequence(percentPos, calculation.length)

    }

    private fun addParenthesis(calculation: String): String {
        // Add ")" which lack
        var cleanCalculation = calculation
        var openParentheses = 0
        var closeParentheses = 0

        for (i in calculation.indices) {
            if (calculation[i] == '(') {
                openParentheses += 1
            }
            if (calculation[i] == ')') {
                closeParentheses += 1
            }
        }
        if (closeParentheses < openParentheses) {
            for (i in 0 until openParentheses - closeParentheses) {
                cleanCalculation += ')'
            }
        }

        return cleanCalculation
    }

    private fun addMultiply(calculation: String): String {
        // Add "*" which lack
        var cleanCalculation = calculation
        var cleanCalculationLength = cleanCalculation.length
        var i = 0
        while (i < cleanCalculationLength) {

            if (cleanCalculation[i] == '(') {
                if (i != 0 && (cleanCalculation[i - 1] in ".e0123456789)")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i)
                    cleanCalculationLength++
                }
            } else if (cleanCalculation[i] == ')') {
                if (i + 1 < cleanCalculation.length && (cleanCalculation[i + 1] in "0123456789(")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                    cleanCalculationLength++
                }
            } else if (cleanCalculation[i] == '!') {
                if (i + 1 < cleanCalculation.length && (cleanCalculation[i + 1] in "0123456789π(")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                    cleanCalculationLength++
                }
            } else if (cleanCalculation[i] == '%') {
                if (i + 1 < cleanCalculation.length && (cleanCalculation[i + 1] in "0123456789π(")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                    cleanCalculationLength++
                }
            } else if (cleanCalculation[i] == 'e') {
                if (i + 1 < cleanCalculation.length && (cleanCalculation[i + 1] in "π0123456789(")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i + 1)
                    cleanCalculationLength++
                }
                if (i - 1 >= 0 && (cleanCalculation[i - 1] in ".%πe0123456789)")) {
                    cleanCalculation = cleanCalculation.addCharAtIndex('*', i)
                    cleanCalculationLength++
                }
            }
            i++
        }
        return cleanCalculation
    }

    private fun String.addCharAtIndex(char: Char, index: Int) =
        StringBuilder(this).apply { insert(index, char) }.toString()

}
