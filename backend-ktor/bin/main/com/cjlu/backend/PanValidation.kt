package com.cjlu.backend

object PanValidation {
    private val panRegex = Regex("\\d{13,19}")

    fun containsLikelyPan(text: String): Boolean {
        val compact = text.replace(" ", "").replace("-", "").replace("\n", "")
        return panRegex.containsMatchIn(compact)
    }
}
