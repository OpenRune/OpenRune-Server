package org.rsmod.api.combat.manager

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InventoryServerType
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.events.AttackKind
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AttackDelayTest {
    @Test
    fun `no subscriber keeps the attack's own delay`() {
        val f = Fixture()

        f.manager.beginAttack(f.player, f.npc, AttackKind.Melee, null)
        f.manager.setNextAttackDelay(f.player, 4)
        f.manager.finishAttack(f.player)

        assertEquals(4, f.delay())
    }

    @Test
    fun `a delay outside an attack is unaffected`() {
        val f = Fixture()
        f.onPrepare { cycles = 1 }

        f.manager.setNextAttackDelay(f.player, 4)

        assertEquals(4, f.delay())
    }

    @Test
    fun `the event starts from the attack's own delay`() {
        val f = Fixture()
        var seen = -1
        f.onPrepare { seen = cycles }

        f.attack(baseCycles = 5)

        assertEquals(5, seen)
        assertEquals(5, f.delay())
    }

    @Test
    fun `a handler can speed an attack up`() {
        val f = Fixture()
        f.onPrepare { cycles -= 2 }

        f.attack(baseCycles = 5)

        assertEquals(3, f.delay())
    }

    @Test
    fun `a handler can slow an attack down`() {
        val f = Fixture()
        f.onPrepare { cycles += 2 }

        f.attack(baseCycles = 5)

        assertEquals(7, f.delay())
    }

    @Test
    fun `the delay never drops below one tick`() {
        val f = Fixture()
        f.onPrepare { cycles -= 50 }

        f.attack(baseCycles = 4)

        assertEquals(1, f.delay())
    }

    @Test
    fun `a weapon that sets its own delay later keeps the change`() {
        val f = Fixture()
        f.onPrepare { cycles -= 1 }

        f.manager.beginAttack(f.player, f.npc, AttackKind.Staff, null)
        f.manager.setNextAttackDelay(f.player, 4)
        f.manager.setNextAttackDelay(f.player, 5)
        f.manager.finishAttack(f.player)

        assertEquals(4, f.delay())
    }

    @Test
    fun `the result reports the final delay`() {
        val f = Fixture()
        val results = mutableListOf<PlayerAttackEvents.AttackResult>()
        f.events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) { results += this }
        f.onPrepare { cycles -= 2 }

        f.manager.beginAttack(f.player, f.npc, AttackKind.Melee, null)
        f.manager.setNextAttackDelay(f.player, 5)
        f.manager.queueMeleeHit(f.player, f.npc, 1)
        f.manager.finishAttack(f.player)

        assertEquals(3, results.single().cycles)
    }

    @Test
    fun `the change does not carry into the next attack`() {
        val f = Fixture()
        var first = true
        f.onPrepare {
            if (first) cycles -= 2
            first = false
        }

        f.attack(baseCycles = 5)
        assertEquals(3, f.delay())

        f.attack(baseCycles = 5)
        assertEquals(5, f.delay())
    }

    @OptIn(InternalApi::class)
    private class Fixture {
        val events = EventBus()
        val player =
            Player().apply { worn = Inventory(InventoryServerType(size = 14), arrayOfNulls(14)) }
        val npc =
            Npc(ServerCacheManager.getNpc("npc.bob".asRSCM(RSCMType.NPC))!!, CoordGrid.ZERO).apply {
                slotId = 1
                assignUid()
            }
        val manager =
            PlayerAttackManager(
                random = mockk(relaxed = true),
                eventBus = events,
                worldRepo = mockk(relaxed = true),
                accuracy = mockk(relaxed = true),
                maxHits = mockk(relaxed = true),
                npcHitModifier = mockk(relaxed = true),
                playerHitModifier = mockk(relaxed = true),
                npcInteractions = mockk(relaxed = true),
                npcTInteractions = mockk(relaxed = true),
                playerInteractions = mockk(relaxed = true),
                playerTInteractions = mockk(relaxed = true),
                pvpPlayerHitHooks = emptySet(),
            )

        fun onPrepare(action: PlayerAttackEvents.AttackPrepare.() -> Unit) {
            events.subscribeUnbound(PlayerAttackEvents.AttackPrepare::class.java, action)
        }

        fun attack(baseCycles: Int) {
            manager.beginAttack(player, npc, AttackKind.Melee, null)
            manager.setNextAttackDelay(player, baseCycles)
            manager.finishAttack(player)
        }

        fun delay(): Int = player.actionDelay - player.currentMapClock
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
