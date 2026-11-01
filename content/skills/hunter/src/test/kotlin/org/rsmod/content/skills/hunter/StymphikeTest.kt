package org.rsmod.content.skills.hunter

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class StymphikeTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val f = HunterFixture()
    private val script = StymphikeScript(mockk(relaxed = true), ObjRepository(MapClock(100), registry), mockk(relaxed = true))
    private val npc = mockk<Npc>(relaxed = true).also { every { it.isSlotAssigned } returns true }
    private val spear = StymphikeScript.MAKESHIFT_SPEAR

    private fun land(hidden: Boolean = true) {
        val bird =
            Class.forName("${StymphikeScript::class.java.name}\$Bird")
                .getDeclaredConstructor(Npc::class.java, Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
                .newInstance(npc, 0)
        f.field<HashMap<Player, Any>>(script, "birds")[f.player] = bird
        VarPlayerIntMapSetter.set(f.player, StymphikeScript.HIDING, if (hidden) 1 else 0)
        f.player.inv[0] = InvObj(spear, 1)
    }

    private fun ground(obj: String) = registry.findAll(f.player.coords).filter { it.type == InvObj(obj, 1).id }.sumOf { it.count }

    private fun loot() = StymphikeScript.LOOT.sumOf { ground(it.first) }

    @Test
    fun `a full inventory takes no spears from the pile and a free one takes ten each time`() {
        f.fill()
        f.access.invAdd(f.player.inv, spear, StymphikeScript.PILE_TAKE)
        assertEquals(0L, f.total(spear))
        f.player.inv.indices.forEach { f.player.inv[it] = null }
        repeat(2) { f.access.invAdd(f.player.inv, spear, StymphikeScript.PILE_TAKE) }
        assertEquals(20L, f.total(spear))
    }

    @Test
    fun `a spear thrown twice at one bird uses one spear and drops one loot`() {
        land()
        f.run(script, "spear", npc)
        f.run(script, "spear", npc)
        assertEquals(0L, f.total(spear))
        assertEquals(StymphikeScript.LOOT.sumOf { it.second }, loot())
    }

    @Test
    fun `a bird gone before the spear lands drops nothing`() {
        land()
        f.call(script, "spear", npc)
        every { npc.isSlotAssigned } returns false
        f.finish()
        assertEquals(0L, f.total(spear))
        assertEquals(0, loot())
    }

    @Test
    fun `an alerted bird flies off without loot`() {
        land(hidden = false)
        f.run(script, "spear", npc)
        assertEquals(0L, f.total(spear))
        assertEquals(0, loot())
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
