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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
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
class FarmingEquipmentTest {
    @Test
    fun `bounty only rolls for an equipped amulet and allotment planting`() {
        val f = Fixture()
        val crop = FarmingCrops.all.first { it.kind == PatchKind.ALLOTMENT }
        f.player.inv[0] = InvObj(FarmingEquipment.BOUNTY)
        assertEquals(3, f.equipment.seedCost(f.player, crop))
        assertEquals(0, f.random.calls)
        f.player.inv[0] = null
        f.player.worn[2] = InvObj(FarmingEquipment.BOUNTY)
        val herb = FarmingCrops.all.first { it.kind == PatchKind.HERB }
        assertEquals(1, f.equipment.seedCost(f.player, herb))
        assertEquals(0, f.random.calls)
        assertEquals(1, f.equipment.seedCost(f.player, crop))
        assertEquals(1, f.random.calls)
        assertEquals(10, f.equipment.bountyCharges(f.player))
    }

    @Test
    fun `bounty charges survive swapping amulets and the tenth success destroys only the equipped one`() {
        val f = Fixture()
        val crop = FarmingCrops.all.first { it.kind == PatchKind.ALLOTMENT }
        f.player.worn[2] = InvObj(FarmingEquipment.BOUNTY)
        f.player.inv[0] = InvObj(FarmingEquipment.BOUNTY)
        f.equipment.planted(f.player, crop, 3)
        assertEquals(10, f.equipment.bountyCharges(f.player))
        repeat(9) { f.equipment.planted(f.player, crop, 1) }
        assertEquals(1, f.equipment.bountyCharges(f.player))
        f.player.worn[2] = InvObj(FarmingEquipment.BOUNTY)
        assertEquals(1, f.equipment.bountyCharges(f.player))
        f.equipment.planted(f.player, crop, 1)
        assertEquals(null, f.player.worn[2])
        assertEquals(1, f.player.inv.count(FarmingEquipment.BOUNTY))
        assertEquals(10, f.equipment.bountyCharges(f.player))
    }

    @Test
    fun `breaking bounty resets charges only when the selected amulet is actually consumed`() {
        val f = Fixture()
        val script = FarmingEquipmentScript(f.equipment)
        with(script) { ScriptContext(f.events, CheatCommandMap(), EngineQueueCache()).startup() }
        f.player.inv[0] = InvObj(FarmingEquipment.BOUNTY)
        f.player.worn[2] = InvObj(FarmingEquipment.BOUNTY)
        VarPlayerIntMapSetter.set(f.player, FarmingEquipment.BOUNTY_USES, 9)
        val obj = InvObj(FarmingEquipment.BOUNTY)
        val type = checkNotNull(ServerCacheManager.getItem(obj.id))
        f.run {
            assertTrue(f.events.publish(this,
                org.rsmod.api.player.events.interact.HeldObjEvents.Op4(0, obj, type, inv)))
        }
        assertEquals(0, f.player.inv.count(FarmingEquipment.BOUNTY))
        assertEquals(1, f.player.worn.count(FarmingEquipment.BOUNTY))
        assertEquals(10, f.equipment.bountyCharges(f.player))
        VarPlayerIntMapSetter.set(f.player, FarmingEquipment.BOUNTY_USES, 4)
        f.run {
            assertTrue(f.events.publish(this,
                org.rsmod.api.player.events.interact.HeldObjEvents.Op4(0, obj, type, inv)))
        }
        assertEquals(6, f.equipment.bountyCharges(f.player))
        assertEquals(1, f.player.worn.count(FarmingEquipment.BOUNTY))
    }

    @Test
    fun `inventory cape gives no bonus while inventory secateurs do and cape only boosts herbs`() {
        val f = Fixture()
        val herb = FarmingCrops.all.first { it.kind == PatchKind.HERB && it.magicSecateurs }
        val allotment = FarmingCrops.all.first { it.kind == PatchKind.ALLOTMENT }
        f.player.inv[0] = InvObj("obj.skillcape_farming")
        f.player.inv[1] = InvObj("obj.fairy_enchanted_secateurs")
        assertEquals(10, f.equipment.harvestItemPercent(f.player, herb))
        f.player.worn[1] = f.player.inv[0]
        f.player.inv[0] = null
        assertEquals(15, f.equipment.harvestItemPercent(f.player, herb))
        assertEquals(10, f.equipment.harvestItemPercent(f.player, allotment))
        val seaweed = FarmingCrops.all.first { it.kind == PatchKind.SEAWEED }
        assertEquals(0, f.equipment.harvestItemPercent(f.player, seaweed))
    }

