package org.rsmod.api.combatachievements

import dev.openrune.ServerCacheManager
import dev.openrune.types.varp.VarpLifetime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.output.PlayerNotifications
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player

@ResourceLock("ServerCacheManager")
class CombatAchievementsTest {
    private val catalogue = CombatAchievementCatalogue()
    private val achievements = CombatAchievements(catalogue)

    @Test
    fun `native catalogue uses full revision 240 thresholds and stable task identities`() {
        assertEquals(655, catalogue.tasks.size)
        assertEquals(listOf(41, 169, 436, 1100, 1965, 2697), catalogue.thresholds)
        assertEquals("Open the Barrows chest 10 times.", catalogue.task(24).description)
        assertEquals("Open the Barrows chest 25 times.", catalogue.task(25).description)
        assertEquals("Kill the Kraken 20 times.", catalogue.task(168).description)
        assertEquals("Kill the Kraken after killing all four tentacles.", catalogue.task(169).description)
    }

    @Test
    fun `legitimate Barrows boundaries ignore historical chest totals and permit partial chests`() {
        val player = Player()
        VarPlayerIntMapSetter.set(player, "varp.total_barrows_chests", 1000)
        repeat(9) { achievements.recordBarrowsChest(player, false) }
        assertFalse(achievements.isCompleted(player, 24))
        assertEquals(0, achievements.points(player))
        achievements.recordBarrowsChest(player, false)
        assertTrue(achievements.isCompleted(player, 24))
        assertEquals(1, achievements.points(player))
        repeat(14) { achievements.recordBarrowsChest(player, true) }
        assertFalse(achievements.isCompleted(player, 25))
        achievements.recordBarrowsChest(player, false)
        repeat(4) { achievements.recordBarrowsChest(player, true) }
        assertTrue(achievements.isCompleted(player, 25))
        assertEquals(3, achievements.points(player))
        assertEquals(2, player.vars["varbit.ca_total_tasks_completed_barrows"])
        assertFalse(achievements.isCompleted(player, 27))
    }

    @Test
    fun `Kraken bypass events cannot count and real kills require exact threshold`() {
        val player = Player()
        repeat(30) { achievements.recordKrakenKill(player, false, true) }
        assertEquals(0, player.vars["varp.ca_legitimate_kraken_kills"])
        assertEquals(0, achievements.points(player))
        repeat(19) { achievements.recordKrakenKill(player, true) }
        assertFalse(achievements.isCompleted(player, 168))
        achievements.recordKrakenKill(player, true)
        assertTrue(achievements.isCompleted(player, 168))
        achievements.recordKrakenKill(player, true, true)
        repeat(3) { achievements.recordKrakenKill(player, true, true) }
        assertTrue(achievements.isCompleted(player, 169))
        assertEquals(6, achievements.points(player))
        assertEquals(2, player.vars["varbit.ca_total_tasks_completed_kraken_boss"])
    }

    @Test
    fun `permanent native state survives save load and refresh repairs stale derived points`() {
        val first = Player()
        repeat(10) { achievements.recordBarrowsChest(first, false) }
        repeat(19) { achievements.recordKrakenKill(first, true) }
        val restored = Player()
        for ((id, value) in first.vars.backing) {
            if (ServerCacheManager.getVarp(id)?.scope == VarpLifetime.Perm) {
                restored.vars.backing[id] = value
            }
        }
        VarPlayerIntMapSetter.set(restored, "varbit.ca_points", 999)
        achievements.refresh(restored)
        assertTrue(achievements.isCompleted(restored, 24))
        assertEquals(1, restored.vars["varbit.ca_points"])
        achievements.recordKrakenKill(restored, true)
        achievements.recordBarrowsChest(restored, false)
        assertEquals(4, achievements.points(restored))
        assertEquals(11, restored.vars["varp.ca_legitimate_barrows_chests"])
    }

