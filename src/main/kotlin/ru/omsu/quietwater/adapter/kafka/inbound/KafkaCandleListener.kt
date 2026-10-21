package ru.omsu.quietwater.adapter.kafka.inbound

import org.apache.kafka.clients.consumer.CloseOptions
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.KafkaException
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import ru.omsu.quietwater.adapter.config.EnvConfig
import ru.omsu.quietwater.adapter.health.KafkaLink
import ru.omsu.quietwater.adapter.kafka.BrokerTemporarilyUnavailable
import ru.omsu.quietwater.adapter.kafka.KafkaClientProperties
import ru.omsu.quietwater.adapter.kafka.KafkaProducerHandle
import ru.omsu.quietwater.adapter.kafka.outbound.RequestRegistration
import ru.omsu.quietwater.adapter.logging.StructuredLog
import ru.omsu.quietwater.application.port.inbound.CandleListener
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class KafkaCandleListener(
    private val config: EnvConfig,
    private val link: KafkaLink,
    private val producerHandle: KafkaProducerHandle,
    private val requestRegistration: RequestRegistration,
    private val handler: IncomingEventHandler,
    private val properties: KafkaClientProperties,
    private val log: StructuredLog,
    private val sleeper: (Long) -> Unit = Thread::sleep
) : CandleListener {

    private val running = AtomicBoolean(false)

    @Volatile
    private var consumer: KafkaConsumer<String, String>? = null

    @Volatile
    private var worker: Thread? = null

    override fun start() {
        if (!config.isKafkaConfigured()) {
            link.markNotConfigured(config.kafkaUnconfiguredDetails)
            log.info("kafka.skipped", mapOf("reason" to config.kafkaUnconfiguredDetails))
            return
        }
        if (!running.compareAndSet(false, true)) {
            return
        }
        link.markUnavailable(config.kafkaUnavailableDetails)
        val thread = Thread(this::runLoop, THREAD_NAME)
        worker = thread
        thread.start()
    }

    override fun stop() {
        running.set(false)
        consumer?.wakeup()
        worker?.join(config.shutdownTimeoutMillis)
        worker = null
    }

    private fun runLoop() {
        var delay = config.kafkaReconnectInitialMillis
        while (running.get()) {
            var currentConsumer: KafkaConsumer<String, String>? = null
            var currentProducer: KafkaProducer<String, String>? = null
            try {
                currentConsumer = KafkaConsumer(properties.consumer())
                currentProducer = KafkaProducer(properties.producer())
                consumer = currentConsumer
                producerHandle.attach(currentProducer)
                currentConsumer.subscribe(listOf(config.inboundTopic()))
                requestRegistration.sendIfNeeded()
                delay = config.kafkaReconnectInitialMillis
                pollUntilStopped(currentConsumer)
            } catch (error: WakeupException) {
                if (running.get()) {
                    reportUnavailable(error)
                }
            } catch (error: Exception) {
                reportUnavailable(error)
                if (error is InterruptedException) {
                    Thread.currentThread().interrupt()
                    running.set(false)
                }
            } finally {
                producerHandle.detach()
                consumer = null
                closeQuietly(currentConsumer)
                closeQuietly(currentProducer)
            }
            if (running.get()) {
                sleeper(delay)
                delay = (delay * 2).coerceAtMost(config.kafkaReconnectMaxMillis)
            }
        }
    }

    private fun reportUnavailable(error: Exception) {
        link.markUnavailable(config.kafkaUnavailableDetails)
        log.warn(
            "broker.unavailable",
            mapOf("reason" to log.safeReason(error, config.kafkaPassword)),
            error
        )
    }

    private fun pollUntilStopped(currentConsumer: KafkaConsumer<String, String>) {
        while (running.get()) {
            val records = currentConsumer.poll(Duration.ofMillis(config.kafkaPollMillis))
            link.markUp(config.kafkaUpDetails)
            for (record in records) {
                try {
                    handler.handle(record.value())
                    commit(currentConsumer, record.topic(), record.partition(), record.offset())
                } catch (error: Exception) {
                    if (isRetryable(error) || !running.get()) {
                        throw error
                    }
                    log.warn(
                        "message.skipped",
                        mapOf(
                            "reason" to log.safeReason(error, config.kafkaPassword),
                            "topic" to record.topic(),
                            "offset" to record.offset().toString()
                        )
                    )
                    commit(currentConsumer, record.topic(), record.partition(), record.offset())
                }
            }
        }
    }

    private fun commit(
        currentConsumer: KafkaConsumer<String, String>,
        topic: String,
        partition: Int,
        offset: Long
    ) {
        currentConsumer.commitSync(
            mapOf(TopicPartition(topic, partition) to OffsetAndMetadata(offset + 1))
        )
    }

    private fun isRetryable(error: Throwable): Boolean {
        return error is BrokerTemporarilyUnavailable || error is KafkaException
    }

    private fun closeQuietly(closeable: AutoCloseable?) {
        try {
            when (closeable) {
                is KafkaConsumer<*, *> -> {
                    closeable.close(CloseOptions.timeout(Duration.ofMillis(config.kafkaCloseMillis)))
                }

                is KafkaProducer<*, *> -> {
                    closeable.close(Duration.ofMillis(config.kafkaCloseMillis))
                }

                else -> {
                    closeable?.close()
                }
            }
        } catch (error: Exception) {
            log.warn(
                "kafka.close",
                mapOf("reason" to log.safeReason(error, config.kafkaPassword))
            )
        }
    }

    companion object {
        const val THREAD_NAME: String = "quiet-water-kafka"
    }
}
