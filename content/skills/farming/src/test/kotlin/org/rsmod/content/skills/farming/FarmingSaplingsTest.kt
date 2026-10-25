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
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.generic.locs.water.WateringCans
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
class FarmingSaplingsTest {
    @Test
    fun `one seed and pot produce one sapling at the boostable level with a full inventory`() {
        val chain = FarmingSaplings.chains.first()
        val player = player()
        player.inv[0] = InvObj(chain.seed, 2)
        player.inv[1] = InvObj("obj.plantpot_compost")
        player.inv[2] = InvObj("obj.gardening_trowel")
        for (slot in 3..27) player.inv[slot] = InvObj("obj.spade")
        player.statMap.setCurrentLevel("stat.farming", (chain.level - 1).toByte())
        assertFalse(FarmingSaplings.sow(player, chain, 0, 1))
        assertEquals(2, player.inv[0]?.count)
        player.statMap.setCurrentLevel("stat.farming", chain.level.toByte())
        assertTrue(FarmingSaplings.sow(player, chain, 0, 1))
        assertEquals(1, player.inv[0]?.count)
        assertEquals(chain.dry.id, player.inv[1]?.id)
        assertFalse(FarmingSaplings.sow(player, chain, 0, 1))
        assertEquals(1, player.inv[0]?.count)
        player.inv[3] = InvObj("obj.watering_can_1")
        assertTrue(FarmingSaplings.water(player, 1, "obj.watering_can_1", 3, NOW))
        assertEquals(chain.wet.id, player.inv[1]?.id)
        FarmingSaplings.grow(player, NOW + 300_000)
        assertEquals(chain.sapling.id, player.inv[1]?.id)
        assertEquals(0, player.statMap.getXP("stat.farming"))
    }

    @Test
    fun `wrong trowel missing seed and noted pot change nothing`() {
        val chain = FarmingSaplings.chains.first()
        val player = player()
        player.statMap.setCurrentLevel("stat.farming", 99)
        player.inv[0] = InvObj(chain.seed)
        player.inv[1] = InvObj("obj.plantpot_compost")
        player.inv[2] = InvObj("obj.trowel")
        assertFalse(FarmingSaplings.sow(player, chain, 0, 1))
        player.inv[2] = InvObj("obj.gardening_trowel")
        player.inv[0] = null
        assertFalse(FarmingSaplings.sow(player, chain, 0, 1))
        assertEquals("obj.plantpot_compost".asRSCM(), player.inv[1]?.id)
        player.inv[0] = InvObj(chain.seed)
        player.inv[1] = InvObj("obj.cert_plantpot_compost")
        assertFalse(FarmingSaplings.sow(player, chain, 0, 1))
        assertEquals(chain.seed.id, player.inv[0]?.id)
    }

    @Test
    fun `selected standard can consumes its exact dose and empty can does not borrow another`() {
        for (doses in 0..8) {
            val player = player()
            player.inv[0] = InvObj("obj.watering_can_$doses")
            player.inv[1] = InvObj("obj.watering_can_8")
            assertEquals(doses > 0, WateringCans.consume(player, "obj.watering_can_$doses"))
            assertEquals("obj.watering_can_${maxOf(0, doses - 1)}".asRSCM(), player.inv[0]?.id)
            assertEquals("obj.watering_can_8".asRSCM(), player.inv[1]?.id)
        }
        assertFalse(WateringCans.consume(player(), "obj.cert_watering_can_0"))
    }

    @Test
    fun `Gricoller copies share depletion refill and preserve unrelated parent bits`() {
        val player = player()
        player.inv[0] = InvObj("obj.zeah_wateringcan")
        player.inv[1] = InvObj("obj.zeah_wateringcan")
        val otherBits = (1 shl 20) or 1
        set(player, "varp.prayguild", otherBits or (1 shl 1))
        assertTrue(WateringCans.consume(player, "obj.zeah_wateringcan"))
        assertFalse(WateringCans.consume(player, "obj.zeah_wateringcan"))
        assertEquals(otherBits, player.vars["varp.prayguild"])
        assertTrue(WateringCans.refill(player, "obj.zeah_wateringcan"))
        assertEquals(1000, player.vars["varbit.zeah_wateringcan_charges"])
        assertEquals(otherBits, player.vars["varp.prayguild"] and (1023 shl 1).inv())
        assertFalse(WateringCans.refill(player, "obj.zeah_wateringcan"))
        val chain = FarmingSaplings.chains.first()
        player.inv[2] = InvObj(chain.dry)
        set(player, "varbit.zeah_wateringcan_charges", 1)
        assertTrue(FarmingSaplings.water(player, 2, "obj.zeah_wateringcan", 1, NOW))
        assertEquals(chain.wet.id, player.inv[2]?.id)
        assertEquals(0, player.vars["varbit.zeah_wateringcan_charges"])
        assertFalse(FarmingSaplings.water(player, 2, "obj.zeah_wateringcan", 1, NOW))
        FarmingSaplings.humidify(player)
        assertEquals(1000, player.vars["varbit.zeah_wateringcan_charges"])
        assertEquals(otherBits, player.vars["varp.prayguild"] and (1023 shl 1).inv())
    }

