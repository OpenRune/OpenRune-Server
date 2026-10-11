@file:OptIn(org.rsmod.annotations.InternalApi::class)
package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.interact.NpcUInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.*
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class QuiverExchangeInteractionTest {
    @Test fun `actual Talk-to exchanges one quiver for exactly four thousand splinters and works with full inventory`() {
        val f = Fixture(); for (slot in 1..27) f.put(slot, "obj.lobster"); f.talk(); f.choose(0); f.confirm()
        assertEquals(0, f.player.inv.count("obj.dizanas_quiver_uncharged")); assertEquals(4000, f.player.inv.count(DizanasQuiverScript.SPLINTER)); assertEquals(28, f.player.inv.occupiedSpace())
        verifyNoInteractions(f.random, f.rewards); f.talk(); assertNull(f.player.activeCoroutine); assertEquals(4000, f.player.inv.count(DizanasQuiverScript.SPLINTER))
    }

    @Test fun `native quiver on Minimus reaches exchange and rejects charged blessed and max cape forms`() {
        for (symbol in listOf("obj.dizanas_quiver_uncharged", "obj.dizanas_quiver_uncharged_trouver")) {
            val f = Fixture(); f.put(0, symbol); f.use(); f.choose(0); f.confirm(); assertEquals(4000, f.player.inv.count(DizanasQuiverScript.SPLINTER)); assertEquals(0, f.player.inv.count(symbol))
        }
        for (symbol in org.rsmod.api.player.worn.DizanasQuiver.usable.filter { !it.contains("uncharged") }) {
            val f = Fixture(); f.put(0, symbol); f.use(); assertEquals(1, f.player.inv.count(symbol)); assertNull(f.player.activeCoroutine); verifyNoInteractions(f.random, f.rewards)
        }
    }

    @Test fun `native menu and confirmation cancellation preserve quiver`() {
        for (stage in 0..1) {
            val f = Fixture(); f.talk(); if (stage == 0) f.choose(2) else { f.choose(0); f.confirm(false) }
            assertEquals(1, f.player.inv.count("obj.dizanas_quiver_uncharged")); assertEquals(0, f.player.inv.count(DizanasQuiverScript.SPLINTER)); verifyNoInteractions(f.random, f.rewards)
        }
    }

    @Test fun `late quiver replacement removal death logout and movement never consumes inputs or rolls pet`() {
        for (mode in 0..5) {
            val f = Fixture(); f.talk(); f.choose(1)
            when (mode) { 0 -> f.put(0, "obj.dizanas_quiver_uncharged"); 1 -> f.player.inv[0] = null; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.player.pendingLogout = true; 4 -> f.player.loggingOut = true; 5 -> f.player.coords = f.player.coords.translateX(1) }
            f.confirm(); assertEquals(if (mode == 1) 0 else 1, f.player.inv.count("obj.dizanas_quiver_uncharged")); verifyNoInteractions(f.random, f.rewards)
        }
    }

    @Test fun `gamble consumes exactly one confirmed quiver and rolls one in two hundred through native pet rewards`() {
        for (roll in listOf(0, 199)) {
            val f = Fixture(); f.put(1, "obj.dizanas_quiver_uncharged"); `when`(f.random.of(200)).thenReturn(roll); f.talk(); f.choose(1); f.confirm()
            assertEquals(1, f.player.inv.count("obj.dizanas_quiver_uncharged")); assertEquals(0, f.player.inv.count(DizanasQuiverScript.SPLINTER)); verify(f.random).of(200)
            if (roll == 0) verify(f.rewards).give(f.player, QuiverExchangeScript.PET, source = "Fortis Colosseum") else verifyNoInteractions(f.rewards)
        }
    }

    @Test fun `owned inventory bank and following pet remove gamble choice and newly acquired pet aborts pending gamble`() {
        for (where in 0..2) {
            val f = Fixture(); f.pet(where); f.talk(); f.choose(1); assertEquals(1, f.player.inv.count("obj.dizanas_quiver_uncharged")); assertEquals(0, f.player.inv.count(DizanasQuiverScript.SPLINTER)); verifyNoInteractions(f.random, f.rewards)
        }
        val g = Fixture(); g.talk(); g.choose(1); g.pet(2); g.confirm(); assertEquals(1, g.player.inv.count("obj.dizanas_quiver_uncharged")); verifyNoInteractions(g.random, g.rewards)
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(1804, 9508); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val npc = Npc(QuiverExchangeScript.NPC, QuiverExchangeScript.LOCATION).apply { slotId = 2; assignUid() }
        val rewards = mock(PetRewards::class.java); val random = mock(GameRandom::class.java); val launcher: ProtectedAccessLauncher
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(QuiverExchangeScript(rewards, random, mock(NpcRepository::class.java), NpcList())) { scripts.startup() }; put(0, "obj.dizanas_quiver_uncharged")
        }
        fun put(slot: Int, symbol: String) { player.inv[slot] = InvObj(symbol) }
        fun talk() { player.ifClose(bus); val producer = NpcInteractions(bus); producer.interact(player, npc, InteractionOp.Op1); launcher.launch(player) { bus.publish(this, checkNotNull(producer.opTrigger(player, npc, InteractionOp.Op1))) } }
        fun use() { player.ifClose(bus); val ctor = NpcUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }; val producer = ctor.newInstance(bus); launcher.launch(player) { producer.interactOp(this, npc, inv, 0, npc.type, ServerCacheManager.getItem(inv[0]!!.id)!!) } }
        fun choose(index: Int) { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", index)) }
        fun confirm(yes: Boolean = true) { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", if (yes) 1 else 2)) }
        fun pet(where: Int) { when (where) { 0 -> put(1, QuiverExchangeScript.PET); 1 -> player.invMap["inv.bank"] = Inventory.create("inv.bank").apply { this[0] = InvObj(QuiverExchangeScript.PET) }; 2 -> VarPlayerIntMapSetter.set(player, "varp.follower_obj", InvObj(QuiverExchangeScript.PET).id) } }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
