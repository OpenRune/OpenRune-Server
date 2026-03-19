package dev.openrune.gamevals

import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.filesystem.Cache
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

object GamevalDumper {

    private val NAME_REMAP = mapOf(
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
        "varbits" to "varbit"
    )

    fun dumpGamevals(cache: Cache, rev: Int) {
        val gamevals = mutableMapOf<String, List<String>>()

        GameValGroupTypes.entries.forEach { group ->
            val elements = GameValHandler.readGameVal(group, cache, rev)

            when (group) {
                GameValGroupTypes.SPRITETYPES -> {
                    gamevals["sprites"] = elements.mapNotNull { it.elementAs<Sprite>()?.formatSprite() }
                }

                GameValGroupTypes.IFTYPES_V2 -> {
                    val interfaces = elements.mapNotNull { it.elementAs<Interface>() }

                    gamevals["interfaces"] =
                        interfaces.map { "${it.name}=${it.id}" }

                    gamevals["components"] =
                        interfaces.flatMap { iface ->
                            iface.components.map { comp ->
                                "${iface.name}:${comp.name}=${comp.packed}"
                            }
                        }
                }

                GameValGroupTypes.IFTYPES -> Unit

                else -> {
                    val key = group.groupName.replace("dbtables", "tables")
                    gamevals[key] = elements.map { "${it.name}=${it.id}" }
                }
            }
        }

        val outputDir = File("../.data/gamevals-binary").apply { mkdirs() }
        encodeGameValDat(File(outputDir, "gamevals.dat").path, gamevals)

        dumpCols(cache, rev)
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

        encodeGameValDat("../.data/gamevals-binary/gamevals_columns.dat", mapOf("dbcol" to data))
    }


    private fun encodeGameValDat(output: String, tables: Map<String, List<String>>) {
        DataOutputStream(FileOutputStream(output)).use { out ->
            out.writeInt(tables.size)

            tables.forEach { (rawName, items) ->
                val name = NAME_REMAP[rawName] ?: rawName
                val nameBytes = name.toByteArray(Charsets.UTF_8)

                out.writeShort(nameBytes.size)
                out.write(nameBytes)

                out.writeInt(items.size)

                items.forEach { entry ->
                    val bytes = entry.toByteArray(Charsets.UTF_8)
                    out.writeShort(bytes.size)
                    out.write(bytes)
                }
            }
        }
    }

    private fun Sprite.formatSprite(): String =
        if (index == -1) "$name=$id"
        else "$name:$index=$id"
}
