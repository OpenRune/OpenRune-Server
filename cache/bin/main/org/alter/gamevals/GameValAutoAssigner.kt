package org.alter.gamevals

import org.alter.rscm.RSCMType
import java.io.File

/**
 * Scans TOML and RSCM files for gamevals = -1, assigns next available IDs (preferring gaps near
 * existing IDs in the same file), and rewrites the files.
 */
class GameValAutoAssigner(
    private val mappings: Map<String, MutableMap<String, Int>>,
    private val maxBaseID: Map<String, Int>,
) {
    fun run(contentDir: File?, gamevalsDir: File?) {
        val scanResult = scanForUnassigned(contentDir, gamevalsDir)
        if (scanResult.unassigned.isEmpty()) return

        val replacements = assignIds(scanResult)
        rewriteFiles(replacements)
    }

    private data class ScanResult(
        val usedIds: Map<String, MutableSet<Int>>,
        val idsInSameFile: Map<Pair<File, String>, MutableSet<Int>>,
        val unassigned: List<UnassignedEntry>,
    )

    private data class UnassignedEntry(
        val file: File,
        val lineIndex: Int,
        val line: String,
        val table: String,
        val key: String,
    )

    private fun scanForUnassigned(contentDir: File?, gamevalsDir: File?): ScanResult {
        val usedIds = mutableMapOf<String, MutableSet<Int>>()
        val idsInSameFile = mutableMapOf<Pair<File, String>, MutableSet<Int>>()
        val unassigned = mutableListOf<UnassignedEntry>()

        fun recordEntry(file: File, lineIndex: Int, line: String, table: String, key: String, value: Int) {
            val tableUsed = usedIds.getOrPut(table) {
                mutableSetOf<Int>().also { mappings[table]?.values?.forEach(it::add) }
            }
            if (value == -1) {
                idsInSameFile.getOrPut(file to table) { mutableSetOf() }
                unassigned.add(UnassignedEntry(file, lineIndex, line, table, key))
            } else {
                tableUsed.add(value)
                idsInSameFile.getOrPut(file to table) { mutableSetOf() }.add(value)
            }
        }

        contentDir?.walk()?.filter { it.isFile && it.name == "gamevals.toml" }?.forEach { file ->
            var currentTable: String? = null
            file.readLines().forEachIndexed { index, line ->
                GAMEVALS_SECTION_REGEX.find(line.trim())?.let {
                    currentTable = it.groupValues[1].takeIf { t -> t in RSCMType.RSCM_PREFIXES }
                }
                if (currentTable != null && line.contains("=")) {
                    parseKeyValue(line)?.let { (key, value) ->
                        recordEntry(file, index, line, currentTable!!, key, value)
                    }
                }
            }
        }

        gamevalsDir?.walk()?.filter { it.isFile }?.forEach { file ->
            val table = file.nameWithoutExtension.takeIf { it in RSCMType.RSCM_PREFIXES } ?: return@forEach
            file.readLines().forEachIndexed { index, line ->
                if (line.isNotBlank()) {
                    parseKeyValue(line)?.let { (key, value) ->
                        recordEntry(file, index, line, table, key, value)
                    }
                }
            }
        }

        return ScanResult(usedIds, idsInSameFile, unassigned)
    }

    private fun parseKeyValue(line: String): Pair<String, Int>? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) return null
        return when {
            KEY_VALUE_REGEX.matches(trimmed) -> {
                val m = KEY_VALUE_REGEX.matchEntire(trimmed)!!
                m.groupValues[1].trim() to m.groupValues[2].toInt()
            }
            KEY_SUBPROP_REGEX.matches(trimmed) -> {
                val m = KEY_SUBPROP_REGEX.matchEntire(trimmed)!!
                m.groupValues[1].trim() to m.groupValues[3].toInt()
            }
            else -> null
        }
    }

    private fun assignIds(scanResult: ScanResult): Map<File, List<Pair<Int, String>>> {
        val (usedIds, idsInSameFile, unassigned) = scanResult
        val unassignedCount = unassigned.groupingBy { it.file to it.table }.eachCount()
        val replacements = mutableMapOf<File, MutableList<Pair<Int, String>>>()

        for (entry in unassigned.sortedWith(compareBy({ it.file.absolutePath }, { it.lineIndex }))) {
            val tableUsed = usedIds[entry.table]!!
            val floor = maxOf(MIN_ID, (maxBaseID[entry.table] ?: -1) + 1)
            val sameFileIds = idsInSameFile[entry.file to entry.table].orEmpty().filter { it >= floor }

            val id = when {
                sameFileIds.isNotEmpty() -> {
                    val (minInFile, maxInFile) = sameFileIds.minOrNull()!! to sameFileIds.maxOrNull()!!
                    val count = unassignedCount[entry.file to entry.table] ?: 1
                    val gapLo = maxOf(floor, minInFile - count)
                    (gapLo..minInFile - 1).firstOrNull { it !in tableUsed }
                        ?: (maxInFile + 1..Int.MAX_VALUE).firstOrNull { it !in tableUsed }
                        ?: (floor..Int.MAX_VALUE).first { it !in tableUsed }
                }
                else -> (floor..Int.MAX_VALUE).first { it !in tableUsed }
            }
            tableUsed.add(id)
            replacements.getOrPut(entry.file) { mutableListOf() }
                .add(entry.lineIndex to entry.line.replaceFirst(NEGATIVE_ONE_REGEX, "= $id"))
        }
        return replacements
    }

    private fun rewriteFiles(replacements: Map<File, List<Pair<Int, String>>>) {
        for ((file, lineReplacements) in replacements) {
            val lines = file.readLines().toMutableList()
            lineReplacements.forEach { (lineIndex, newLine) ->
                if (lineIndex < lines.size) lines[lineIndex] = newLine
            }
            file.writeText(lines.joinToString("\n"))
        }
    }

    private companion object {
        const val MIN_ID = 65536  // 64k floor
        val KEY_VALUE_REGEX = Regex("^([^=]+)=\\s*(-?\\d+)\\s*$")
        val KEY_SUBPROP_REGEX = Regex("^([^:]+):([^=]+)=\\s*(-?\\d+)\\s*$")
        val GAMEVALS_SECTION_REGEX = Regex("^\\s*\\[gamevals\\.([^.\\]]+)\\]\\s*$")
        val NEGATIVE_ONE_REGEX = Regex("=\\s*-1\\s*$")
    }
}
