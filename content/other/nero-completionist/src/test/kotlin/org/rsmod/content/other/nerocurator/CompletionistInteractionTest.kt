@file:OptIn(org.rsmod.annotations.InternalApi::class)
package org.rsmod.content.other.nerocurator

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.VarpLifetime
import dev.openrune.util.Wearpos
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.net.rsprot.handlers.ResumePauseButtonHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.quiver
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.player.worn.hasMaxCapePerks
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.spells.autocast.MagicSpellbookManager
import org.rsmod.content.other.consumables.potion.PotionEffectService
import org.rsmod.content.other.maxcape.MaxCapeScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
class CompletionistInteractionTest {
    @Test fun `native NPC Trade buys cape or hood and reuses the final coin slot`() {
        for ((option, symbol, cost) in listOf(Triple(1, CAPE, 10000000), Triple(2, HOOD, 5000000))) {
            val f = Fixture(); for (slot in 0..26) f.player.inv[slot] = InvObj("obj.shark")
            f.player.inv[27] = InvObj("obj.coins", cost)
            f.npc(InteractionOp.Op2); f.confirm(option); f.confirm(1)
            assertEquals(1, f.player.inv.physicalCount(symbol)); assertEquals(0, f.player.inv.physicalCount("obj.coins"))
            assertEquals(27, f.player.inv.physicalCount("obj.shark"))
        }
    }

    @Test fun `native purchase cancels or rolls back full inventory missing coins death logout and departure`() {
        for (mode in 0..6) {
            val f = Fixture(); for (slot in 0..26) f.player.inv[slot] = InvObj("obj.shark")
            f.player.inv[27] = InvObj("obj.coins", if (mode == 1) 9999999 else if (mode == 0) 10000001 else 10000000)
            f.npc(InteractionOp.Op2); f.confirm(1)
            when (mode) { 3 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 4 -> f.player.pendingLogout = true; 5 -> f.player.loggingOut = true; 6 -> f.player.coords = f.player.coords.translateX(1) }
            val before = f.player.inv.objs.copyOf(); f.confirm(if (mode == 2) 2 else 1)
            assertArrayEquals(before, f.player.inv.objs, "purchase mode $mode")
        }
    }

    @Test fun `all twenty two actual item-on-item recipes work in either order with a full inventory`() {
        for (recipe in CompletionistRecipes.all) for (reverse in listOf(false, true)) {
            val f = Fixture(); f.recipe(recipe)
            for (slot in 3..27) f.player.inv[slot] = InvObj("obj.shark")
            f.use(if (reverse) 2 else 0, if (reverse) 0 else 2); f.confirm(1)
            assertEquals(1, f.player.inv.physicalCount(recipe.cape), recipe.name)
            assertEquals(1, f.player.inv.physicalCount(recipe.hood), recipe.name)
            assertEquals(0, f.player.inv.physicalCount(CAPE)); assertEquals(0, f.player.inv.physicalCount(HOOD)); assertEquals(0, f.player.inv.physicalCount(recipe.input))
            assertEquals(25, f.player.inv.physicalCount("obj.shark"))
            val before = f.player.inv.objs.copyOf(); f.confirm(1); assertArrayEquals(before, f.player.inv.objs)
        }
    }

    @Test fun `fusion preserves every input after missing hood cancellation stale references death logout or movement`() {
        val recipe = CompletionistRecipes.all.first()
        for (mode in 0..7) {
            val f = Fixture(); f.recipe(recipe); if (mode == 0)f.player.inv[1] = null
            f.use(0, 2)
            when (mode) { 2 -> f.player.inv[0] = f.player.inv[0]!!.copy(); 3 -> f.player.inv[2] = f.player.inv[2]!!.copy(); 4 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 5 -> f.player.pendingLogout = true; 6 -> f.player.loggingOut = true; 7 -> f.player.coords = f.player.coords.translateX(1) }
            val before = f.player.inv.objs.copyOf(); f.confirm(if (mode == 1)2 else 1); assertArrayEquals(before, f.player.inv.objs, "fusion mode $mode")
        }
    }

