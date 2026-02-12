package gg.rsmod.util

import java.io.File

fun main() {
    // Helper function to load mappings from a file
    fun loadMap(path: String, sep: String = ":"): Map<String, Int> {
        return File(path).readLines()
            .map { it.split(sep) }
            .associate { it[0] to it[1].toInt() }
    }

    // Load old and new maps for items
    val oldItemMap = loadMap("C:\\Users\\Home\\Desktop\\Alter-feature-support-231\\Alter-feature-support-231\\data\\cfg\\rscm\\item.rscm")
    val newItemMap = File("C:\\Users\\Home\\Desktop\\Alter-feature-support-231\\Alter-feature-support-231\\data\\cfg\\rscm2\\items.rscm")
        .readLines()
        .map { it.split("=") }
        .associate { it[1].toInt() to it[0] }

    val itemLookup = oldItemMap.mapNotNull { (oldName, id) ->
        val newName = newItemMap[id]
        if (newName != null) oldName to newName else null
    }.toMap()

    // Load old and new maps for NPCs
    val oldNpcMap = loadMap("C:\\Users\\Home\\Desktop\\Alter-feature-support-231\\Alter-feature-support-231\\data\\cfg\\rscm\\npc.rscm")
    val newNpcMap = File("C:\\Users\\Home\\Desktop\\Alter-feature-support-231\\Alter-feature-support-231\\data\\cfg\\rscm2\\npcs.rscm")
        .readLines()
        .map { it.split("=") }
        .associate { it[1].toInt() to it[0] }

    val npcLookup = oldNpcMap.mapNotNull { (oldName, id) ->
        val newName = newNpcMap[id]
        if (newName != null) oldName to newName else null
    }.toMap()

    // Load old and new maps for objects
    val oldObjMap = loadMap("C:\\Users\\Home\\Desktop\\Alter-feature-support-231\\Alter-feature-support-231\\data\\cfg\\rscm\\object.rscm")
    val newObjMap = File("C:\\Users\\Home\\Desktop\\Alter-feature-support-231\\Alter-feature-support-231\\data\\cfg\\rscm2\\objects.rscm")
        .readLines()
        .map { it.split("=") }
        .associate { it[1].toInt() to it[0] }

    val objLookup = oldObjMap.mapNotNull { (oldName, id) ->
        val newName = newObjMap[id]
        if (newName != null) oldName to newName else null
    }.toMap()

    val rootDir = File("C:\\Users\\Home\\Desktop\\Alter-feature-support-231\\Alter-feature-support-231")

    rootDir.walkTopDown()
        .filter { it.isFile && (it.extension == "kt" || it.extension == "kts") }
        .forEach { file ->
            var text = file.readText()
            var replaced = false

            // Regex to match string literals like "item.*", "npc.*", "object.*"
            val regex = """"(item|npc|object)\.([^"]+)"""".toRegex()

            text = regex.replace(text) { matchResult ->
                val prefix = matchResult.groupValues[1]
                val oldStr = matchResult.groupValues[2]
                val newStr = when (prefix) {
                    "item" -> itemLookup[oldStr]
                    "npc" -> npcLookup[oldStr]
                    "object" -> objLookup[oldStr]
                    else -> null
                }

                if (newStr != null) {
                    replaced = true
                    "\"$prefix.$newStr\""
                } else {
                    println("Unknown mapping in ${file.path}: \"${prefix}.${oldStr}\"")
                    matchResult.value // keep original
                }
            }

            if (replaced) {
                println("Updated file: ${file.path}")
                file.writeText(text)
            }
        }
}
