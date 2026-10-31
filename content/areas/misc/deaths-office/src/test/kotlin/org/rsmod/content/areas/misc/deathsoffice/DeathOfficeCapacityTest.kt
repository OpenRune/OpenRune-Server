package org.rsmod.content.areas.misc.deathsoffice

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import io.mockk.every
import io.mockk.mockk
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.ResumePauseButtonHandler
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.content.other.gravestones.GraveFees
import org.rsmod.content.other.gravestones.Gravestones
import org.rsmod.content.other.gravestones.deathCoffer
import org.rsmod.content.other.gravestones.deathOffice
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(InternalApi::class)
@ResourceLock("ServerCacheManager")
class DeathOfficeCapacityTest {
    @Test fun paidSelectedStackChargesOnlyTheTenUnitsThatFitForNormalAndIronman() {
        for (ironman in listOf(false, true)) {
            val f = Fixture(ironman); f.nearlyFull(); f.player.deathCoffer = 1_000_000
            f.click("items", 0); f.click("all")
            assertEquals(Int.MAX_VALUE, f.player.inv.count(NOTE)); assertEquals(30, f.player.deathOffice.count(NOTE))
            assertEquals(1_000_000 - f.feeEach * 10, f.player.deathCoffer)
            assertEquals(f.feeEach * 10, f.player.vars["varp.tracking_coins_lost"])
            assertEquals(26, f.player.inv.count("obj.black_bead")); assertEquals(0, f.xp())
            val before = f.snapshot(); f.click("all"); assertEquals(before, f.snapshot())
        }
    }

    @Test fun takeEverythingSharesCapacityAcrossRepeatedOfficeSlotsAndChargesOnlyMovedUnits() {
        val f = Fixture(true); f.nearlyFull(); f.player.deathOffice[0] = InvObj(NOTE, 20); f.player.deathOffice[1] = InvObj(NOTE, 20)
        f.player.deathCoffer = 1_000_000; f.click("takeall"); f.confirm(true)
        assertEquals(Int.MAX_VALUE, f.player.inv.count(NOTE)); assertEquals(30, f.player.deathOffice.filterNotNull { it.id == NOTE.asRSCM() }.sumOf { it.count }); assertEquals(10, f.player.deathOffice[0]?.count)
        assertEquals(1_000_000 - f.feeEach * 10, f.player.deathCoffer); assertEquals(26, f.player.inv.count("obj.black_bead"))
    }

    @Test fun alreadyFullStackCannotChargeEitherSelectedOrWholeOfficeReclaim() {
        for (all in listOf(false, true)) {
            val f = Fixture(); f.nearlyFull(); f.player.inv[0] = InvObj(NOTE, Int.MAX_VALUE); f.player.deathCoffer = 1_000_000
            val before = f.snapshot()
            if (all) { f.click("takeall"); f.confirm(true) } else { f.click("items", 0); f.click("all") }
            assertEquals(before, f.snapshot())
        }
    }

    @Test fun insufficientFundsAndCancelledWholeOfficePaymentLeaveEveryUnitAndFeeIntact() {
        for (cancel in listOf(false, true)) {
            val f = Fixture(); f.nearlyFull(); f.player.deathCoffer = if (cancel) 1_000_000 else f.feeEach * 10 - 1
            val before = f.snapshot()
            if (cancel) { f.click("takeall"); f.confirm(false) } else { f.click("items", 0); f.click("all") }
            assertEquals(before, f.snapshot())
        }
    }

    @Test fun inventoryAndBankPaymentChargeOnlyTheQuantityThatFits() {
        for (bank in listOf(false, true)) {
            val f = Fixture(true); f.nearlyFull(); val cash = if (bank)f.player.invMap.getOrPut("inv.bank")else f.player.inv
            cash[if (bank)0 else 27] = InvObj("obj.coins", 1_000_000)
            f.click("items", 0); f.click("all")
            assertEquals(1_000_000 - f.feeEach * 10, cash.count("obj.coins")); assertEquals(30, f.player.deathOffice.count(NOTE)); assertEquals(Int.MAX_VALUE, f.player.inv.count(NOTE))
            assertEquals(0, f.player.deathCoffer)
        }
    }

