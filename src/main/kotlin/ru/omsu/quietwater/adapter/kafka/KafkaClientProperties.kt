package ru.omsu.quietwater.adapter.kafka

import ru.omsu.quietwater.adapter.config.EnvConfig
import java.util.Properties

class KafkaClientProperties(private val config: EnvConfig) {

    fun consumer(): Properties = base().apply {
        this[GROUP_ID] = config.kafkaGroupId
        this[CLIENT_ID] = "${config.kafkaGroupId}$CONSUMER_SUFFIX"
        this[KEY_DESERIALIZER] = STRING_DESERIALIZER
        this[VALUE_DESERIALIZER] = STRING_DESERIALIZER
        this[AUTO_OFFSET_RESET] = config.kafkaAutoOffsetReset
        this[ENABLE_AUTO_COMMIT] = "false"
    }

    fun producer(): Properties = base().apply {
        this[CLIENT_ID] = "${config.kafkaGroupId}$PRODUCER_SUFFIX"
        this[KEY_SERIALIZER] = STRING_SERIALIZER
        this[VALUE_SERIALIZER] = STRING_SERIALIZER
        this[ACKS] = "all"
    }

    private fun base(): Properties {
        val properties = Properties()
        properties[BOOTSTRAP_SERVERS] = config.kafkaBootstrapServers
        properties[SECURITY_PROTOCOL] = config.kafkaSecurityProtocol
        if (!config.kafkaSecurityProtocol.equals(EnvConfig.PLAINTEXT, ignoreCase = true)) {
            properties[SASL_MECHANISM] = config.kafkaSaslMechanism
            properties[SASL_JAAS_CONFIG] = jaas(
                username = config.kafkaUsername.orEmpty(),
                password = config.kafkaPassword.orEmpty()
            )
        }
        return properties
    }

    private fun jaas(username: String, password: String): String {
        return "org.apache.kafka.common.security.scram.ScramLoginModule required " +
            "username=\"${escape(username)}\" password=\"${escape(password)}\";"
    }

    private fun escape(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    companion object {
        const val BOOTSTRAP_SERVERS: String = "bootstrap.servers"
        const val GROUP_ID: String = "group.id"
        const val CLIENT_ID: String = "client.id"
        const val SECURITY_PROTOCOL: String = "security.protocol"
        const val SASL_MECHANISM: String = "sasl.mechanism"
        const val SASL_JAAS_CONFIG: String = "sasl.jaas.config"
        const val AUTO_OFFSET_RESET: String = "auto.offset.reset"
        const val ENABLE_AUTO_COMMIT: String = "enable.auto.commit"
        const val KEY_DESERIALIZER: String = "key.deserializer"
        const val VALUE_DESERIALIZER: String = "value.deserializer"
        const val KEY_SERIALIZER: String = "key.serializer"
        const val VALUE_SERIALIZER: String = "value.serializer"
        const val ACKS: String = "acks"
        const val STRING_DESERIALIZER: String = "org.apache.kafka.common.serialization.StringDeserializer"
        const val STRING_SERIALIZER: String = "org.apache.kafka.common.serialization.StringSerializer"
        const val CONSUMER_SUFFIX: String = "-consumer"
        const val PRODUCER_SUFFIX: String = "-producer"
    }
}
