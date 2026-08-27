package ir.hadipoor.eviltower.ui

fun fa(value: Any): String = value.toString().map { char ->
    when (char) { '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'; '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'; else -> char }
}.joinToString("")
