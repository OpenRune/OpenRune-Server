package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.buttons.IfSubOp
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.net.rsprot.handlers.IfSubOpHandler
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.*
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.script.onIfOverlaySubOpMenu
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.WornXpModifiers
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.bank.bankCapacity
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.skills.crafting.scripts.HeldCraftingScript
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLogs
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.*
import org.rsmod.game.inv.*
import org.rsmod.game.loc.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

private val forestryTestCollision = CollisionFlagMap()

@ResourceLock("ServerCacheManager")
internal class ForestryInteractionTest {
    @Test fun `harness combines both log basket forms with kit without moving shared contents and Configure returns all components`() {
        for (open in listOf(false, true)) {
            val f = Fixture(); val log = "obj.log_basket_" + if (open) "open" else "closed"
            f.put(0, log); f.put(1, ForestryContainers.KIT); f.put(2, ForestryContainers.HARNESS); f.store.update(f.player, ForestryContainers.logs, "obj.logs".asRSCM(), 21); f.store.update(f.player, ForestryContainers.kit, "obj.leaves".asRSCM(), 38)
            f.use(2, 0); assertEquals(1, f.player.inv.physicalCount(if (open) ForestryContainers.OPEN else ForestryContainers.CLOSED)); assertEquals(21, f.logs()); assertEquals(38, f.leaves())
            f.op(0, HeldOp.Op4); f.choose(1)
            assertEquals(1, f.player.inv.physicalCount(log)); assertEquals(1, f.player.inv.physicalCount(ForestryContainers.KIT)); assertEquals(1, f.player.inv.physicalCount(ForestryContainers.HARNESS)); assertEquals(21, f.logs()); assertEquals(38, f.leaves())
        }
    }

    @Test fun `basket fill kit and logs stay separate and closing only disables automatic logs`() {
        val f = Fixture(); f.put(0, ForestryContainers.CLOSED); f.put(1, "obj.logs"); f.put(2, "obj.leaves", 30)
        f.op(0, HeldOp.Op1); f.choose(0); assertEquals(30, f.leaves()); assertEquals(0, f.logs())
        f.op(0, HeldOp.Op1); f.choose(1); assertEquals(1, f.logs()); assertEquals(30, f.leaves())
        assertFalse(f.store.owns(f.player, ContainerCatalog.byId["obj.log_basket_open".asRSCM()]!!)); f.op(0, HeldOp.Op4); f.choose(0)
        assertEquals(ForestryContainers.OPEN.asRSCM(), f.player.inv[0]!!.id); assertTrue(f.store.owns(f.player, ContainerCatalog.byId["obj.log_basket_open".asRSCM()]!!)); assertEquals(1, f.logs()); assertEquals(30, f.leaves())
    }

    @Test fun `dismantle cancels on stale reference movement death logout and insufficient component room`() {
        for (mode in 0..5) {
            val f = Fixture(); f.put(0, ForestryContainers.OPEN); f.store.update(f.player, ForestryContainers.logs, "obj.logs".asRSCM(), 28); f.op(0, HeldOp.Op4)
            when (mode) { 1 -> f.put(0, ForestryContainers.OPEN); 2 -> f.player.coords = f.player.coords.translateX(1); 3 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 4 -> f.player.pendingLogout = true; 5 -> for (slot in 1..27) f.put(slot, "obj.lobster") }
            f.choose(1); assertEquals(if (mode == 0)1 else 0, f.player.inv.physicalCount(ForestryContainers.HARNESS)); assertEquals(28, f.logs())
        }
    }

