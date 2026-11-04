package org.rsmod.api.combat.manager

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InventoryServerType
import io.mockk.mockk
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.player.events.AttackKind
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PenetrationXpTest {
    @Test
    fun `a full prayer gives no xp`() {
        val f = Fixture()
        f.npc.vars["varn.protect_melee"] = 100

        assertEquals(0, f.xpFor(damage = 40))
    }

    @Test
    fun `immunity gives no xp, whatever the penetration`() {
        val f = Fixture(penetration = 100)
        f.npc.vars["varn.immune_melee"] = 1

        assertEquals(0, f.xpFor(damage = 40))
    }

    @Test
    fun `a penetrated hit gives xp for the damage it dealt`() {
        val unprotected = Fixture().xpFor(damage = 10)
        assertTrue(unprotected > 0)

        val f = Fixture(penetration = 25)
        f.npc.vars["varn.protect_melee"] = 100

        assertEquals(unprotected, f.xpFor(damage = 40))
    }

    @Test
    fun `xp is capped at the target's hitpoints after the reduction`() {
        val atFifty = Fixture().xpFor(damage = 50)
        assertTrue(atFifty > 0)

        val f = Fixture(penetration = 50)
        f.npc.vars["varn.protect_melee"] = 100

        assertEquals(atFifty, f.xpFor(damage = 400))
    }

    @OptIn(InternalApi::class)
    private class Fixture(penetration: Int = 0) {
        val events = EventBus()
        val player =
            Player().apply {
                worn = Inventory(InventoryServerType(size = 14), arrayOfNulls(14))
                statMap.setBaseLevel(HITPOINTS, 99.toByte())
                statMap.setCurrentLevel(HITPOINTS, 99.toByte())
            }
        val npc =
            Npc(ServerCacheManager.getNpc("npc.bob".asRSCM(RSCMType.NPC))!!, CoordGrid.ZERO).apply {
                slotId = 1
                assignUid()
                baseHitpointsLvl = 50
                hitpoints = 50
                combatXpMultiplier = 1000
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

        init {
            events.subscribeUnbound(PlayerAttackEvents.AttackPrepare::class.java) {
                prayerPenetration = penetration
            }
        }

        fun xpFor(damage: Int): Int {
            val attack = CombatAttack.Melee(null, null, null, CombatStance.Stance1)
            manager.beginAttack(player, npc, AttackKind.Melee, null)
            manager.setNextAttackDelay(player, 4)
            manager.giveCombatXp(player, npc, attack, damage)
            manager.finishAttack(player)
            return player.statMap.getFineXP(HITPOINTS)
        }
    }

    private companion object {
        const val HITPOINTS = "stat.hitpoints"

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
