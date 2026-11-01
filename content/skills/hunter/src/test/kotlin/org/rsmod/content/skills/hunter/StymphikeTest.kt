package org.rsmod.content.skills.hunter

import dev.openrune.types.HitmarkTypeGroup
import dev.openrune.types.ItemServerType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.enums.HunterEnums.hunter_stymphike_loot
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class StymphikeTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val f = HunterFixture()
    private val script =
        StymphikeScript(mockk(relaxed = true), ObjRepository(MapClock(100), registry), mockk(relaxed = true), mockk(relaxed = true))
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

    private fun loot() = hunter_stymphike_loot.sumOf { ground(it.key.internalName) }

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
        assertEquals(hunter_stymphike_loot.values.sumOf { it ?: 0 }, loot())
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
    fun `an alerted bird uses one spear and drops no loot`() {
        mockkStatic("org.rsmod.api.npc.hit.NpcExtensionsKt")
        every { npc.queueHit(any<Player>(), any(), any<HitType>(), any(), any<NpcHitModifier>(), any<HitmarkTypeGroup>(), any(), isNull<ItemServerType>()) } returns mockk<Hit>(relaxed = true)
        land(hidden = false)
        f.run(script, "spear", npc)
        assertEquals(0L, f.total(spear))
        assertEquals(0, loot())
        verify(exactly = 1) { npc.queueHit(any<Player>(), any(), any<HitType>(), StymphikeScript.ALERTED_DAMAGE, any<NpcHitModifier>(), any<HitmarkTypeGroup>(), any(), isNull<ItemServerType>()) }
    }

    @Test
    fun `spears from the pile onto a stack at the maximum leave the stack untouched`() {
        f.player.inv[0] = InvObj(spear, Int.MAX_VALUE - 5)
        f.access.invAdd(f.player.inv, spear, StymphikeScript.PILE_TAKE)
        assertEquals(Int.MAX_VALUE - 5L, f.total(spear))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
