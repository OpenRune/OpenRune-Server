package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import dev.openrune.types.InventoryServerType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.content.minigames.motherlodemine.MotherlodeMiningTest.Fixture
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeCrateScript
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
class MotherlodeCrateTest {
    @Test fun `hammer crate grants a hammer when none is carried`() {
        val f = Fixture()
        val crate = f.loc(MotherlodeConstants.CRATE_LOC, HAMMER_CRATE)
        val player = cratePlayer(f, HAMMER_CRATE)
        search(f, crate, player)
        assertEquals(1, player.inv.count(MotherlodeConstants.OBJ_HAMMER))
    }

    @Test fun `hammer crate refuses a second hammer`() {
        val f = Fixture()
        val crate = f.loc(MotherlodeConstants.CRATE_LOC, HAMMER_CRATE)
        val player = cratePlayer(f, HAMMER_CRATE)
        player.inv[0] = InvObj(MotherlodeConstants.OBJ_HAMMER)
        search(f, crate, player)
        assertEquals(1, player.inv.count(MotherlodeConstants.OBJ_HAMMER))
    }

    @Test fun `pickaxe crate grants a bronze pickaxe when none is carried`() {
        val f = Fixture()
        val crate = f.loc(MotherlodeConstants.CRATE_LOC, PICKAXE_CRATE)
        val player = cratePlayer(f, PICKAXE_CRATE)
        search(f, crate, player)
        assertEquals(1, player.inv.count(MotherlodeConstants.OBJ_BRONZE_PICKAXE))
    }

    @Test fun `pickaxe crate refuses a bronze pickaxe when any pickaxe is carried`() {
        val f = Fixture()
        val crate = f.loc(MotherlodeConstants.CRATE_LOC, PICKAXE_CRATE)
        val player = cratePlayer(f, PICKAXE_CRATE)
        player.inv[0] = InvObj("obj.rune_pickaxe")
        search(f, crate, player)
        assertEquals(0, player.inv.count(MotherlodeConstants.OBJ_BRONZE_PICKAXE))
        assertEquals(1, player.inv.count("obj.rune_pickaxe"))
    }

    private fun cratePlayer(f: Fixture, crate: CoordGrid) =
        Player().apply {
            coords = crate.translate(1, 0)
            previousCoords = coords
            currentMapClock = 100
            processedMapClock = 100
            inv = invMap.getOrPut("inv.inv")
            worn = Inventory(InventoryServerType(size = 14), arrayOfNulls(14))
        }

    private fun search(f: Fixture, crate: BoundLocInfo, player: Player) {
        with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { f.context.startup() }
        val script = MotherlodeCrateScript()
        with(script) { f.context.startup() }
        val context =
            ProtectedAccessContextFactory.empty().copy(getEventBus = { f.events })
        val type = checkNotNull(ServerCacheManager.getObject(crate.id))
        val coroutine = player.launch {
            val access = ProtectedAccess(player, this, context)
            f.events.publish(access, LocEvents.Op1(crate, crate, type))
        }
        repeat(4) {
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            if (coroutine.isSuspended) coroutine.advance()
        }
        assertTrue(coroutine.isIdle)
    }

    private companion object {
        private val HAMMER_CRATE = CoordGrid(3752, 5674, 0)
        private val PICKAXE_CRATE = CoordGrid(3755, 5665, 0)

        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(241).close()
        }
    }
}
