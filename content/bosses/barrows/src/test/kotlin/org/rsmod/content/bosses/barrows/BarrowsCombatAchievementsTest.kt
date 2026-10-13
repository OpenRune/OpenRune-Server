package org.rsmod.content.bosses.barrows

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combatachievements.CombatAchievementCatalogue
import org.rsmod.api.combatachievements.CombatAchievements
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifCloseModal
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.route.BoundValidator
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
class BarrowsCombatAchievementsTest {
    @Test
    fun `native chest search records all brothers once and replay cannot award another chest`() {
        val f = Fixture()
        BarrowsBrother.entries.forEach { f.player.setSlain(it, true) }
        f.operate("loc.barrows_stone_chest_open")
        assertTrue(f.player.barrowsLooted)
        assertEquals(1, f.player.chestCount)
        verify(f.achievements).recordBarrowsChest(f.player, true)
        f.closeRewards()
        f.operate("loc.barrows_stone_chest_open")
        assertEquals(1, f.player.chestCount)
        verifyNoMoreInteractions(f.achievements)
    }

    @Test
    fun `partial native chest reports its actual brother state while opening never awards a chest`() {
        val f = Fixture()
        f.player.setSlain(BarrowsBrother.DHAROK, true)
        f.operate("loc.barrows_stone_chest_closed")
        verifyNoInteractions(f.achievements)
        f.operate("loc.barrows_stone_chest_open")
        verify(f.achievements).recordBarrowsChest(f.player, false)
    }

    @Test
    fun `administrator loot simulation cannot reach achievement producer or mutate chest progress`() {
        val f = Fixture()
        BarrowsTestLoot(mock(ObjRepository::class.java), DefaultGameRandom(2)).generate(f.player, 2)
        assertFalse(f.player.barrowsLooted)
        assertEquals(0, f.player.chestCount)
        verifyNoInteractions(f.achievements)
    }

    @Test
    fun `native chest interaction reaches persistent task completion and points`() {
        val achievements = CombatAchievements(CombatAchievementCatalogue())
        val f = Fixture(achievements)
        f.player.chestCount = 5000
        repeat(9) {
            f.player.barrowsLooted = false
            f.operate("loc.barrows_stone_chest_open")
            f.closeRewards()
        }
        assertFalse(achievements.isCompleted(f.player, 24))
        f.player.barrowsLooted = false
        f.operate("loc.barrows_stone_chest_open")
        assertTrue(achievements.isCompleted(f.player, 24))
        assertEquals(1, f.player.vars["varbit.ca_points"])
        assertEquals(10, f.player.vars["varp.ca_legitimate_barrows_chests"])
    }

    private class Fixture(val achievements: CombatAchievements = mock(CombatAchievements::class.java)) {
        val bus = EventBus()
        val player = Player().apply {
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            coords = CoordGrid(3552, 9694)
        }
        private val locs = LocInteractions(mock(BoundValidator::class.java), bus)
        private val accessContext = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { bus },
            getRandom = { DefaultGameRandom(1) },
        )

        init {
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            val script = BarrowsScript(
                mock(CollectionLog::class.java), bus, mock(ObjRepository::class.java),
                mock(ObjChargeManager::class.java), mock(BarrowsNpcSpawner::class.java),
                mock(BarrowsDoors::class.java), mock(DropTableRegistry::class.java), achievements,
            )
            with(script) { scripts.startup() }
        }

        fun operate(symbol: String) {
            val type = checkNotNull(ServerCacheManager.getObject(symbol.asRSCM()))
            val loc = BoundLocInfo(LocInfo(2, player.coords, LocEntity(type.id, 10, 0)), type)
            val event = checkNotNull(locs.opTrigger(player, loc, InteractionOp.Op1))
            assertTrue(ProtectedAccessLauncher.withProtectedAccess(player, accessContext) {
                assertTrue(bus.publish(this, event))
            })
        }

        fun closeRewards() {
            assertFalse(player.ui.modals.isEmpty())
            player.ifCloseModal("interface.barrows_reward", bus)
            assertTrue(player.ui.modals.isEmpty())
        }
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
