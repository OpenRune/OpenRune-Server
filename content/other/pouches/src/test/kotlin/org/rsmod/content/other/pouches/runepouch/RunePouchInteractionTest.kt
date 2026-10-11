package org.rsmod.content.other.pouches.runepouch

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onIfOpen
import org.rsmod.content.interfaces.bank.bankCapacity
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.other.pouches.PouchBankDeposits
import org.rsmod.content.other.pouches.lootingbag.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class RunePouchInteractionTest {
    @Test fun `native bankside Configure opens every pouch instead of throwing an invalid-op exception`() {
        for (pouch in RunePouches.ALL) {
            val f = Fixture(); f.put(0, pouch)
            f.launcher.launch(f.player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", 0..27, dev.openrune.definition.type.widget.IfEvent.Op1) }
            f.button("component.bankside:items", 0, pouch, 1); f.choose(0)
            assertTrue(f.player.ui.containsModal("interface.rune_pouch"), pouch)
        }
        val f = Fixture(); f.put(0, RunePouches.REGULAR)
        f.launcher.launch(f.player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", 0..27, dev.openrune.definition.type.widget.IfEvent.Op1) }
        f.player.pendingLogout = true; f.button("component.bankside:items", 0, RunePouches.REGULAR, 1)
        assertFalse(f.player.ui.containsModal("interface.rune_pouch"))
    }

    @Test fun `native bankside Empty honours capacity and maximum stacks without clearing excess runes`() {
        for (pouch in RunePouches.ALL) {
            val f = Fixture(); f.put(0, pouch); val runes = RunePouchContents.empty(4); runes.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 100); f.player.writeRunePouch(runes)
            f.player.bankCapacity = 1; val bank = f.player.invMap.getOrPut("inv.bank"); bank[0] = InvObj("obj.coins", 1)
            f.launcher.launch(f.player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", inv.indices, dev.openrune.definition.type.widget.IfEvent.Op9) }
            f.button("component.bankside:items", 0, pouch, 9); assertEquals(100, f.player.readRunePouch(4).counts[0]); assertEquals(0, bank.physicalCount("obj.airrune"))
            bank[0] = InvObj("obj.airrune", Int.MAX_VALUE - 7); f.button("component.bankside:items", 0, pouch, 9)
            assertEquals(93, f.player.readRunePouch(4).counts[0]); assertEquals(Int.MAX_VALUE, bank.physicalCount("obj.airrune"))
            bank[0] = null; f.button("component.bankside:items", 0, pouch, 9); assertTrue(f.player.readRunePouch(4).isEmpty()); assertEquals(93, bank.physicalCount("obj.airrune"))
        }
    }

    @Test fun `all ten native loadouts round trip split fields without corrupting neighbouring presets`() {
        val f = Fixture(); val expected = mutableListOf<RunePouchContents>()
        val runeTypes = listOf("obj.wrathrune", "obj.lavarune", "obj.soulrune", "obj.bloodrune").map { RunePouchRunes.byObj(it.asRSCM())!!.compactId }
        for (index in 0..9) { val contents = RunePouchContents(runeTypes.toIntArray(), intArrayOf(16000 - index, 12345 - index, 15001 - index, 9999 - index)); expected += contents; RunePouchLoadoutCodec.write(f.player, index, contents) }
        for (index in 0..9) { val contents = RunePouchLoadoutCodec.read(f.player, index); assertArrayEquals(expected[index].types, contents.types); assertArrayEquals(expected[index].counts, contents.counts) }
        val restored = Player(); restored.vars.backing.putAll(f.player.vars.backing)
        for (index in 0..9) assertArrayEquals(expected[index].counts, RunePouchLoadoutCodec.read(restored, index).counts)
    }

    @Test fun `native bank Configure can save and load every preset with quantities and native slot order`() {
        for (index in 0..9) {
            val f = Fixture(); f.put(0, RunePouches.DIVINE)
            val types = listOf("obj.wrathrune", "obj.lavarune", "obj.soulrune", "obj.bloodrune"); val compact = types.map { RunePouchRunes.byObj(it.asRSCM())!!.compactId }
            val contents = RunePouchContents(compact.toIntArray(), intArrayOf(101, 202, 303, 404)); f.player.writeRunePouch(contents); f.bankOpen()
            f.config(); f.choose(1); if (index >= 5)f.choose(5); f.choose(index % 5); f.choose(1); f.choose(6)
            assertArrayEquals(contents.counts, RunePouchLoadoutCodec.read(f.player, index).counts)
            f.player.writeRunePouch(RunePouchContents.empty(4)); val bank = f.player.invMap.getOrPut("inv.bank")
            for ((slot, item)in types.withIndex()) bank[slot] = InvObj(item, 1000)
            VarPlayerIntMapSetter.set(f.player, "varbit.bank_tab_main", 4)
            f.config(); f.choose(1); if (index >= 5)f.choose(5); f.choose(index % 5); f.choose(0); f.choose(6)
            val loaded = f.player.readRunePouch(4); assertArrayEquals(compact.toIntArray(), loaded.types); assertArrayEquals(contents.counts, loaded.counts)
            for ((slot, item)in types.withIndex())assertEquals(1000 - contents.counts[slot], bank.physicalCount(item))
        }
    }

    @Test fun `loadout fills existing runes and returns obsolete runes while preserving placeholders and bank tabs`() {
        val f = Fixture(); f.put(0, RunePouches.REGULAR); val current = RunePouchContents.empty(3); current.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 100); f.player.writeRunePouch(current)
        val wanted = RunePouchContents.empty(3); wanted.add(RunePouchRunes.byObj("obj.soulrune".asRSCM())!!.compactId, 75); RunePouchLoadoutCodec.write(f.player, 0, wanted)
        val bank = f.player.invMap.getOrPut("inv.bank"); bank[0] = InvObj("obj.soulrune", 75); VarPlayerIntMapSetter.set(f.player, "varbit.bank_tab_main", 1); VarPlayerIntMapSetter.set(f.player, "varbit.bank_leaveplaceholders", 1)
        f.bankOpen(); f.config(); f.choose(1); f.choose(0); f.choose(0); f.choose(6)
        assertEquals(100, bank.physicalCount("obj.airrune")); assertArrayEquals(wanted.types, f.player.readRunePouch(3).types); assertArrayEquals(wanted.counts, f.player.readRunePouch(3).counts)
        assertEquals(2, f.player.vars["varbit.bank_tab_main"]); assertTrue(bank[0]!!.id != "obj.soulrune".asRSCM())
    }

    @Test fun `full banks missing supplies regular-pouch capacity and late movement never lose runes through loadouts`() {
        for (mode in 0..4) {
            val f = Fixture(); f.put(0, RunePouches.REGULAR); val current = RunePouchContents.empty(3); current.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 100); f.player.writeRunePouch(current)
            val wanted = RunePouchContents.empty(4); for (type in listOf("obj.soulrune", "obj.lavarune", "obj.bloodrune", "obj.wrathrune").take(if (mode == 2)4 else 1))wanted.add(RunePouchRunes.byObj(type.asRSCM())!!.compactId, 75); RunePouchLoadoutCodec.write(f.player, 0, wanted)
            val bank = f.player.invMap.getOrPut("inv.bank"); f.player.bankCapacity = 1; bank[0] = InvObj("obj.coins", 1); VarPlayerIntMapSetter.set(f.player, "varbit.bank_tab_main", 1)
            f.bankOpen(); f.config(); f.choose(1); f.choose(0)
            when (mode) { 3 -> f.player.coords = f.player.coords.translateX(1); 4 -> f.player.pendingLogout = true }
            f.choose(0); if (mode < 3)f.choose(6)
            assertEquals(100, f.player.readRunePouch(3).counts[0]); assertEquals(0, bank.physicalCount("obj.airrune")); assertEquals(1, bank.physicalCount("obj.coins"))
        }
    }

    @Test fun `native preset quantity and rename inputs persist and reject out-of-range or late changes`() {
        for (mode in 0..2) {
            val f = Fixture(); f.put(0, RunePouches.REGULAR); val saved = RunePouchContents.empty(3); saved.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 100); RunePouchLoadoutCodec.write(f.player, 0, saved); f.bankOpen()
            f.config(); f.choose(1); f.choose(0); f.choose(2); f.choose(0)
            if (mode == 2)f.player.pendingLogout = true
            val coroutine = f.player.activeCoroutine!!; assertTrue(coroutine.isAwaiting(org.rsmod.api.player.input.ResumePCountDialogInput::class)); coroutine.resumeWith(org.rsmod.api.player.input.ResumePCountDialogInput(if (mode == 1)17000 else 250))
            if (mode != 2)f.choose(6)
            assertEquals(if (mode == 0)250 else 100, RunePouchLoadoutCodec.read(f.player, 0).counts[0])
        }
        val f = Fixture(); f.put(0, RunePouches.REGULAR); f.bankOpen(); f.config(); f.choose(1); f.choose(0); f.choose(3)
        val coroutine = f.player.activeCoroutine!!; assertTrue(coroutine.isAwaiting(org.rsmod.api.player.input.ResumePStringDialogInput::class)); coroutine.resumeWith(org.rsmod.api.player.input.ResumePStringDialogInput("Boss mage 1")); f.choose(6)
        val restored = Player(); restored.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); assertEquals("Boss mage 1", restored.attr[RunePouchLoadoutCodec.names]!!["0"])
    }

    @Test fun `a fully withdrawn bank stack releases space for obsolete pouch runes in the same loadout`() {
        val f = Fixture(); f.put(0, RunePouches.REGULAR); val current = RunePouchContents.empty(3); current.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 100); f.player.writeRunePouch(current)
        val wanted = RunePouchContents.empty(3); wanted.add(RunePouchRunes.byObj("obj.soulrune".asRSCM())!!.compactId, 75); RunePouchLoadoutCodec.write(f.player, 0, wanted)
        val bank = f.player.invMap.getOrPut("inv.bank"); bank[0] = InvObj("obj.soulrune", 75); f.player.bankCapacity = 1; VarPlayerIntMapSetter.set(f.player, "varbit.bank_tab_main", 1)
        f.bankOpen(); f.config(); f.choose(1); f.choose(0); f.choose(0); f.choose(6)
        assertArrayEquals(wanted.types, f.player.readRunePouch(3).types); assertArrayEquals(wanted.counts, f.player.readRunePouch(3).counts); assertEquals(100, bank.physicalCount("obj.airrune")); assertEquals(0, bank.physicalCount("obj.soulrune")); assertEquals(1, f.player.vars["varbit.bank_tab_main"])
    }

    @Test fun `real item use stores matching runes up to capacity without making a fourth regular slot`() {
        val f = Fixture(); f.put(0, RunePouches.REGULAR)
        for ((i, rune)in listOf("obj.airrune", "obj.waterrune", "obj.earthrune").withIndex()) { f.put(1, rune, 20000); f.use(1, 0); assertEquals(16000, f.player.readRunePouch(3).counts[i]); assertEquals(4000, f.player.inv[1]!!.count) }
        f.put(1, "obj.firerune", 100); f.use(0, 1); assertEquals(100, f.player.inv[1]!!.count)
        f.op(0, HeldOp.Op4); assertTrue(f.player.readRunePouch(3).isEmpty()); assertEquals(16000, f.player.inv.physicalCount("obj.airrune"))
    }

    @Test fun `native pouch interface deposits and full inventory withdrawals preserve remaining runes`() {
        val f = Fixture(); f.put(0, RunePouches.DIVINE); f.put(1, "obj.airrune", 100)
        f.op(0, HeldOp.Op1); f.button("component.rune_pouch:inventory", 1, "obj.airrune", 6)
        assertEquals(100, f.player.readRunePouch(4).counts[0]); assertTrue(f.player.inv[1] == null)
        for (i in 1..27)f.put(i, "obj.lobster")
        f.button("component.rune_pouch:pouch", 0, "obj.airrune", 6); assertEquals(100, f.player.readRunePouch(4).counts[0])
        f.player.inv[1] = null; f.button("component.rune_pouch:pouch", 0, "obj.airrune", 3)
        assertEquals(95, f.player.readRunePouch(4).counts[0]); assertEquals(5, f.player.inv[1]!!.count)
    }

    @Test fun `native Divine creation preserves rune vars and cancels if the thread or player moves`() {
        for (cancel in 0..3) {
            val f = Fixture(); f.put(0, RunePouches.REGULAR); f.put(1, "obj.thread_of_elidinis"); f.put(2, "obj.needle")
            f.player.statMap.setCurrentLevel("stat.crafting", 75); val contents = RunePouchContents.empty(3); contents.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 20); f.player.writeRunePouch(contents)
            f.use(1, 0)
            when (cancel) { 1 -> f.player.inv[1] = null; 2 -> f.player.coords = f.player.coords.translateX(1); 3 -> f.player.pendingLogout = true }
            f.finish(); assertEquals(if (cancel == 0)RunePouches.DIVINE.asRSCM() else RunePouches.REGULAR.asRSCM(), f.player.inv[0]!!.id)
            assertEquals(20, f.player.readRunePouch(4).counts[0]); assertEquals(0, f.player.statMap.getXP("stat.crafting"))
            if (cancel == 0)assertTrue(f.player.inv[1] == null)
        }
        val f = Fixture(); f.put(0, RunePouches.REGULAR); f.put(1, "obj.thread_of_elidinis"); f.put(2, "obj.needle"); f.player.statMap.setCurrentLevel("stat.crafting", 74); f.use(0, 1); f.finish(); assertEquals(RunePouches.REGULAR.asRSCM(), f.player.inv[0]!!.id)
    }

    @Test fun `confirmed native Revert returns both components atomically and warns before clearing slot four`() {
        for (full in listOf(true, false)) {
            val f = Fixture(); f.put(0, RunePouches.DIVINE)
            val contents = RunePouchContents.empty(4); for ((i, rune)in listOf("obj.airrune", "obj.waterrune", "obj.earthrune", "obj.firerune").withIndex())contents.add(RunePouchRunes.byObj(rune.asRSCM())!!.compactId, 10 + i); f.player.writeRunePouch(contents)
            if (full)for (i in 1..27)f.put(i, "obj.lobster")
            f.op(0, HeldOp.Op3); f.choose(1)
            assertEquals(if (full)RunePouches.DIVINE.asRSCM() else RunePouches.REGULAR.asRSCM(), f.player.inv[0]!!.id)
            assertEquals(if (full)13 else 0, f.player.readRunePouch(4).counts[3]); assertEquals(10, f.player.readRunePouch(4).counts[0])
            assertEquals(if (full)0 else 1, f.player.inv.physicalCount("obj.thread_of_elidinis"))
        }
        val f = Fixture(); f.put(0, RunePouches.DIVINE); f.op(0, HeldOp.Op3); f.choose(2); assertEquals(RunePouches.DIVINE.asRSCM(), f.player.inv[0]!!.id)
    }

    @Test fun `native Empty containers banks runes and looting contents without overflowing aggregate coin counts`() {
        for (symbol in RunePouches.ALL) {
            val f = Fixture(); f.put(0, symbol); f.put(1, LootingBags.OPEN)
            val contents = RunePouchContents.empty(if (symbol in listOf(RunePouches.DIVINE, RunePouches.DIVINE_LOCKED)) 4 else 3)
            contents.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 50); f.player.writeRunePouch(contents)
            f.player.lootingBag[0] = InvObj("obj.coins", Int.MAX_VALUE); f.player.saveLootingBag(); f.bulk(); f.bulk()
            assertEquals(50, f.player.invMap.getOrPut("inv.bank").physicalCount("obj.airrune")); assertEquals(Int.MAX_VALUE, f.player.invMap.getOrPut("inv.bank").physicalCount("obj.coins")); assertTrue(f.player.readRunePouch(4).isEmpty()); assertTrue(f.player.lootingBag.isEmpty()); assertTrue(f.player.attr[LOOTING_BAG_ATTR] == null); assertEquals(2, f.player.inv.occupiedSpace())
        }
    }

    @Test fun `native Empty containers cannot discard runes or looting contents when bank is full`() {
        val f = Fixture(); f.put(0, RunePouches.REGULAR); f.put(1, LootingBags.CLOSED); f.player.bankCapacity = 1; f.player.invMap.getOrPut("inv.bank")[0] = InvObj("obj.lobster")
        val contents = RunePouchContents.empty(3); contents.add(RunePouchRunes.byObj("obj.airrune".asRSCM())!!.compactId, 50); f.player.writeRunePouch(contents); f.player.lootingBag[0] = InvObj("obj.coins", 100); f.player.saveLootingBag()
        f.bulk(); assertEquals(50, f.player.readRunePouch(3).counts[0]); assertEquals(100, f.player.lootingBag[0]!!.count); assertTrue(f.player.attr[LOOTING_BAG_ATTR] != null)
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); bankCapacity = 800; coords = CoordGrid(3222, 3218); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() })
        val launcher: ProtectedAccessLauncher; val held: HeldInteractions
        init {
            scripts.onIfOpen("interface.bankside") { player.ifSetEvents("component.bankside:items", player.inv.indices, dev.openrune.definition.type.widget.IfEvent.Op1, dev.openrune.definition.type.widget.IfEvent.Op9) }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            val bankScript = BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java), setOf(PouchBankDeposits()))
            with(RunePouchScript(mock(ObjRepository::class.java), launcher, bankScript)) { scripts.startup() }
            with(bankScript) { scripts.startup() }
            val c = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; held = c.newInstance(bus, mock(c.parameterTypes[1]), mock(c.parameterTypes[2]), HeldEquipOp(bus))as HeldInteractions
        }
        fun bulk() { player.ifClose(bus); bankOpen(); val c = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, c.newInstance("component.bankmain:depositcontainers".asRSCM(), -1, -1, 1, null) as If3Button) }
        fun bankOpen() { launcher.launch(player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", inv.indices, dev.openrune.definition.type.widget.IfEvent.Op1) } }
        fun config() = button("component.bankside:items", 0, if (player.inv[0]!!.id in RunePouches.divineIds)RunePouches.DIVINE else RunePouches.REGULAR, 1)
        fun put(slot: Int, symbol: String, count: Int = 1) { player.inv[slot] = InvObj(symbol, count) }
        fun run(action: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player) { action(this) } }
        fun op(slot: Int, op: HeldOp) = run { held.interact(this, inv, slot, op) }
        fun use(first: Int, second: Int) = run { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }
        fun choose(index: Int) { val c = player.activeCoroutine!!; assertTrue(c.isAwaiting(ResumePauseButtonInput::class)); c.resumeWith(ResumePauseButtonInput("component.chatmenu:options", index)) }
        fun finish() { repeat(7) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
        fun button(component: String, slot: Int, symbol: String, op: Int) {
            val c = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, c.newInstance(component.asRSCM(), slot, symbol.asRSCM(), op, null)as If3Button)
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
