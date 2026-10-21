package ru.omsu.quietwater.application.port.outbound

interface OutboundSender {
    fun send(topic: String, key: String, value: String)
}
