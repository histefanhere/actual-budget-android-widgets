package com.histefanhere.actualwidgets.model

enum class WidgetSize { SMALL, MEDIUM, LARGE, MASSIVE }

data class TextSizes(
    val headerSp: Float,
    val labelSp: Float,
    val valueSp: Float,
    val footerSp: Float,
    val paddingDp: Float,
)

val WidgetSize.textSizes: TextSizes get() = when (this) {
    WidgetSize.SMALL  -> TextSizes(headerSp = 10f, labelSp =  8f, valueSp = 10f, footerSp = 7f,  paddingDp = 6f)
    WidgetSize.MEDIUM -> TextSizes(headerSp = 13f, labelSp =  9f, valueSp = 12f, footerSp = 9f,  paddingDp = 8f)
    WidgetSize.LARGE   -> TextSizes(headerSp = 19f, labelSp = 13f, valueSp = 18f, footerSp = 11f, paddingDp = 12f)
    WidgetSize.MASSIVE -> TextSizes(headerSp = 20f, labelSp = 18f, valueSp = 28f, footerSp = 13f, paddingDp = 16f)
}
