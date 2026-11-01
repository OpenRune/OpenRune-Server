package org.rsmod.api.player.events

import dev.openrune.types.NpcServerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.game.damage.recordDamageOn
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.hit.Hitmark
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class NpcKilledEventTest {
    @Test
    fun `hero comes first and everyone who dealt damage follows`() {
        val f = Fixture()
        f.npc.recordDamage(f.helper, 80)
        f.npc.recordDamage(f.hero, 20)

        val events = npcKilledEvents(f.hero, f.npc, f.players)

        assertEquals(listOf(f.hero, f.helper), events.map { it.player })
        assertEquals(listOf(true, false), events.map { it.isHero })
        assertEquals(listOf(20, 80), events.map { it.damage })
    }

    @Test
    fun `hero without recorded damage is still included`() {
        val f = Fixture()

        val events = npcKilledEvents(f.hero, f.npc, f.players)

        assertEquals(1, events.size)
        assertTrue(events.single().isHero)
        assertEquals(0, events.single().damage)
    }

    @Test
    fun `contributor who is no longer online is skipped`() {
        val f = Fixture()
        f.npc.recordDamage(f.hero, 10)
        f.npc.recordDamage(f.offline, 50)

        val events = npcKilledEvents(f.hero, f.npc, f.players)

        assertEquals(listOf(f.hero), events.map { it.player })
    }

    @Test
    fun `only the player who dealt the killing hit has a killing style`() {
        val f = Fixture()
        f.npc.recordDamage(f.hero, 10)
        f.npc.recordDamage(f.helper, 10)
        f.npc.killingHit = f.hitBy(f.helper, HitType.Ranged)

        val events = npcKilledEvents(f.hero, f.npc, f.players).associateBy { it.player }

        assertFalse(events.getValue(f.hero).dealtKillingHit)
        assertNull(events.getValue(f.hero).killingStyle)
        assertTrue(events.getValue(f.helper).dealtKillingHit)
        assertEquals(HitType.Ranged, events.getValue(f.helper).killingStyle)
    }

    @Test
    fun `no killing hit means nobody dealt it`() {
        val f = Fixture()
        f.npc.recordDamage(f.hero, 10)

        val event = npcKilledEvents(f.hero, f.npc, f.players).single()

        assertFalse(event.dealtKillingHit)
        assertNull(event.killingStyle)
    }

    @Test
    fun `took damage is tracked per player`() {
        val f = Fixture()
        f.npc.recordDamage(f.hero, 10)
        f.npc.recordDamage(f.helper, 10)
        f.npc.hurtPlayer(f.helper)

        val events = npcKilledEvents(f.hero, f.npc, f.players).associateBy { it.player }

        assertFalse(events.getValue(f.hero).tookDamage)
        assertTrue(events.getValue(f.helper).tookDamage)
    }

    @Test
    fun `npc damage marks the player only when it lands`() {
        val f = Fixture()
        val npcHit = { damage: Int ->
            Hit(
                HitType.Melee,
                Hitmark.fromNpcSource(0, 0, null, damage, 0, f.npc.slotId),
                f.npc.uid.packed,
                null,
                null,
            )
        }

        npcHit(0).recordDamageOn(f.hero, 0, f.players, f.npcs)
        assertFalse(f.npc.hasHurt(f.hero))

        npcHit(4).recordDamageOn(f.hero, 4, f.players, f.npcs)
        assertTrue(f.npc.hasHurt(f.hero))
        assertFalse(f.npc.hasHurt(f.helper))
    }

    @OptIn(InternalApi::class)
    private class Fixture {
        val players = PlayerList()
        val npcs = NpcList()
        val hero = player(slot = 1)
        val helper = player(slot = 2)
        val offline = player(slot = 3, online = false)
        val npc =
            Npc(NpcServerType(id = 1, name = "Test", size = 1, hitpoints = 100), CoordGrid.ZERO)
                .apply {
                    slotId = 5
                    assignUid()
                    npcs[5] = this
                }

        fun hitBy(source: Player, type: HitType): Hit =
            Hit(
                type,
                Hitmark.fromPlayerSource(0, 0, null, 10, 0, source.slotId),
                source.uid.packed,
                null,
                null,
            )

        private fun player(slot: Int, online: Boolean = true): Player =
            Player().apply {
                slotId = slot
                uuid = slot * 100L
                assignUid()
                if (online) players[slot] = this
            }
    }
}
