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

    private val MEANINGFUL_CHARS = mapOf("#" to "_sharp", "&" to "_and")

    private class Entry(val raw: String, val standardized: String, val id: Int)

    fun dumpGamevals(cache: Cache, rev: Int, snapshotDir: File? = null) {
        val gamevals = mutableMapOf<String, List<String>>()

        GameValGroupTypes.entries.forEach { group ->
            val elements = GameValHandler.readGameVal(group, cache, rev)

            when (group) {
                GameValGroupTypes.SPRITETYPES -> {
                    gamevals["sprites"] =
                        elements.mapNotNull { it.elementAs<Sprite>()?.toEntry() }.resolveNames()
                }

                GameValGroupTypes.IFTYPES_V2 -> {
                    val interfaces = elements.mapNotNull { it.elementAs<Interface>() }
                    gamevals["interfaces"] = interfaces.map { entry(it.name, it.id) }.resolveNames()
                }

                GameValGroupTypes.IFTYPES -> Unit

                else -> {
                    val key = group.groupName.replace("dbtables", "tables")
                    gamevals[key] = elements.map { entry(it.name, it.id) }.resolveNames()
                }
            }
        }

        val outputDir = snapshotDir ?: File("../.data/gamevals-binary")
        GameValDat.write(File(outputDir, "gamevals.dat"), gamevals.remapNames())
        val generated = mapOf("dbcol" to columnEntries(cache, rev), "component" to componentEntries(cache, rev))
        if (snapshotDir == null) {
            GeneratedGameVals.replaceTables(generated)
        } else {
            GameValDat.write(File(outputDir, GeneratedGameVals.FILE_NAME), generated)
        }
    }

    fun dumpComponents(cache: Cache, rev: Int) {
        GeneratedGameVals.replaceTables(mapOf("component" to componentEntries(cache, rev)))
    }

    private fun componentEntries(cache: Cache, rev: Int): List<String> {
        val elements = GameValHandler.readGameVal(GameValGroupTypes.IFTYPES_V2, cache = cache, rev)
        val data = mutableListOf<Entry>()

        elements.forEach { gameValElement ->
            val iftype = gameValElement.elementAs<Interface>() ?: return@forEach
            iftype.components.forEach { comp ->
                data.add(entry(iftype.name, comp.name, comp.packed))
            }
        }

        return data.resolveNames()
    }

    fun dumpCols(cache: Cache, rev: Int) {
        GeneratedGameVals.replaceTables(mapOf("dbcol" to columnEntries(cache, rev)))
    }

    private fun columnEntries(cache: Cache, rev: Int): List<String> {
        val elements = GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache = cache, rev)
        val data = mutableListOf<Entry>()

        elements.forEach { gameValElement ->
            val table = gameValElement.elementAs<Table>() ?: return@forEach
            table.columns.forEach { column ->
                data.add(entry(table.name, column.name, (gameValElement.id shl 16) or column.id))
            }
        }

        return data.resolveNames()
    }

    private fun entry(name: String, id: Int): Entry = Entry(name, standardizeName(name), id)

    private fun entry(parent: String, child: String, id: Int): Entry =
        Entry("$parent:$child", "${standardizeName(parent)}:${standardizeName(child)}", id)

    private fun Sprite.toEntry(): Entry =
        if (index == -1) {
            entry(name, id)
        } else {
            Entry("$name:$index", "${standardizeName(name)}:$index", id)
        }

    internal fun childNames(parent: String, children: List<Pair<String, Int>>): List<String> =
        children.map { (name, id) -> entry(parent, name, id) }
            .resolveNames()
            .map { it.substringBeforeLast('=') }

    internal fun standardizeName(name: String): String {
        val expanded =
            MEANINGFUL_CHARS.entries.fold(name) { acc, (char, word) -> acc.replace(char, word) }
        return GameValHandler.standardizeGamevalName(expanded)
    }

    /**
     * Emits `name=id` lines with every name put through [standardizeName], so a dumped symbol is
     * always the same symbol `encodeGameVals` would pack back into the cache.
     *
     * Jagex authored the `jingle`, `midi`, `synth` and some `component` names as prose rather than
     * symbols, and standardizing prose can land on a name another entry already holds verbatim
     * (`cws_doomsayer` has both `goblin cave rope` and `goblin_cave_rope`). Entries that were
     * already symbol-shaped are therefore reserved first and keep their name unchanged; a rewritten
     * name that collides falls back to an id suffix.
     */
    private fun List<Entry>.resolveNames(): List<String> {
        val taken = HashSet<String>(size)
        for (item in this) {
            if (item.raw == item.standardized) {
                taken.add(item.standardized)
            }
        }
        return map { item ->
            val name =
                when {
                    item.raw == item.standardized -> item.standardized
                    taken.add(item.standardized) -> item.standardized
                    else -> "${item.standardized}_${item.id}"
                }
            "$name=${item.id}"
        }
    }

    private fun Map<String, List<String>>.remapNames(): Map<String, List<String>> =
        mapKeys { (name, _) -> NAME_REMAP[name] ?: name }
}
