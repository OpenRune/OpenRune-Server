package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.HerbiboarHerbsRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class HerbiboarTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val f = HunterFixture()
    private val script =
        HerbiboarScript(
            ObjRepository(MapClock(100), registry),
            mockk(relaxed = true),
            mockk(relaxed = true),
            f.random,
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    private val npc = mockk<Npc>(relaxed = true)
    private val herbs by lazy { HerbiboarHerbsRow.all().map { InvObj(it.herb.internalName, 1).id }.toSet() }

    private fun caught() {
        for (stat in listOf("stat.hunter", "stat.herblore")) {
            f.player.statMap.setBaseLevel(stat, 99)
            f.player.statMap.setCurrentLevel(stat, 99)
        }
        VarPlayerIntMapSetter.set(f.player, HerbiboarScript.VISIBLE, 1)
    }

    private fun held() = f.player.inv.count { it != null && it.id in herbs }

    private fun ground() = registry.findAll(f.player.coords).filter { it.type in herbs }.sumOf { it.count }

    @Test
    fun `a herbiboar harvested twice gives one harvest`() {
        caught()
        f.run(script, "harvest", npc)
        f.run(script, "harvest", npc)
        assertTrue(held() in 1..3)
        assertEquals(1, f.player.vars[HerbiboarScript.HARVESTED])
    }

    @Test
    fun `a full inventory drops the herbs`() {
        caught()
        f.fill()
        f.run(script, "harvest", npc)
        assertEquals(0, held())
        assertTrue(ground() in 1..3)
    }

    @Test
    fun `a herbiboar gone during the harvest gives nothing`() {
        caught()
        f.call(script, "harvest", npc)
        VarPlayerIntMapSetter.set(f.player, HerbiboarScript.VISIBLE, 0)
        f.finish()
        assertEquals(0, held() + ground())
        assertEquals(0, f.player.vars[HerbiboarScript.HARVESTED])
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
