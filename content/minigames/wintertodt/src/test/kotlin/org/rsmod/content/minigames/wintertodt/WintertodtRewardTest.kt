package org.rsmod.content.minigames.wintertodt

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactions
import org.rsmod.api.random.GameRandom
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class WintertodtRewardTest {
    @Test
    fun `woodcutting resources use cumulative chances`() {
        assertEquals(
            "obj.magic_logs",
            WintertodtRewards.chooseResourceRow(99, "logs", 0.0)?.obj?.internalName,
        )
        assertEquals(
            "obj.mahogany_logs",
            WintertodtRewards.chooseResourceRow(99, "logs", 0.7)?.obj?.internalName,
        )
        assertEquals(
            "obj.oak_logs",
            WintertodtRewards.chooseResourceRow(99, "logs", 0.9999)?.obj?.internalName,
        )
    }

    @Test
    fun `low level rolls still use the full cumulative range`() {
        assertEquals(
            "obj.maple_logs",
            WintertodtRewards.chooseResourceRow(1, "logs", 0.0)?.obj?.internalName,
        )
        assertEquals(
            "obj.oak_logs",
            WintertodtRewards.chooseResourceRow(1, "logs", 0.9999)?.obj?.internalName,
        )
    }

    @Test
    fun `phoenix takes priority over every other unique`() {
        val reward = WintertodtRewards.roll(player(), FixedRandom(mutableListOf(1, 1, 1, 1, 1, 1)))

        assertEquals("obj.phoenixpet", reward.item)
        assertEquals(1, reward.count)
    }

    @Test
    fun `dragon axe is rolled after a missing phoenix`() {
        val reward = WintertodtRewards.roll(player(), FixedRandom(mutableListOf(2, 1)))

        assertEquals("obj.dragon_axe", reward.item)
    }

    @Test
    fun `tome is rolled after missing earlier uniques`() {
        val reward = WintertodtRewards.roll(player(), FixedRandom(mutableListOf(2, 2, 1)))

        assertEquals("obj.tome_of_fire_uncharged", reward.item)
    }

    @Test
    fun `three banked warm gloves are replaced by a magic seed`() {
        val player = player()
        player.bank()[0] = InvObj("obj.pyromancer_gloves", 3)

        val reward = WintertodtRewards.roll(player, FixedRandom(mutableListOf(2, 2, 2, 1)))

        assertEquals("obj.magic_tree_seed", reward.item)
    }

    @Test
    fun `three torches across hands and bank are replaced by torstol seeds`() {
        val player = player()
        player.inv[0] = InvObj("obj.wint_torch", 1)
        player.worn[3] = InvObj("obj.wint_torch", 1)
        player.bank()[0] = InvObj("obj.wint_torch_offhand", 1)

        val reward =
            WintertodtRewards.roll(player, FixedRandom(mutableListOf(2, 2, 2, 2, 1, 3)))

        assertEquals("obj.torstol_seed", reward.item)
        assertEquals(3, reward.count)
    }

    @Test
    fun `banked pyro piece is skipped when choosing the next outfit piece`() {
        val player = player()
        player.bank()[0] = InvObj("obj.pyromancer_top", 1)

        val reward =
            WintertodtRewards.roll(player, FixedRandom(mutableListOf(2, 2, 2, 2, 2, 1)))

        assertEquals("obj.pyromancer_hood", reward.item)
    }

    @Test
    fun `owned counts inventory worn bank and off hand torches`() {
        val player = player()
        player.inv[0] = InvObj("obj.pyromancer_gloves", 1)
        player.worn[4] = InvObj("obj.pyromancer_gloves", 1)
        player.bank()[0] = InvObj("obj.pyromancer_gloves", 1)
        assertEquals(3, WintertodtRewards.owned(player, "obj.pyromancer_gloves"))

        player.inv[1] = InvObj("obj.wint_torch", 1)
        player.worn[3] = InvObj("obj.wint_torch", 1)
        player.bank()[1] = InvObj("obj.wint_torch_offhand", 1)
        assertEquals(3, WintertodtRewards.owned(player, "obj.wint_torch"))
    }

    @Test
    fun `pet delivery does not insert the pet into inventory`() {
        val player = player()
        var delivered: String? = null

        val granted =
            grantCartReward(player, WintertodtReward("obj.phoenixpet", 1)) { _, item ->
                delivered = item
                true
            }

        assertTrue(granted)
        assertEquals("obj.phoenixpet", delivered)
        assertEquals(0, player.inv.count("obj.phoenixpet"))
    }

    @Test
    fun `non pet rewards fall back to inventory insertion`() {
        val player = player()

        val granted =
            grantCartReward(player, WintertodtReward("obj.dragon_axe", 1)) { _, _ -> false }

        assertTrue(granted)
        assertEquals(1, player.inv.count("obj.dragon_axe"))
    }

    private fun player(): Player =
        Player().apply {
            client = SilentClient()
            uuid = 917L
            slotId = 1
            assignUid()
            currentMapClock = 100
            processedMapClock = 100
            inv =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                    arrayOfNulls(28),
                )
            worn =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                    arrayOfNulls(14),
                )
            invMap["inv.bank"] =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.bank".asRSCM())),
                    arrayOfNulls(800),
                )
        }

    private fun Player.bank(): Inventory = invMap.getValue("inv.bank")

    private class FixedRandom(
        private val ints: MutableList<Int> = mutableListOf(),
        private val doubles: MutableList<Double> = mutableListOf(),
    ) : GameRandom {
        override fun of(maxExclusive: Int): Int = ints.removeAt(0)

        override fun of(minInclusive: Int, maxInclusive: Int): Int = ints.removeAt(0)

        override fun randomDouble(): Double = doubles.removeAt(0)
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
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
            val field =
                Class.forName("org.rsmod.api.invtx.InvTransactionsScriptKt")
                    .getDeclaredField("cachedInventoryTransactions")
                    .apply { isAccessible = true }
            field.set(null, InvTransactions.from())
            val storageField =
                Class.forName("org.rsmod.api.invtx.VirtualInvTransactionsKt")
                    .getDeclaredField("cachedPlayerItemStorage")
                    .apply { isAccessible = true }
            storageField.set(null, PlayerItemStorage(emptySet()))
        }
    }
}
