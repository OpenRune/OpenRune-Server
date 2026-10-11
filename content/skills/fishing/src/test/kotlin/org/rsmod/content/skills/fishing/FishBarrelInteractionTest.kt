package org.rsmod.content.skills.fishing

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import kotlin.coroutines.*
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.death.*
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.*
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.skilling.*
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.skilling.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.skills.fishing.scripts.FishBarrelScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class FishBarrelInteractionTest {
    @Test fun `native fill open close and sack options retain their actual meanings`() {
        val f = Fixture(); f.put(0, "obj.fish_barrel_closed"); f.put(1, "obj.raw_lobster"); f.op(0, HeldOp.Op1)
        assertEquals(1, FishBarrelContents.total(f.player)); assertTrue(f.player.inv[1] == null)
        f.op(0, HeldOp.Op3); assertEquals("obj.fish_barrel_open".asRSCM(), f.player.inv[0]!!.id)
        f.op(0, HeldOp.Op3); assertEquals("obj.fish_barrel_closed".asRSCM(), f.player.inv[0]!!.id)
        f.put(0, "obj.fish_sack_barrel_closed"); assertFalse(f.bus.contains(HeldObjEvents.Op2::class.java, f.player.inv[0]!!.id.toLong()))
        f.op(0, HeldOp.Op4); assertEquals("obj.fish_sack_barrel_open".asRSCM(), f.player.inv[0]!!.id)
        f.op(0, HeldOp.Op4); assertEquals("obj.fish_sack_barrel_closed".asRSCM(), f.player.inv[0]!!.id)
    }

    @Test fun `actual skilling reward fills an open barrel in full inventory without phantom fish or double awards`() {
        val f = Fixture(); f.put(0, "obj.fish_barrel_open"); for (i in 1..27)f.put(i, "obj.lobster")
        f.run { assertEquals(SkillingAwardResult.Success, awardSkillingProduct(product(player))) }
        assertEquals(1, FishBarrelContents.total(f.player)); assertEquals(0, f.player.inv.physicalCount("obj.raw_lobster")); assertEquals(90, f.player.statMap.getXP("stat.fishing"))
        FishBarrelContents.update(f.player, "obj.raw_lobster", 27)
        f.run { assertEquals(SkillingAwardResult.InventoryFull, awardSkillingProduct(product(player))) }
        assertEquals(28, FishBarrelContents.total(f.player)); assertEquals(90, f.player.statMap.getXP("stat.fishing"))
        assertEquals(0, FishBarrelContents.room(f.player, "obj.minnow")); assertEquals(0, FishBarrelContents.room(f.player, "obj.lobster"))
    }

    @Test fun `atomic acquisition previews are pure and a late closed barrel invalidates collection`() {
        val f = Fixture(); f.put(0, "obj.fish_barrel_open")
        val tx = f.player.invAdd(f.player.inv, "obj.raw_lobster", 3, autoCommit = false)
        assertEquals(0, FishBarrelContents.total(f.player)); assertEquals(0, f.player.inv.physicalCount("obj.raw_lobster"))
        f.op(0, HeldOp.Op3); assertThrows(IllegalStateException::class.java) { tx.commitAll() }
        assertEquals(0, FishBarrelContents.total(f.player)); assertEquals(0, f.player.inv.physicalCount("obj.raw_lobster"))
    }

    @Test fun `actual native bank button preserves all fish on full bank and empties only after successful insertion`() {
        for (symbol in listOf("obj.fish_barrel_open", "obj.fish_sack_barrel_open")) {
            val f = Fixture(); f.put(0, symbol); FishBarrelContents.update(f.player, "obj.raw_lobster", 10)
            val bank = f.player.invMap.getOrPut("inv.bank"); for (i in bank.indices)bank[i] = InvObj("obj.lobster", 1)
            f.bankClick(); assertEquals(10, FishBarrelContents.total(f.player))
            bank[0] = null; f.bankClick(); assertEquals(0, FishBarrelContents.total(f.player)); assertEquals(10, bank.physicalCount("obj.raw_lobster"))
            assertEquals(symbol.asRSCM(), f.player.inv[0]!!.id)
        }
    }

    @Test fun `native combining and configure dismantling are atomic and preserve the shared contents`() {
        val f = Fixture(); f.put(0, "obj.fish_sack"); f.put(1, "obj.fish_barrel_open"); FishBarrelContents.update(f.player, "obj.raw_lobster", 4)
        f.use(1, 0); assertEquals("obj.fish_sack_barrel_open".asRSCM(), f.player.inv[1]!!.id); assertTrue(f.player.inv[0] == null); assertEquals(4, FishBarrelContents.total(f.player))
        for (i in f.player.inv.indices)if (i != 1)f.put(i, "obj.lobster")
        f.op(1, HeldOp.Op3); f.choose(1); assertEquals("obj.fish_sack_barrel_open".asRSCM(), f.player.inv[1]!!.id)
        f.player.inv[0] = null; f.op(1, HeldOp.Op3); f.choose(1)
        assertEquals("obj.fish_barrel_open".asRSCM(), f.player.inv[1]!!.id); assertEquals(1, f.player.inv.physicalCount("obj.fish_sack")); assertEquals(4, FishBarrelContents.total(f.player))
    }

    @Test fun `worn barrel collects fish and native worn close disables automatic storage`() {
        val f = Fixture(); f.player.worn[1] = InvObj("obj.fish_sack_barrel_open")
        f.run { invAdd(inv, "obj.raw_lobster", 1) }; assertEquals(1, FishBarrelContents.total(f.player))
        val c = WornInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }
        val ops = c.newInstance(*c.parameterTypes.map { if (it == EventBus::class.java)f.bus else mock(it) }.toTypedArray())as WornInteractions
        f.run { ops.interact(this, player.worn, 1, IfButtonOp.Op3) }
        assertEquals("obj.fish_sack_barrel_closed".asRSCM(), f.player.worn[1]!!.id)
        f.run { invAdd(inv, "obj.raw_lobster", 1) }; assertEquals(1, FishBarrelContents.total(f.player)); assertEquals(1, f.player.inv.physicalCount("obj.raw_lobster"))
    }

    @Test fun `PvP death retains a barrel and repeated previews never modify its contents`() {
        val f = Fixture(); f.put(0, "obj.fish_barrel_open"); FishBarrelContents.update(f.player, "obj.raw_lobster", 7)
        val hook = FishBarrelDeathHook(); val context = PlayerDeathContext(f.player, CoordGrid(3100, 3500), true, 1, false, false, false, false, true, 0, Player())
        val result = DeathDropResult(emptyList(), emptyList(), emptyList(), listOf(f.player.inv[0]!!), 0)
        repeat(2) { val resolved = hook.resolve(context, result); assertEquals(1, resolved.kept.size); assertTrue(resolved.lostUntradeable.isEmpty()); assertEquals(7, FishBarrelContents.total(f.player)) }
        assertEquals(result, hook.resolve(context.copy(killer = null, recentPvpDamage = false), result))
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player(client).apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); currentMapClock = 100; processedMapClock = 100 }
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            with(InvTransactionsScript(PlayerItemStorage(setOf(FishBarrelStorageHook())))) { scripts.startup() }
            with(FishBarrelScript()) { scripts.startup() }
            with(BankInvScript(bus, mock(org.rsmod.api.player.bonus.WornBonuses::class.java), mock(org.rsmod.api.combat.weapon.WeaponSpeeds::class.java))) { scripts.startup() }
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            val c = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }
            held = c.newInstance(*c.parameterTypes.map { if (it == EventBus::class.java)bus else mock(it) }.toTypedArray())as HeldInteractions
        }
        fun put(slot: Int, symbol: String) { player.inv[slot] = InvObj(symbol) }
        fun run(action: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player) { action(this) } }
        fun op(slot: Int, op: HeldOp) = run { held.interact(this, inv, slot, op) }
        fun use(first: Int, second: Int) = run { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }
        fun choose(index: Int) { val c = player.activeCoroutine!!; assertTrue(c.isAwaiting(ResumePauseButtonInput::class)); c.resumeWith(ResumePauseButtonInput("component.chatmenu:options", index)) }
        fun bankClick() {
            run { ifOpenSide("interface.bankside"); ifSetEvents("component.bankside:items", 0..27, IfEvent.Op9) }
            val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }
            If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance("component.bankside:items".asRSCM(), 0, player.inv[0]!!.id, 9, null)as If3Button)
        }
    }
    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
        fun product(player: Player) = SkillingProduct(player, "stat.fishing", "obj.raw_lobster", 1, 90.0, true, SkillingProductSource.Fishing(ServerCacheManager.getItem("obj.raw_lobster".asRSCM())!!))
        val client = object : Client<Any, Any> { override fun write(message: Any) = Unit; override fun close() = Unit; override fun read(player: Player) = Unit; override fun flush() = Unit; override fun flushHighPriority() = Unit; override fun unregister(service: Any, player: Player) = Unit }
    }
}
