package dev.openrune.gamevals

import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.filesystem.Cache
import java.io.File

object GamevalDumper {

    private val NAME_REMAP =
        mapOf(
            "objects" to "loc",
            "items" to "obj",
            "jingles" to "jingle",
            "spotanims" to "spotanim",
            "npcs" to "npc",
            "components" to "component",
            "interfaces" to "interface",
            "tables" to "dbtable",
            "dbrows" to "dbrow",
            "sequences" to "seq",
            "varbits" to "varbit",
        )

    fun dumpGamevals(cache: Cache, rev: Int) {
        val gamevals = mutableMapOf<String, List<String>>()

        GameValGroupTypes.entries.forEach { group ->
            // IFTYPES transparently resolves to the active interface-gameval archive
            // (legacy group 13 or V2 group 14). Do not process V2 separately or custom
            // interfaces packed into the active legacy archive will be omitted.
            if (group == GameValGroupTypes.IFTYPES_V2) return@forEach

            val elements = GameValHandler.readGameVal(group, cache, rev)

            when (group) {
                GameValGroupTypes.SPRITETYPES -> {
                    gamevals["sprites"] =
                        elements.mapNotNull { it.elementAs<Sprite>()?.formatSprite() }
                }

                GameValGroupTypes.IFTYPES -> {
                    val interfaces = elements.mapNotNull { it.elementAs<Interface>() }
                    gamevals["interfaces"] = interfaces.map { "${it.name}=${it.id}" }
                }

                GameValGroupTypes.IFTYPES_V2 -> Unit

                else -> {
                    val key = group.groupName.replace("dbtables", "tables")
                    gamevals[key] = elements.map { "${it.name}=${it.id}" }
                }
            }
        }

        val outputDir = File("../.data/gamevals-binary").apply { mkdirs() }
        GameValDat.write(File(outputDir, "gamevals.dat"), gamevals.remapNames())

        dumpCols(cache, rev)
        dumpComponents(cache, rev)
    }

    fun dumpComponents(
        cache: Cache,
        rev: Int,
        verifySourceContracts: Boolean = false,
    ) {
        // IFTYPES selects the actual interface-gameval archive for this cache revision.
        // Reading IFTYPES_V2 directly misses custom interfaces whenever the active archive
        // is the legacy IFTYPES group, which leaves component.* mappings unavailable at runtime.
        val elements = GameValHandler.readGameVal(GameValGroupTypes.IFTYPES, cache = cache, rev)
        val data = mutableListOf<String>()

        elements.forEach { gameValElement ->
            val iftype = gameValElement.elementAs<Interface>() ?: return@forEach
            iftype.components.forEach { comp ->
                data.add("${iftype.name}:${comp.name}=${comp.packed}")
            }
        }

        if (verifySourceContracts) {
            InterfaceGamevalContracts.verify(File("../content"), data)
        }
        GeneratedGameVals.replaceTables(mapOf("component" to data))
    }

    fun dumpCols(cache: Cache, rev: Int) {
        val elements = GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache = cache, rev)
        val data = mutableListOf<String>()

        elements.forEach { gameValElement ->
            val table = gameValElement.elementAs<Table>() ?: return@forEach
            table.columns.forEach { column ->
                data.add("${table.name}:${column.name}=${(gameValElement.id shl 16) or column.id}")
            }
        }

        GeneratedGameVals.replaceTables(mapOf("dbcol" to data))
    }

    private fun Map<String, List<String>>.remapNames(): Map<String, List<String>> =
        mapKeys { (name, _) -> NAME_REMAP[name] ?: name }

    private fun Sprite.formatSprite(): String = if (index == -1) "$name=$id" else "$name:$index=$id"
}