    @Test fun `actual IF3 editor previews saves cancels restores presets and discards on close`() {
        val f = Fixture(); f.npc(InteractionOp.Op3); assertTrue(f.player.ui.containsModal(INTERFACE)); assertEquals(1, f.value(34))
        f.click("particle"); f.click("colour21"); f.click("size_up"); f.click("save0")
        assertEquals(0xc277e8, f.value(32)); assertEquals(125, f.value(33)); assertEquals(true, f.feed.snapshot()["cape_tester"]!!["editing"])
        f.click("done"); assertEquals(0xc277e8, f.value(4)); assertEquals(125, f.value(5)); assertFalse(f.player.ui.containsModal(INTERFACE))
        f.npc(InteractionOp.Op3); f.click("reset"); f.click("cancel"); assertEquals(125, f.value(5)); assertEquals(0xc277e8, f.value(4))
        f.npc(InteractionOp.Op3); f.click("reset"); f.click("load0"); assertEquals(125, f.value(33)); assertEquals(0xc277e8, f.value(32))
        f.click("reset"); f.player.ifClose(f.bus); assertEquals(0, f.value(34)); assertEquals(125, f.feed.snapshot()["cape_tester"]!!["size"])
        f.npc(InteractionOp.Op3); f.click("preview_mode"); assertEquals(1, f.value(36))
        f.npc(InteractionOp.Op3); assertEquals(1, f.value(34)); assertEquals(0, f.value(36))
    }

    @Test fun `editor rejects out of range dead or logged out clicks and logout clears the appearance feed`() {
        for (mode in 0..3) {
            val f = Fixture(); f.npc(InteractionOp.Op3)
            when (mode) { 0 -> f.player.coords = CoordGrid(3200, 3200); 1 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 2 -> f.player.pendingLogout = true; 3 -> f.player.loggingOut = true }
            f.click("size_up"); f.click("done"); assertEquals(100, f.value(5)); assertEquals(100, f.value(33))
            f.bus.publish(SessionStateEvent.Logout(f.player)); assertTrue(f.feed.snapshot().isEmpty())
        }
    }

    @Test fun `login migrates old size units exactly once preserving all colours and presets`() {
        val f = Fixture(); for (c in 0..3)f.set(c, 1234 + c); f.set(4, 0x123456); f.set(5, 300); f.set(6, 1)
        for (n in 0..2) { f.set(13 + n * 7, 1); f.set(12 + n * 7, 900) }
        f.bus.publish(SessionStateEvent.Login(f.player)); assertEquals(100, f.value(5)); assertEquals(2, f.value(6))
        for (c in 0..3)assertEquals(1234 + c, f.value(c)); assertEquals(0x123456, f.value(4))
        for (n in 0..2)assertEquals(300, f.value(12 + n * 7))
        f.bus.publish(SessionStateEvent.Login(f.player)); assertEquals(100, f.value(5)); assertEquals(300, f.value(12))
    }

    @Test fun `commands are atomic and cannot overwrite full inventory`() {
        val f = Fixture(); assertTrue(f.commands.execute(f.player, "compcapes", listOf("all"))); assertEquals(24, f.player.inv.objs.count { it != null })
        val before = f.player.inv.objs.copyOf(); assertTrue(f.commands.execute(f.player, "compcapes", listOf("recipe", "fire"))); assertEquals(27, f.player.inv.objs.count { it != null })
        val full = f.player.inv.objs.copyOf(); assertTrue(f.commands.execute(f.player, "compcapes", listOf("regular"))); assertArrayEquals(full, f.player.inv.objs)
        assertEquals(24, before.count { it != null })
    }

    @Test fun `all cape and hood Wear options use actual held interactions and populate the correct worn slots`() {
        for (symbol in listOf(CAPE, HOOD, "obj.nero_completionist_2011_trimmed", "obj.nero_completionist_hood_trimmed") + CompletionistRecipes.all.flatMap { listOf(it.cape, it.hood) }) {
            val f = Fixture(); f.player.inv[0] = InvObj(symbol)
            val type = ServerCacheManager.getItem(symbol.asRSCM())!!
            val option = type.interfaceOptions.indexOf("Wear")
            assertTrue(option >= 0, symbol)
            f.held(0, HeldOp.entries[option])
            assertEquals(symbol.asRSCM(), f.player.worn[type.wearpos1]?.id, symbol)
            assertNull(f.player.inv[0], symbol)
        }
    }

    @Test fun `Dizana completionist forms store and empty ammo through actual held actions without losing full inventory output`() {
        for (symbol in CompletionistQuiverScript.ITEMS) {
            val f = Fixture(); f.player.inv[0] = InvObj(symbol); f.player.inv[1] = InvObj("obj.rune_arrow", 100)
            f.use(0, 1); assertNull(f.player.inv[1]); assertEquals(100, DizanasQuiver.stored(f.player)!!.count)
            for (slot in 1..27)f.player.inv[slot] = InvObj("obj.shark")
            f.held(0, HeldOp.Op4); assertEquals(100, DizanasQuiver.stored(f.player)!!.count)
            f.player.inv[1] = null; f.held(0, HeldOp.Op4); assertNull(DizanasQuiver.stored(f.player)); assertEquals(100, f.player.inv[1]!!.count)
        }
    }

