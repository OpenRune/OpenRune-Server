package org.rsmod.content.skills.hunter

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.HerbiboarFossilsRow
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
    private val fossils by lazy { HerbiboarFossilsRow.all().map { InvObj(it.fossil.internalName, 1).id }.toSet() }
    private val leg by lazy { f.field<List<HerbiboarScript.Leg>>(script, "legs").first { it.toGroup != null } }

    private fun caught() {
        for (stat in listOf("stat.hunter", "stat.herblore")) {
            f.player.statMap.setBaseLevel(stat, 99)
            f.player.statMap.setCurrentLevel(stat, 99)
        }
        VarPlayerIntMapSetter.set(f.player, HerbiboarScript.VISIBLE, 1)
    }

    private fun held() = f.player.inv.count { it != null && it.id in herbs }

    private fun ground() = registry.findAll(f.player.coords).filter { it.type in herbs }.sumOf { it.count }

    private fun heldFossils() = f.player.inv.sumOf { if (it != null && it.id in fossils) it.count.toLong() else 0L }

    private fun groundFossils() = registry.findAll(f.player.coords).filter { it.type in fossils }.sumOf { it.count.toLong() }

    private fun onTrail() {
        caught()
        VarPlayerIntMapSetter.set(f.player, HerbiboarScript.VISIBLE, 0)
        VarPlayerIntMapSetter.set(f.player, HerbiboarScript.ROUTE_LENGTH, HerbiboarScript.MAX_LEGS)
        VarPlayerIntMapSetter.set(f.player, leg.trail, leg.dir)
    }

    private fun hunterXp() = f.player.statMap.getXP(HerbiboarScript.HUNTER)

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

    @Test
    fun `herbs from twenty harvests are all held or dropped`() {
        f.fill()
        for (slot in 0 until 5) f.player.inv[slot] = null
        repeat(20) {
            caught()
            f.run(script, "harvest", npc)
        }
        assertEquals(5, held())
        assertTrue(held() + ground() in 20..60)
        assertEquals(20, f.player.vars[HerbiboarScript.HARVESTED])
    }

    @Test
    fun `the horn of plenty spends each charge once`() {
        f.player.worn[5] = InvObj(HerbiboarScript.HORN_OF_PLENTY, 1)
        VarPlayerIntMapSetter.set(f.player, HerbiboarScript.HORN_CHARGES, 1)
        repeat(100) {
            caught()
            f.run(script, "harvest", npc)
        }
        assertEquals(0, f.player.vars[HerbiboarScript.HORN_CHARGES])
        assertTrue(held() + ground() in 101..303)
    }

    @Test
    fun `a full numulite stack and inventory drop every fossil`() {
        f.fill()
        f.player.inv[0] = InvObj("obj.fossil_numulite", Int.MAX_VALUE)
        repeat(50) { f.call(script, "rollFossil") }
        assertEquals(Int.MAX_VALUE.toLong(), f.total("obj.fossil_numulite"))
        assertEquals(1, f.player.inv.count { it != null && it.id in fossils })
        assertTrue(groundFossils() > 0)
    }

    @Test
    fun `a spot inspected twice rolls its fossil once`() {
        val lucky =
            mockk<GameRandom> {
                every { of(any<Int>()) } returns 0
                every { of(any<Int>(), any<Int>()) } answers { firstArg() }
            }
        val script =
            HerbiboarScript(
                ObjRepository(MapClock(100), registry),
                mockk(relaxed = true),
                mockk(relaxed = true),
                lucky,
                mockk(relaxed = true),
                mockk(relaxed = true),
            )
        onTrail()
        val before = hunterXp()
        f.run(script, "inspectSpot", leg.from)
        val found = heldFossils() + groundFossils()
        f.run(script, "inspectSpot", leg.from)
        assertTrue(found > 0)
        assertEquals(found, heldFossils() + groundFossils())
        assertEquals(HerbiboarScript.INSPECT_XP.toInt(), hunterXp() - before)
    }

    @Test
    fun `a trail reset during the inspect rewards nothing`() {
        onTrail()
        val before = hunterXp()
        f.call(script, "inspectSpot", leg.from)
        VarPlayerIntMapSetter.set(f.player, leg.trail, 0)
        f.finish()
        assertEquals(0, hunterXp() - before)
        assertEquals(0L, groundFossils())
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