    @Test
    fun `offline replay emits only the final nature status and unchanged regrowth does not notify`() {
        val crop = FarmingCrops.all.first { it.kind == PatchKind.HERB }
        val growing = PatchState(weeds = 3, cropIndex = crop.index, stage = 1)
        val diseased = growing.copy(health = Health.DISEASED)
        val dead = diseased.copy(health = Health.DEAD)
        val mature = growing.copy(stage = crop.stages, produce = 3)
        assertTrue(FarmingNatureAmulet.shouldNotify(dead, growing))
        assertFalse(FarmingNatureAmulet.shouldNotify(dead, dead))
        assertTrue(FarmingNatureAmulet.shouldNotify(diseased, growing))
        assertTrue(FarmingNatureAmulet.shouldNotify(mature, growing))
        assertFalse(FarmingNatureAmulet.shouldNotify(mature.copy(produce = 4), mature))
        assertFalse(FarmingNatureAmulet.shouldNotify(growing.empty(), dead))
    }

    @Test
    fun `only completed barehanded training waives the dibber and grapes still require it`() {
        val f = Fixture()
        val herb = FarmingCrops.all.first { it.kind == PatchKind.HERB }
        val grape = FarmingCrops.all.first { it.kind == PatchKind.GRAPE_VINE }
        assertTrue(f.equipment.needsDibber(f.player, herb))
        VarPlayerIntMapSetter.set(f.player, "varbit.brut_farming_planting", 3)
        assertFalse(f.equipment.needsDibber(f.player, herb))
        assertTrue(f.equipment.needsDibber(f.player, grape))
        assertTrue(f.equipment.barehandedPlanting(f.player))
    }

    @Test
    fun `first pot trial unlocks smashing once while later planting respects the toggle`() {
        val f = Fixture()
        assertFalse(f.equipment.smashesPlantPot(f.player))
        VarPlayerIntMapSetter.set(f.player, "varbit.brut_farming_smashing", 1)
        assertTrue(f.equipment.smashesPlantPot(f.player))
        f.equipment.saplingPlanted(f.player)
        assertEquals(2, f.player.vars["varbit.brut_farming_smashing"])
        assertTrue(f.equipment.smashesPlantPot(f.player))
        VarPlayerIntMapSetter.set(f.player, "varbit.brut_smash_pots_automatically", 0)
        assertFalse(f.equipment.smashesPlantPot(f.player))
        f.equipment.saplingPlanted(f.player)
        assertEquals(0, f.player.vars["varbit.brut_smash_pots_automatically"])
        VarPlayerIntMapSetter.set(f.player, "varbit.brut_farming_smashing", 3)
        VarPlayerIntMapSetter.set(f.player, "varbit.brut_smash_pots_automatically", 1)
        assertTrue(f.equipment.smashesPlantPot(f.player))
    }

    @Test
    fun `auto weed unlock and free toggles preserve existing patch weeds`() {
        val f = Fixture()
        val patch = FarmingPatches.all.first { it.kind == PatchKind.FLOWER }
        val weeds = PatchState(weeds = 1, scarecrow = true)
        VarPlayerIntMapSetter.set(f.player, patch.varp, weeds.pack())
        FarmingAutoWeed.toggle(f.player)
        assertFalse(FarmingAutoWeed.unlocked(f.player))
        FarmingAutoWeed.completePurchase(f.player)
        assertTrue(FarmingAutoWeed.active(f.player))
        assertTrue(FarmingAutoWeed.purchaseRefusal(f.player) != null)
        FarmingAutoWeed.toggle(f.player)
        assertTrue(FarmingAutoWeed.unlocked(f.player))
        assertFalse(FarmingAutoWeed.active(f.player))
        FarmingAutoWeed.completePurchase(f.player)
        assertFalse(FarmingAutoWeed.active(f.player))
        FarmingAutoWeed.toggle(f.player)
        assertTrue(FarmingAutoWeed.active(f.player))
        assertEquals(weeds, PatchState.unpack(f.player.vars[patch.varp]))
    }