    @Test fun `real combat manager uses Dizana extra ammo while keeping the ordinary blessing and permanent Sunfire`() {
        for (symbol in CompletionistQuiverScript.ITEMS) {
            val f = Fixture(); val bow = ServerCacheManager.getItems().values.first { it.name == "Magic shortbow" && it.wearpos1 == Wearpos.RightHand.slot }
            val arrow = ServerCacheManager.getItem("obj.rune_arrow".asRSCM())!!
            val blessing = ServerCacheManager.getItems().values.first { it.name == "Holy blessing" }
            f.player.worn[Wearpos.Back.slot] = InvObj(symbol); f.player.worn[Wearpos.RightHand.slot] = InvObj(bow); f.player.worn[Wearpos.Quiver.slot] = InvObj(blessing)
            DizanasQuiver.store(f.player, InvObj(arrow, 2)); assertEquals(arrow.id, f.player.quiver!!.id); assertTrue(DizanasQuiver.sunfire(f.player))
            val random = mock(GameRandom::class.java); `when`(random.randomBoolean(3)).thenReturn(true); `when`(random.randomBoolean(5)).thenReturn(false); `when`(random.of(maxExclusive = 100)).thenReturn(99)
            val manager = RangedAmmoManager(random, f.bus, CollisionFlagMap(), WorldQueueList(), mock(ObjRepository::class.java))
            manager.useQuiverAmmo(f.player, arrow, f.player.coords, 0); assertEquals(1, DizanasQuiver.stored(f.player)!!.count); assertEquals(blessing.id, f.player.worn[Wearpos.Quiver.slot]!!.id); assertTrue(DizanasQuiver.sunfire(f.player))
            f.setNative("varbit.dizanas_quiver_ammo_save", 2); `when`(random.of(maxExclusive = 100)).thenReturn(0); manager.useQuiverAmmo(f.player, arrow, f.player.coords, 0); assertEquals(1, DizanasQuiver.stored(f.player)!!.count)
        }
    }

    @Test fun `replacing a worn Dizana cape clears its native charged equipment flag`() {
        val f = Fixture(); f.player.inv[0] = InvObj("obj.nero_completionist_dizana"); f.held(0, HeldOp.Op2)
        assertEquals(1, f.player.vars["varbit.worn_items_dizanas_quiver_charged"])
        f.player.inv[0] = InvObj("obj.nero_completionist_fire"); f.held(0, HeldOp.Op2)
        assertEquals(0, f.player.vars["varbit.worn_items_dizanas_quiver_charged"])
    }

    @Test fun `saved style and extra ammo use native permanent vars while editor drafts are temporary`() {
        for (n in 0..41)assertEquals(if (n < 28)VarpLifetime.Perm else VarpLifetime.Temp, ServerCacheManager.getVarp("varp.nero_style_$n".asRSCM(RSCMType.VARP))!!.scope, "style $n")
        for (symbol in listOf(DizanasQuiver.AMMO, DizanasQuiver.COUNT))assertEquals(VarpLifetime.Perm, ServerCacheManager.getVarp(symbol.asRSCM(RSCMType.VARP))!!.scope, symbol)
    }

    @Test fun `all twenty four worn capes have their matching max bonuses plus two and only regular forms have all skill perks`() {
        val pairs = listOf(CAPE to "obj.skillcape_max", "obj.nero_completionist_2011_trimmed" to "obj.skillcape_max") + CompletionistRecipes.all.map { it.cape to nativeSource(it.cape) }
        val calc = WornBonuses(); val f = Fixture()
        for ((target, source)in pairs) { f.player.worn[Wearpos.Back.slot] = InvObj(source); val base = calc.calculate(f.player); f.player.worn[Wearpos.Back.slot] = InvObj(target); val cape = calc.calculate(f.player)
            assertEquals(listOf(base.offStab + 2, base.offSlash + 2, base.offCrush + 2, base.offMagic + 2, base.offRange + 2, base.defStab + 2, base.defSlash + 2, base.defCrush + 2, base.defMagic + 2, base.defRange + 2, base.meleeStr + 2, base.rangedStr + 2, base.magicDmg + 20, base.prayer + 2), listOf(cape.offStab, cape.offSlash, cape.offCrush, cape.offMagic, cape.offRange, cape.defStab, cape.defSlash, cape.defCrush, cape.defMagic, cape.defRange, cape.meleeStr, cape.rangedStr, cape.magicDmg, cape.prayer), target)
            assertEquals(target in listOf(CAPE, "obj.nero_completionist_2011_trimmed"), f.player.hasMaxCapePerks(), target)
        }
    }

