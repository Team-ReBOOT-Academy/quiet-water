package ru.omsu.quietwater.adapter.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.KafkaException
import ru.omsu.quietwater.application.port.outbound.OutboundSender
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class BrokerTemporarilyUnavailable(message: String) : RuntimeException(message)

class KafkaProducerHandle(
    private val sendTimeoutMillis: Long
) : OutboundSender {

    @Volatile
    private var producer: KafkaProducer<String, String>? = null

    fun attach(producer: KafkaProducer<String, String>) {
        this.producer = producer
    }

    fun detach() {
        producer = null
    }

    override fun send(topic: String, key: String, value: String) {
        val current = producer ?: throw BrokerTemporarilyUnavailable("kafka producer is not connected")
        try {
            current.send(ProducerRecord(topic, key, value)).get(sendTimeoutMillis, TimeUnit.MILLISECONDS)
        } catch (error: TimeoutException) {
            throw BrokerTemporarilyUnavailable(error.message ?: "kafka send timed out")
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw BrokerTemporarilyUnavailable("kafka send interrupted")
        } catch (error: ExecutionException) {
            val cause = error.cause
            if (cause is KafkaException) {
                throw cause
            }
            throw BrokerTemporarilyUnavailable(cause?.message ?: "kafka send failed")
        }
    }
}
