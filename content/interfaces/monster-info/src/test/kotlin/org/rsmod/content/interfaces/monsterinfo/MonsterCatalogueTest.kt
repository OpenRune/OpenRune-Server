package org.rsmod.content.interfaces.monsterinfo

import dev.openrune.ServerCacheManager
import dtx.rs.RSDropTable
import dtx.rs.rsGuaranteedTable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class MonsterCatalogueTest {
    @Test fun `reverse lookup includes noted drops and preserves area variants without rolling`() {
        val npc = ServerCacheManager.getNpcs().values.first { it.name == "Waterfiend" }
        val base = ServerCacheManager.getItems().values.first { !it.isCert && it.name == "Dragon bones" }
        val note = ServerCacheManager.getItems().values.first { it.isCert && it.certlink == base.id }
        val drop = DropRollItem(note.internalName, 3, condition = { error("Must not roll") })
        val one = RSDropTable<Player, DropRollItem>("one", guaranteed = rsGuaranteedTable { add(drop) })
        val two = RSDropTable<Player, DropRollItem>("two", areas = listOf("area.wilderness"), guaranteed = rsGuaranteedTable { add(drop) })
        val registry = mock(DropTableRegistry::class.java)
        `when`(registry.npcTables()).thenReturn(mapOf(npc.internalName to listOf(one, two)))
        val c = MonsterCatalogue(registry, NpcList(), BossInstanceRegistry())
        assertEquals(2, c.searchMonsters("WATERFIEND").size)
        assertEquals(2, c.sources(note.certlink).size)
        assertEquals(c.sources(note.certlink), c.sources(note.id))
        assertEquals(listOf(note.certlink), c.searchItems("dragon bones").map { it.id })
        assertTrue(c.monsters.any { "wilderness" in it.label })
    }

    @Test fun `locations use original world spawn tiles and exclude instance coordinates`() {
        val npc = ServerCacheManager.getNpcs().values.first { it.name == "Waterfiend" }
        val table = RSDropTable<Player, DropRollItem>("one")
        val registry = mock(DropTableRegistry::class.java)
        `when`(registry.npcTables()).thenReturn(mapOf(npc.internalName to listOf(table)))
        val npcs = NpcList().apply {
            this[1] = Npc(npc, CoordGrid(3200, 3201))
            this[2] = Npc(npc, CoordGrid(7000, 3201))
        }
        val c = MonsterCatalogue(registry, npcs, BossInstanceRegistry())
        assertEquals(listOf("World spawn: 3200, 3201 (plane 0)"), c.locations(c.monsters.single()))
    }

    companion object {
        @JvmStatic @BeforeAll fun loadCache() { ServerCacheManager.init(240).close() }
    }
}
