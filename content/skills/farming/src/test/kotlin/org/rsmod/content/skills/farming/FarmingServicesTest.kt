package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
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
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class FarmingServicesTest {
    @Test
    fun `gardener combines exact noted and unnoted payment without requiring empty slots`() {
        val f = Fixture()
        val expected = f.plant("obj.potato_seed")
        f.give("obj.bucket_compost")
        f.give("obj.cert_bucket_compost")
        f.fill()
        assertEquals(ProtectResult.PROTECTED, f.gardeners.protect(f.player, f.patch, expected))
        assertEquals(0, f.count("obj.bucket_compost"))
        assertEquals(0, f.count("obj.cert_bucket_compost"))
        assertEquals(26, f.count("obj.spade"))
        assertTrue(f.state().paid)
        assertEquals(listOf(FarmingProtectionEvent(f.player, f.patch, checkNotNull(expected.crop))), f.protections)
        assertEquals(ProtectResult.STALE, f.gardeners.protect(f.player, f.patch, expected))
        assertEquals(26, f.count("obj.spade"))
        assertEquals(ProtectResult.ALREADY, f.gardeners.protect(f.player, f.patch, f.state()))
        assertEquals(1, f.protections.size)
    }

    @Test
    fun `partial sacks and loose produce cannot pay for protection but a noted full sack can`() {
        val f = Fixture()
        val expected = f.plant("obj.onion_seed")
        f.give("obj.sack_potato_9")
        f.give("obj.potato", 10)
        assertEquals(ProtectResult.UNPAID, f.gardeners.protect(f.player, f.patch, expected))
        assertEquals(1, f.count("obj.sack_potato_9"))
        assertEquals(10, f.count("obj.potato"))
        assertEquals(expected, f.state())
        assertTrue(f.protections.isEmpty())
        f.give("obj.cert_sack_potato_10")
        assertEquals(ProtectResult.PROTECTED, f.gardeners.protect(f.player, f.patch, expected))
        assertEquals(0, f.count("obj.cert_sack_potato_10"))
        assertEquals(1, f.count("obj.sack_potato_9"))
        assertEquals(10, f.count("obj.potato"))
        assertEquals(listOf(FarmingProtectionEvent(f.player, f.patch, checkNotNull(expected.crop))), f.protections)
    }

    @Test
    fun `missing later payment item rolls back the whole gardener payment`() {
        val f = Fixture()
        f.give("obj.bucket_compost", 2)
        assertFalse(f.gardeners.takePayment(f.player, listOf("obj.bucket_compost" to 2, "obj.jute_fibre" to 1)))
        assertEquals(2, f.count("obj.bucket_compost"))
        assertEquals(0, f.count("obj.jute_fibre"))
    }

    @Test
    fun `full inventory with insufficient noted payment emits no protection event`() {
        val f = Fixture()
        val expected = f.plant("obj.potato_seed")
        f.give("obj.cert_bucket_compost")
        f.fill()
        assertEquals(ProtectResult.UNPAID, f.gardeners.protect(f.player, f.patch, expected))
        assertEquals(expected, f.state())
        assertEquals(1, f.count("obj.cert_bucket_compost"))
        assertEquals(27, f.count("obj.spade"))
        assertTrue(f.protections.isEmpty())
    }

    @Test
    fun `replaced crop and vanished payment do not charge or protect anything`() {
        val f = Fixture()
        val quoted = f.plant("obj.potato_seed")
        f.give("obj.bucket_compost", 2)
        val current = f.plant("obj.cabbage_seed")
        assertEquals(ProtectResult.STALE, f.gardeners.protect(f.player, f.patch, quoted))
        assertEquals(current, f.state())
        assertEquals(2, f.count("obj.bucket_compost"))
        f.player.inv.fillNulls()
        assertEquals(ProtectResult.UNPAID, f.gardeners.protect(f.player, f.patch, current))
        assertEquals(current, f.state())
        assertTrue(f.protections.isEmpty())
    }

    @Test
    fun `tree removal charges the right fee once and refuses unchecked or stale trees`() {
        val (seed, price) = "obj.plantpot_oak_sapling" to 200
        val f = Fixture()
        val crop = checkNotNull(FarmingCrops.bySeed(seed))
        f.patch = FarmingPatches.all.first { it.kind == crop.kind }
        val unchecked = PatchState(weeds = 3, cropIndex = crop.index, stage = crop.stages)
        f.state(unchecked)
        f.give("obj.coins", price + 1)
        assertFalse(f.gardeners.removeTree(f.player, f.patch, unchecked))
        assertEquals(price + 1, f.count("obj.coins"))
        val checked = unchecked.copy(phase = Phase.CHECKED)
        f.state(checked)
        assertFalse(f.gardeners.removeTree(f.player, f.patch, unchecked))
        assertEquals(price + 1, f.count("obj.coins"))
        assertTrue(f.gardeners.removeTree(f.player, f.patch, checked))
        assertEquals(1, f.count("obj.coins"))
        assertEquals(0, f.state().cropIndex)
        assertFalse(f.gardeners.removeTree(f.player, f.patch, checked))
        assertEquals(1, f.count("obj.coins"))
    }

    @Test
    fun `native split counters cap deposits and preserve noted remainder through withdrawal`() {
        val f = Fixture()
        f.variable("varbit.farming_tools_rake", 1)
        f.variable("varbit.farming_tools_extrarakes", 49)
        val slot = f.give("obj.cert_rake", 3)
        assertEquals(1, FarmingLeprechaunStorage.deposit(f.player, slot, 3))
        assertEquals(2, f.count("obj.cert_rake"))
        assertEquals(100, f.stored("rake")?.count)
        assertEquals(0, FarmingLeprechaunStorage.deposit(f.player, slot, 3))
        assertEquals(100, FarmingLeprechaunStorage.withdraw(f.player, "rake", Int.MAX_VALUE, notes = true))
        assertEquals(102, f.count("obj.cert_rake"))
        assertEquals(null, f.stored("rake"))
        assertEquals(0, FarmingLeprechaunStorage.withdraw(f.player, "rake", Int.MAX_VALUE, notes = true))
        assertEquals(102, f.count("obj.cert_rake"))
    }

    @Test
    fun `full inventory and note overflow leave stored tools untouched`() {
        val f = Fixture()
        f.variable("varbit.farming_tools_buckets", 31)
        f.variable("varbit.farming_tools_extrabuckets", 7)
        f.variable("varbit.farming_tools_extra2buckets", 2)
        f.fill()
        assertEquals(0, FarmingLeprechaunStorage.withdraw(f.player, "bucket", 5))
        assertEquals(767, f.stored("bucket")?.count)
        f.player.inv[0] = InvObj("obj.cert_bucket_empty", Int.MAX_VALUE)
        assertEquals(0, FarmingLeprechaunStorage.withdraw(f.player, "bucket", 5, notes = true))
        assertEquals(Int.MAX_VALUE, f.count("obj.cert_bucket_empty"))
        assertEquals(767, f.stored("bucket")?.count)
        f.player.inv[1] = null
        assertEquals(1, FarmingLeprechaunStorage.withdraw(f.player, "bucket", 5))
        assertEquals(1, f.count("obj.bucket_empty"))
        assertEquals(766, f.stored("bucket")?.count)
    }

    @Test
    fun `bottomless storage round trip preserves its tier and every charge without duplication`() {
        val kind = Compost.SUPER
        val f = Fixture()
        val bucket = FarmingBottomlessCompost.bucket(kind, 10000)
        f.player.inv[0] = bucket
        assertEquals(1, FarmingLeprechaunStorage.deposit(f.player, 0, 1))
        assertEquals(null, f.player.inv[0])
        assertEquals(0, FarmingLeprechaunStorage.deposit(f.player, 0, 1))
        assertEquals(bucket, f.stored("bottomless_bucket"))
        f.fill()
        assertEquals(0, FarmingLeprechaunStorage.withdraw(f.player, "bottomless_bucket", 1))
        assertEquals(bucket, f.stored("bottomless_bucket"))
        f.player.inv[0] = null
        assertEquals(1, FarmingLeprechaunStorage.withdraw(f.player, "bottomless_bucket", 1))
        assertEquals(bucket, f.player.inv[0])
        assertEquals(null, f.stored("bottomless_bucket"))
        assertEquals(0, FarmingLeprechaunStorage.withdraw(f.player, "bottomless_bucket", 1))
        assertEquals(bucket, f.player.inv[0])
    }

    @Test
    fun `noted empty bottomless bucket stores only one physical bucket`() {
        val f = Fixture()
        val slot = f.give("obj.cert_bottomless_compost_bucket", 2)
        assertEquals(1, FarmingLeprechaunStorage.deposit(f.player, slot, 2))
        assertEquals(1, f.count("obj.cert_bottomless_compost_bucket"))
        assertEquals(1, FarmingLeprechaunStorage.withdraw(f.player, "bottomless_bucket", 1))
        assertEquals(1, f.count(FarmingBottomlessCompost.EMPTY))
        assertEquals(1, f.count("obj.cert_bottomless_compost_bucket"))
    }

    @Test
    fun `magic secateurs and watering cans retain their native variants`() {
        val f = Fixture()
        val magic = f.give("obj.fairy_enchanted_secateurs")
        assertEquals(1, FarmingLeprechaunStorage.deposit(f.player, magic, 1))
        assertEquals(1, f.player.vars["varbit.farming_tools_fairysecateurs"])
        assertEquals(1, FarmingLeprechaunStorage.withdraw(f.player, "secateurs", 1))
        assertEquals(1, f.count("obj.fairy_enchanted_secateurs"))
        assertEquals(0, f.player.vars["varbit.farming_tools_fairysecateurs"])
        val can = f.give("obj.watering_can_5")
        assertEquals(1, FarmingLeprechaunStorage.deposit(f.player, can, 1))
        assertEquals("obj.watering_can_5".asRSCM(), f.stored("wateringcan")?.id)
        assertEquals(1, FarmingLeprechaunStorage.withdraw(f.player, "wateringcan", 1))
        assertEquals(1, f.count("obj.watering_can_5"))
    }

    @Test
    fun `noting accepts clean and grimy herbs but never consumes excluded containers or logs`() {
        for (obj in listOf("obj.guam_leaf", "obj.unidentified_guam", "obj.huasca")) {
            val f = Fixture()
            f.give(obj, 3)
            assertTrue(FarmingLeprechaunStorage.note(f.player, obj.asRSCM()))
            val note = checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).certlink
            assertEquals(0, f.count(obj))
            assertEquals(3, f.player.inv.physicalCount(checkNotNull(ServerCacheManager.getItem(note)).internalName))
        }
        for (obj in listOf("obj.basket_apple_5", "obj.sack_potato_10", "obj.logs", "obj.plantpot_crystal_tree_sapling", "obj.plantpot_spirit_tree_sapling")) {
            val f = Fixture()
            f.give(obj)
            assertFalse(FarmingLeprechaunStorage.note(f.player, obj.asRSCM()))
            assertEquals(1, f.count(obj))
        }
    }

    @Test
    fun `note stack overflow rolls back all produce and selected vanished produce awards nothing`() {
        val f = Fixture()
        val cabbage = checkNotNull(ServerCacheManager.getItem("obj.cabbage".asRSCM()))
        val note = checkNotNull(ServerCacheManager.getItem(cabbage.certlink)).internalName
        f.give(note, Int.MAX_VALUE)
        val slot = f.give("obj.cabbage")
        assertFalse(FarmingLeprechaunStorage.note(f.player, "obj.cabbage".asRSCM()))
        assertEquals(1, f.count("obj.cabbage"))
        assertEquals(Int.MAX_VALUE, f.count(note))
        f.player.inv[slot] = null
        assertFalse(FarmingLeprechaunStorage.note(f.player, "obj.cabbage".asRSCM()))
        assertEquals(Int.MAX_VALUE, f.count(note))
    }

    private class Fixture {
        private val events = EventBus()
        private val random = object : GameRandom {
            override fun of(maxExclusive: Int): Int = maxExclusive - 1
            override fun of(minInclusive: Int, maxInclusive: Int): Int = maxInclusive
            override fun randomDouble(): Double = 0.999
        }
        private val farming = FarmingService(
            XpModifiers(emptySet()), random, events,
            FarmingRewardTestSupport.rewards(random, events), FarmingEquipment(random),
            farmingSiteAccess(),
        )
        val gardeners = FarmingGardeners(farming)
        var patch = FarmingPatches.all.first { it.kind == PatchKind.ALLOTMENT }
        val protections = mutableListOf<FarmingProtectionEvent>()

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        }
        init {
            variable("varp.farming_clock", Int.MAX_VALUE)
            events.subscribeUnbound(FarmingProtectionEvent::class.java) {
                assertEquals(this@Fixture.player, player)
                assertEquals(this@Fixture.patch, patch)
                assertEquals(state().crop, crop)
                assertTrue(state().paid)
                for ((obj, _) in gardeners.payment(crop)) {
                    assertEquals(0, count(obj))
                    val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
                    if (type.canCert) {
                        assertEquals(0, count(checkNotNull(ServerCacheManager.getItem(type.certlink)).internalName))
                    }
                }
                protections += this
            }
        }
        fun count(obj: String): Int = player.inv.physicalCount(obj)
        fun variable(name: String, value: Int) = VarPlayerIntMapSetter.set(player, name, value)
        fun state(state: PatchState) = variable(patch.varp, state.pack())
        fun state(): PatchState = PatchState.unpack(player.vars[patch.varp])
        fun plant(seed: String): PatchState = PatchState(weeds = 3, cropIndex = checkNotNull(FarmingCrops.bySeed(seed)).index).also { state(it) }
        fun fill() { for (slot in player.inv.indices) if (player.inv[slot] == null) player.inv[slot] = InvObj("obj.spade") }
        fun stored(component: String): InvObj? = FarmingLeprechaunStorage.stored(player, FarmingLeprechaunStorage.tools.first { it.component == component })
        fun give(obj: String, count: Int = 1): Int {
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            val stackable = type.isStackable
            var first = -1
            repeat(if (stackable) 1 else count) {
                val slot = player.inv.indices.first { player.inv[it] == null }
                player.inv[slot] = InvObj(type, if (stackable) count else 1)
                if (first < 0) first = slot
            }
            return first
        }
    }

    companion object {
        private val RESTORED = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            loadFarmingTestCache()
            installFarmingTestInventory(RESTORED)
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
