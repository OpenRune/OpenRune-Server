package org.rsmod.content.skills.hunter

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

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