    @Test
    fun `watered pot matures at next aligned boundary not the missed previous one`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        player.inv[0] = InvObj(chain.wet)
        player.inv[1] = InvObj(chain.dry)
        player.inv[2] = InvObj("obj.watering_can_1")
        set(player, "varp.farming_sapling_clock", 1999)
        assertTrue(FarmingSaplings.water(player, 1, "obj.watering_can_1", 2, NOW))
        assertEquals(chain.sapling.id, player.inv[0]?.id)
        assertEquals(chain.wet.id, player.inv[1]?.id)
        FarmingSaplings.grow(player, NOW + 299_999)
        assertEquals(chain.wet.id, player.inv[1]?.id)
        FarmingSaplings.grow(player, NOW + 300_000)
        assertEquals(chain.sapling.id, player.inv[1]?.id)
        assertEquals(0, player.inv[1]?.vars)
        assertFalse(FarmingSaplings.water(player, 1, "obj.watering_can_1", 2, NOW + 300_000))
    }

    @Test
    fun `bank transfer and offline reload retain next-cycle eligibility and merge saplings`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        player.inv[0] = InvObj(chain.dry)
        player.inv[1] = InvObj("obj.watering_can_1")
        assertTrue(FarmingSaplings.water(player, 0, "obj.watering_can_1", 1, NOW))
        val bank = player.invMap.getOrPut("inv.bank")
        bank[0] = InvObj(chain.sapling, 7)
        assertTrue(player.invTransfer(player.inv, 0, 1, bank).success)
        val loaded = player()
        loaded.invMap["inv.bank"] = Inventory(bank.type, bank.objs.copyOf())
        set(loaded, "varp.farming_sapling_clock", player.vars["varp.farming_sapling_clock"])
        FarmingSaplings.grow(loaded, NOW + 86_400_000)
        assertEquals(8, loaded.invMap.getValue("inv.bank").physicalCount(chain.sapling.internalName))
        assertEquals(0, loaded.invMap.getValue("inv.bank").physicalCount(chain.wet.internalName))
        FarmingSaplings.grow(loaded, NOW + 86_700_000)
        assertEquals(8, loaded.invMap.getValue("inv.bank").physicalCount(chain.sapling.internalName))
    }

    @Test
    fun `bank stack overflow conserves the wet pot and retries at the next tick`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        val bank = player.invMap.getOrPut("inv.bank")
        bank[0] = InvObj(chain.sapling, Int.MAX_VALUE)
        bank[1] = InvObj(chain.wet)
        set(player, "varp.farming_sapling_clock", 1999)
        FarmingSaplings.grow(player, NOW)
        assertEquals(Int.MAX_VALUE, bank[0]?.count)
        assertEquals(chain.wet.id, bank[1]?.id)
        bank[0] = InvObj(chain.sapling, Int.MAX_VALUE - 1)
        FarmingSaplings.grow(player, NOW + 300_000)
        assertEquals(Int.MAX_VALUE, bank[0]?.count)
        assertEquals(null, bank[1])
    }

    @Test
    fun `container refills are atomic with stale input and full inventory`() {
        val player = player()
        player.inv[0] = InvObj("obj.watering_can_0")
        player.inv[1] = InvObj("obj.jug_water")
        for (slot in 2..27) player.inv[slot] = InvObj("obj.spade")
        assertTrue(WateringCans.refillFrom(player, "obj.watering_can_0", 0, "obj.jug_water", 1))
        assertEquals("obj.watering_can_8".asRSCM(), player.inv[0]?.id)
        assertEquals("obj.jug_empty".asRSCM(), player.inv[1]?.id)
        assertFalse(WateringCans.refillFrom(player, "obj.watering_can_0", 0, "obj.jug_water", 1))
        assertEquals("obj.watering_can_8".asRSCM(), player.inv[0]?.id)
    }

    @Test
    fun `water source helper refills the selected slot rather than the first matching can`() {
        val player = player()
        player.inv[0] = InvObj("obj.watering_can_0")
        player.inv[1] = InvObj("obj.watering_can_0")
        assertTrue(WateringCans.refill(player, "obj.watering_can_0", 1))
        assertEquals("obj.watering_can_0".asRSCM(), player.inv[0]?.id)
        assertEquals("obj.watering_can_8".asRSCM(), player.inv[1]?.id)
        assertFalse(WateringCans.refill(player, "obj.watering_can_0", 1))
    }

    @Test
    fun `Humidify waters every dry seedling and fills cans without maturing the new pots`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        player.inv[0] = InvObj(chain.dry)
        player.inv[1] = InvObj(chain.dry)
        player.inv[2] = InvObj("obj.watering_can_0")
        player.inv[3] = InvObj("obj.zeah_wateringcan")
        assertTrue(FarmingSaplings.hasHumidifyTarget(player))
        FarmingSaplings.humidify(player)
        assertFalse(FarmingSaplings.hasHumidifyTarget(player))
        assertEquals(chain.wet.id, player.inv[0]?.id)
        assertEquals(chain.wet.id, player.inv[1]?.id)
        assertEquals("obj.watering_can_8".asRSCM(), player.inv[2]?.id)
        assertEquals(1000, player.vars["varbit.zeah_wateringcan_charges"])
        val cycle = player.vars["varp.farming_sapling_clock"]
        FarmingSaplings.grow(player, cycle * 300_000L + 299_999)
        assertEquals(chain.wet.id, player.inv[0]?.id)
        FarmingSaplings.grow(player, (cycle + 1) * 300_000L)
        assertEquals(chain.sapling.id, player.inv[0]?.id)
        assertEquals(chain.sapling.id, player.inv[1]?.id)
    }

    @Test
    fun `bank maturation moves new saplings to main and preserves later custom tab contents`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        val bank = player.invMap.getOrPut("inv.bank")
        bank[0] = InvObj(chain.wet)
        bank[1] = InvObj("obj.spade")
        bank[2] = InvObj(chain.wet)
        set(player, "varbit.bank_tab_1", 1)
        set(player, "varbit.bank_tab_2", 2)
        set(player, "varbit.bank_currenttab", 2)
        set(player, "varp.farming_sapling_clock", 1999)
        FarmingSaplings.grow(player, NOW)
        assertEquals("obj.spade".asRSCM(), bank[0]?.id)
        assertEquals(chain.sapling.id, bank[1]?.id)
        assertEquals(2, bank[1]?.count)
        assertEquals(1, player.vars["varbit.bank_tab_1"])
        assertEquals(0, player.vars["varbit.bank_tab_2"])
        assertEquals(1, player.vars["varbit.bank_tab_main"])
        assertEquals(1, player.vars["varbit.bank_currenttab"])
    }

    @Test
    fun `bank maturation restores an existing sapling placeholder without creating duplicate slots`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        val bank = player.invMap.getOrPut("inv.bank")
        val placeholder = checkNotNull(ServerCacheManager.getItem(chain.sapling.placeholderLink))
        bank[0] = InvObj(placeholder)
        bank[1] = InvObj(chain.wet, 5)
        set(player, "varp.farming_sapling_clock", 1999)
        FarmingSaplings.grow(player, NOW)
        assertEquals(chain.sapling.id, bank[0]?.id)
        assertEquals(5, bank[0]?.count)
        assertEquals(null, bank[1])
    }

    @Test
    fun `busy player and stacked bank seedlings mature without losing a single pot`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        player.inv[0] = InvObj(chain.wet)
        val bank = player.invMap.getOrPut("inv.bank")
        bank[0] = InvObj(chain.wet, 28)
        set(player, "varp.farming_sapling_clock", 1999)
        player.delay = Int.MAX_VALUE
        FarmingSaplings.grow(player, NOW)
        assertEquals(chain.sapling.id, player.inv[0]?.id)
        assertEquals(28, bank.physicalCount(chain.sapling.internalName))
        assertEquals(0, bank.physicalCount(chain.wet.internalName))
    }

    @Test
    fun `failed selected-can watering preserves both seedlings and every can`() {
        val player = player()
        val chain = FarmingSaplings.chains.first()
        player.inv[0] = InvObj(chain.dry)
        player.inv[1] = InvObj("obj.watering_can_0")
        player.inv[2] = InvObj("obj.watering_can_8")
        assertFalse(FarmingSaplings.water(player, 0, "obj.watering_can_0", 1, NOW))
        assertFalse(FarmingSaplings.water(player, 0, "obj.watering_can_1", 2, NOW))
        assertEquals(chain.dry.id, player.inv[0]?.id)
        assertEquals("obj.watering_can_0".asRSCM(), player.inv[1]?.id)
        assertEquals("obj.watering_can_8".asRSCM(), player.inv[2]?.id)
        player.inv[0] = null
        assertFalse(FarmingSaplings.water(player, 0, "obj.watering_can_8", 2, NOW))
        assertEquals("obj.watering_can_8".asRSCM(), player.inv[2]?.id)
    }

    @Test
    fun `clock is negative offset aligned and zero offset marker survives repeated access`() {
        assertEquals(1L, FarmingClock.cycle(31, 40, 9 * 60_000L))
        assertEquals(0L, FarmingClock.cycle(31, 40, 9 * 60_000L - 1))
        val player = player()
        assertEquals(0, FarmingClock.offset(player))
        assertEquals(0, FarmingClock.offset(player))
        assertEquals(32, player.vars["varp.farming_growth_offset"])
    }

    private fun player(): Player = Player().apply {
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        set(this, "varp.farming_growth_offset", 32)
    }

    private fun set(player: Player, key: String, value: Int) = VarPlayerIntMapSetter.set(player, key, value)

    companion object {
        private const val NOW = 600_000_000L
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
