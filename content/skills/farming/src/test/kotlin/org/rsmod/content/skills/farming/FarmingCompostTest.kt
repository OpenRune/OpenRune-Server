package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class FarmingCompostTest {
    @Test
    fun `all compost tiers double and return the input bucket form in a full inventory`() {
        for (form in FarmingCompostSupplies.BUCKETS) {
            val f = Fixture()
            f.player.inv[0] = InvObj(FarmingBottomlessCompost.EMPTY)
            f.give(form.obj.internalName, 3)
            f.fillInventory()
            assertEquals(3, f.fill(3))
            assertEquals(BottomlessCompost(form.kind, 6), f.contents())
            assertEquals(0, f.count(form.obj.internalName))
            assertEquals(3, f.count(form.empty.internalName))
            assertEquals(0, f.fill(3))
            assertEquals(BottomlessCompost(form.kind, 6), f.contents())
        }
    }

    @Test
    fun `partial noted fill rolls back when its empty notes cannot fit or would overflow`() {
        for (overflow in listOf(false, true)) {
            val f = Fixture()
            f.player.inv[0] = InvObj(FarmingBottomlessCompost.EMPTY)
            f.player.inv[1] = InvObj("obj.cert_bucket_ultracompost", 10)
            f.fillInventory()
            if (overflow) f.player.inv[2] = InvObj("obj.cert_bucket_empty", Int.MAX_VALUE)
            val before = f.player.inv.objs.copyOf()
            assertEquals(0, f.fill(1))
            assertTrue(before.contentEquals(f.player.inv.objs))
            f.player.inv[2] = null
            assertEquals(1, f.fill(1))
            assertEquals(9, f.count("obj.cert_bucket_ultracompost"))
            assertEquals(1, f.count("obj.cert_bucket_empty"))
            assertEquals(BottomlessCompost(Compost.ULTRA, 2), f.contents())
        }
    }

    @Test
    fun `capacity refuses the odd final use and rejects a different compost type`() {
        val f = Fixture()
        f.player.inv[0] = FarmingBottomlessCompost.bucket(Compost.SUPER, 9999)
        f.give("obj.bucket_supercompost")
        assertEquals(0, f.fill(Int.MAX_VALUE))
        assertEquals(1, f.count("obj.bucket_supercompost"))
        f.player.inv[0] = FarmingBottomlessCompost.bucket(Compost.SUPER, 9998)
        assertEquals(1, f.fill(Int.MAX_VALUE))
        assertEquals(BottomlessCompost(Compost.SUPER, 10000), f.contents())
        assertEquals(1, f.count("obj.bucket_empty"))
        f.give("obj.bucket_ultracompost")
        assertEquals(0, f.fill(1))
        f.player.inv[0] = FarmingBottomlessCompost.bucket(Compost.SUPER, 2)
        assertEquals(0, f.fill(1))
        assertEquals(1, f.count("obj.bucket_ultracompost"))
        assertEquals(BottomlessCompost(Compost.SUPER, 2), f.contents())
    }

    @Test
    fun `maximum noted input fills only capacity and stale repeat cannot debit its remainder`() {
        for (form in FarmingCompostSupplies.BUCKETS.filter { it.noted }) {
            val f = Fixture()
            val empty = InvObj(FarmingBottomlessCompost.EMPTY)
            f.player.inv[0] = empty
            val sourceSlot = f.give(form.obj.internalName, Int.MAX_VALUE)
            val source = checkNotNull(f.player.inv[sourceSlot])
            val consumed = FarmingBottomlessCompost.capacity / FarmingBottomlessCompost.chargesPerBucket
            assertEquals(consumed, f.fill(Int.MAX_VALUE))
            assertEquals(BottomlessCompost(form.kind, 10000), f.contents())
            assertEquals(Int.MAX_VALUE - consumed, f.count(form.obj.internalName))
            assertEquals(consumed, f.count(form.empty.internalName))
            val before = f.player.inv.objs.copyOf()
            assertEquals(0, f.compost.fill(f.player, 0, empty, sourceSlot, source, form, Int.MAX_VALUE))
            assertTrue(before.contentEquals(f.player.inv.objs))
        }
    }

    @Test
    fun `same item id with different contents invalidates fill empty and delayed application`() {
        val f = Fixture()
        val selected = FarmingBottomlessCompost.bucket(Compost.NORMAL, 2)
        f.player.inv[0] = selected
        val sourceSlot = f.give("obj.bucket_compost")
        val source = checkNotNull(f.player.inv[sourceSlot])
        val form = checkNotNull(FarmingCompostSupplies.bucket("obj.bucket_compost"))
        f.start { with(f.compost) { apply(f.patch, FarmingBottomlessCompost.FILLED, 0) } }
        val replacement = FarmingBottomlessCompost.bucket(Compost.ULTRA, 7)
        f.player.inv[0] = replacement
        val before = f.player.inv.objs.copyOf()
        f.finish()
        assertEquals(Compost.NONE, f.state().compost)
        assertEquals(0, f.xp())
        assertEquals(0, f.compost.fill(f.player, 0, selected, sourceSlot, source, form, 1))
        assertFalse(f.compost.empty(f.player, 0, selected))
        assertTrue(before.contentEquals(f.player.inv.objs))
        f.player.inv[0] = selected
        f.player.inv[sourceSlot] = null
        f.give("obj.cert_bucket_compost", 2)
        assertEquals(0, f.compost.fill(f.player, 0, selected, sourceSlot, source, form, 1))
        assertEquals(selected, f.player.inv[0])
        assertEquals(2, f.count("obj.cert_bucket_compost"))
        assertEquals(0, f.count("obj.bucket_empty"))
    }

    @Test
    fun `vanished selected bucket or compost never consumes a different slot`() {
        val f = Fixture()
        val empty = InvObj(FarmingBottomlessCompost.EMPTY)
        f.player.inv[0] = empty
        val sourceSlot = f.give("obj.bucket_compost", 2)
        val source = checkNotNull(f.player.inv[sourceSlot])
        val form = checkNotNull(FarmingCompostSupplies.bucket("obj.bucket_compost"))
        f.player.inv[sourceSlot] = InvObj("obj.spade")
        assertEquals(0, f.compost.fill(f.player, 0, empty, sourceSlot, source, form, 1))
        assertEquals(1, f.count("obj.bucket_compost"))
        f.player.inv[sourceSlot] = source
        f.player.inv[0] = InvObj("obj.spade")
        assertEquals(0, f.compost.fill(f.player, 0, empty, sourceSlot, source, form, 1))
        assertEquals(2, f.count("obj.bucket_compost"))
    }

    @Test
    fun `each potion dose upgrades one selected bucket and returns the remaining dose or vial`() {
        for (recipe in FarmingCompostSupplies.UPGRADES.filter { it.kind == CompostUpgradeKind.POTION }) {
            val f = Fixture()
            val bucket = f.give(recipe.bucket.internalName)
            val reagent = f.give(recipe.reagent.internalName)
            f.fillInventory()
            val selected = checkNotNull(f.player.inv[bucket])
            val dose = checkNotNull(f.player.inv[reagent])
            assertTrue(f.compost.upgrade(f.player, bucket, selected, reagent, dose, recipe))
            assertEquals(InvObj(recipe.output), f.player.inv[bucket])
            assertEquals(InvObj(recipe.remainder), f.player.inv[reagent])
            assertFalse(f.compost.upgrade(f.player, bucket, selected, reagent, dose, recipe))
            assertEquals(1, f.count(recipe.output.internalName))
        }
    }

    @Test
    fun `saltpetre removes three physical items per batch and refuses insufficient material`() {
        val f = Fixture()
        val recipe = FarmingCompostSupplies.UPGRADES.first { it.kind == CompostUpgradeKind.SALTPETRE }
        val bucket = f.give(recipe.bucket.internalName, 2)
        val reagent = f.give(recipe.reagent.internalName, 7)
        f.start { with(f.compost) { upgrade(bucket, reagent, recipe) } }
        f.finish()
        assertEquals(2, f.count("obj.bucket_supercompost"))
        assertEquals(1, f.count("obj.hosidius_saltpetre"))
        assertEquals(40, f.xp())
        val another = f.give("obj.bucket_compost")
        val remaining = f.slot("obj.hosidius_saltpetre")
        assertFalse(f.compost.upgrade(f.player, another, checkNotNull(f.player.inv[another]), remaining, checkNotNull(f.player.inv[remaining]), recipe))
        assertEquals(1, f.count("obj.bucket_compost"))
        assertEquals(1, f.count("obj.hosidius_saltpetre"))
    }

    @Test
    fun `ash creation revalidates both selected slots after its delay and awards only committed output`() {
        for (removeBucket in listOf(false, true)) {
            val f = Fixture()
            val recipe = FarmingCompostSupplies.UPGRADES.first { it.kind == CompostUpgradeKind.ASH }
            val bucket = f.give(recipe.bucket.internalName, 2)
            val ash = f.give(recipe.reagent.internalName, 6)
            f.start { with(f.compost) { upgrade(bucket, ash, recipe) } }
            f.player.inv[if (removeBucket) bucket else ash] = InvObj("obj.spade")
            f.finish()
            assertEquals(0, f.count("obj.bucket_ultracompost"))
            assertEquals(0, f.xp())
            assertEquals(if (removeBucket) 1 else 2, f.count("obj.bucket_supercompost"))
            assertEquals(if (removeBucket) 6 else 0, f.count("obj.fossil_volcanic_ash"))
        }
        val f = Fixture()
        val recipe = FarmingCompostSupplies.UPGRADES.first { it.kind == CompostUpgradeKind.ASH }
        val bucket = f.give(recipe.bucket.internalName, 3)
        val ash = f.give(recipe.reagent.internalName, 6)
        f.start { with(f.compost) { upgrade(bucket, ash, recipe) } }
        f.finish()
        assertEquals(3, f.count("obj.bucket_ultracompost"))
        assertEquals(0, f.count("obj.bucket_supercompost"))
        assertEquals(0, f.count("obj.fossil_volcanic_ash"))
        assertEquals(33, f.xp())
    }

    @Test
    fun `application cannot substitute another bucket after a delay or pay twice for one patch`() {
        val f = Fixture()
        val selected = f.give("obj.bucket_compost", 2)
        f.start { with(f.compost) { apply(f.patch, "obj.bucket_compost", selected) } }
        f.player.inv[selected] = InvObj("obj.spade")
        f.finish()
        assertEquals(Compost.NONE, f.state().compost)
        assertEquals(1, f.count("obj.bucket_compost"))
        assertEquals(0, f.xp())
        val other = f.slot("obj.bucket_compost")
        f.start { with(f.compost) { apply(f.patch, "obj.bucket_compost", other) } }
        f.finish()
        assertEquals(Compost.NORMAL, f.state().compost)
        assertEquals(1, f.count("obj.bucket_empty"))
        assertEquals(180, f.xp())
        val extra = f.give("obj.bucket_compost")
        f.start { with(f.compost) { apply(f.patch, "obj.bucket_compost", extra) } }
        f.finish()
        assertEquals(1, f.count("obj.bucket_compost"))
        assertEquals(180, f.xp())
    }

    @Test
    fun `bottomless last application returns an empty tradeable bucket and another patch consumes nothing`() {
        val f = Fixture()
        f.player.inv[0] = FarmingBottomlessCompost.bucket(Compost.ULTRA, 1)
        f.start { with(f.compost) { apply(f.patch, FarmingBottomlessCompost.FILLED, 0) } }
        f.finish()
        assertEquals(Compost.ULTRA, f.state().compost)
        assertEquals(InvObj(FarmingBottomlessCompost.EMPTY), f.player.inv[0])
        assertEquals(360, f.xp())
        f.start { with(f.compost) { apply(f.patch, FarmingBottomlessCompost.FILLED, 0) } }
        f.finish()
        assertEquals(360, f.xp())
    }

    @Test
    fun `bank transfers preserve independent compost contents and emptying discards no other bucket`() {
        val f = Fixture()
        val first = FarmingBottomlessCompost.bucket(Compost.NORMAL, 9)
        val second = FarmingBottomlessCompost.bucket(Compost.ULTRA, 42)
        f.player.inv[0] = first
        f.player.inv[1] = second
        val bank = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.bank".asRSCM())), arrayOfNulls(100))
        assertTrue(f.player.invTransfer(f.player.inv, 0, 1, bank).success)
        assertTrue(f.player.invTransfer(f.player.inv, 1, 1, bank).success)
        assertEquals(2, bank.objs.count { it != null })
        assertTrue(f.player.invTransfer(bank, 0, 1, f.player.inv).success)
        assertTrue(f.player.invTransfer(bank, 1, 1, f.player.inv).success)
        assertEquals(first, f.player.inv[0])
        assertEquals(second, f.player.inv[1])
        assertTrue(f.compost.empty(f.player, 0, first))
        assertFalse(f.compost.empty(f.player, 0, first))
        assertEquals(InvObj(FarmingBottomlessCompost.EMPTY), f.player.inv[0])
        assertEquals(second, f.player.inv[1])
        assertEquals(0, f.count("obj.bucket_compost"))
    }

    @Test
    fun `ordinary emptying replaces only the selected bucket in a full inventory`() {
        val f = Fixture()
        val selected = InvObj("obj.bucket_compost")
        f.player.inv[16] = selected
        f.player.inv[3] = InvObj("obj.bucket_compost")
        f.player.inv[1] = InvObj("obj.cert_bucket_empty", Int.MAX_VALUE)
        f.fillInventory()
        val expected = f.player.inv.objs.copyOf()
        expected[16] = InvObj("obj.bucket_empty")
        assertTrue(f.compost.empty(f.player, 16, selected))
        assertTrue(expected.contentEquals(f.player.inv.objs))
        assertFalse(f.compost.empty(f.player, 16, selected))
        assertTrue(expected.contentEquals(f.player.inv.objs))
        assertEquals(0, f.xp())
    }

    @Test
    fun `ordinary emptying never redirects a vanished or replaced selected bucket`() {
        val selected = InvObj("obj.bucket_compost")
        for (replacement in listOf(
            null,
            InvObj("obj.spade"),
            InvObj("obj.bucket_supercompost"),
            InvObj("obj.cert_bucket_compost", Int.MAX_VALUE),
        )) {
            val f = Fixture()
            f.player.inv[16] = replacement
            f.player.inv[3] = selected
            val before = f.player.inv.objs.copyOf()
            assertFalse(f.compost.empty(f.player, 16, selected))
            assertTrue(before.contentEquals(f.player.inv.objs))
            assertEquals(0, f.xp())
        }
    }

    @Test
    fun `one physical compost funds exactly two patches across storage then permits a new tier`() {
        for (form in FarmingCompostSupplies.BUCKETS) {
            val f = Fixture()
            f.player.inv[0] = InvObj(FarmingBottomlessCompost.EMPTY)
            f.give(form.obj.internalName)
            assertEquals(1, f.fill(1))
            assertEquals(0, f.count(form.obj.internalName))
            assertEquals(1, f.count(form.empty.internalName))
            assertEquals(BottomlessCompost(form.kind, 2), f.contents())
            assertEquals(1, FarmingLeprechaunStorage.deposit(f.player, 0, 1))
            assertEquals(null, f.player.inv[0])
            assertEquals(1, FarmingLeprechaunStorage.withdraw(f.player, "bottomless_bucket", 1))
            assertEquals(BottomlessCompost(form.kind, 2), f.contents())
            f.start { with(f.compost) { apply(f.patch, FarmingBottomlessCompost.FILLED, 0) } }
            f.finish()
            assertEquals(form.kind, f.state().compost)
            assertEquals(BottomlessCompost(form.kind, 1), f.contents())
            val paidXp = f.xp()
            f.start { with(f.compost) { apply(f.patch, FarmingBottomlessCompost.FILLED, 0) } }
            f.finish()
            assertEquals(BottomlessCompost(form.kind, 1), f.contents())
            assertEquals(paidXp, f.xp())
            val second = FarmingPatches.all.first { it.kind == PatchKind.HERB && it.varp != f.patch.varp }
            VarPlayerIntMapSetter.set(f.player, second.varp, PatchState().empty().pack())
            f.start { with(f.compost) { apply(second, FarmingBottomlessCompost.FILLED, 0) } }
            f.finish()
            assertEquals(form.kind, PatchState.unpack(f.player.vars[second.varp]).compost)
            assertEquals(InvObj(FarmingBottomlessCompost.EMPTY), f.player.inv[0])
            assertEquals(1, f.count(form.empty.internalName))
            val next = FarmingCompostSupplies.BUCKETS.first { !it.noted && it.kind != form.kind }
            f.give(next.obj.internalName)
            assertEquals(1, f.fill(1))
            assertEquals(BottomlessCompost(next.kind, 2), f.contents())
            assertEquals(0, f.count(next.obj.internalName))
            assertTrue(f.compost.empty(f.player, 0, checkNotNull(f.player.inv[0])))
            assertEquals(BottomlessCompost(Compost.NONE, 0), f.contents())
            assertEquals(0, f.count(next.obj.internalName))
            assertEquals(if (form.noted) 1 else 2, f.count(next.empty.internalName))
        }
    }

    private class Fixture {
        private val events = EventBus()
        private val random = object : GameRandom {
            override fun of(maxExclusive: Int): Int = maxExclusive - 1
            override fun of(minInclusive: Int, maxInclusive: Int): Int = maxInclusive
            override fun randomDouble(): Double = 0.999
        }
        private val modifiers = XpModifiers(emptySet())
        private val farming = FarmingService(modifiers, random, events,
            FarmingRewardTestSupport.rewards(random, events), FarmingEquipment(random), farmingSiteAccess())
        val compost = FarmingCompostService(farming, modifiers)
        val patch = FarmingPatches.all.first { it.kind == PatchKind.HERB && it.site == "north_catherby" }
        private val coroutine = GameCoroutine("farming-compost-test")
        private val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events })
        private var result: Result<Unit>? = null

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 805L
            slotId = 1
            assignUid()
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            statMap.setBaseLevel(STAT_FARMING, 99)
            statMap.setCurrentLevel(STAT_FARMING, 99)
            VarPlayerIntMapSetter.set(this, "varp.farming_clock", Int.MAX_VALUE)
            VarPlayerIntMapSetter.set(this, patch.varp, PatchState().empty().pack())
        }

        fun state(): PatchState = PatchState.unpack(player.vars[patch.varp])
        fun xp(): Int = player.statMap.getFineXP(STAT_FARMING)
        fun contents(): BottomlessCompost? = FarmingBottomlessCompost.contents(player.inv[0])
        fun count(obj: String): Int = player.inv.physicalCount(obj)
        fun slot(obj: String): Int = player.inv.objs.indexOfFirst { it?.id == obj.asRSCM() }
        fun fillInventory() {
            for (slot in player.inv.indices) if (player.inv[slot] == null) player.inv[slot] = InvObj("obj.spade")
        }
        fun give(obj: String, count: Int = 1): Int {
            val stackable = checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).isStackable
            var first = -1
            repeat(if (stackable) 1 else count) {
                val slot = player.inv.objs.indexOfFirst { it == null }
                player.inv[slot] = InvObj(obj, if (stackable) count else 1)
                if (first == -1) first = slot
            }
            return first
        }
        fun fill(amount: Int): Int {
            val sourceSlot = player.inv.objs.indexOfFirst { obj -> FarmingCompostSupplies.BUCKETS.any { obj?.id == it.obj.id } }
            if (sourceSlot < 0) return 0
            val source = checkNotNull(player.inv[sourceSlot])
            val form = FarmingCompostSupplies.BUCKETS.first { it.obj.id == source.id }
            return compost.fill(player, 0, checkNotNull(player.inv[0]), sourceSlot, source, form, amount)
        }
        fun start(action: suspend ProtectedAccess.() -> Unit) {
            while (player.isDelayed) {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
            }
            result = null
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { access.action() }
            body.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { this@Fixture.result = result }
            })
            result?.getOrThrow()
        }
        fun finish() {
            repeat(200) {
                if (coroutine.isIdle) {
                    result?.getOrThrow()
                    return
                }
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
                result?.getOrThrow()
            }
            fail<Unit>("Compost action did not finish")
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
        private val restored = mutableListOf<() -> Unit>()

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