    @Test
    fun `both scarecrow assembly stages require level 23 and only completion awards XP`() {
        val f = Fixture()
        val script = FarmingScarecrowScript(XpModifiers(emptySet()))
        f.fill()
        f.player.inv[0] = InvObj("obj.scarecrow_torso")
        f.player.inv[1] = InvObj("obj.bronze_spear")
        f.player.inv[2] = InvObj("obj.watermelon")
        val before = f.player.inv.toList()
        val xp = f.player.statMap.getFineXP(STAT_FARMING)
        f.player.statMap.setCurrentLevel(STAT_FARMING, 22)
        f.run { with(script) { assemble("obj.scarecrow_torso", 0, "obj.bronze_spear", 1, "obj.scarecrow_torso_spear", false) } }
        assertEquals(before, f.player.inv.toList())
        assertEquals(xp, f.player.statMap.getFineXP(STAT_FARMING))
        f.player.statMap.setCurrentLevel(STAT_FARMING, 23)
        f.run { with(script) { assemble("obj.scarecrow_torso", 0, "obj.bronze_spear", 1, "obj.scarecrow_torso_spear", false) } }
        assertEquals(0, f.player.inv.count("obj.scarecrow_torso"))
        assertEquals(0, f.player.inv.count("obj.bronze_spear"))
        assertEquals(1, f.player.inv.count("obj.scarecrow_torso_spear"))
        assertEquals(1, f.player.inv.count("obj.watermelon"))
        assertEquals(25, f.player.inv.count(SPADE))
        assertEquals(xp, f.player.statMap.getFineXP(STAT_FARMING))
        val torso = f.player.inv.indices.first { f.player.inv[it]?.id == "obj.scarecrow_torso_spear".asRSCM() }
        val assembled = f.player.inv.toList()
        f.player.statMap.setCurrentLevel(STAT_FARMING, 22)
        f.run { with(script) { assemble("obj.scarecrow_torso_spear", torso, "obj.watermelon", 2, FarmingScarecrow.SCARECROW, true) } }
        assertEquals(assembled, f.player.inv.toList())
        assertEquals(xp, f.player.statMap.getFineXP(STAT_FARMING))
        f.player.statMap.setCurrentLevel(STAT_FARMING, 23)
        f.run { with(script) { assemble("obj.scarecrow_torso_spear", torso, "obj.watermelon", 2, FarmingScarecrow.SCARECROW, true) } }
        assertEquals(0, f.player.inv.count("obj.scarecrow_torso_spear"))
        assertEquals(0, f.player.inv.count("obj.watermelon"))
        assertEquals(1, f.player.inv.count(FarmingScarecrow.SCARECROW))
        assertEquals(25, f.player.inv.count(SPADE))
        assertEquals(xp + 250, f.player.statMap.getFineXP(STAT_FARMING))
    }

    @Test
    fun `scarecrow torso refuses poisoned noted or stale selected spears without taking another copy`() {
        val f = Fixture()
        val script = FarmingScarecrowScript(XpModifiers(emptySet()))
        val xp = f.player.statMap.getFineXP(STAT_FARMING)
        for (spear in listOf("obj.bronze_spear_p", "obj.bronze_spear_p+", "obj.bronze_spear_p++", "obj.cert_bronze_spear", SPADE)) {
            f.fill()
            f.player.inv[0] = InvObj("obj.scarecrow_torso")
            f.player.inv[1] = InvObj(spear)
            f.player.inv[2] = InvObj("obj.bronze_spear")
            val before = f.player.inv.toList()
            f.run { with(script) { assemble("obj.scarecrow_torso", 0, "obj.bronze_spear", 1, "obj.scarecrow_torso_spear", false) } }
            assertEquals(before, f.player.inv.toList(), spear)
            assertEquals(xp, f.player.statMap.getFineXP(STAT_FARMING), spear)
        }
    }

    @Test
    fun `scarecrow assembly consumes both components with full inventory and cannot repeat the reward`() {
        val f = Fixture()
        val script = FarmingScarecrowScript(XpModifiers(emptySet()))
        f.fill()
        f.player.inv[0] = InvObj("obj.scarecrow_torso_spear")
        f.player.inv[1] = InvObj("obj.watermelon")
        val xp = f.player.statMap.getFineXP(STAT_FARMING)
        repeat(2) {
            f.run { with(script) { assemble("obj.scarecrow_torso_spear", 0, "obj.watermelon", 1, FarmingScarecrow.SCARECROW, true) } }
        }
        assertEquals(0, f.player.inv.count("obj.scarecrow_torso_spear"))
        assertEquals(0, f.player.inv.count("obj.watermelon"))
        assertEquals(1, f.player.inv.count(FarmingScarecrow.SCARECROW))
        assertEquals(xp + 250, f.player.statMap.getFineXP(STAT_FARMING))
    }

    @Test
    fun `vanished or noted scarecrow component leaves the other item and XP untouched`() {
        val f = Fixture()
        val script = FarmingScarecrowScript(XpModifiers(emptySet()))
        f.player.inv[0] = InvObj("obj.scarecrow_torso_spear")
        f.player.inv[1] = InvObj("obj.cert_watermelon")
        val before = f.player.inv.toList()
        val xp = f.player.statMap.getFineXP(STAT_FARMING)
        f.run { with(script) { assemble("obj.scarecrow_torso_spear", 0, "obj.watermelon", 1, FarmingScarecrow.SCARECROW, true) } }
        assertEquals(before, f.player.inv.toList())
        f.player.inv[1] = null
        f.run { with(script) { assemble("obj.scarecrow_torso_spear", 0, "obj.watermelon", 1, FarmingScarecrow.SCARECROW, true) } }
        assertEquals(1, f.player.inv.count("obj.scarecrow_torso_spear"))
        assertEquals(0, f.player.inv.count(FarmingScarecrow.SCARECROW))
        assertEquals(xp, f.player.statMap.getFineXP(STAT_FARMING))
    }