    @Test
    fun `completion bit 31 and last native word retain adjacent bits`() {
        val player = Player()
        for (id in listOf(0, 31, 32, 654)) {
            val word = "varp.ca_task_completed_${id / 32}"
            VarPlayerIntMapSetter.set(player, word, player.vars[word] or (1 shl (id % 32)))
        }
        achievements.refresh(player)
        for (id in listOf(0, 31, 32, 654)) assertTrue(achievements.isCompleted(player, id))
        for (id in listOf(1, 30, 33, 653)) assertFalse(achievements.isCompleted(player, id))
        assertEquals(listOf(0, 31, 32, 654).sumOf { catalogue.task(it).tier }, achievements.points(player))
    }

    @Test
    fun `rank availability depends on points without manufacturing claimed rewards`() {
        val player = Player()
        for (task in catalogue.tasks.filter { it.tier == 1 }) {
            val word = "varp.ca_task_completed_${task.id / 32}"
            VarPlayerIntMapSetter.set(player, word, player.vars[word] or (1 shl (task.id % 32)))
        }
        achievements.refresh(player)
        assertEquals(41, achievements.points(player))
        assertEquals(1, achievements.earnedTier(player))
        assertEquals(1, player.vars["varbit.ca_tier_status_easy"])
        assertEquals(0, player.vars["varbit.ca_tier_status_medium"])
        assertEquals(41, player.vars["varbit.ca_total_tasks_completed_easy"])
    }

    @Test
    fun `group iron profiles match native exclusions without lowering unavailable boss thresholds`() {
        assertEquals(listOf(41, 169, 436, 1096, 1916, 2570), catalogue.thresholds(2))
        assertEquals(listOf(41, 169, 436, 1096, 1936, 2620), catalogue.thresholds(3))
        assertEquals(listOf(41, 169, 436, 1096, 1941, 2637), catalogue.thresholds(4))
        assertEquals(listOf(41, 169, 436, 1100, 1965, 2691), catalogue.thresholds(5))
        val player = Player()
        VarPlayerIntMapSetter.set(player, "varbit.ironman", 4)
        VarPlayerIntMapSetter.set(player, "varbit.gim_groupsize", 2)
        achievements.refresh(player)
        assertEquals(2570, player.vars["varbit.ca_threshold_grandmaster"])
        assertEquals(0, achievements.earnedTier(player))
    }

    @Test
    fun `native inverted popup preference controls notification without changing completion`() {
        for (setting in 0..1) {
            val messages = mutableListOf<Any>()
            val player = Player(object : Client<Any, Any> {
                override fun close() {}
                override fun write(message: Any) { messages += message }
                override fun read(player: Player) {}
                override fun flush() {}
                override fun flushHighPriority() {}
                override fun unregister(service: Any, player: Player) {}
            })
            VarPlayerIntMapSetter.set(player, "varbit.ca_task_popup", setting)
            repeat(20) { achievements.recordKrakenKill(player, true) }
            val before = messages.size
            PlayerNotifications.pulse(player)
            assertEquals(if (setting == 0) 1 else 0, messages.size - before)
            assertTrue(achievements.isCompleted(player, 168))
            assertEquals(3, achievements.points(player))
        }
    }

    @Test
    fun `full native completion fits all aggregates and excluded group tasks retain their bits`() {
        val player = Player()
        for (task in catalogue.tasks) {
            val word = "varp.ca_task_completed_${task.id / 32}"
            VarPlayerIntMapSetter.set(player, word, player.vars[word] or (1 shl (task.id % 32)))
        }
        achievements.refresh(player)
        assertEquals(2697, player.vars["varbit.ca_points"])
        assertEquals(6, achievements.earnedTier(player))
        assertEquals(166, player.vars["varbit.ca_total_tasks_completed_elite"])
        assertEquals(173, player.vars["varbit.ca_total_tasks_completed_master"])
        VarPlayerIntMapSetter.set(player, "varbit.ironman", 4)
        VarPlayerIntMapSetter.set(player, "varbit.gim_groupsize", 2)
        achievements.refresh(player)
        assertEquals(2570, player.vars["varbit.ca_points"])
        assertEquals(6, achievements.earnedTier(player))
        for (task in catalogue.tasks.filter { !catalogue.isAvailable(it, 2) }) {
            assertTrue(achievements.isCompleted(player, task.id))
        }
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
