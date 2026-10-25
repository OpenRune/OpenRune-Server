package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingSpellInventoryTest {
    @Test
    fun `full inventory fills ordinary vessels cans and seedlings once without consuming noted items`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        player.inv[0] = InvObj("obj.bucket_empty")
        player.inv[1] = InvObj("obj.clay")
        player.inv[2] = InvObj("obj.water_skin2")
        player.inv[3] = InvObj("obj.watering_can_3")
        player.inv[4] = InvObj(chain.dry)
        player.inv[5] = InvObj("obj.zeah_wateringcan")
        player.inv[6] = InvObj("obj.cert_bucket_empty", Int.MAX_VALUE)
        for (slot in 7..27) player.inv[slot] = InvObj("obj.spade")
        assertTrue(FarmingHumidify.hasTarget(player))
        FarmingHumidify.fill(player)
        assertEquals("obj.bucket_water".asRSCM(), player.inv[0]?.id)
        assertEquals("obj.softclay".asRSCM(), player.inv[1]?.id)
        assertEquals("obj.water_skin4".asRSCM(), player.inv[2]?.id)
        assertEquals("obj.watering_can_8".asRSCM(), player.inv[3]?.id)
        assertEquals(chain.wet.id, player.inv[4]?.id)
        assertEquals("obj.zeah_wateringcan".asRSCM(), player.inv[5]?.id)
        assertEquals(1000, player.vars["varbit.zeah_wateringcan_charges"])
        assertEquals(Int.MAX_VALUE, player.inv[6]?.count)
        assertEquals("obj.cert_bucket_empty".asRSCM(), player.inv[6]?.id)
        for (slot in 7..27) assertEquals("obj.spade".asRSCM(), player.inv[slot]?.id)
        assertFalse(FarmingHumidify.hasTarget(player))
        val after = player.inv.toList()
        FarmingHumidify.fill(player)
        assertEquals(after, player.inv.toList())
    }

    @Test
    fun `a lone Gricoller can refills even when no ordinary vessel needs replacing`() {
        val player = player()
        player.inv[0] = InvObj("obj.zeah_wateringcan")
        assertTrue(FarmingHumidify.hasTarget(player))
        FarmingHumidify.fill(player)
        assertEquals("obj.zeah_wateringcan".asRSCM(), player.inv[0]?.id)
        assertEquals(1000, player.vars["varbit.zeah_wateringcan_charges"])
        assertFalse(FarmingHumidify.hasTarget(player))
    }

    @Test
    fun `noted vessels and watered saplings alone do not allow another charged cast`() {
        val player = player()
        player.inv[0] = InvObj("obj.cert_bucket_empty", 100)
        player.inv[1] = InvObj(FarmingSaplings.chains.first().wet)
        player.inv[2] = InvObj("obj.watering_can_8")
        assertFalse(FarmingHumidify.hasTarget(player))
    }

    @Test
    fun `tome confirmation cannot consume a second copy after unlocking`() {
        val player = player()
        for (slot in 0..27) player.inv[slot] = InvObj("obj.spade")
        player.inv[0] = InvObj("obj.fossil_mine_ultrasoil_book")
        player.inv[1] = InvObj("obj.fossil_mine_ultrasoil_book")
        VarPlayerIntMapSetter.set(player, "varp.fossil_perm_transmit", 7)
        assertTrue(FarmingAshTomeScript.unlock(player, 0))
        assertEquals(null, player.inv[0])
        assertEquals(1, player.vars[FarmingSpellsScript.ULTRA_UNLOCK])
        assertEquals(7, player.vars["varp.fossil_perm_transmit"] and 7)
        assertFalse(FarmingAshTomeScript.unlock(player, 1))
        assertEquals("obj.fossil_mine_ultrasoil_book".asRSCM(), player.inv[1]?.id)
        for (slot in 2..27) assertEquals("obj.spade".asRSCM(), player.inv[slot]?.id)
    }

    @Test
    fun `vanished selected tome cannot consume a different inventory copy`() {
        val player = player()
        player.inv[0] = InvObj("obj.spade")
        player.inv[1] = InvObj("obj.fossil_mine_ultrasoil_book")
        assertFalse(FarmingAshTomeScript.unlock(player, 0))
        assertEquals(0, player.vars[FarmingSpellsScript.ULTRA_UNLOCK])
        assertEquals("obj.spade".asRSCM(), player.inv[0]?.id)
        assertEquals("obj.fossil_mine_ultrasoil_book".asRSCM(), player.inv[1]?.id)
    }

    private fun player(): Player = Player().apply {
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        VarPlayerIntMapSetter.set(this, "varp.farming_growth_offset", 32)
        VarPlayerIntMapSetter.set(this, "varp.farming_sapling_clock", Int.MAX_VALUE)
    }

    companion object {
        private val RESTORED = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
