package org.rsmod.api.npc.hit.processor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.api.player.events.publishAttackImpact
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.hit.Hitmark
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AttackImpactEventTest {
    @Test
    fun `a player's hit on an npc names the attacker and the target`() {
        val f = Fixture()
        f.npc.hitpoints = 50

        f.land(f.hitBy(f.attacker, damage = 12))

        val event = f.impacts.single()
        assertSame(f.attacker, event.player)
        assertSame(f.npc, event.target)
        assertEquals(12, event.hit.damage)
    }

    @Test
    fun `damage is capped by the npc's remaining hitpoints`() {
        val f = Fixture()
        f.npc.hitpoints = 5

        f.land(f.hitBy(f.attacker, damage = 20))

        assertEquals(5, f.impacts.single().hit.damage)
    }

    @Test
    fun `a zero damage hit still publishes`() {
        val f = Fixture()
        f.npc.hitpoints = 50

        f.land(f.hitBy(f.attacker, damage = 0))

        assertEquals(0, f.impacts.single().hit.damage)
    }

    @Test
    fun `the npc's own event runs first so a boss reaction is already applied`() {
        val f = Fixture()
        f.npc.hitpoints = 50
        val order = mutableListOf<String>()
        f.events.subscribeKeyed(NpcHitEvents.Impact::class.java, f.npc.visType.id.toLong()) {
            order += "npc"
        }
        f.events.subscribeUnbound(PlayerAttackEvents.HitImpact::class.java) { order += "attacker" }

        f.land(f.hitBy(f.attacker, damage = 1))

        assertEquals(listOf("npc", "attacker"), order)
    }

    @Test
    fun `a hit with no player source publishes nothing`() {
        val f = Fixture()
        f.npc.hitpoints = 50

        f.land(Hit(HitType.Typeless, Hitmark.fromNoSource(0, 0, null, 3, 0), null, null, null))

        assertTrue(f.impacts.isEmpty())
    }

    @Test
    fun `an attacker who has logged out publishes nothing`() {
        val f = Fixture()
        f.npc.hitpoints = 50
        val hit = f.hitBy(f.attacker, damage = 3)
        f.players.remove(f.attacker.slotId)

        f.land(hit)

        assertTrue(f.impacts.isEmpty())
    }

    @Test
    fun `no subscriber does not resolve the source`() {
        val f = Fixture()
        val hit = f.hitBy(f.attacker, damage = 3)
        val unusedPlayers = mockk<PlayerList>()

        EventBus().publishAttackImpact(f.npc, hit, unusedPlayers)
    }

    @OptIn(InternalApi::class)
    private class Fixture {
        val events = EventBus()
        val players = PlayerList()
        val impacts = mutableListOf<PlayerAttackEvents.HitImpact>()
        val attacker =
            Player().apply {
                slotId = 1
                uuid = 100L
                assignUid()
                players[1] = this
            }
        val npc =
            Npc(ServerCacheManager.getNpc("npc.bob".asRSCM(RSCMType.NPC))!!, CoordGrid.ZERO).apply {
                slotId = 5
                assignUid()
                baseHitpointsLvl = 100
            }
        private val processor = StandardNpcHitProcessor(players, events, emptySet(), emptySet())

        init {
            events.subscribeUnbound(PlayerAttackEvents.HitImpact::class.java) { impacts += this }
        }

        fun land(hit: Hit) {
            with(processor) { StandardNpcAccess(npc, mockk(), mockk()).process(hit) }
        }

        fun hitBy(source: Player, damage: Int): Hit =
            Hit(
                HitType.Melee,
                Hitmark.fromPlayerSource(0, 0, null, damage, 0, source.slotId),
                source.uid.packed,
                null,
                null,
            )
    }

    private companion object {
        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
        }
    }
}