    @Test fun capacityIsRecomputedAfterWholeOfficeConfirmation() {
        val f = Fixture(); f.nearlyFull(); f.player.deathCoffer = 1_000_000; f.click("takeall")
        f.player.inv[0] = InvObj(NOTE, Int.MAX_VALUE); val before = f.snapshot(); f.confirm(true)
        assertEquals(before, f.snapshot())
    }

    @Test fun freeCoinsAlsoReclaimOnlyTheirRemainingStackCapacity() {
        val f = Fixture(); f.player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE - 10); f.player.deathOffice[0] = InvObj("obj.coins", 40)
        val before = f.player.deathCoffer; f.click("items", 0)
        assertEquals(Int.MAX_VALUE, f.player.inv.count("obj.coins")); assertEquals(30, f.player.deathOffice.count("obj.coins")); assertEquals(before, f.player.deathCoffer)
    }

    @Test fun wholeOfficeCanSkipAFullStackAndStillReclaimAnotherAffordableItem() {
        val f = Fixture(); f.nearlyFull(); f.player.inv[0] = InvObj(NOTE, Int.MAX_VALUE); f.player.deathOffice[1] = InvObj("obj.black_bead", 1); f.player.deathCoffer = 1_000_000
        f.click("takeall"); f.confirm(true)
        assertEquals(40, f.player.deathOffice.count(NOTE)); assertEquals(27, f.player.inv.count("obj.black_bead")); assertEquals(0, f.player.deathOffice.count("obj.black_bead")); assertEquals(1_000_000, f.player.deathCoffer)
    }

    private class Fixture(ironman: Boolean = false) {
        private val events = EventBus()
        private val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events }, getAlignment = { TextAlignment() })
        val player = Player().apply {
            client = mockk<Client<Any, Any>>(relaxed = true); uuid = 600; observerUUID = 600; slotId = 1; assignUid(); members = true
            currentMapClock = 100; processedMapClock = 100; gamemode = if (ironman)1 else 0
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }
        val feeEach = GraveFees.officeFee(200_000, ironman)
        init {
            val graves = mockk<Gravestones>(); every { graves.value(any()) } answers { if (firstArg<InvObj>().id == NOTE.asRSCM())200_000L else 1L }
            with(DeathsOfficeScript(graves, mockk(), mockk())) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
        }
        fun nearlyFull() {
            player.inv[0] = InvObj(NOTE, Int.MAX_VALUE - 10); player.deathOffice[0] = InvObj(NOTE, 40)
            for (slot in 1..26)player.inv[slot] = InvObj("obj.black_bead", 1)
        }
        fun click(component: String, slot: Int = 0) {
            ProtectedAccessLauncher.withProtectedAccess(player, context) {
                assertTrue(events.publish(this, IfModalButton(ServerCacheManager.fromComponent("component.death_office:" + component), slot, null, IfButtonOp.Op1)))
            }
            player.advanceActiveCoroutine()
        }
        fun confirm(yes: Boolean) {
            assertTrue(player.ui.containsModal("interface.chatmenu"))
            val packed = "component.chatmenu:options".asRSCM()
            ResumePauseButtonHandler().handle(player, ResumePauseButton(CombinedId(packed ushr 16, packed and 0xffff), if (yes)1 else 2))
        }
        fun xp() = player.statMap.getFineXP("stat.hitpoints")
        fun snapshot() = listOf(player.inv.objs.toList(), player.deathOffice.objs.toList(), player.invMap.getOrPut("inv.bank").objs.toList(), player.deathCoffer, player.vars["varp.tracking_coins_lost"], xp())
    }
    companion object {
        private const val NOTE = "obj.cert_dragon_platelegs"
        private val restore = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            ServerCacheManager.init(241).close()
            val note = checkNotNull(ServerCacheManager.getItem(NOTE.asRSCM())); assertTrue(note.isStackable)
            for ((owner, name) in listOf("org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions", "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage")) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }; val old = field.get(null); restore += { field.set(null, old) }
            }
            val storage = InvVirtualStorageHolder.instance; restore += { InvVirtualStorageHolder.instance = storage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
        }

        @JvmStatic @AfterAll fun restore() { restore.asReversed().forEach { it() }; restore.clear() }
    }
}