    @Test
    fun `scarecrow placement rejects occupied patches and consumes exactly one on weeded soil`() {
        val f = Fixture()
        val patch = FarmingPatches.all.first { it.kind == PatchKind.FLOWER }
        val service = FarmingService(XpModifiers(emptySet()), f.random, f.events,
            FarmingRewardTestSupport.rewards(f.random, f.events), f.equipment, farmingSiteAccess())
        val scarecrow = FarmingScarecrow(service)
        f.fill()
        f.player.inv[0] = InvObj(FarmingScarecrow.SCARECROW)
        val flower = checkNotNull(FarmingCrops.bySeed("obj.marigold_seed"))
        for (state in listOf(PatchState(weeds = 1), PatchState(weeds = 3, cropIndex = flower.index))) {
            VarPlayerIntMapSetter.set(f.player, patch.varp, state.pack())
            f.run { with(scarecrow) { place(patch) } }
            assertEquals(state, service.stored(f.player, patch))
            assertEquals(1, f.player.inv.count(FarmingScarecrow.SCARECROW))
        }
        val empty = PatchState(weeds = 3, compost = Compost.SUPER)
        VarPlayerIntMapSetter.set(f.player, patch.varp, empty.pack())
        repeat(2) { f.run { with(scarecrow) { place(patch) } } }
        assertEquals(empty.copy(scarecrow = true), service.stored(f.player, patch))
        assertEquals(0, f.player.inv.count(FarmingScarecrow.SCARECROW))
        assertEquals(27, f.player.inv.count(SPADE))
        repeat(2) { f.run { with(scarecrow) { remove(patch) } } }
        assertEquals(empty, service.stored(f.player, patch))
        assertEquals(1, f.player.inv.count(FarmingScarecrow.SCARECROW))
        assertEquals(27, f.player.inv.count(SPADE))
    }

    @Test
    fun `full inventory cannot remove or duplicate a placed scarecrow and weeds survive removal`() {
        val f = Fixture()
        val patch = FarmingPatches.all.first { it.kind == PatchKind.FLOWER }
        val service = FarmingService(XpModifiers(emptySet()), f.random, f.events,
            FarmingRewardTestSupport.rewards(f.random, f.events), f.equipment, farmingSiteAccess())
        val scarecrow = FarmingScarecrow(service)
        val placed = PatchState(weeds = 1, scarecrow = true)
        VarPlayerIntMapSetter.set(f.player, patch.varp, placed.pack())
        f.fill()
        f.run { with(scarecrow) { remove(patch) } }
        assertEquals(placed, service.stored(f.player, patch))
        assertEquals(0, f.player.inv.count(FarmingScarecrow.SCARECROW))
        f.player.inv[0] = null
        repeat(2) { f.run { with(scarecrow) { remove(patch) } } }
        assertEquals(placed.copy(scarecrow = false), service.stored(f.player, patch))
        assertEquals(1, f.player.inv.count(FarmingScarecrow.SCARECROW))
    }

    private class FixedRandom : GameRandom {
        var calls = 0
        override fun of(maxExclusive: Int): Int { calls++; return 0 }
        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
        override fun randomDouble(): Double = 0.0
    }

    private class Fixture {
        val random = FixedRandom()
        val equipment = FarmingEquipment(random)
        val events = EventBus()

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = object : Client<Any, Any> {
                override fun write(message: Any) {}
                override fun close() {}
                override fun read(player: Player) {}
                override fun flush() {}
                override fun flushHighPriority() {}
                override fun unregister(service: Any, player: Player) {}
            }
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            statMap.setBaseLevel(STAT_FARMING, 99)
            statMap.setCurrentLevel(STAT_FARMING, 99)
        }
        init { VarPlayerIntMapSetter.set(player, "varp.farming_clock", Int.MAX_VALUE) }
        fun fill() { for (slot in player.inv.indices) player.inv[slot] = InvObj(SPADE) }
        fun run(action: suspend ProtectedAccess.() -> Unit) {
            val coroutine = GameCoroutine("farming-equipment-test")
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, ProtectedAccessContextFactory.empty().copy(getEventBus = { events }))
            var outcome: Result<Unit>? = null
            val body: suspend () -> Unit = { access.action() }
            body.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { outcome = result }
            })
            checkNotNull(outcome).getOrThrow()
        }
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
