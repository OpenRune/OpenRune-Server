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
    private val fixed =
        mockk<GameRandom> {
            every { of(any<Int>()) } returns 0
            every { of(any<Int>(), any<Int>()) } answers { secondArg() }
        }
    private val f = HunterFixture(random = fixed)
    private val script =
        HerbiboarScript(
            ObjRepository(MapClock(100), registry),
            mockk(relaxed = true),
            mockk(relaxed = true),
            fixed,
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

    @Test
    fun `a herbiboar harvested twice gives one harvest`() {
        caught()
        f.run(script, "harvest", npc)
        f.run(script, "harvest", npc)
        assertEquals(HERBS, held())
        assertEquals(1, f.player.vars[HerbiboarScript.HARVESTED])
    }

    @Test
    fun `a full inventory drops the herbs`() {
        caught()
        f.fill()
        f.run(script, "harvest", npc)
        assertEquals(0, held())
        assertEquals(HERBS, ground())
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
        assertEquals(20 * HERBS, held() + ground())
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
        assertEquals(101 * HERBS, held() + ground())
    }

    @Test
    fun `a full numulite stack and inventory drop every fossil`() {
        f.fill()
        f.player.inv[0] = InvObj("obj.fossil_numulite", Int.MAX_VALUE)
        repeat(50) { f.call(script, "rollFossil") }
        assertEquals(Int.MAX_VALUE.toLong(), f.total("obj.fossil_numulite"))
        assertEquals(1, f.player.inv.count { it != null && it.id in fossils })
        assertEquals(50L * HerbiboarFossilsRow.all().first().max, groundFossils())
    }

    @Test
    fun `a spot inspected twice rolls its fossil once`() {
        onTrail()
        f.run(script, "inspectSpot", leg.from)
        val found = heldFossils() + groundFossils()
        f.run(script, "inspectSpot", leg.from)
        assertTrue(found > 0)
        assertEquals(found, heldFossils() + groundFossils())
    }

    @Test
    fun `a trail reset during the inspect rewards nothing`() {
        onTrail()
        f.call(script, "inspectSpot", leg.from)
        VarPlayerIntMapSetter.set(f.player, leg.trail, 0)
        f.finish()
        assertEquals(0L, heldFossils() + groundFossils())
    }

    companion object {
        private const val HERBS = 3

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
