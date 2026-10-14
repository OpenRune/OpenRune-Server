package org.rsmod.api.game.process.player

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.events.PlayerQueueEvents
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.queueDeath
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

class PlayerQueueProcessorTest {
    @Test
    fun GameTestState.`no death queue speed-up if last hit kills player`() =
        runGameTest(PlayerHitScript::class) {
            bindDeathQueueLaunch()
            val npc = spawnNpc(CoordGrid(0, 50, 50, 0, 0), man)
            player.placeAt(npc.coords.translateZ(1))
            player.setCurrentLevel("stat.hitpoints", 7)

            player.queueCombatRetaliate(npc, delay = 1)
            player.queueHit(npc, delay = 1, HitType.Typeless, damage = 5, NO_HIT_MODIFIER)
            player.queueHit(npc, delay = 1, HitType.Typeless, damage = 7, NO_HIT_MODIFIER)

            advance(ticks = 1)
            check(player.hitpoints == 0)
            assertFalse(player.isDelayed)

            advance(ticks = 1)
            assertTrue(player.isDelayed)
        }

    @Test
    fun GameTestState.`trigger death queue speed-up if first hit kills player`() =
        runGameTest(PlayerHitScript::class) {
            bindDeathQueueLaunch()
            val npc = spawnNpc(CoordGrid(0, 50, 50, 0, 0), man)
            player.placeAt(npc.coords.translateZ(1))
            player.setCurrentLevel("stat.hitpoints", 7)

            player.queueCombatRetaliate(npc, delay = 1)
            player.queueHit(npc, delay = 1, HitType.Typeless, damage = 7, NO_HIT_MODIFIER)
            player.queueHit(npc, delay = 1, HitType.Typeless, damage = 5, NO_HIT_MODIFIER)

            check(player.isNotDelayed)

            // On the next cycle (advance by 1), both hits (7 and 5 damage) are processed.
            // The 7 damage hit reduces the player's hitpoints to 0 and queues the death sequence.
            // Because the death queue is added at the end of the list, and the queue that submitted
            // it was not the last in the list, the death queue ends up being processed in the same
            // cycle - after the remaining queues - instead of waiting for the next cycle.
            // This is the "speed-up" bug/quirk, where (death) queues are triggered sooner than they
            // normally would.
            advance(ticks = 1)
            check(player.hitpoints == 0)
            check(!player.hasMovedPreviousCycle)

            // If death queue script was triggered, the player should now be delayed.
            assertTrue(player.isDelayed)
        }

    @Test
    fun GameTestState.`death queue launches while player has open dialog`() =
        runGameTest {
            bindDeathQueueLaunch()
            player.setCurrentLevel("stat.hitpoints", 0)
            player.withProtectedAccess { countDialog() }
            check(player.isAccessProtected)
            check(player.activeCoroutine != null)

            player.queueDeath()
            advance(ticks = 1)

            assertTrue(player.isDelayed)
            assertFalse("queue.death" in player.queueList)
        }

    private companion object {
        private val NO_HIT_MODIFIER = PlayerHitModifier { _: Player -> }

        private val man =
            npcTypeFactory.create {
                name = "Man"
                size = 1
                hitpoints = 7
            }

        // Game-test injector lacks death-hook multibinders; delay mirrors deathSequence launch.
        private fun GameTestScope.bindDeathQueueLaunch() {
            val queueId = "queue.death".asRSCM(RSCMType.QUEUE).toLong()
            eventBus.subscribeSuspend(
                PlayerQueueEvents.Protected::class.java,
                queueId,
            ) { _: PlayerQueueEvents.Protected<*> ->
                delay(2)
            }
        }
    }
}
