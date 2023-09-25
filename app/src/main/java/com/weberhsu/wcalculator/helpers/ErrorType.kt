package com.weberhsu.wcalculator.helpers

/**
 * author : weber
 * e-mail : weber0207@gmail.com
 * time : 2023/09/25 11:19 AM * version: 1.0
 * desc :
 */
sealed class ErrorType {
    object None : ErrorType()
    object DivisionBy0 : ErrorType()
    object SyntaxError : ErrorType()
}