package org.alter.gamevals

import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.gameval.GameValHandler.elementAs
import dev.openrune.cache.gameval.impl.Interface
import dev.openrune.cache.gameval.impl.Sprite
import dev.openrune.cache.gameval.impl.Table
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.filesystem.Cache
import java.io.DataOutputStream
import java.io.FileOutputStream

object GamevalDumper {

    fun dumpGamevals(cache: Cache, rev: Int) {
        val gamevals = mutableMapOf<String, List<String>>()

        GameValGroupTypes.entries.forEach { groupType ->
            val elements = GameValHandler.readGameVal(groupType, cache = cache, rev)

            when (groupType) {
                GameValGroupTypes.SPRITETYPES -> {
                    val sprites = elements.mapNotNull { it.elementAs<Sprite>()?.let { e ->
                        if (e.index == -1) "${e.name}=${e.id}" else "${e.name}:${e.index}=${e.id}"
                    } }
                    gamevals["sprites"] = sprites
                }

                GameValGroupTypes.IFTYPES_V2 -> {
                    val interfaces = elements.mapNotNull { it.elementAs<Interface>()?.let { iface -> "${iface.name}=${iface.id}" } }

                    val components = elements.flatMap { elem ->
                        elem.elementAs<Interface>()?.components?.map { comp ->
                            "${elem.elementAs<Interface>()?.name}:${comp.name}=${comp.packed}"
                        } ?: emptyList()
                    }

                    gamevals["interfaces"] = interfaces
                    gamevals["components"] = components
                }

                GameValGroupTypes.IFTYPES -> Unit

                else -> {
                    val key = groupType.groupName.replace("dbtables", "tables")
                    gamevals[key] = elements.map { "${it.name}=${it.id}" }
                }
            }
        }

        encodeGameValDat("../data/cfg/gamevals-binary/gamevals.dat", gamevals)
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

        encodeGameValDat("../data/cfg/gamevals-binary/gamevals_columns.dat", mapOf("columns" to data))
    }

    private fun encodeGameValDat(output: String, tables: Map<String, List<String>>) {
        DataOutputStream(FileOutputStream(output)).use { out ->
            out.writeInt(tables.size)

            tables.forEach { (name, items) ->
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
}