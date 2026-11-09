package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import io.mockk.every
import io.mockk.mockk
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.enums.GiantsFoundryEnums
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.random.GameRandom
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class FoundryRewardTransactionsTest {
    @Test
    fun `full ultimate inventory leaves hand-in retryable without offering another commission`() {
        val f = Fixture(ultimate = true)
        f.fill(f.player.inv)
        val before = f.swordState()
        repeat(2) {
            f.handIn()
            f.finish()
            assertEquals(before, f.swordState())
            assertEquals(0, f.player.inv.count("obj.coins"))
            assertFalse(f.offeredCommission)
            assertTrue(f.output().contains("enough room"))
        }
        f.player.inv[0] = null
        f.handIn()
        f.finish()
        f.assertReward(inBank = false)
        f.handIn()
        f.finish()
        f.assertReward(inBank = false)
    }

    @Test
    fun `ultimate coin overflow is all or nothing including exact stack boundary`() {
        val f = Fixture(ultimate = true)
        val initial = Int.MAX_VALUE - COINS + 1
        f.player.inv[0] = InvObj("obj.coins", initial)
        val before = f.swordState()
        f.handIn()
        f.finish()
        assertEquals(before, f.swordState())
        assertEquals(initial, f.player.inv.count("obj.coins"))
        f.player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE - COINS)
        f.handIn()
        f.finish()
        f.assertReward(inBank = false, coins = Int.MAX_VALUE)
        f.handIn()
        f.finish()
        f.assertReward(inBank = false, coins = Int.MAX_VALUE)
    }

    @Test
    fun `normal bank payment succeeds with a full inventory`() {
        val f = Fixture()
        f.fill(f.player.inv)
        f.handIn()
        f.finish()
        f.assertReward(inBank = true)
        assertTrue(f.output().contains("deposits 50,460 coins"))
        f.handIn()
        f.finish()
        f.assertReward(inBank = true)
    }

    @Test
    fun `full bank and inventory refuse payment then inventory fallback succeeds once`() {
        val f = Fixture()
        f.fill(f.bank)
        f.fill(f.player.inv)
        val before = f.swordState()
        f.handIn()
        f.finish()
        assertEquals(before, f.swordState())
        assertEquals(0, f.bank.count("obj.coins"))
        assertFalse(f.offeredCommission)
        f.player.inv[0] = null
        f.handIn()
        f.finish()
        f.assertReward(inBank = false)
        f.handIn()
        f.finish()
        f.assertReward(inBank = false)
    }

    @Test
    fun `overflowing bank and inventory stacks cannot partially pay either destination`() {
        val f = Fixture()
        f.bank[0] = InvObj("obj.coins", Int.MAX_VALUE - COINS + 1)
        f.player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE)
        val before = f.swordState()
        f.handIn()
        f.finish()
        assertEquals(before, f.swordState())
        assertEquals(Int.MAX_VALUE - COINS + 1, f.bank.count("obj.coins"))
        assertEquals(Int.MAX_VALUE, f.player.inv.count("obj.coins"))
        f.bank[0] = InvObj("obj.coins", Int.MAX_VALUE - COINS)
        f.handIn()
        f.finish()
        f.assertReward(inBank = true, coins = Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `preform disappearing during the hand-over dialogue cannot grant a reward`() {
        val f = Fixture()
        f.handIn()
        assertFalse(f.coroutine.isIdle)
        f.player.worn[Wearpos.RightHand.slot] = null
        val before = f.swordState()
        f.finish()
        assertEquals(before, f.swordState())
        assertEquals(0, f.bank.count("obj.coins"))
        assertFalse(f.offeredCommission)
    }

    @Test
    fun `ore pack replaces its own slot with exactly thirty ores and repeated click grants nothing`() {
        val f = Fixture()
        f.fill(f.player.inv)
        f.player.inv[0] = InvObj(PACK, 1)
        f.openPack()
        assertEquals(0, f.player.inv.count(PACK))
        assertEquals(30, f.player.inv.count("obj.cert_coal"))
        val before = f.player.inv.objs.toList()
        f.openPack()
        assertEquals(before, f.player.inv.objs.toList())
    }

    @Test
    fun `overflowing noted ore leaves pack and every rolled reward untouched`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(PACK, 1)
        f.player.inv[1] = InvObj("obj.cert_coal", Int.MAX_VALUE)
        val before = f.player.inv.objs.toList()
        repeat(2) {
            f.openPack()
            assertEquals(before, f.player.inv.objs.toList())
        }
        f.player.inv[1] = InvObj("obj.cert_coal", Int.MAX_VALUE - 30)
        f.openPack()
        assertEquals(Int.MAX_VALUE, f.player.inv.count("obj.cert_coal"))
        assertEquals(0, f.player.inv.count(PACK))
    }

    @Test
    fun `multiple rolled ore types cannot partially replace a pack without enough space`() {
        val f = Fixture()
        val firstWeight = GiantsFoundryEnums.giants_foundry_ore_pack_weights
            .mapNotNull { (_, weight) -> weight }.max()
        every { f.random.of(any<Int>(), any<Int>()) } returnsMany listOf(1, firstWeight + 1)
        f.fill(f.player.inv)
        f.player.inv[0] = InvObj(PACK, 1)
        val before = f.player.inv.objs.toList()
        f.openPack()
        assertEquals(before, f.player.inv.objs.toList())
    }

    @Test
    fun `missing ore pack never grants free ores even when there is space`() {
        val f = Fixture()
        repeat(2) { f.openPack() }
        assertTrue(f.player.inv.isEmpty())
        assertFalse(f.output().contains("You open the pack and find"))
    }

    @Test
    fun `overflow on a later ore roll rolls back previously added ores and pack deletion`() {
        val f = Fixture()
        val ores = GiantsFoundryEnums.giants_foundry_ore_pack_weights
            .mapNotNull { (ore, weight) -> weight?.let { ore to it } }
            .sortedByDescending { it.second }
        val secondNote = checkNotNull(ServerCacheManager.getItem(ores[1].first.certlink))
        every { f.random.of(any<Int>(), any<Int>()) } returnsMany listOf(1, ores[0].second + 1)
        f.player.inv[0] = InvObj(PACK, 1)
        f.player.inv[1] = InvObj(secondNote.internalName, Int.MAX_VALUE)
        val before = f.player.inv.objs.toList()
        f.openPack()
        assertEquals(before, f.player.inv.objs.toList())
    }

    private class Fixture(ultimate: Boolean = false) {
        private val client = RecordingClient()
        val coroutine = GameCoroutine("foundry-reward-test")
        private val events = EventBus()
        val random = mockk<GameRandom> { every { of(any<Int>(), any<Int>()) } returns 1 }
        private val kovac = KovacScript(random)
        private val pack = OrePackScript(random)
        private var result: Result<Any?>? = null
        var offeredCommission = false
            private set
        val player = Player().apply {
            this.client = this@Fixture.client
            uuid = 892L
            slotId = 1
            assignUid()
            coords = CoordGrid(3364, 11492, 0)
            currentMapClock = 100
            processedMapClock = 100
            gamemode = if (ultimate) PlayerGamemode.ULTIMATE_IRONMAN else PlayerGamemode.NORMAL
            inv = inventory("inv.inv", 28)
            worn = inventory("inv.worn", 14)
            invMap["inv.bank"] = inventory("inv.bank", 800)
            worn[Wearpos.RightHand.slot] = InvObj(PREFORM, 1)
            commissionWord1 = 1
            commissionWord2 = 2
            commissionStart = 0
            preformQuality = 199
            preformStartQuality = 200
            preformCompletion = 1000
            preformDifficulty = 3
            runEnergy = 100
            xpRate = 1.0
        }
        val bank: Inventory get() = player.invMap.getValue("inv.bank")
        private val access = ProtectedAccess(player, coroutine,
            ProtectedAccessContextFactory.empty().copy(
                getEventBus = { events }, getAlignment = { TextAlignment() },
            ))

        fun handIn() {
            offeredCommission = false
            result = null
            player.activeCoroutine = coroutine
            val method = KovacScript::class.java.getDeclaredMethod(
                "handInDialogue", Dialogue::class.java, Continuation::class.java,
            ).apply { isAccessible = true }
            val continuation = object : Continuation<Any?> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Any?>) { this@Fixture.result = result }
            }
            val returned = invoke { method.invoke(kovac, Dialogue(access, Npc(KOVAC, player.coords), false), continuation) }
            if (returned !== COROUTINE_SUSPENDED) result = Result.success(returned)
            result?.getOrThrow()
        }

        fun finish() {
            repeat(100) {
                result?.let { it.getOrThrow(); return }
                if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                    val parent = listOf("chat_left", "chat_right", "messagebox", "chatmenu")
                        .firstOrNull { player.ui.containsModal("interface.$it") }
                        ?: error("Unknown dialogue: ${output()}")
                    if (parent == "chatmenu") offeredCommission = true
                    coroutine.resumeWith(if (parent == "chatmenu") {
                        ResumePauseButtonInput("component.chatmenu:options", 2)
                    } else ResumePauseButtonInput("component.$parent:continue", -1))
                } else {
                    player.currentMapClock++
                    player.processedMapClock = player.currentMapClock
                    coroutine.advance()
                }
            }
            fail<Unit>("Hand-in did not finish: ${output()}")
        }

        fun openPack() {
            val method = OrePackScript::class.java.getDeclaredMethod("open", ProtectedAccess::class.java)
                .apply { isAccessible = true }
            invoke { method.invoke(pack, access) }
        }

        fun fill(inv: Inventory) {
            for (slot in inv.indices) inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun swordState(): List<Any?> = listOf(
            player.worn.objs.toList(), player.commissionWord1, player.commissionWord2,
            player.commissionStart, player.preformQuality, player.preformStartQuality,
            player.preformCompletion, player.preformDifficulty, player.foundryReputation,
            player.swordsMade, player.bestQuality, player.bestTime, player.runEnergy,
            player.statMap.getXP("stat.smithing"),
        )

        fun assertReward(inBank: Boolean, coins: Int = COINS) {
            assertEquals(coins, (if (inBank) bank else player.inv).count("obj.coins"))
            assertFalse(player.holdsPreform())
            assertEquals(0, player.commissionWord1)
            assertEquals(0, player.commissionWord2)
            assertEquals(0, player.preformQuality)
            assertEquals(0, player.preformCompletion)
            assertEquals(199, player.foundryReputation)
            assertEquals(1, player.swordsMade)
            assertEquals(199, player.bestQuality)
            assertEquals(5100, player.runEnergy)
            assertEquals(COINS / 2, player.statMap.getXP("stat.smithing"))
        }

        fun output(): String = client.messages.joinToString("\n")

        private fun inventory(name: String, size: Int) = Inventory(
            checkNotNull(ServerCacheManager.getInventory(name.asRSCM())), arrayOfNulls(size),
        )

        private fun invoke(block: () -> Any?): Any? = try {
            block()
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val PACK = "obj.giants_foundry_ore_pack"
        private const val COINS = 50_460
        private val restored = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
