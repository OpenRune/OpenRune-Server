package org.rsmod.tools.wiki.dumping

import java.nio.file.Path
import kotlin.io.path.bufferedReader

data class CsvNpcSpawnRow(
    val ingameName: String,
    val npcId: Int,
    val x: Int,
    val z: Int,
    val plane: Int,
    val region: String?,
    val wikiVersion: String?,
)

object CsvNpcSpawnReader {
    fun read(path: Path): List<CsvNpcSpawnRow> {
        val rows = mutableListOf<CsvNpcSpawnRow>()
        path.bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->
                if (line.isBlank()) {
                    return@forEach
                }
                parseLine(line)?.let { rows += it }
            }
        }
        return rows
    }

    private fun parseLine(line: String): CsvNpcSpawnRow? {
        val parts = line.split(',')
        if (parts.size < 9) {
            return null
        }
        val ingameName = parts[0].trim()
        val npcId = parts[1].trim().toIntOrNull() ?: return null
        val x = parts[3].trim().toIntOrNull() ?: return null
        val z = parts[4].trim().toIntOrNull() ?: return null
        val plane = parts[5].trim().toIntOrNull() ?: return null
        val region = parts[6].trim().takeIf { it.isNotBlank() }
        val wikiVersion = parts[8].trim().takeIf { it.isNotBlank() }
        return CsvNpcSpawnRow(
            ingameName = ingameName,
            npcId = npcId,
            x = x,
            z = z,
            plane = plane,
            region = region,
            wikiVersion = wikiVersion,
        )
    }

    fun locationKey(x: Int, z: Int, plane: Int): String = "$x|$z|$plane"

    fun wikiPageTitle(wikiVersion: String): String = wikiVersion.substringBefore('#').trim()
}
