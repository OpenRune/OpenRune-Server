package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUDefaultEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ironman.PlayerGamemode
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

private var Player.bankCapacity by intVarBit("varbit.bank_capacity")
private var Player.emptyHerbSack by intVarBit("varbit.emptyondeath_herbsack")
private var Player.emptySeedBox by intVarBit("varbit.emptyondeath_seedbox")

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingBagStorageTest {
    @Test
    fun `bulk tool deposit conserves capacity remainder and can charges across repeat clicks`() {
        val f = Fixture()
        val buckets = FarmingLeprechaunStorage.tools.single { it.component == "bucket" }
        val can = FarmingLeprechaunStorage.tools.single { it.component == "wateringcan" }
        val trowel = FarmingLeprechaunStorage.tools.single { it.component == "trowel" }
        val initial = f.give("obj.cert_bucket_empty", buckets.capacity - 10)
        assertEquals(buckets.capacity - 10, FarmingLeprechaunStorage.deposit(f.player, initial, Int.MAX_VALUE))
        f.give("obj.cert_bucket_empty", 20)
        f.give("obj.watering_can_7")
        f.give("obj.gardening_trowel")
        f.give("obj.tomato", 25)

        f.depositTools()
        assertEquals(buckets.capacity, FarmingLeprechaunStorage.stored(f.player, buckets)?.count)
        assertEquals(10, f.count("obj.cert_bucket_empty"))
        assertEquals("obj.watering_can_7".asRSCM(), FarmingLeprechaunStorage.stored(f.player, can)?.id)
        assertEquals(1, FarmingLeprechaunStorage.stored(f.player, trowel)?.count)
        assertEquals(0, f.count("obj.watering_can_7"))
        assertEquals(0, f.count("obj.gardening_trowel"))
        assertEquals(25, f.count("obj.tomato"))

        f.depositTools()
        assertEquals(buckets.capacity, FarmingLeprechaunStorage.stored(f.player, buckets)?.count)
        assertEquals(10, f.count("obj.cert_bucket_empty"))
        f.give("obj.tomato", 2)
        assertEquals(0, FarmingLeprechaunStorage.withdraw(f.player, "wateringcan", 1))
        assertEquals("obj.watering_can_7".asRSCM(), FarmingLeprechaunStorage.stored(f.player, can)?.id)
        f.player.inv[27] = null
        assertEquals(1, FarmingLeprechaunStorage.withdraw(f.player, "wateringcan", 1))
        assertEquals(0, FarmingLeprechaunStorage.withdraw(f.player, "wateringcan", 1))
        assertEquals(1, f.count("obj.watering_can_7"))
        assertEquals(26, f.count("obj.tomato"))
    }

    @Test
    fun `herb sack fill stops at 30 per herb and ignores clean and noted herbs`() {
        val f = Fixture()
        val sack = f.give(FarmingBags.HERB_SACK)
        f.give("obj.unidentified_guam", 20)
        f.give("obj.guam_leaf", 2)
        f.give("obj.cert_unidentified_guam", 50)
        f.op1(sack)
        assertEquals(20, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))

        f.give("obj.unidentified_guam", 15)
        f.op1(sack)
        assertEquals(30, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertEquals(5, f.count("obj.unidentified_guam"))
        assertEquals(2, f.count("obj.guam_leaf"))
        assertEquals(50, f.count("obj.cert_unidentified_guam"))
    }

    @Test
    fun `silklined herb sack holds 100 of each herb`() {
        val f = Fixture()
        val guam = "obj.unidentified_guam".asRSCM()
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 99)
        assertEquals(1, FarmingBags.herbSack.room(f.player, FarmingBags.SILKLINED_HERB_SACK, guam))
        assertEquals(0, FarmingBags.herbSack.room(f.player, FarmingBags.HERB_SACK, guam))
    }

    @Test
    fun `herb sack empty moves lowest level herbs first and only what fits`() {
        val f = Fixture()
        val sack = f.give(FarmingBags.HERB_SACK)
        f.store(FarmingBags.herbSack, "obj.unidentified_marentill", 5)
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 10)
        f.fillExcept(6)
        f.op4(sack)
        assertEquals(6, f.count("obj.unidentified_guam"))
        assertEquals(4, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertEquals(5, f.stored(FarmingBags.herbSack, "obj.unidentified_marentill"))
        assertEquals(0, f.count("obj.unidentified_marentill"))
    }

    @Test
    fun `harvest splits between open sack and inventory and is all or nothing`() {
        val f = Fixture()
        f.give(FarmingBags.HERB_SACK_OPEN)
        f.store(FarmingBags.herbSack, "obj.unidentified_ranarr", 28)
        f.fillExcept(1)

        assertTrue(FarmingHarvestStorage.canAddHarvest(f.player, "obj.unidentified_ranarr", 3))
        assertEquals(28, f.stored(FarmingBags.herbSack, "obj.unidentified_ranarr"))
        assertEquals(0, f.count("obj.unidentified_ranarr"))

        assertFalse(FarmingHarvestStorage.addHarvest(f.player, "obj.unidentified_ranarr", 4))
        assertEquals(28, f.stored(FarmingBags.herbSack, "obj.unidentified_ranarr"))
        assertEquals(0, f.count("obj.unidentified_ranarr"))

        assertTrue(FarmingHarvestStorage.addHarvest(f.player, "obj.unidentified_ranarr", 3))
        assertEquals(30, f.stored(FarmingBags.herbSack, "obj.unidentified_ranarr"))
        assertEquals(1, f.count("obj.unidentified_ranarr"))
    }

    @Test
    fun `harvest ignores a closed sack and fills the open reagent pouch to 26`() {
        val f = Fixture()
        f.give(FarmingBags.HERB_SACK)
        assertTrue(FarmingHarvestStorage.addHarvest(f.player, "obj.unidentified_guam", 2))
        assertEquals(0, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertEquals(2, f.count("obj.unidentified_guam"))

        f.give(FarmingBags.REAGENT_POUCH_OPEN)
        f.store(FarmingBags.reagentPouch, "obj.eye_of_newt", 24)
        assertTrue(FarmingHarvestStorage.addHarvest(f.player, "obj.eye_of_newt", 3))
        assertEquals(26, f.stored(FarmingBags.reagentPouch, "obj.eye_of_newt"))
        assertEquals(1, f.count("obj.eye_of_newt"))
    }

    @Test
    fun `seed box holds six types and caps each stack at int max`() {
        val f = Fixture()
        val box = f.give(FarmingBags.SEED_BOX)
        for (seed in SIX_SEEDS) f.store(FarmingBags.seedBox, seed, 1)
        f.give("obj.watermelon_seed", 4)
        f.give("obj.potato_seed", 7)
        f.op1(box)
        assertEquals(4, f.count("obj.watermelon_seed"))
        assertEquals(8, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
        assertEquals(0, f.count("obj.potato_seed"))

        FarmingBags.seedBox.remove(f.player, "obj.potato_seed", 8)
        f.store(FarmingBags.seedBox, "obj.potato_seed", Int.MAX_VALUE - 5)
        f.give("obj.potato_seed", 10)
        f.op1(box)
        assertEquals(Int.MAX_VALUE, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
        assertEquals(5, f.count("obj.potato_seed"))
    }

    @Test
    fun `seed box keeps slot order for first suitable seed and strict removal`() {
        val f = Fixture()
        f.store(FarmingBags.seedBox, "obj.onion_seed", 2)
        f.store(FarmingBags.seedBox, "obj.cabbage_seed", 3)
        f.store(FarmingBags.seedBox, "obj.potato_seed", 4)
        val allotment = { type: ItemServerType -> type.internalName != "obj.onion_seed" }
        assertEquals("obj.cabbage_seed", FarmingBags.firstSuitableSeed(f.player, allotment)?.internalName)

        assertFalse(FarmingBags.seedBox.remove(f.player, "obj.cabbage_seed", 4))
        assertEquals(3, FarmingBags.seedBox.count(f.player, "obj.cabbage_seed"))
        assertTrue(FarmingBags.seedBox.remove(f.player, "obj.cabbage_seed", 3))
        assertNull(FarmingBags.seedBox.store(f.player)[1])
        assertEquals("obj.potato_seed", FarmingBags.firstSuitableSeed(f.player, allotment)?.internalName)
    }

    @Test
    fun `seed box withdraw only moves what fits and repeated clicks cannot overdraw`() {
        val f = Fixture()
        f.give(FarmingBags.SEED_BOX)
        f.store(FarmingBags.seedBox, "obj.potato_seed", 3)
        f.fillExcept(0)
        f.withdraw(0, IfButtonOp.Op5)
        assertEquals(3, f.stored(FarmingBags.seedBox, "obj.potato_seed"))

        f.player.inv[27] = null
        f.withdraw(0, IfButtonOp.Op3)
        assertEquals(0, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
        assertEquals(3, f.count("obj.potato_seed"))
        f.withdraw(0, IfButtonOp.Op3)
        assertEquals(3, f.count("obj.potato_seed"))
    }

    @Test
    fun `seed box withdraw rejects a stale or missing clicked item before transferring`() {
        val f = Fixture()
        f.give(FarmingBags.SEED_BOX)
        f.store(FarmingBags.seedBox, "obj.onion_seed", 7)
        val potato = f.type(InvObj("obj.potato_seed"))
        for (op in listOf(IfButtonOp.Op1, IfButtonOp.Op4, IfButtonOp.Op5)) {
            for (clicked in listOf(potato, null)) {
                f.withdrawClicked(0, op, clicked)
                assertEquals(7, f.stored(FarmingBags.seedBox, "obj.onion_seed"))
                assertEquals(0, f.count("obj.onion_seed"))
                assertEquals(0, f.count("obj.potato_seed"))
            }
        }
    }

    @Test
    fun `seed box withdraw with the matching clicked item conserves exactly and an emptied source adds nothing`() {
        val f = Fixture()
        f.give(FarmingBags.SEED_BOX)
        f.store(FarmingBags.seedBox, "obj.onion_seed", 7)
        val onion = f.type(InvObj("obj.onion_seed"))
        f.withdrawClicked(0, IfButtonOp.Op1, onion)
        assertEquals(1, f.count("obj.onion_seed"))
        assertEquals(6, f.stored(FarmingBags.seedBox, "obj.onion_seed"))
        f.withdrawClicked(0, IfButtonOp.Op5, onion)
        assertEquals(7, f.count("obj.onion_seed"))
        assertEquals(0, f.stored(FarmingBags.seedBox, "obj.onion_seed"))
        f.withdrawClicked(0, IfButtonOp.Op5, onion)
        assertEquals(7, f.count("obj.onion_seed"))
        assertEquals(0, f.stored(FarmingBags.seedBox, "obj.onion_seed"))
    }

    @Test
    fun `stale open-close click does not toggle twice`() {
        val f = Fixture()
        val sack = f.give(FarmingBags.HERB_SACK)
        val stale = f.held(sack)
        f.op2(sack)
        assertEquals(FarmingBags.HERB_SACK_OPEN, f.at(sack))
        f.publish(HeldObjEvents.Op2(sack, stale, f.type(stale), f.player.inv))
        assertEquals(FarmingBags.HERB_SACK_OPEN, f.at(sack))
    }

    @Test
    fun `herb used on sack moves exactly one and other items are refused`() {
        val f = Fixture()
        val sack = f.give(FarmingBags.HERB_SACK)
        val herb = f.give("obj.unidentified_guam", 2)
        val leaf = f.give("obj.guam_leaf")
        f.use(sack, herb)
        f.use(sack, leaf)
        assertEquals(1, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertEquals(1, f.count("obj.unidentified_guam"))
        assertEquals(1, f.count("obj.guam_leaf"))
    }

    @Test
    fun `pickup hook only stores into an open bag with room for the whole stack`() {
        val f = Fixture()
        val hook = FarmingBagTakeHook()
        val guam = checkNotNull(ServerCacheManager.getItem("obj.unidentified_guam".asRSCM()))
        val sack = f.give(FarmingBags.HERB_SACK)
        assertFalse(hook.canReceive(f.player, guam, 1))

        f.op2(sack)
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 29)
        assertTrue(hook.receive(f.player, guam, 1))
        assertEquals(30, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertFalse(hook.canReceive(f.player, guam, 1))
        assertFalse(hook.receive(f.player, guam, 1))
        assertEquals(30, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
    }

    @Test
    fun `master farmer seed goes to an open box only when it all fits`() {
        val f = Fixture()
        assertFalse(FarmingBags.storePickpocketSeed(f.player, "obj.potato_seed", 2))
        f.give(FarmingBags.SEED_BOX_OPEN)
        for (seed in SIX_SEEDS) f.store(FarmingBags.seedBox, seed, 1)
        assertFalse(FarmingBags.storePickpocketSeed(f.player, "obj.watermelon_seed", 2))
        assertTrue(FarmingBags.storePickpocketSeed(f.player, "obj.potato_seed", 2))
        assertEquals(3, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
    }

    @Test
    fun `pickpocket capacity dry run does not award seeds and stale box cannot receive`() {
        val f = Fixture()
        val box = f.give(FarmingBags.SEED_BOX_OPEN)
        f.store(FarmingBags.seedBox, "obj.potato_seed", Int.MAX_VALUE - 2)
        f.fillExcept(0)
        assertTrue(FarmingBags.storePickpocketSeed(f.player, "obj.potato_seed", 2, autoCommit = false))
        assertEquals(Int.MAX_VALUE - 2, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
        assertFalse(FarmingBags.storePickpocketSeed(f.player, "obj.potato_seed", 3))
        assertEquals(Int.MAX_VALUE - 2, f.stored(FarmingBags.seedBox, "obj.potato_seed"))

        f.player.inv[box] = InvObj(FarmingBags.SEED_BOX)
        assertFalse(FarmingBags.storePickpocketSeed(f.player, "obj.potato_seed", 2))
        f.player.inv[box] = InvObj(FarmingBags.SEED_BOX_OPEN)
        assertTrue(FarmingBags.storePickpocketSeed(f.player, "obj.potato_seed", 2))
        assertFalse(FarmingBags.storePickpocketSeed(f.player, "obj.potato_seed", 1))
        assertEquals(Int.MAX_VALUE, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
        assertEquals(0, f.count("obj.potato_seed"))
    }

    @Test
    fun `pvm retains contents in lost herb sacks and seed boxes even in deep wilderness`() {
        val f = Fixture()
        f.give(FarmingBags.SILKLINED_HERB_SACK_OPEN)
        f.give(FarmingBags.SEED_BOX)
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 100)
        f.store(FarmingBags.seedBox, "obj.potato_seed", Int.MAX_VALUE)
        val result = f.death(wilderness = 30)
        assertEquals(100, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertEquals(Int.MAX_VALUE, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
        assertEquals(
            listOf(InvObj(FarmingBags.SILKLINED_HERB_SACK_OPEN), InvObj(FarmingBags.SEED_BOX)),
            result.lostUntradeable,
        )
        assertTrue(result.lostTradeable.isEmpty())
    }

    @Test
    fun `native empty settings spill contents even when both containers are kept`() {
        val f = Fixture()
        f.give(FarmingBags.HERB_SACK)
        f.give(FarmingBags.SEED_BOX_OPEN)
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 30)
        f.store(FarmingBags.seedBox, "obj.potato_seed", 5)
        f.player.emptyHerbSack = 1
        f.player.emptySeedBox = 1
        val result = f.death(kept = true)
        assertEquals(0, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertEquals(0, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
        assertEquals(
            listOf(InvObj("obj.unidentified_guam", 30), InvObj("obj.potato_seed", 5)),
            result.lostTradeable,
        )
        assertEquals(
            listOf(InvObj(FarmingBags.HERB_SACK), InvObj(FarmingBags.SEED_BOX_OPEN)),
            result.kept,
        )
        assertTrue(f.death(kept = true).lostTradeable.isEmpty())
    }

    @Test
    fun `pvp spills protected silklined sack and seed contents without touching banked bags`() {
        val f = Fixture()
        f.give(FarmingBags.SILKLINED_HERB_SACK)
        f.give(FarmingBags.SEED_BOX_OPEN)
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 100)
        f.store(FarmingBags.seedBox, "obj.potato_seed", Int.MAX_VALUE)
        val result = f.death(kept = true, pvp = true)
        assertEquals(0, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertEquals(0, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
        assertEquals(
            listOf(InvObj("obj.unidentified_guam", 100), InvObj("obj.potato_seed", Int.MAX_VALUE)),
            result.lostTradeable,
        )

        val banked = Fixture()
        banked.bank()[0] = InvObj(FarmingBags.HERB_SACK)
        banked.store(FarmingBags.herbSack, "obj.unidentified_guam", 20)
        assertTrue(banked.death(pvp = true).lostTradeable.isEmpty())
        assertEquals(20, banked.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
    }

    @Test
    fun `destroyed lost containers spill rather than preserve hidden contents`() {
        val f = Fixture()
        f.give(FarmingBags.HERB_SACK_OPEN)
        f.give(FarmingBags.SEED_BOX)
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 10)
        f.store(FarmingBags.seedBox, "obj.potato_seed", 2)
        val result = f.death(destroyed = true)
        assertEquals(0, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertEquals(0, f.stored(FarmingBags.seedBox, "obj.potato_seed"))
        assertEquals(
            listOf(InvObj("obj.unidentified_guam", 10), InvObj("obj.potato_seed", 2)),
            result.lostTradeable,
        )
    }

    @Test
    fun `Olivia embargo expires and unrelated stalls do not extend it`() {
        val f = Fixture()
        FarmingSeedShopAccess.recordSuccessfulTheft(f.player, "loc.seed_stall")
        assertFalse(FarmingSeedShopAccess.canTrade(f.player))
        val until = f.player.vars["varp.farming_seed_stall_theft_until"]
        FarmingSeedShopAccess.recordSuccessfulTheft(f.player, "loc.rag_market_stall")
        assertEquals(until, f.player.vars["varp.farming_seed_stall_theft_until"])
        VarPlayerIntMapSetter.set(
            f.player,
            "varp.farming_seed_stall_theft_until",
            (System.currentTimeMillis() / 1000L).toInt(),
        )
        assertTrue(FarmingSeedShopAccess.canTrade(f.player))
    }

    @Test
    fun `bank op deposits every stored herb and merges with bank stacks`() {
        val f = Fixture()
        val sack = f.give(FarmingBags.HERB_SACK)
        f.player.bankCapacity = 800
        f.bank()[0] = InvObj("obj.unidentified_guam", 3)
        VarPlayerIntMapSetter.set(f.player, "varbit.bank_tab_main", 1)
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 10)
        f.store(FarmingBags.herbSack, "obj.unidentified_marentill", 5)
        f.bankside(sack)
        assertEquals(13, f.bank().physicalCount("obj.unidentified_guam"))
        assertEquals(5, f.bank().physicalCount("obj.unidentified_marentill"))
        assertTrue(FarmingBags.herbSack.store(f.player).isEmpty())
        assertEquals(FarmingBags.HERB_SACK, f.at(sack))
    }

    @Test
    fun `bank op keeps herbs in the sack when the bank has no free slot`() {
        val f = Fixture()
        val sack = f.give(FarmingBags.HERB_SACK)
        f.player.bankCapacity = 1
        f.bank()[0] = InvObj("obj.coins", 1)
        VarPlayerIntMapSetter.set(f.player, "varbit.bank_tab_main", 1)
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 10)
        f.bankside(sack)
        assertEquals(10, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        assertEquals(0, f.bank().physicalCount("obj.unidentified_guam"))
    }

    @Test
    fun `seed box bank op never overflows a bank stack or loses seeds`() {
        val f = Fixture()
        val box = f.give(FarmingBags.SEED_BOX)
        f.player.bankCapacity = 800
        f.bank()[0] = InvObj("obj.potato_seed", Int.MAX_VALUE - 2)
        VarPlayerIntMapSetter.set(f.player, "varbit.bank_tab_main", 1)
        f.store(FarmingBags.seedBox, "obj.potato_seed", 5)
        f.store(FarmingBags.seedBox, "obj.onion_seed", 4)
        f.bankside(box)
        val banked = f.bank().physicalCount("obj.potato_seed").toLong()
        val stored = f.stored(FarmingBags.seedBox, "obj.potato_seed").toLong()
        assertEquals(Int.MAX_VALUE.toLong() + 3, banked + stored)
        assertEquals(4, f.bank().physicalCount("obj.onion_seed"))
        assertEquals(0, f.stored(FarmingBags.seedBox, "obj.onion_seed"))
    }

    @Test
    fun `bag bank deposits maintain tabs for withdrawing the final stack`() {
        for ((bag, variant, item) in listOf(
            Triple(FarmingBags.herbSack, FarmingBags.HERB_SACK, "obj.unidentified_guam"),
            Triple(FarmingBags.seedBox, FarmingBags.SEED_BOX, "obj.potato_seed"),
            Triple(FarmingBags.reagentPouch, FarmingBags.REAGENT_POUCH_OPEN, "obj.chocolate_dust"),
        )) {
            val f = Fixture()
            val held = f.give(variant)
            f.player.bankCapacity = 800
            f.store(bag, item, 2)
            f.bankside(held)
            assertEquals(2, f.bank().physicalCount(item))
            assertEquals(1, f.player.vars["varbit.bank_tab_main"])
            assertEquals(0, f.stored(bag, item))
            f.withdrawBank(0)
            assertEquals(2, f.count(item))
            assertEquals(0, f.bank().physicalCount(item))
            assertEquals(0, f.player.vars["varbit.bank_tab_main"])
        }
    }

    @Test
    fun `silk upgrade swaps the sack in place and keeps its herbs`() {
        val f = Fixture()
        val sack = f.give(FarmingBags.HERB_SACK)
        val silk = f.give("obj.slayer_spider_silk")
        f.store(FarmingBags.herbSack, "obj.unidentified_guam", 30)
        f.start(f.useEvent(sack, silk))
        f.answer(choice = 1)
        assertEquals(FarmingBags.SILKLINED_HERB_SACK, f.at(sack))
        assertEquals(0, f.count("obj.slayer_spider_silk"))
        assertEquals(0, f.count(FarmingBags.HERB_SACK))
        assertEquals(30, f.stored(FarmingBags.herbSack, "obj.unidentified_guam"))
        val guam = "obj.unidentified_guam".asRSCM()
        assertEquals(70, FarmingBags.herbSack.room(f.player, FarmingBags.SILKLINED_HERB_SACK, guam))
    }

    @Test
    fun `cancelled or stale silk upgrade changes nothing`() {
        val f = Fixture()
        val sack = f.give(FarmingBags.HERB_SACK_OPEN)
        val silk = f.give("obj.slayer_spider_silk")
        f.start(f.useEvent(sack, silk))
        f.answer(choice = 2)
        assertEquals(FarmingBags.HERB_SACK_OPEN, f.at(sack))
        assertEquals(1, f.count("obj.slayer_spider_silk"))

        f.start(f.useEvent(sack, silk))
        f.player.inv[silk] = null
        f.answer(choice = 1)
        assertEquals(FarmingBags.HERB_SACK_OPEN, f.at(sack))
        assertEquals(0, f.count(FarmingBags.SILKLINED_HERB_SACK_OPEN))
    }

    private class Fixture {
        private val coroutine = GameCoroutine("farming-bag-test")
        private val events = EventBus()
        private val banking = BankInvScript(events, WornBonuses(), WeaponSpeeds(AttackStyles()))
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(getEventBus = { events }, getAlignment = { TextAlignment() })

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 801L
            slotId = 1
            assignUid()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        init {
            with(banking) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
            with(FarmingBagScript(banking)) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
            with(FarmingLeprechaunScript()) {
                ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        fun type(obj: InvObj) = checkNotNull(ServerCacheManager.getItem(obj.id))
        fun held(slot: Int) = checkNotNull(player.inv[slot])
        fun at(slot: Int): String? = player.inv[slot]?.let { type(it).internalName }
        fun count(obj: String): Int = player.inv.physicalCount(obj)
        fun stored(bag: FarmingBag, obj: String): Int = bag.count(player, obj)

        fun store(bag: FarmingBag, obj: String, count: Int) {
            assertTrue(bag.put(player, obj.asRSCM(), count).success)
        }

        fun fillExcept(free: Int) {
            val empty = (0 until 28).filter { player.inv[it] == null }
            for (slot in empty.dropLast(free)) player.inv[slot] = InvObj("obj.spade")
        }

        fun give(obj: String, count: Int = 1): Int {
            val stackable = checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).isStackable
            var first = -1
            repeat(if (stackable) 1 else count) {
                val slot = (0 until 28).first { player.inv[it] == null }
                player.inv[slot] = InvObj(obj, if (stackable) count else 1)
                if (first == -1) first = slot
            }
            return first
        }

        fun death(
            kept: Boolean = false,
            pvp: Boolean = false,
            wilderness: Int = 0,
            destroyed: Boolean = false,
        ): DeathDropResult {
            val carried = player.inv.filterNotNull { true }
            val context = PlayerDeathContext(
                player = player,
                coords = player.coords,
                inWilderness = wilderness > 0,
                wildernessLevel = wilderness,
                inRevenantCaves = false,
                inInstance = false,
                isSkulled = false,
                hasProtectItem = false,
                recentPvpDamage = pvp,
                gamemode = PlayerGamemode.NORMAL,
                killer = null,
            )
            val handling = PlayerDeathHandling(
                keepCount = if (kept) 3 else 0,
                dropReceiver = player,
                dropDuration = 6000,
                revealDelay = 100,
                supplyPile = false,
                untradeableHandling =
                    if (destroyed) UntradeableHandling.DESTROY else UntradeableHandling.DROP,
            )
            val result = DeathDropResult(
                kept = if (kept) carried else emptyList(),
                supplyPile = emptyList(),
                lostTradeable = emptyList(),
                lostUntradeable = if (kept) emptyList() else carried,
                coinsForKiller = 0,
            )
            return FarmingBagDeathHook().prepare(context, handling, result)
        }

        fun op1(slot: Int) = publish(HeldObjEvents.Op1(slot, held(slot), type(held(slot)), player.inv))
        fun op2(slot: Int) = publish(HeldObjEvents.Op2(slot, held(slot), type(held(slot)), player.inv))
        fun op4(slot: Int) = publish(HeldObjEvents.Op4(slot, held(slot), type(held(slot)), player.inv))

        fun useEvent(bag: Int, item: Int) =
            HeldUDefaultEvents.Type(type(held(bag)), bag, type(held(item)), item)

        fun use(bag: Int, item: Int) = publish(useEvent(bag, item))

        fun withdraw(slot: Int, op: IfButtonOp) =
            withdrawClicked(slot, op, FarmingBags.seedBox.store(player)[slot]?.let(::type))

        fun withdrawClicked(slot: Int, op: IfButtonOp, clicked: ItemServerType?) {
            val layer = ServerCacheManager.fromComponent("component.hosidius_seedbox:seed_layer")
            publish(IfModalButton(layer, slot, clicked, op))
        }

        fun depositTools() {
            val component = ServerCacheManager.fromComponent("component.farming_tools:deposit_all")
            publish(IfModalButton(component, -1, null, IfButtonOp.Op1))
        }

        fun bankside(slot: Int) {
            publish(HeldBanksideEvents.Type(slot, type(held(slot))))
        }

        fun withdrawBank(slot: Int) {
            val layer = ServerCacheManager.fromComponent("component.bankmain:items")
            val obj = bank()[slot]?.let(::type)
            publish(IfModalButton(layer, slot, obj, IfButtonOp.Op7))
        }

        fun bank(): Inventory = player.invMap.getOrPut("inv.bank")

        fun publish(event: SuspendEvent<ProtectedAccess>): Boolean {
            val published = start(event)
            assertTrue(coroutine.isIdle)
            return published
        }

        /** Publishes [event], leaving the coroutine paused at its first dialogue if it opens one. */
        fun start(event: SuspendEvent<ProtectedAccess>): Boolean {
            var published = false
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { published = events.publish(access, event) }
            body.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { result.getOrThrow() }
            })
            return published
        }

        /** Clicks through the open dialogues, picking [choice] in every option menu. */
        fun answer(choice: Int) {
            repeat(10) {
                if (coroutine.isIdle) return
                assertTrue(coroutine.isAwaiting(ResumePauseButtonInput::class))
                val input =
                    when {
                        player.ui.containsModal("interface.chatmenu") ->
                            ResumePauseButtonInput("component.chatmenu:options", choice)
                        player.ui.containsModal("interface.objectbox_double") ->
                            ResumePauseButtonInput("component.objectbox_double:pausebutton", -1)
                        else -> ResumePauseButtonInput("component.objectbox:universe", -1)
                    }
                coroutine.resumeWith(input)
            }
            assertTrue(coroutine.isIdle)
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
        private val SIX_SEEDS =
            listOf(
                "obj.potato_seed",
                "obj.onion_seed",
                "obj.cabbage_seed",
                "obj.tomato_seed",
                "obj.sweetcorn_seed",
                "obj.strawberry_seed",
            )

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