    @Test fun `native bankside use deposits logs then leaves then withdraws leaves preserving capacity`() {
        val f = Fixture(); f.put(0, ForestryContainers.OPEN); f.store.update(f.player, ForestryContainers.logs, "obj.logs".asRSCM(), 3); f.store.update(f.player, ForestryContainers.kit, "obj.leaves".asRSCM(), 30); f.openBank(); f.bankClick()
        assertEquals(0, f.logs()); assertEquals(3, f.bank.physicalCount("obj.logs")); assertEquals(30, f.leaves()); f.bankClick(); assertEquals(0, f.leaves()); assertEquals(30, f.bank.physicalCount("obj.leaves")); f.bankClick(); assertEquals(30, f.leaves()); assertEquals(0, f.bank.physicalCount("obj.leaves"))
        val g = Fixture(); g.put(0, ForestryContainers.CLOSED); g.player.bankCapacity = 1; g.bank[0] = InvObj("obj.coins", 1); g.store.update(g.player, ForestryContainers.logs, "obj.logs".asRSCM(), 28); g.openBank(); g.bankClick(); assertEquals(28, g.logs()); assertEquals(0, g.bank.physicalCount("obj.logs"))
    }

    @Test fun `clothes and cape pouches are irreversible single attachments and guard their stored outfit and cape`() {
        val f = Fixture(); f.put(0, ForestryContainers.KIT); f.put(1, "obj.ramble_lumberjack_hat"); assertEquals(0, f.store.store(f.player, ForestryContainers.kit, f.player.inv[1]!!.id, 1))
        f.put(2, "obj.forestry_clothes_pouch"); f.use(0, 2); assertEquals(1, f.store.store(f.player, ForestryContainers.kit, f.player.inv[1]!!.id, 1)); assertEquals(0, f.store.withdraw(f.player, ForestryContainers.kit, "obj.forestry_clothes_pouch".asRSCM(), 1, f.player.inv))
        f.put(3, "obj.forestry_cape_pouch"); f.player.statMap.setCurrentLevel("stat.woodcutting", 98); f.use(0, 3); assertEquals(1, f.player.inv.physicalCount("obj.forestry_cape_pouch")); f.player.statMap.setCurrentLevel("stat.woodcutting", 99); f.use(0, 3)
        f.put(4, "obj.skillcape_woodcutting_trimmed"); f.use(0, 4); assertEquals(1, f.store.count(f.player, ForestryContainers.kit, "obj.skillcape_woodcutting_trimmed".asRSCM()))
        f.put(5, "obj.ramble_lumberjack_hat"); assertEquals(0, f.store.store(f.player, ForestryContainers.kit, f.player.inv[5]!!.id, 1))
    }

    @Test fun `native harness and clothes recipes enforce both skills and actual needle options`() {
        val f = Fixture(); f.put(0, "obj.forestry_log_brace"); f.put(1, "obj.rope"); f.put(2, "obj.rope"); for (slot in 3..5) f.put(slot, "obj.adamantite_bar"); f.put(6, "obj.nails", 45)
        f.player.statMap.setCurrentLevel("stat.smithing", 74); f.use(0, 1); assertEquals(0, f.player.inv.physicalCount(ForestryContainers.HARNESS)); f.player.statMap.setCurrentLevel("stat.smithing", 99); f.use(0, 1); assertEquals(1, f.player.inv.physicalCount(ForestryContainers.HARNESS)); assertEquals(0, f.player.inv.physicalCount("obj.nails"))
        for (costume in listOf(false, true)) {
            val g = Fixture(); g.put(0, if (costume) "obj.costumeneedle" else "obj.needle"); g.put(1, "obj.forestry_clothes_pouch_blueprint"); g.put(2, "obj.leather"); if (!costume) g.put(3, "obj.thread"); g.use(0, 1); g.advance(); assertEquals(1, g.player.inv.physicalCount("obj.forestry_clothes_pouch")); assertEquals(47, g.player.statMap.getXP("stat.crafting"))
        }
    }

