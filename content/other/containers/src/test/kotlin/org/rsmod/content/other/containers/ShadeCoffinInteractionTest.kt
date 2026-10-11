package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.*
import org.rsmod.game.interact.*
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(org.rsmod.annotations.InternalApi::class)
@ResourceLock("ServerCacheManager")
internal class ShadeCoffinInteractionTest {
    @Test fun `native Talk-to gives one broken coffin and refuses a carried worn or banked duplicate`() {
        for (duplicate in 0..3) {
            val f = Fixture()
            when (duplicate) { 1 -> f.put(0, ShadeCoffins.BROKEN); 2 -> f.player.worn[1] = InvObj("obj.shades_coffin_gold"); 3 -> f.player.invMap.getOrPut("inv.bank")[0] = InvObj("obj.shades_coffin_gold") }
            f.talk(); f.choose(0); assertEquals(if (duplicate in 0..1)1 else 0, f.player.inv.physicalCount(ShadeCoffins.BROKEN))
            f.talk(); f.choose(0); assertEquals(if (duplicate in 0..1)1 else 0, f.player.inv.physicalCount(ShadeCoffins.BROKEN))
        }
    }

    @Test fun `real item-on-Dampe attaches every tier and does not consume missing or noted inputs`() {
        for (tier in ShadeCoffins.tiers) {
            val f = Fixture(); f.put(0, ShadeCoffins.BROKEN); f.put(1, "obj.shades_lock_$tier"); f.useNpc(1)
            assertEquals(1, f.player.inv.physicalCount("obj.shades_coffin_$tier")); assertEquals(0, f.player.inv.physicalCount(ShadeCoffins.BROKEN)); assertEquals(0, f.player.inv.physicalCount("obj.shades_lock_$tier"))
        }
        val g = Fixture(); g.put(0, "obj.shades_lock_gold"); g.useNpc(0); assertEquals(1, g.player.inv.physicalCount("obj.shades_lock_gold")); assertEquals(0, g.player.inv.physicalCount("obj.shades_coffin_gold"))
        val h = Fixture(); h.put(0, ShadeCoffins.BROKEN); h.player.inv[1] = InvObj(ServerCacheManager.getItem(ServerCacheManager.getItem("obj.shades_lock_gold".asRSCM())!!.certlink)!!); h.useNpc(1); assertEquals(1, h.player.inv.physicalCount(ShadeCoffins.BROKEN))
    }

    @Test fun `native Configure opens and closes every coffin preserving remains and worn controls work`() {
        for (tier in ShadeCoffins.tiers) {
            val f = Fixture(); val item = "obj.shades_coffin_$tier"; f.put(0, item); f.put(1, "obj.shade_bones6"); f.op(0, HeldOp.Op1)
            assertEquals(1, f.count(item, "obj.shade_bones6")); f.op(0, HeldOp.Op3); f.choose(1); assertEquals((item + "_open").asRSCM(), f.player.inv[0]!!.id)
            f.op(0, HeldOp.Op3); f.choose(1); assertEquals(item.asRSCM(), f.player.inv[0]!!.id); assertEquals(1, f.count(item, "obj.shade_bones6"))
            f.player.inv[0] = null; f.player.worn[1] = InvObj(item); f.worn(IfButtonOp.Op4); assertEquals((item + "_open").asRSCM(), f.player.worn[1]!!.id)
            f.worn(IfButtonOp.Op5); assertEquals(0, f.count(item, "obj.shade_bones6")); assertEquals(1, f.player.inv.physicalCount("obj.shade_bones6"))
            f.worn(IfButtonOp.Op2); assertEquals(1, f.count(item, "obj.shade_bones6")); f.worn(IfButtonOp.Op4); assertEquals(item.asRSCM(), f.player.worn[1]!!.id)
        }
    }

