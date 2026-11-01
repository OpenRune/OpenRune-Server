package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class CrabTrapTest {
    private val f = HunterFixture()
    private val script = CrabTrapScript(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), f.random, MapClock(100))
    private val site by lazy { f.field<List<CrabTrapScript.Site>>(script, "sites").first() }
    private val loc = LocInfo(0, CoordGrid(3223, 3222, 0), LocEntity(0))
    private val nails = CrabTrapScript.NAILS.first()

    private fun materials() {
        f.player.statMap.setBaseLevel("stat.construction", 99)
        f.player.statMap.setCurrentLevel("stat.construction", 99)
        f.player.inv[0] = InvObj(CrabTrapScript.HAMMER, 1)
        f.player.inv[1] = InvObj(CrabTrapScript.SAWS.first(), 1)
        f.player.inv[2] = InvObj(CrabTrapScript.BUCKET, 1)
        f.player.inv[3] = InvObj(CrabTrapScript.PLANK, 1)
        f.player.inv[4] = InvObj(nails, 2)
    }

    @Test
    fun `a trap built twice takes one set of materials`() {
        materials()
        f.player.inv[5] = InvObj(CrabTrapScript.PLANK, 1)
        f.player.inv[6] = InvObj(CrabTrapScript.BUCKET, 1)
        f.player.inv[4] = InvObj(nails, 4)
        f.run(script, "build", site.varbits.first())
        f.run(script, "build", site.varbits.first())
        assertEquals(1L, f.total(CrabTrapScript.PLANK))
        assertEquals(2L, f.total(nails))
    }

    @Test
    fun `nails dropped during the build take nothing`() {
        materials()
        f.call(script, "build", site.varbits.first())
        f.player.inv[4] = null
        f.finish()
        assertEquals(2L, f.total(CrabTrapScript.PLANK) + f.total(CrabTrapScript.BUCKET))
        assertEquals(0, f.player.vars[site.varbits.first()])
    }

    @Test
    fun `a full trap emptied twice gives one crab`() {
        VarPlayerIntMapSetter.set(f.player, site.varbits.first(), CrabTrapScript.FULL)
        f.run(script, "empty", site, 0, loc.coords, loc)
        f.run(script, "empty", site, 0, loc.coords, loc)
        assertEquals(1L, site.catches.distinct().sumOf { f.total(it) })
    }

    @Test
    fun `a full inventory keeps the crab in the trap`() {
        VarPlayerIntMapSetter.set(f.player, site.varbits.first(), CrabTrapScript.FULL)
        f.fill()
        f.run(script, "empty", site, 0, loc.coords, loc)
        assertEquals(0L, site.catches.distinct().sumOf { f.total(it) })
        assertEquals(CrabTrapScript.FULL, f.player.vars[site.varbits.first()])
    }

    @Test
    fun `an emptied trap re-baits with one bait and none once it is dropped`() {
        f.player.statMap.setBaseLevel("stat.hunter", 99)
        f.player.statMap.setCurrentLevel("stat.hunter", 99)
        f.player.statMap.setBaseLevel(CrabTrapScript.SAILING, 99)
        f.player.statMap.setCurrentLevel(CrabTrapScript.SAILING, 99)
        f.player.inv[0] = InvObj(site.bait, 2)
        VarPlayerIntMapSetter.set(f.player, site.varbits.first(), CrabTrapScript.FULL)
        f.run(script, "empty", site, 0, loc.coords, loc)
        assertEquals(1L, f.total(site.bait))
        assertEquals(CrabTrapScript.ACTIVE, f.player.vars[site.varbits.first()])
        VarPlayerIntMapSetter.set(f.player, site.varbits.first(), CrabTrapScript.FULL)
        f.call(script, "empty", site, 0, loc.coords, loc)
        f.player.inv[0] = null
        f.finish()
        assertEquals(2L, site.catches.distinct().sumOf { f.total(it) })
        assertEquals(CrabTrapScript.BUILT, f.player.vars[site.varbits.first()])
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
