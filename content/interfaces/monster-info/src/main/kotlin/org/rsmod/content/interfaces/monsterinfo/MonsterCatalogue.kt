package org.rsmod.content.interfaces.monsterinfo

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dtx.rs.RSDropTable
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.droptable.DropPreviewEntry
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTablePreview
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

data class MonsterEntry(
    val type: NpcServerType,
    val variants: Set<Int>,
    val table: RSDropTable<Player, DropRollItem>,
    val drops: List<DropPreviewEntry>,
) {
    val label: String get() = type.name + if (table.areas.isEmpty()) "" else " (${table.areas.joinToString { it.removePrefix("area.").replace('_', ' ') }})"
}

@Singleton
class MonsterCatalogue @Inject constructor(
    private val tables: DropTableRegistry,
    private val npcs: NpcList,
    private val bosses: BossInstanceRegistry,
) {
    val monsters: List<MonsterEntry> by lazy {
        tables.npcTables().flatMap { (symbol, tables) ->
            val type = ServerCacheManager.getNpc(symbol.asRSCM())
            if (type == null) emptyList() else tables.map { type to it }
        }.groupBy { (type, table) -> type.name to table.tableIdentifier }.values.map { entries ->
            val (type, table) = entries.first()
            MonsterEntry(type, entries.map { it.first.id }.toSet(), table, DropTablePreview.entries(table))
        }.sortedWith(compareBy({ it.type.name.lowercase() }, { it.table.tableIdentifier }))
    }

    private val byItem: Map<Int, List<MonsterEntry>> by lazy {
        buildMap<Int, MutableList<MonsterEntry>> {
            for (monster in monsters) {
                val ids = monster.drops.mapNotNull { it.item?.obj?.asRSCM() }.map(::canonicalItem).toSet()
                for (id in ids) getOrPut(id) { mutableListOf() }.add(monster)
            }
        }
    }

    val items: List<ItemServerType> by lazy {
        byItem.keys.mapNotNull { ServerCacheManager.getItem(it) }.sortedBy { it.name.lowercase() }
    }

    fun searchMonsters(query: String): List<MonsterEntry> {
        val q = query.trim().lowercase()
        return monsters.filter { q in it.label.lowercase() || q in it.type.internalName.lowercase() || q.toIntOrNull() in it.variants }
            .sortedByDescending { it.type.name.equals(q, ignoreCase = true) }
    }

    fun searchItems(query: String): List<ItemServerType> {
        val q = query.trim().lowercase()
        return items.filter { q in it.name.lowercase() || q == it.id.toString() }
            .sortedByDescending { it.name.equals(q, ignoreCase = true) }
    }

    fun sources(item: Int): List<MonsterEntry> = byItem[canonicalItem(item)].orEmpty()

    fun find(type: NpcServerType, table: RSDropTable<Player, DropRollItem>?): MonsterEntry? =
        monsters.firstOrNull { type.id in it.variants && (table == null || it.table.tableIdentifier == table.tableIdentifier) }

    fun locations(monster: MonsterEntry): List<String> {
        val locations = linkedSetOf<String>()
        for (key in bosses.keys()) {
            val spec = bosses.get(key) ?: continue
            if (spec.area.npcSpawns.none { it.npcType.asRSCM() in monster.variants } && spec.bossNpcs.orEmpty().none { it.id in monster.variants }) continue
            val exit = when (val area = spec.area) {
                is InstanceArea.Template -> area.exitCoord
                is InstanceArea.CopyRegions -> area.exitCoord
            }
            locations += if (exit == CoordGrid.ZERO) "${spec.bossName}: instanced encounter" else "${spec.bossName} entrance: ${coordinate(exit)}"
        }
        for (npc in npcs) {
            if (npc.type.id !in monster.variants || RegionRegistry.inWorkingArea(npc.spawnCoords)) continue
            locations += "World spawn: ${coordinate(npc.spawnCoords)}"
        }
        return locations.toList().ifEmpty { listOf("No static spawn registered.", "May be summoned or encounter-based.") }
    }

    private fun coordinate(coords: CoordGrid) = "${coords.x}, ${coords.z} (plane ${coords.level})"

    private fun canonicalItem(id: Int): Int {
        val item = ServerCacheManager.getItem(id)
        return if (item?.isCert == true) item.certlink else id
    }
}
