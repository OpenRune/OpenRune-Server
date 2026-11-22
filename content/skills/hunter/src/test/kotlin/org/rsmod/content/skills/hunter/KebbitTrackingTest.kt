package org.rsmod.content.skills.hunter

import dev.openrune.util.Wearpos
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class KebbitTrackingTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val f = HunterFixture()
    private val script =
        KebbitTrackingScript(mockk(relaxed = true), ObjRepository(MapClock(100), registry), f.random, mockk(relaxed = true))
    private val area by lazy { f.field<List<KebbitTrackingScript.Area>>(script, "areas").first() }
    private val tracks: HashMap<Player, KebbitTrackingScript.Track> by lazy { f.field(script, "tracks") }
    private val spot by lazy { area.spotCoords.keys.first() }

    private fun tracked() {
        f.player.statMap.setBaseLevel(KebbitTrackingScript.HUNTER, 99)
        f.player.statMap.setCurrentLevel(KebbitTrackingScript.HUNTER, 99)
        f.player.worn[3] = InvObj(KebbitTrackingScript.NOOSE_WAND, 1)
        tracks[f.player] = KebbitTrackingScript.Track(area, mutableSetOf(), 0).also { it.target = spot }
    }

    private fun loot() = area.loot.distinct().sumOf { obj -> f.total(obj) + registry.findAll(f.player.coords).filter { it.type == InvObj(obj, 1).id }.sumOf { it.count.toLong() } }

    @Test
    fun `a kebbit noosed twice gives its loot once`() {
        tracked()
        f.call(script, "attack", area, area.spotCoords.getValue(spot))
        f.call(script, "attack", area, area.spotCoords.getValue(spot))
        assertEquals(area.loot.size.toLong(), loot())
    }

    @Test
    fun `a full inventory drops the loot`() {
        tracked()
        f.fill()
        f.call(script, "attack", area, area.spotCoords.getValue(spot))
        assertEquals(area.loot.size.toLong(), loot())
    }

    @Test
    fun `a ring of pursuit is deleted once after ten charges`() {
        f.player.worn[Wearpos.Ring.slot] = InvObj(KebbitTrackingScript.RING_OF_PURSUIT, 1)
        f.player.inv[0] = InvObj(KebbitTrackingScript.RING_OF_PURSUIT, 1)
        repeat(KebbitTrackingScript.PURSUIT_CHARGES - 1) { f.call(script, "useRingCharge") }
        assertEquals(2L, f.total(KebbitTrackingScript.RING_OF_PURSUIT))
        f.call(script, "useRingCharge")
        f.call(script, "useRingCharge")
        assertEquals(1L, f.total(KebbitTrackingScript.RING_OF_PURSUIT))
        assertEquals(0, f.player.vars[KebbitTrackingScript.PURSUIT_USES])
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
