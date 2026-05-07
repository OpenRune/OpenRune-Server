package org.rsmod.tools.mcp.wiki

import java.io.DataOutputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GamevalIndexTest {
    @Test
    fun `load parses gamevals and supports exact full key search`() {
        val root = Files.createTempDirectory("gameval-index-test")
        val binaryDir = root.resolve(".data").resolve("gamevals-binary")
        Files.createDirectories(binaryDir)

        writeMockDat(
            binaryDir.resolve("gamevals.dat"),
            mapOf(
                "npc" to listOf("kraken=1234", "cave_kraken=5678"),
                "obj" to listOf("kraken_tentacle=9012"),
            ),
        )
        writeMockDat(binaryDir.resolve("gamevals_columns.dat"), mapOf("dbcolumn" to emptyList()))

        val index = GamevalIndex.load(root.toString())
        val result = index.search(query = "npc.kraken", table = null, id = null, limit = 10)

        assertEquals(1, result.totalMatches)
        assertEquals("npc.kraken", result.matches.first().fullKey)
        assertEquals(1234, result.matches.first().id)
    }

    @Test
    fun `search supports ambiguous matches and limit truncation`() {
        val root = Files.createTempDirectory("gameval-index-test")
        val binaryDir = root.resolve(".data").resolve("gamevals-binary")
        Files.createDirectories(binaryDir)

        writeMockDat(
            binaryDir.resolve("gamevals.dat"),
            mapOf(
                "npc" to listOf("kraken=1234", "cave_kraken=5678", "kraken_boss=7777"),
            ),
        )
        writeMockDat(binaryDir.resolve("gamevals_columns.dat"), mapOf("dbcolumn" to emptyList()))

        val index = GamevalIndex.load(root.toString())
        val result = index.search(query = "kraken", table = "npc", id = null, limit = 2)

        assertEquals(3, result.totalMatches)
        assertEquals(2, result.matches.size)
        assertTrue(result.truncated)
    }

    // Test fixture helper only: writes mock binary files under a temp directory.
    private fun writeMockDat(path: java.nio.file.Path, tables: Map<String, List<String>>) {
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            output.writeInt(tables.size)
            for ((table, lines) in tables) {
                writeSizedUtf(output, table)
                output.writeInt(lines.size)
                for (line in lines) {
                    writeSizedUtf(output, line)
                }
            }
        }
    }

    private fun writeSizedUtf(output: DataOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        output.writeShort(bytes.size)
        output.write(bytes)
    }
}