    private fun nativeSource(target: String): String {
        val key = target.removePrefix("obj.nero_completionist_")
        val ids = mapOf("fire" to 13329, "saradomin" to 13331, "zamorak" to 13333, "guthix" to 13335, "accumulator" to 13337, "ardougne" to 20760, "infernal" to 21285, "imbued_saradomin" to 21776, "imbued_zamorak" to 21780, "imbued_guthix" to 21784, "assembler" to 21898, "mythical" to 24855, "masori_assembler" to 27363, "dizana" to 28902, "infernal_locked" to 24133, "fire_locked" to 24134, "assembler_locked" to 24135, "imbued_saradomin_locked" to 24232, "imbued_zamorak_locked" to 24233, "imbued_guthix_locked" to 24234, "masori_assembler_locked" to 27365, "dizana_locked" to 28906)
        return dev.openrune.rscm.RSCM.getReverseMapping(RSCMType.OBJ, checkNotNull(ids[key]))
    }

    private class Fixture {
        val bus = EventBus(); val commands = CheatCommandMap(); val feed = CompletionistAppearanceFeed()
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3087, 3493); currentMapClock = 100; processedMapClock = 100; displayName = "Cape Tester"; for (stat in listOf("hitpoints", "defence", "attack", "strength", "ranged", "magic", "prayer")) { statMap.setBaseLevel("stat.$stat", 99); statMap.setCurrentLevel("stat.$stat", 99) } }
        val npc = Npc("npc.nero_curator", CoordGrid(3088, 3493)).apply { slotId = 2; assignUid() }
        val heldProducer = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }.let { it.newInstance(bus, mock(it.parameterTypes[1]), mock(it.parameterTypes[2]), HeldEquipOp(bus)) as HeldInteractions }
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() }, getHeldInteractions = { heldProducer })
        val launcher = ProtectedAccessLauncher(mock(ProtectedAccessContextFactory::class.java).apply { `when`(create()).thenReturn(context) })
        val plugin = CuratorPlugin(mock(NpcRepository::class.java), launcher, PlayerList(), feed)
        init { val scripts = ScriptContext(bus, commands, EngineQueueCache()); with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }; with(plugin) { scripts.startup() }; with(CompletionistQuiverScript()) { scripts.startup() }; with(MaxCapeScript(mock(MagicSpellbookManager::class.java), mock(PotionEffectService::class.java), mock(PlayerTeleportValidator::class.java), mock(AreaChecker::class.java), launcher, mock(GameRandom::class.java))) { scripts.startup() }; bus.publish(SessionStateEvent.Login(player)) }
        fun npc(op: InteractionOp) { player.clearPendingAction(bus); val producer = NpcInteractions(bus); producer.interact(player, npc, op); assertTrue(launcher.launch(player) { assertTrue(bus.publish(this, checkNotNull(producer.opTrigger(player, npc, op)))) }) }
        fun confirm(option: Int) { ResumePauseButtonHandler().handle(player, ResumePauseButton(CombinedId("component.chatmenu:options".asRSCM(RSCMType.COMPONENT)), option)) }
        fun recipe(r: CompletionistRecipes.Recipe) { player.inv[0] = InvObj(CAPE); player.inv[1] = InvObj(HOOD); player.inv[2] = InvObj(r.input) }
        fun held(slot: Int, op: HeldOp) { assertTrue(launcher.launch(player) { heldProducer.interact(this, inv, slot, op) }) }
        fun use(first: Int, second: Int) { assertTrue(launcher.launch(player) { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }) }
        fun click(name: String) { val id = "component.nero_cape_customiser:$name".asRSCM(RSCMType.COMPONENT); If3ButtonHandler(bus, launcher).handle(player, If3Button(CombinedId(id), -1, -1, 1)) }
        fun value(n: Int) = player.vars["varp.nero_style_$n"]
        fun setNative(symbol: String, value: Int) = VarPlayerIntMapSetter.set(player, symbol, value)
        fun set(n: Int, value: Int) = VarPlayerIntMapSetter.set(player, "varp.nero_style_$n", value)
    }
    companion object {
        private const val CAPE = "obj.nero_completionist_2011"
        private const val HOOD = "obj.nero_completionist_hood"
        private const val INTERFACE = "interface.nero_cape_customiser"

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
