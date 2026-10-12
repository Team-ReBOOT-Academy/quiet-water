package ru.omsu.quietwater.domain.candle

data class Instrument(val code: String) {
    init {
        require(code.isNotBlank()) { "Код инструмента не может быть пустым" }
    }

    val key: String = code.trim().uppercase()
}