    @Test fun `actual tree input can fill open log and forestry baskets through a full inventory and refuses closed or full stores`() {
        for (form in listOf("obj.log_basket_open", ForestryContainers.OPEN, "obj.log_basket_closed", ForestryContainers.CLOSED)) for (filled in listOf(false, true)) {
            val f = Fixture(); f.put(0, form); for (slot in 1..27) f.put(slot, "obj.lobster"); f.player.worn[3] = InvObj("obj.rune_axe"); if (filled) f.store.update(f.player, ForestryContainers.logs, "obj.logs".asRSCM(), 28)
            f.cut(); val success = form.endsWith("_open") && !filled
            assertEquals(if (filled)28 else if (success)1 else 0, f.logs(), "$form filled=$filled delay=${f.player.actionDelay} tree axe=${Woodcutting.findAxe(f.player, ServerCacheManager.getObjects().values.first { it.isContentType("content.tree") && it.treeLogs.id == "obj.logs".asRSCM() })}"); assertEquals(28, f.player.inv.occupiedSpace()); assertEquals(if (success)25 else 0, f.player.statMap.getXP("stat.woodcutting"))
        }
    }

    @Test fun `tree inputs stop without logs or XP on death and logout`() {
        for (mode in 0..2) { val f = Fixture(); f.put(0, ForestryContainers.OPEN); f.player.worn[3] = InvObj("obj.rune_axe"); when (mode) { 0 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 1 -> f.player.pendingLogout = true; 2 -> f.player.loggingOut = true }; f.cut(); assertEquals(0, f.logs()); assertEquals(0, f.player.statMap.getXP("stat.woodcutting")) }
    }

    @Test fun `native forestry screen displays all fixed rows and quantity buttons transfer with stale-X checks`() {
        val f = Fixture(); f.put(0, ForestryContainers.KIT); f.put(1, "obj.leaves", 30); f.op(0, HeldOp.Op1); assertTrue(f.player.ui.containsModal(ForestryKitScript.MAIN)); f.ui("fill"); assertEquals(30, f.leaves()); assertEquals(30, f.player.invMap["inv.forestry_kit"]!![4]!!.count)
        f.ui("5"); f.ui("list", 48, 2); assertEquals(25, f.leaves()); f.ui("x"); f.ui("list", 48, 2); f.amount(7); assertEquals(18, f.leaves()); f.ui("list", 48, 1); assertEquals(5, f.player.vars["varbit.forestry_kit_selected_item"]); f.ui("all"); f.ui("list", 48, 2); assertEquals(0, f.leaves()); assertEquals(30, f.player.inv.physicalCount("obj.leaves"))
        for (mode in 0..3) { val g = Fixture(); g.put(0, ForestryContainers.KIT); g.store.update(g.player, ForestryContainers.kit, "obj.leaves".asRSCM(), 30); g.op(0, HeldOp.Op1); g.ui("list", 48, 5); when (mode) { 0 -> g.player.inv[0] = null; 1 -> g.player.pendingLogout = true; 2 -> g.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> g.player.coords = g.player.coords.translateX(1) }; g.amount(12); assertEquals(30, g.leaves()); assertEquals(0, g.player.inv.physicalCount("obj.leaves")) }
    }

    @Test fun `actual inventory submenu packet bypasses menus and shares the same forestry controls`() {
        val f = Fixture(); f.put(0, ForestryContainers.CLOSED); f.put(1, "obj.logs"); f.subOp(1, 1); assertEquals(1, f.logs()); f.subOp(4, 0); assertEquals(ForestryContainers.OPEN.asRSCM(), f.player.inv[0]!!.id); f.subOp(3, 1); assertEquals(0, f.logs()); assertEquals(1, f.player.inv.physicalCount("obj.logs")); f.subOp(3, 0); assertTrue(f.player.ui.containsModal(ForestryKitScript.MAIN))
    }

