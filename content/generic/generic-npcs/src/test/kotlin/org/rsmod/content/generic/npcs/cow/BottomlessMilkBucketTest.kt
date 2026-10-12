package org.rsmod.content.generic.npcs.cow

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bottomless.milk.MilkSources
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.content.generic.npcs.cow.BottomlessMilkBucket.Companion.EMPTY
import org.rsmod.content.generic.npcs.cow.BottomlessMilkBucket.Companion.FILLED
import org.rsmod.content.generic.npcs.cow.BottomlessMilkBucket.Companion.MAX_USES
import org.rsmod.content.generic.npcs.cow.BottomlessMilkBucket.Companion.USES
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BottomlessMilkBucketTest {
    @Test
    fun `noted milk fills the bucket and comes back as noted empty buckets`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(EMPTY)
        f.player.inv[1] = InvObj(MILK_NOTE, 10)
        f.run { with(f.script) { fill(0, MILK_NOTE) } }
        f.finish(count = 10, choices = listOf(YES))
        assertTrue(f.player.inv[0].isType(FILLED))
        assertEquals(10, f.uses(0))
        assertEquals(0, f.player.inv.count(MILK_NOTE))
        assertEquals(10, f.player.inv.count(BUCKET_NOTE))
    }

    @Test
    fun `a single bucket of milk fills without a prompt`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(11)
        f.player.inv[1] = InvObj(MILK)
        f.run { with(f.script) { fill(0, MILK) } }
        f.finish()
        assertEquals(12, f.uses(0))
        assertEquals(0, f.player.inv.count(MILK))
        assertEquals(1, f.player.inv.count(BUCKET))
    }

    @Test
    fun `filling takes unnoted and noted milk together when one form runs short`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(5)
        f.player.inv[1] = InvObj(MILK)
        f.player.inv[2] = InvObj(MILK_NOTE, 4)
        f.run { with(f.script) { fill(0, MILK) } }
        f.finish(count = 3)
        assertEquals(8, f.uses(0))
        assertEquals(0, f.player.inv.count(MILK))
        assertEquals(1, f.player.inv.count(BUCKET))
        assertEquals(2, f.player.inv.count(MILK_NOTE))
        assertEquals(2, f.player.inv.count(BUCKET_NOTE))
    }

    @Test
    fun `declining the first fill keeps the milk`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(EMPTY)
        f.player.inv[1] = InvObj(MILK_NOTE, 10)
        f.run { with(f.script) { fill(0, MILK_NOTE) } }
        f.finish(count = 10, choices = listOf(NO))
        assertTrue(f.player.inv[0].isType(EMPTY))
        assertEquals(10, f.player.inv.count(MILK_NOTE))
        assertEquals(0, f.player.inv.count(BUCKET_NOTE))
    }

    @Test
    fun `milk that vanishes during the prompt fills nothing`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(5)
        f.player.inv[1] = InvObj(MILK_NOTE, 10)
        f.run { with(f.script) { fill(0, MILK_NOTE) } }
        f.player.inv[1] = InvObj(MILK_NOTE, 3)
        f.finish(count = 10)
        assertEquals(5, f.uses(0))
        assertEquals(3, f.player.inv.count(MILK_NOTE))
        assertEquals(0, f.player.inv.count(BUCKET_NOTE))
    }

    @Test
    fun `a bucket that vanishes during the prompt keeps the milk`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(5)
        f.player.inv[1] = InvObj(MILK_NOTE, 10)
        f.run { with(f.script) { fill(0, MILK_NOTE) } }
        f.player.inv[0] = null
        f.finish(count = 10)
        assertEquals(10, f.player.inv.count(MILK_NOTE))
        assertEquals(0, f.player.inv.count(BUCKET_NOTE))
    }

    @Test
    fun `filling stops at ten thousand uses and leaves the rest`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(MAX_USES - 3)
        f.player.inv[1] = InvObj(MILK_NOTE, 10)
        f.run { with(f.script) { fill(0, MILK_NOTE) } }
        f.finish(count = 10)
        assertEquals(MAX_USES, f.uses(0))
        assertEquals(7, f.player.inv.count(MILK_NOTE))
        assertEquals(3, f.player.inv.count(BUCKET_NOTE))
    }

    @Test
    fun `a full bucket takes no milk`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(MAX_USES)
        f.player.inv[1] = InvObj(MILK_NOTE, 10)
        f.run { with(f.script) { fill(0, MILK_NOTE) } }
        f.finish()
        assertEquals(MAX_USES, f.uses(0))
        assertEquals(10, f.player.inv.count(MILK_NOTE))
    }

    @Test
    fun `noted empty buckets that can't stack any higher keep the milk`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(5)
        f.player.inv[1] = InvObj(MILK_NOTE, 10)
        f.player.inv[2] = InvObj(BUCKET_NOTE, Int.MAX_VALUE)
        f.run { with(f.script) { fill(0, MILK_NOTE) } }
        f.finish(count = 10)
        assertEquals(5, f.uses(0))
        assertEquals(10, f.player.inv.count(MILK_NOTE))
        assertEquals(Int.MAX_VALUE, f.player.inv.count(BUCKET_NOTE))
    }

    @Test
    fun `emptying discards the milk only once confirmed`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(40)
        f.run { with(f.script) { empty(0) } }
        f.finish(choices = listOf(NO))
        assertEquals(40, f.uses(0))
        f.run { with(f.script) { empty(0) } }
        f.finish(choices = listOf(YES))
        assertTrue(f.player.inv[0].isType(EMPTY))
        assertEquals(0, f.uses(0))
    }

    @Test
    fun `a quest takes one use of milk from the bucket`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(28)
        assertTrue(MilkSources.pour(f.player.inv))
        assertEquals(27, f.uses(0))
        assertTrue(f.player.inv[0].isType(FILLED))
    }

    @Test
    fun `the last use empties the bucket and an empty one gives nothing`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(1)
        assertTrue(MilkSources.pour(f.player.inv))
        assertTrue(f.player.inv[0].isType(EMPTY))
        assertEquals(0, f.uses(0))
        assertEquals(false, MilkSources.pour(f.player.inv))
    }

    @Test
    fun `pouring from a slot takes one use from that bucket only`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(5)
        f.player.inv[1] = f.filled(9)
        assertTrue(MilkSources.pourFrom(f.player.inv, 1))
        assertEquals(5, f.uses(0))
        assertEquals(8, f.uses(1))
    }

    @Test
    fun `pouring from a slot without a filled bucket takes nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(EMPTY)
        f.player.inv[1] = InvObj(MILK)
        assertEquals(false, MilkSources.pourFrom(f.player.inv, 0))
        assertEquals(false, MilkSources.pourFrom(f.player.inv, 1))
        assertEquals(false, MilkSources.pourFrom(f.player.inv, 2))
        assertEquals(1, f.player.inv.count(MILK))
    }

    @Test
    fun `chocolatey milk fills an empty bucket and leaves empty buckets`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(EMPTY)
        f.player.inv[1] = InvObj(CHOCOLATEY)
        f.run { with(f.script) { fill(0, CHOCOLATEY) } }
        f.finish(choices = listOf(YES))
        assertEquals(1, f.uses(0))
        assertEquals(MilkSources.typeOf(CHOCOLATEY), MilkSources.typeIn(f.player.inv[0]))
        assertEquals(0, f.player.inv.count(CHOCOLATEY))
        assertEquals(1, f.player.inv.count(BUCKET))
    }

    @Test
    fun `a bucket of regular milk takes no chocolatey milk`() {
        val f = Fixture()
        f.player.inv[0] = f.filled(5)
        f.player.inv[1] = InvObj(CHOCOLATEY)
        f.run { with(f.script) { fill(0, CHOCOLATEY) } }
        f.finish()
        f.run { with(f.script) { fill(0, null) } }
        f.finish()
        assertEquals(5, f.uses(0))
        assertEquals(1, f.player.inv.count(CHOCOLATEY))
    }

    @Test
    fun `chocolatey milk in the bucket is no bucket of milk`() {
        val f = Fixture()
        f.player.inv[0] = MilkSources.withType(f.filled(5), checkNotNull(MilkSources.typeOf(CHOCOLATEY)))
        assertEquals(false, MilkSources.has(f.player.inv))
        assertEquals(false, MilkSources.pour(f.player.inv))
        assertEquals(false, MilkSources.pourFrom(f.player.inv, 0))
        assertTrue(MilkSources.pour(f.player.inv, CHOCOLATEY))
        assertEquals(4, f.uses(0))
    }

    @Test
    fun `emptying clears the milk type`() {
        val f = Fixture()
        f.player.inv[0] = MilkSources.withType(f.filled(5), checkNotNull(MilkSources.typeOf(CHOCOLATEY)))
        f.run { with(f.script) { empty(0) } }
        f.finish(choices = listOf(YES))
        assertEquals(0, f.player.inv[0]!!.vars)
        f.player.inv[1] = InvObj(MILK)
        f.run { with(f.script) { fill(0, MILK) } }
        f.finish(choices = listOf(YES))
        assertEquals(MilkSources.typeOf(MILK), MilkSources.typeIn(f.player.inv[0]))
    }

    private class Fixture {
        private val coroutine = GameCoroutine("bottomless-milk-bucket-test")
        private var result: Result<Unit>? = null
        private val context =
            ProtectedAccessContextFactory.empty().copy(
                getEventBus = { EventBus() },
                getAlignment = { TextAlignment() },
            )
        val script = BottomlessMilkBucket(ObjChargeManager())

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = SilentClient()
                uuid = 796L
                slotId = 1
                assignUid()
                coords = CoordGrid(3252, 3275, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
                worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            }

        fun filled(uses: Int): InvObj {
            val inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            inv[0] = InvObj(EMPTY)
            ObjChargeManager().addCharges(inv, 0, uses, USES, MAX_USES)
            return checkNotNull(inv[0])
        }

        fun uses(slot: Int): Int = ObjChargeManager().getCharges(player.inv[slot], USES)

        fun run(block: suspend ProtectedAccess.() -> Unit) {
            result = null
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { access.block() }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun finish(count: Int = 0, choices: List<Int> = emptyList()) {
            val options = choices.iterator()
            repeat(50) {
                if (coroutine.isIdle) return
                when {
                    coroutine.isAwaiting(ResumePCountDialogInput::class) ->
                        coroutine.resumeWith(ResumePCountDialogInput(count))
                    coroutine.isAwaiting(ResumePauseButtonInput::class) -> {
                        val input =
                            when {
                                player.ui.containsModal("interface.chatmenu") ->
                                    ResumePauseButtonInput("component.chatmenu:options", options.next())
                                player.ui.containsModal("interface.objectbox") ->
                                    ResumePauseButtonInput("component.objectbox:universe", 0)
                                else -> ResumePauseButtonInput("component.messagebox:continue", -1)
                            }
                        coroutine.resumeWith(input)
                    }
                    else -> {
                        player.currentMapClock++
                        player.processedMapClock = player.currentMapClock
                        coroutine.advance()
                    }
                }
                result?.getOrThrow()
            }
            fail<Unit>("Interaction did not finish")
        }
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
        private const val MILK = "obj.bucket_milk"
        private const val MILK_NOTE = "obj.cert_bucket_milk"
        private const val BUCKET = "obj.bucket_empty"
        private const val BUCKET_NOTE = "obj.cert_bucket_empty"
        private const val CHOCOLATEY = "obj.chocolaty_milk"
        private const val YES = 1
        private const val NO = 2
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in
                listOf(
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

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
