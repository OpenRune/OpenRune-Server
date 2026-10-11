@file:OptIn(org.rsmod.annotations.InternalApi::class)
package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.AfterEach
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
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.quest.manager.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class SteelKeyRingYanniInteractionTest {
    @AfterEach fun resetPolicy() { QuestRequirements.install(QuestRequirementPolicy()) }

    @Test fun `native Yanni sells a replacement for five hundred coins without recovering lost keys or duplicates`() {
        QuestRequirements.install(QuestRequirementPolicy()); val f = Fixture(); f.player.inv[0] = InvObj("obj.coins", 500); f.player.attr[SteelKeyRing.CONTENTS] = 8191
        f.talk(); f.confirm(); assertTrue(f.player.inv.contains(SteelKeyRing.ITEM)); assertEquals(0, f.player.inv.physicalCount("obj.coins")); assertEquals(0, SteelKeyRing.mask(f.player)); f.talk(); assertTrue(f.player.activeCoroutine == null); assertEquals(1, f.player.inv.physicalCount(SteelKeyRing.ITEM))
    }

    @Test fun `real quest policy denies unfinished reward lamps and accepts the native completed state`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress)); val f = Fixture(); f.player.inv[0] = InvObj("obj.coins", 500)
        VarPlayerIntMapSetter.set(f.player, "varp.onesmallfavour", 284); f.talk(); assertTrue(f.player.activeCoroutine == null); assertFalse(f.player.inv.contains(SteelKeyRing.ITEM))
        VarPlayerIntMapSetter.set(f.player, "varp.onesmallfavour", 285); f.talk(); f.confirm(); assertTrue(f.player.inv.contains(SteelKeyRing.ITEM))
    }

    @Test fun `native replacement rechecks ownership position lifecycle and atomic payment capacity`() {
        QuestRequirements.install(QuestRequirementPolicy())
        for (mode in 0..8) {
            val f = Fixture(); f.player.inv[0] = InvObj("obj.coins", if (mode == 0) 499 else if (mode == 1) 501 else 500)
            if (mode == 1) for (slot in 1..27) f.player.inv[slot] = InvObj("obj.lobster")
            f.talk(); when (mode) { 3 -> f.player.coords = f.player.coords.translateX(1); 4 -> f.player.pendingLogout = true; 5 -> f.player.loggingOut = true; 6 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 7 -> f.player.inv[1] = InvObj(SteelKeyRing.ITEM); 8 -> f.player.invMap.getOrPut("inv.bank")[0] = InvObj(SteelKeyRing.ITEM) }
            val snapshot = f.player.inv.objs.copyOf(); f.confirm(mode != 2); assertArrayEquals(snapshot, f.player.inv.objs)
        }
    }
    private class Fixture {
        val bus = EventBus(); val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(2852, 2982); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }; val npc = Npc("npc.shiloantiques", player.coords).apply { slotId = 2; assignUid() }; val launcher: ProtectedAccessLauncher
        init { val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(SteelKeyRingYanniScript()) { scripts.startup() } }
        fun talk() { player.ifClose(bus); val producer = NpcInteractions(bus); producer.interact(player, npc, InteractionOp.Op1); launcher.launch(player) { assertTrue(bus.publish(this, checkNotNull(producer.opTrigger(player, npc, InteractionOp.Op1)))) } }
        fun confirm(yes: Boolean = true) { player.activeCoroutine?.resumeWith(ResumePauseButtonInput("component.chatmenu:options", if (yes) 1 else 2)) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