    @Test fun `stored and worn lumberjack pieces grant two-point-five-percent once and conflicting graceful only blocks its slot`() {
        val f = Fixture(); f.put(0, ForestryContainers.KIT); f.store.update(f.player, ForestryContainers.kit, "obj.forestry_clothes_pouch".asRSCM(), 1); for (piece in ForestryContainers.outfits) f.store.update(f.player, ForestryContainers.kit, piece.first().asRSCM(), 1)
        val xp = XpModifiers(setOf(WornXpModifiers(), ForestryOutfitXpMod(f.store)))
        assertEquals(1.025, xp.get(f.player, "stat.woodcutting"), 0.000001); f.player.worn[0] = InvObj("obj.forestry_lumberjack_hat"); assertEquals(1.025, xp.get(f.player, "stat.woodcutting"), 0.000001)
        val graceful = ServerCacheManager.getItems().values.first { it.name == "Graceful hood" && it.certtemplate == 0 }; f.player.worn[0] = InvObj(graceful); assertEquals(1.016, xp.get(f.player, "stat.woodcutting"), 0.000001)
        f.player.inv[0] = null; assertEquals(1.0, xp.get(f.player, "stat.woodcutting"), 0.000001)
        f.player.worn[0] = InvObj("obj.ramble_lumberjack_hat"); assertEquals(1.004, xp.get(f.player, "stat.woodcutting"), 0.000001)
    }

    @Test fun `actual successful woodcutting consumes ration only below ninety-percent and retains it on failed capacity`() {
        for (mode in 0..2) { val f = Fixture(); f.put(0, ForestryContainers.OPEN); f.player.worn[3] = InvObj("obj.rune_axe"); f.store.update(f.player, ForestryContainers.kit, "obj.forestry_ration".asRSCM(), 5); f.player.runEnergy = if (mode == 1)9000 else 5000
            if (mode == 2) { for (slot in 1..27) f.put(slot, "obj.lobster"); f.store.update(f.player, ForestryContainers.logs, "obj.logs".asRSCM(), 28) }
            f.cut(); assertEquals(if (mode == 0)4 else 5, f.store.count(f.player, ForestryContainers.kit, "obj.forestry_ration".asRSCM())); assertEquals(if (mode == 0)7000 else if (mode == 1)9000 else 5000, f.player.runEnergy)
        }
    }

    @Test fun `native last-basket destruction preserves permanent upgrades outfit and bark and duplicates retain all shared stores`() {
        for (duplicate in listOf(false, true)) { val f = Fixture(); f.put(0, ForestryContainers.OPEN); if (duplicate) f.put(1, ForestryContainers.KIT); f.store.update(f.player, ForestryContainers.logs, "obj.logs".asRSCM(), 12); f.store.update(f.player, ForestryContainers.kit, "obj.forestry_clothes_pouch".asRSCM(), 1); f.store.update(f.player, ForestryContainers.kit, "obj.ramble_lumberjack_hat".asRSCM(), 1); f.store.update(f.player, ForestryContainers.kit, "obj.forestry_currency".asRSCM(), 200); f.store.update(f.player, ForestryContainers.kit, "obj.leaves".asRSCM(), 30)
            f.op(0, HeldOp.Op5); f.player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.confirmdestroy:universe", 1)); assertEquals(0, f.logs()); assertEquals(if (duplicate)30 else 0, f.leaves()); assertEquals(1, f.store.count(f.player, ForestryContainers.kit, "obj.ramble_lumberjack_hat".asRSCM()))
            val restored = Player(); restored.attr.putAllFromPersistence(f.player.attr.toPersistentMap()); assertEquals(200, f.store.count(restored, ForestryContainers.kit, "obj.forestry_currency".asRSCM())); assertEquals(1, f.store.count(restored, ForestryContainers.kit, "obj.forestry_clothes_pouch".asRSCM()))
        }
    }

    @Test fun `real worn submenu fills and empties the basket without reading the inventory slot`() {
        val f = Fixture(); f.player.worn[1] = InvObj(ForestryContainers.OPEN); f.put(0, "obj.logs"); f.wornSubOp(2, 1); assertEquals(1, f.logs()); f.wornSubOp(3, 1); assertEquals(0, f.logs()); assertEquals(1, f.player.inv.physicalCount("obj.logs")); f.wornSubOp(3, 0); assertTrue(f.player.ui.containsModal(ForestryKitScript.MAIN)); assertEquals(0, f.legacySubmenuCalls)
    }

