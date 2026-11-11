package ru.omsu.quietwater.adapter.config

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalEnvFileTest {

    @Test
    fun readsAssignmentsAndSkipsComments() {
        val path = Files.createTempFile("quiet-water-env", ".env")
        Files.writeString(
            path,
            """
            # комментарий
            KAFKA_BOOTSTRAP_SERVERS=broker:9094
            SECRET_KEY="student-key"
            EMPTY=
            broken
            """.trimIndent()
        )

        val values = LocalEnvFile(path).read()

        assertEquals("broker:9094", values["KAFKA_BOOTSTRAP_SERVERS"])
        assertEquals("student-key", values["SECRET_KEY"])
        assertEquals("", values["EMPTY"])
        assertTrue(values["broken"] == null)
        Files.deleteIfExists(path)
    }

    @Test
    fun missingFileIsEmpty() {
        val path = Files.createTempDirectory("quiet-water-env").resolve(".env")
        assertTrue(LocalEnvFile(path).read().isEmpty())
    }
}
