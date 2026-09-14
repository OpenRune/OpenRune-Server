package org.rsmod.content.areas.city.draynor

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContext
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DraynorClosetKeyTest {
    @Test
    fun `a lost key is replaceable even when an older save says it was found`() {
        val access = access()
        access.player.attr[AttributeKey<Boolean>(persistenceKey = "ernest_found_closet_key")] = true
        assertTrue(DraynorManorSearch.findClosetKey(access))
        assertEquals(1, access.inv.count("obj.closet_key"))
        assertFalse(DraynorManorSearch.findClosetKey(access))
        assertEquals(1, access.inv.count("obj.closet_key"))

        access.inv.fillNulls()
        assertTrue(DraynorManorSearch.findClosetKey(access))
        assertEquals(1, access.inv.count("obj.closet_key"))
    }

    @Test
    fun `a key stored in the bank prevents digging up another`() {
        val access = access()
        access.bank[0] = InvObj("obj.closet_key")
        assertFalse(DraynorManorSearch.findClosetKey(access))
        assertEquals(0, access.inv.count("obj.closet_key"))
        assertEquals(1, access.bank.count("obj.closet_key"))

        access.bank[0] = null
        assertTrue(DraynorManorSearch.findClosetKey(access))
        assertEquals(1, access.inv.count("obj.closet_key"))
    }

    @Test
    fun `a full inventory can retry after making space`() {
        val access = access()
        for (slot in access.inv.indices) access.inv[slot] = InvObj("obj.spade")
        assertFalse(DraynorManorSearch.findClosetKey(access))
        assertEquals(0, access.inv.count("obj.closet_key"))

        access.inv[0] = null
        assertTrue(DraynorManorSearch.findClosetKey(access))
        assertEquals(1, access.inv.count("obj.closet_key"))
    }

    @Test
    fun `finding the tube does not prevent replacing the reusable closet key`() {
        val access = access()
        access.inv[0] = InvObj("obj.rubber_tube")
        assertTrue(DraynorManorSearch.findClosetKey(access))
        assertEquals(1, access.inv.count("obj.rubber_tube"))
        assertEquals(1, access.inv.count("obj.closet_key"))
    }

    private fun access(): ProtectedAccess {
        val player = Player()
        player.inv = player.invMap.getOrPut("inv.inv")
        val context =
            ProtectedAccessContext(
                getRandom = { error("Unexpected random access") },
                getEventBus = { EventBus() },
                getNpcList = { error("Unexpected NPC access") },
                getPlayerList = { error("Unexpected player list access") },
                getCollision = { error("Unexpected collision access") },
                getAreaChecker = { error("Unexpected area access") },
                getAlignment = { error("Unexpected dialogue access") },
                getLocInteractions = { error("Unexpected location interaction") },
                getNpcInteractions = { error("Unexpected NPC interaction") },
                getPlayerInteractions = { error("Unexpected player interaction") },
                getHeldInteractions = { error("Unexpected held interaction") },
                getWornInteractions = { error("Unexpected worn interaction") },
                getMusicPlayer = { error("Unexpected music player access") },
                getMarketPrices = { error("Unexpected market access") },
                getInstantHitProcessor = { error("Unexpected hit processing") },
                getTeleportValidator = { error("Unexpected teleport") },
                getHitModifier = { error("Unexpected hit modification") },
            )
        return ProtectedAccess(player, GameCoroutine(), context)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            val context = ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { context.startup() }
        }
    }
}