    @Test fun `clothes crafting preserves every ingredient XP and output after a late interrupted action`() {
        for (mode in 0..4) { val f = Fixture(); f.put(0, "obj.needle"); f.put(1, "obj.forestry_clothes_pouch_blueprint"); f.put(2, "obj.leather"); f.put(3, "obj.thread"); f.use(0, 1); when (mode) { 0 -> f.player.coords = f.player.coords.translateX(1); 1 -> f.player.pendingLogout = true; 2 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); 3 -> f.put(1, "obj.forestry_clothes_pouch_blueprint"); 4 -> f.player.inv[0] = null }; f.advance(); assertEquals(0, f.player.inv.physicalCount("obj.forestry_clothes_pouch")); assertEquals(1, f.player.inv.physicalCount("obj.forestry_clothes_pouch_blueprint")); assertEquals(1, f.player.inv.physicalCount("obj.leather")); assertEquals(1, f.player.inv.physicalCount("obj.thread")); assertEquals(0, f.player.statMap.getXP("stat.crafting")) }
    }

    @Test fun `actual native harvesting consumes physical or stored offerings even on failed bonus rolls without bonus XP`() {
        for (stored in listOf(false, true)) for (roll in listOf(59, 60)) {
            val f = Fixture(); f.put(0, ForestryContainers.OPEN); f.player.worn[3] = InvObj("obj.rune_axe"); if (stored) f.store.update(f.player, ForestryContainers.kit, "obj.nature_offerings".asRSCM(), 5) else f.put(1, "obj.nature_offerings", 5)
            `when`(f.random.of(100)).thenReturn(roll); f.cut(); assertEquals(if (roll == 59) 2 else 1, f.logs()); assertEquals(25, f.player.statMap.getXP("stat.woodcutting")); assertEquals(4, if (stored) f.store.count(f.player, ForestryContainers.kit, "obj.nature_offerings".asRSCM()) else f.player.inv.physicalCount("obj.nature_offerings"))
        }
    }

    @Test fun `offering group chance counts active choppers and caps at ten while idle dead departed or other-tree players add nothing`() {
        for (mode in 0..6) {
            val f = Fixture(); f.put(0, ForestryContainers.OPEN); f.player.worn[3] = InvObj("obj.rune_axe"); f.put(1, "obj.nature_offerings", 5)
            repeat(12) { val other = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = f.player.coords; currentMapClock = 100; processedMapClock = 100; actionDelay = 103; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }; f.locs.interact(other, f.tree(), InteractionOp.Op1); if (mode == 6) other.coords = other.coords.translateX(1).also { f.locs.interact(other, f.tree().let { tree -> BoundLocInfo(LocInfo(2, tree.coords.translateX(1), LocEntity(tree.id, 10, 0)), ServerCacheManager.getObject(tree.id)!!) }, InteractionOp.Op1) }; when (mode) { 1 -> other.clearInteraction(); 2 -> other.pendingLogout = true; 3 -> other.statMap.setCurrentLevel("stat.hitpoints", 0); 4 -> other.coords = other.coords.translateX(20); 5 -> other.actionDelay = 99 }; f.choppers.add(other) }
            `when`(f.random.of(100)).thenReturn(79); f.cut(); assertEquals(if (mode == 0) 2 else 1, f.logs()); assertEquals(4, f.player.inv.physicalCount("obj.nature_offerings"))
        }
    }

    @Test fun `native successful cuts consume offerings with one free slot but cannot create a bonus and failed cuts consume nothing`() {
        for (slots in listOf(0, 1, 2)) {
            val f = Fixture(); f.put(0, ForestryContainers.KIT); f.player.worn[3] = InvObj("obj.rune_axe"); f.store.update(f.player, ForestryContainers.kit, "obj.nature_offerings".asRSCM(), 5)
            for (slot in 1 until 28 - slots) f.put(slot, "obj.lobster")
            f.cut(); assertEquals(if (slots == 0) 5 else 4, f.store.count(f.player, ForestryContainers.kit, "obj.nature_offerings".asRSCM())); assertEquals(if (slots == 0) 0 else if (slots == 1) 1 else 2, f.player.inv.physicalCount("obj.logs")); assertEquals(if (slots == 0) 0 else 25, f.player.statMap.getXP("stat.woodcutting"))
        }
    }

    @Test fun `native oak harvesting rolls leaves once and consumes one physical or stored attachment only on leaf success`() {
        for (stored in listOf(false, true)) for (roll in listOf(0, 1)) {
            val f = Fixture(); f.put(0, ForestryContainers.OPEN); f.player.worn[3] = InvObj("obj.rune_axe"); if (stored) f.store.update(f.player, ForestryContainers.kit, "obj.forestry_secateurs_attachment".asRSCM(), 5) else f.put(1, "obj.forestry_secateurs_attachment", 5)
            `when`(f.random.of(4)).thenReturn(roll); f.cut("obj.oak_logs"); assertEquals(if (roll == 0) 2 else 0, f.store.count(f.player, ForestryContainers.kit, "obj.leaves_oak".asRSCM())); assertEquals(if (roll == 0) 4 else 5, if (stored) f.store.count(f.player, ForestryContainers.kit, "obj.forestry_secateurs_attachment".asRSCM()) else f.player.inv.physicalCount("obj.forestry_secateurs_attachment")); assertEquals(1, f.store.count(f.player, ForestryContainers.logs, "obj.oak_logs".asRSCM())); assertEquals(37, f.player.statMap.getXP("stat.woodcutting")); verify(f.random, times(1)).of(4)
        }
    }

    @Test fun `regular logs have no leaf roll and bonus logs cannot spend extra rations or offerings`() {
        val f = Fixture(); f.put(0, ForestryContainers.OPEN); f.player.worn[3] = InvObj("obj.rune_axe"); f.player.runEnergy = 5000; for (item in listOf("obj.forestry_ration", "obj.forestry_secateurs_attachment", "obj.nature_offerings")) f.store.update(f.player, ForestryContainers.kit, item.asRSCM(), 5)
        f.cut(); assertEquals(2, f.logs()); assertEquals(4, f.store.count(f.player, ForestryContainers.kit, "obj.forestry_ration".asRSCM())); assertEquals(4, f.store.count(f.player, ForestryContainers.kit, "obj.nature_offerings".asRSCM())); assertEquals(5, f.store.count(f.player, ForestryContainers.kit, "obj.forestry_secateurs_attachment".asRSCM())); assertEquals(7000, f.player.runEnergy); verify(f.random, never()).of(4)
    }

    @Test fun `harvest leaves withdraw through native kit UI without reentering automatic storage`() {
        val f = Fixture(); f.put(0, ForestryContainers.KIT); f.player.worn[3] = InvObj("obj.rune_axe"); f.put(1, "obj.forestry_secateurs_attachment", 5); f.cut("obj.oak_logs")
        assertEquals(2, f.store.count(f.player, ForestryContainers.kit, "obj.leaves_oak".asRSCM())); f.op(0, HeldOp.Op1); val row = ForestryKitScript.items.indexOf("obj.leaves_oak"); assertTrue(row >= 0); f.ui("list", row * 12, 6)
        assertEquals(0, f.store.count(f.player, ForestryContainers.kit, "obj.leaves_oak".asRSCM())); assertEquals(2, f.player.inv.physicalCount("obj.leaves_oak")); assertEquals(1, f.player.inv.physicalCount("obj.oak_logs"))
    }

    @Test fun `leaf output with full native and virtual inventories preserves attachment after an original stored log`() {
        val f = Fixture(); f.put(0, ForestryContainers.OPEN); f.player.worn[3] = InvObj("obj.rune_axe"); for (slot in 1..27) f.put(slot, "obj.lobster"); f.store.update(f.player, ForestryContainers.kit, "obj.leaves_oak".asRSCM(), Int.MAX_VALUE); f.store.update(f.player, ForestryContainers.kit, "obj.forestry_secateurs_attachment".asRSCM(), 5); f.cut("obj.oak_logs")
        assertEquals(1, f.store.count(f.player, ForestryContainers.logs, "obj.oak_logs".asRSCM())); assertEquals(Int.MAX_VALUE, f.store.count(f.player, ForestryContainers.kit, "obj.leaves_oak".asRSCM())); assertEquals(5, f.store.count(f.player, ForestryContainers.kit, "obj.forestry_secateurs_attachment".asRSCM())); assertEquals(37, f.player.statMap.getXP("stat.woodcutting"))
    }
    private class Fixture {
        val bus = EventBus(); val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache()); val store = ContainerStore(); val map = forestryTestCollision; val clock = MapClock(); val random = mock(GameRandom::class.java); val players = mock(PlayerRepository::class.java); val choppers = mutableListOf<Player>()
        val player = Player().apply { inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn"); coords = CoordGrid(3000, 3000); currentMapClock = 100; processedMapClock = 100; bankCapacity = 800; for (stat in listOf("hitpoints", "woodcutting", "smithing", "crafting")) { statMap.setBaseLevel("stat.$stat", 99); statMap.setCurrentLevel("stat.$stat", 99) } }
        var legacySubmenuCalls = 0
        val bank = player.invMap.getOrPut("inv.bank"); val launcher: ProtectedAccessLauncher; val held: HeldInteractions; val locs = LocInteractions(BoundValidator(map), bus)
        init {
            `when`(players.findAll(org.rsmod.map.zone.ZoneKey.from(player.coords), 1)).thenAnswer { choppers.asSequence() }
            clock.cycle = 100; for (x in 2990..3010 step 8) for (z in 2990..3010 step 8) map.allocateIfAbsent(x, z, 0)
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getAlignment = { TextAlignment() }, getRandom = { random }, getLocInteractions = { locs }, getCollision = { map })
            val factory = mock(ProtectedAccessContextFactory::class.java); `when`(factory.create()).thenReturn(context); launcher = ProtectedAccessLauncher(factory)
            val bankScript = BankInvScript(bus, mock(WornBonuses::class.java), mock(WeaponSpeeds::class.java))
            with(InvTransactionsScript(PlayerItemStorage(setOf(ContainerStorageHook(store))))) { scripts.startup() }; with(bankScript) { scripts.startup() }; with(ContainerScript(store, bankScript)) { scripts.startup() }; with(ForestryKitScript(store)) { scripts.startup() }; with(ForestryBasketScript(store, bankScript, ForestryKitScript(store))) { scripts.startup() }
            with(scripts) { onIfOverlaySubOpMenu(dev.openrune.rscm.RSCM.getReverseMapping(dev.openrune.rscm.RSCMType.COMPONENT, org.rsmod.api.enums.EquipmentEnums.equipment_tab_to_slots_map[1]!!.packed)) { legacySubmenuCalls++ } }
            with(HeldCraftingScript(setOf(ContainerCraftingScript(store, XpModifiers(emptySet())), ForestryCraftingProvider(XpModifiers(emptySet()))))) { scripts.startup() }
            with(ForestryRationsScript(store, random)) { scripts.startup() }; with(ForestryHarvestScript(store, random, players, bus)) { scripts.startup() }; with(Woodcutting(mock(LocRepository::class.java), mock(ControllerRepository::class.java), mock(PlayerRepository::class.java), XpModifiers(emptySet()), InvisibleLevels(emptySet()), clock)) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }; val dropCtor = ctor.parameterTypes[2].declaredConstructors.single().apply { isAccessible = true }; val drop = dropCtor.newInstance(*dropCtor.parameterTypes.map { if (it == EventBus::class.java) bus else mock(it) }.toTypedArray()); held = ctor.newInstance(bus, mock(ctor.parameterTypes[1]), drop, mock(ctor.parameterTypes[3])) as HeldInteractions
        }
        fun put(slot: Int, name: String, amount: Int = 1) { player.inv[slot] = InvObj(name, amount) }
        fun run(block: suspend ProtectedAccess.() -> Unit) { player.ifClose(bus); launcher.launch(player, block = block) }
        fun op(slot: Int, op: HeldOp) = run { held.interact(this, inv, slot, op) }
        fun use(first: Int, second: Int) = run { HeldUInteractions(bus).interact(this, inv, ServerCacheManager.getItem(inv[first]!!.id)!!, first, ServerCacheManager.getItem(inv[second]!!.id)!!, second) }
        fun choose(index: Int) { player.activeCoroutine!!.resumeWith(ResumePauseButtonInput("component.chatmenu:options", index)) }
        fun advance() { repeat(3) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance() } }
        fun logs() = store.count(player, ForestryContainers.logs, "obj.logs".asRSCM())
        fun leaves() = store.count(player, ForestryContainers.kit, "obj.leaves".asRSCM())
        fun openBank() { launcher.launch(player) { ifOpenMainSidePair("interface.bankmain", "interface.bankside"); ifSetEvents("component.bankside:items", inv.indices, IfEvent.Op9) } }
        fun bankClick() { val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance("component.bankside:items".asRSCM(), 0, player.inv[0]!!.id, 9, null) as If3Button) }
        fun ui(button: String, slot: Int = -1, op: Int = 1) { val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }; If3ButtonHandler(bus, launcher).handle(player, ctor.newInstance("component.forestry_kit_main:$button".asRSCM(), slot, 65535, op, null) as If3Button) }
        fun amount(n: Int) { player.activeCoroutine!!.resumeWith(ResumePCountDialogInput(n)) }
        fun subOp(invOp: Int, subop: Int) { val op = when (invOp) { 1 -> 2; 3 -> 4; else -> 6 }; val component = ServerCacheManager.fromComponent("component.inventory:items"); player.ui.overlays.backing.put(0, component.packed ushr 16); player.ui.events.add("component.inventory:items", 0..27, when (op) { 2 -> IfEvent.Op2.bitmask; 4 -> IfEvent.Op4.bitmask; else -> IfEvent.Op6.bitmask }); IfSubOpHandler(bus, launcher).handle(player, IfSubOp(CombinedId(component.packed), 0, player.inv[0]!!.id, op, subop)) }
        fun wornSubOp(op: Int, subop: Int) { val component = org.rsmod.api.enums.EquipmentEnums.equipment_tab_to_slots_map[1]!!; player.ui.overlays.backing.put(0, component.packed ushr 16); player.ui.events.add(dev.openrune.rscm.RSCM.getReverseMapping(dev.openrune.rscm.RSCMType.COMPONENT, component.packed), -1..0, if (op == 2) IfEvent.Op2.bitmask else IfEvent.Op3.bitmask); IfSubOpHandler(bus, launcher).handle(player, IfSubOp(CombinedId(component.packed), -1, player.worn[1]!!.id, op, subop)) }
        fun tree(item: String = "obj.logs"): BoundLocInfo { val type = ServerCacheManager.getObjects().values.first { it.isContentType("content.tree") && it.treeLogs.id == item.asRSCM() }; return BoundLocInfo(LocInfo(2, player.coords.translateX(1), LocEntity(type.id, 10, 0)), type) }
        fun cut(item: String = "obj.logs") {
            val tree = tree(item); val type = ServerCacheManager.getObject(tree.id)!!; player.actionDelay = 100
            locs.interact(player, tree, InteractionOp.Op1); val event = checkNotNull(locs.opTrigger(player, tree, InteractionOp.Op1)); assertTrue(event is org.rsmod.api.player.events.interact.LocContentEvents.Op1, "tree=${type.id} event=${event::class} id=${event.id}"); run { player.actionDelay = clock.cycle; bus.publish(this, event) }
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