    @Test fun `highest tier remains withdraw first when only one inventory slot is free`() {
        val f = Fixture(); val item = "obj.shades_coffin_gold"; f.put(0, item)
        f.storage.update(f.player, ContainerCatalog.byId[item.asRSCM()]!!, "obj.shade_bones1".asRSCM(), 2); f.storage.update(f.player, ContainerCatalog.byId[item.asRSCM()]!!, "obj.shade_bones6".asRSCM(), 2)
        for (slot in 1..26) f.put(slot, "obj.lobster"); f.op(0, HeldOp.Op4)
        assertEquals("obj.shade_bones6".asRSCM(), f.player.inv[27]!!.id); assertEquals(1, f.count(item, "obj.shade_bones6")); assertEquals(2, f.count(item, "obj.shade_bones1"))
    }

    @Test fun `confirmed Remove lock returns components atomically and only clears contents after success`() {
        for (mode in 0..5) {
            val f = Fixture(); val item = "obj.shades_coffin_gold_open"; f.put(0, item); f.storage.update(f.player, ContainerCatalog.byId[item.asRSCM()]!!, "obj.shade_bones6".asRSCM(), 7)
            if (mode == 1) for (slot in 1..27) f.put(slot, "obj.lobster")
            f.op(0, HeldOp.Op3); f.choose(2)
            when (mode) { 3 -> f.put(0, item); 4 -> f.player.pendingLogout = true; 5 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0) }
            f.choose(if (mode == 2)1 else 0)
            assertEquals(if (mode == 0)0 else 7, f.count(item, "obj.shade_bones6")); assertEquals(if (mode == 0)1 else 0, f.player.inv.physicalCount(ShadeCoffins.BROKEN)); assertEquals(if (mode == 0)1 else 0, f.player.inv.physicalCount("obj.shades_lock_gold"))
        }
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val storage = ContainerStore()
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); currentMapClock = 100; processedMapClock = 100; coords = CoordGrid(3481, 3316); statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val npc = Npc(ShadeCoffins.DAMPE, CoordGrid(3480, 3316)).apply { slotId = 2; assignUid() }; val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })); launcher = ProtectedAccessLauncher(factory)
            with(InvTransactionsScript(PlayerItemStorage(setOf(ContainerStorageHook(storage))))) { scripts.startup() }
            with(ContainerScript(storage, mock(BankInvScript::class.java))) { scripts.startup() }; with(ShadeCoffinScript()) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = ctor.newInstance(*ctor.parameterTypes.map { if (it == EventBus::class.java)bus else mock(it) }.toTypedArray()) as HeldInteractions
        }
        fun put(slot: Int, item: String) { player.inv[slot] = InvObj(item) }
        fun count(coffin: String, item: String) = storage.count(player, ContainerCatalog.byId[coffin.asRSCM()]!!, item.asRSCM())
        fun run(block: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player, block = block) }
        fun talk() { val producer = NpcInteractions(bus); producer.interact(player, npc, InteractionOp.Op1); run { bus.publish(this, checkNotNull(producer.opTrigger(player, npc, InteractionOp.Op1))) } }
        fun useNpc(slot: Int) { val ctor = NpcUInteractions::class.java.getDeclaredConstructor(EventBus::class.java).apply { isAccessible = true }; val producer = ctor.newInstance(bus); run { producer.interactOp(this, npc, inv, slot, npc.type, ServerCacheManager.getItem(inv[slot]!!.id)!!) } }
        fun op(slot: Int, op: HeldOp) = run { held.interact(this, inv, slot, op) }
        fun worn(op: IfButtonOp) { val ctor = WornInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; val producer = ctor.newInstance(*ctor.parameterTypes.map { if (it == EventBus::class.java)bus else mock(it) }.toTypedArray()) as WornInteractions; run { producer.interact(this, worn, 1, op) } }
        fun choose(index: Int) { val c = player.activeCoroutine!!; assertTrue(c.isAwaiting(ResumePauseButtonInput::class)); c.resumeWith(ResumePauseButtonInput("component.chatmenu:options", index)) }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
