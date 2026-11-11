package ru.omsu.quietwater.adapter.config

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class LocalEnvFile(private val path: Path) {

    fun read(): Map<String, String> {
        if (!Files.isRegularFile(path)) {
            return emptyMap()
        }
        val values = linkedMapOf<String, String>()
        Files.readAllLines(path, StandardCharsets.UTF_8).forEach { line ->
            val entry = parse(line) ?: return@forEach
            values[entry.first] = entry.second
        }
        return values
    }

    private fun parse(line: String): Pair<String, String>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null
        }
        val separator = trimmed.indexOf('=')
        if (separator <= 0) {
            return null
        }
        val key = trimmed.substring(0, separator).trim()
        if (key.isEmpty()) {
            return null
        }
        return key to unquote(trimmed.substring(separator + 1).trim())
    }

    private fun unquote(value: String): String {
        if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length - 1)
        }
        return value
    }
}
