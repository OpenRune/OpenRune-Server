package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.*
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@ResourceLock("ServerCacheManager")
internal class ContainerBankProximityTest {
    @Test fun `real banker is usable within three tiles but not across floors or outside the radius`() {
        val type = ServerCacheManager.getNpcs().values.first { (1..5).any { op -> it.actions.getOpOrNull(op) == "Bank" } }
        val banker = Npc(type, CoordGrid(3003, 3000)); val f = Fixture(npcs = listOf(banker))
        assertTrue(f.banks.available(f.player)); banker.coords = CoordGrid(3004, 3000); assertFalse(f.banks.available(f.player)); banker.coords = CoordGrid(3001, 3000, 1); assertFalse(f.banks.available(f.player))
    }

    @Test fun `actual nearby bank chest enables usage and distant or deposit-only objects do not`() {
        val type = ServerCacheManager.getObjects().values.first { it.name == "Bank chest" }
        fun at(coords: CoordGrid) = LocInfo(2, coords, LocEntity(type.id, 10, 0))
        assertTrue(Fixture(locs = listOf(at(CoordGrid(3002, 3000)))).let { it.banks.available(it.player) })
        assertFalse(Fixture(locs = listOf(at(CoordGrid(3003, 3000)))).let { it.banks.available(it.player) })
        assertFalse(Fixture(locs = listOf(at(CoordGrid(3001, 3000, 1)))).let { it.banks.available(it.player) })
        val deposit = ServerCacheManager.getObjects().values.firstOrNull { it.name == "Bank deposit chest" }
        if (deposit != null) assertFalse(Fixture(locs = listOf(LocInfo(2, CoordGrid(3001, 3000), LocEntity(deposit.id, 10, 0)))).let { it.banks.available(it.player) })
    }
    private class Fixture(locs: List<LocInfo> = emptyList(), npcs: List<Npc> = emptyList()) {
        val player = Player().apply { coords = CoordGrid(3000, 3000) }
        val locRepo = mock(LocRepository::class.java) { call -> if (call.method.name.startsWith("findAll")) { val zone = (call.arguments.first() as Number).toInt(); locs.filter { ZoneKey.from(it.coords).packed == zone }.asSequence() } else RETURNS_DEFAULTS.answer(call) }
        val npcRepo = mock(NpcRepository::class.java) { call -> if (call.method.name.startsWith("findAll"))npcs.asSequence() else RETURNS_DEFAULTS.answer(call) }
        val banks = ContainerBankProximity(locRepo, npcRepo)
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
