package org.rsmod.content.other.maxcape

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import net.rsprot.protocol.game.incoming.buttons.IfSubOp
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.enums.EquipmentEnums.equipment_stats_to_slots_map
import org.rsmod.api.enums.EquipmentEnums.equipment_tab_to_slots_map
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.net.rsprot.handlers.IfSubOpHandler
import org.rsmod.api.player.back
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.random.GameRandom
import org.rsmod.api.spells.autocast.AutocastWeapons
import org.rsmod.api.spells.autocast.MagicSpellbookManager
import org.rsmod.content.other.consumables.potion.PotionEffectService
import org.rsmod.content.other.consumables.potion.PotionSpecialEffectService
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class MaxCapeTest {
    @Test fun `every client submenu action resolves and boat actions remain disabled`() {
        for (n in 1..20) assertNotNull(MaxCapeOptions.held(2, n), "teleport $n")
        for (n in 1..5) assertNotNull(MaxCapeOptions.held(3, n), "spellbook $n")
        for (n in 1..6) assertNotNull(MaxCapeOptions.held(4, n), "feature $n")
        assertEquals(CapeAction.Sailing, MaxCapeOptions.held(2, 20))
        assertEquals(CapeAction.Sailing, MaxCapeOptions.held(4, 5))
        assertNull(MaxCapeOptions.held(4, 0))
        assertNull(MaxCapeOptions.held(4, 7))
        assertNull(MaxCapeOptions.worn(1, 1))
    }

    @Test fun `search gives both items and fails atomically with just one free slot`() {
        val f = Fixture()
        for (i in 1..26) f.player.inv[i] = InvObj("obj.shark")
        val before = f.player.inv.objs.toList()
        f.action(CapeAction.Search)
        assertEquals(before, f.player.inv.objs.toList())
        assertEquals(0, f.player.capeSearchUses)
        f.player.inv[26] = null
        f.action(CapeAction.Search)
        assertTrue("obj.xbows_crossbow_bronze" in f.player.inv)
        assertTrue("obj.xbows_grapple_tip_bolt_mithril_rope" in f.player.inv)
        assertEquals(1, f.player.capeSearchUses)
    }

    @Test fun `spellbook changes clear autocast and reject sixth use or same book without consuming a use`() {
        val f = Fixture()
        f.action(CapeAction.Spellbook(0))
        assertEquals(0, f.player.capeSpellbookUses)
        VarPlayerIntMapSetter.set(f.player, "varbit.autocast_set", 1)
        VarPlayerIntMapSetter.set(f.player, "varbit.autocast_spell", 1)
        for (book in listOf(1, 2, 3, 0, 1)) f.action(CapeAction.Spellbook(book))
        assertEquals(5, f.player.capeSpellbookUses)
        assertEquals(0, f.player.vars["varbit.autocast_set"])
        assertEquals(0, f.player.vars["varbit.autocast_spell"])
        f.action(CapeAction.Spellbook(2))
        assertEquals(Spellbook.Ancients, f.books.activeSpellbook(f.player))
    }

    @Test fun `daily ledger survives a relog and only its daily fields reset on a new UTC day`() {
        val f = Fixture()
        f.action(CapeAction.Search)
        f.player.capeStaminaUsed = true
        f.player.capeJunkDisabled = true
        f.player.capeLifeDisabled = true
        val persisted = f.player.vars.backing.toMap()
        val login = Fixture().player
        persisted.forEach { (id, value) -> login.vars.backing[id] = value }
        login.resetCapeDailyUses(f.player.capeResetDay)
        assertEquals(1, login.capeSearchUses)
        assertTrue(login.capeStaminaUsed)
        login.resetCapeDailyUses(f.player.capeResetDay + 1)
        assertEquals(0, login.capeSearchUses)
        assertFalse(login.capeStaminaUsed)
        assertTrue(login.capeJunkDisabled)
        assertTrue(login.capeLifeDisabled)
    }

    @Test fun `stamina restores full energy once and does not shorten an existing potion`() {
        val f = Fixture()
        f.access { f.potions.boostStamina(this, 200) }
        f.player.runEnergy = 100
        f.action(CapeAction.Stamina)
        assertEquals(10_000, f.player.runEnergy)
        assertEquals(1, f.player.vars["varbit.stamina_active"])
        f.player.runEnergy = 250
        f.action(CapeAction.Stamina)
        assertEquals(250, f.player.runEnergy)
        f.player.currentMapClock = 201
        f.clock.cycle = 201
        f.access { f.potions.processStamina(this) }
        assertEquals(1, f.player.vars["varbit.stamina_active"])
        f.player.currentMapClock = 301
        f.clock.cycle = 301
        f.access { f.potions.processStamina(this) }
        assertEquals(0, f.player.vars["varbit.stamina_active"])
    }

    @Test fun `inventory submenu packet invokes spellbook change`() {
        val f = Fixture()
        val obj = f.player.inv[0]!!
        val component = ServerCacheManager.fromComponent("component.inventory:items")
        f.player.ui.overlays.backing.put(0, component.packed ushr 16)
        f.player.ui.events.add("component.inventory:items", 0..27, IfEvent.Op4.bitmask)
        IfSubOpHandler(f.events, f.launcher).handle(f.player, IfSubOp(CombinedId(component.packed), 0, obj.id, 4, 2))
        assertEquals(Spellbook.Ancients, f.books.activeSpellbook(f.player))
    }

    @Test fun `worn submenu packet is not mistaken for an inventory click at the same slot`() {
        val f = Fixture()
        f.player.back = InvObj(MaxCapeOptions.WORN)
        val component = equipment_tab_to_slots_map[Wearpos.Back.slot]!!
        f.player.ui.overlays.backing.put(0, component.packed ushr 16)
        f.player.ui.events.add(RSCM.getReverseMapping(RSCMType.COMPONENT, component.packed), 0..0, IfEvent.Op7.bitmask)
        IfSubOpHandler(f.events, f.launcher).handle(f.player, IfSubOp(CombinedId(component.packed), 0, f.player.back!!.id, 7, 2))
        assertEquals(Spellbook.Ancients, f.books.activeSpellbook(f.player))
    }

    @Test fun `missing cape and forged worn item cannot grant benefits`() {
        val f = Fixture()
        f.player.inv[0] = null
        f.action(CapeAction.Search)
        f.action(CapeAction.Spellbook(1))
        assertEquals(0, f.player.capeSearchUses)
        assertEquals(Spellbook.Standard, f.books.activeSpellbook(f.player))
    }

    @Test fun `equipment stats modal allows its own cape submenu and rejects stale item packets`() {
        val f = Fixture()
        f.player.back = InvObj(MaxCapeOptions.WORN)
        val component = equipment_stats_to_slots_map[Wearpos.Back.slot]!!
        f.player.ui.modals.backing.put(0, component.packed ushr 16)
        f.player.ui.events.add(RSCM.getReverseMapping(RSCMType.COMPONENT, component.packed), 0..0, IfEvent.Op7.bitmask)
        val handler = IfSubOpHandler(f.events, f.launcher)
        handler.handle(f.player, IfSubOp(CombinedId(component.packed), 0, "obj.shark".asRSCM(), 7, 2))
        assertEquals(Spellbook.Standard, f.books.activeSpellbook(f.player))
        handler.handle(f.player, IfSubOp(CombinedId(component.packed), 0, f.player.back!!.id, 7, 2))
        assertEquals(Spellbook.Ancients, f.books.activeSpellbook(f.player))
    }

    @Test fun `equip transforms to the native worn cape`() {
        val f = Fixture()
        val result = HeldEquipOp(f.events).equip(f.player, 0, f.player.inv)
        assertTrue(result is HeldEquipResult.Success, result.toString())
        assertEquals(MaxCapeOptions.WORN.asRSCM(), f.player.back?.id)
        assertNull(f.player.inv[0])
    }

    @Test fun `ring of life requires a pending nonlethal hit and obeys teleport denial`() {
        val f = Fixture()
        f.player.back = InvObj(MaxCapeOptions.WORN)
        f.player.statMap.setCurrentLevel("stat.hitpoints", 9)
        val before = f.player.coords
        f.script.onPostTick(f.player)
        assertEquals(before, f.player.coords)
        f.player.capeEscapePending = 1
        f.script.onPostTick(f.player)
        assertEquals(CapeDestination.Home.coords, f.player.coords)
        assertEquals(0, f.player.capeEscapePending)
        f.player.coords = before
        f.denial = "Blocked"
        f.player.capeEscapePending = 1
        f.script.onPostTick(f.player)
        assertEquals(before, f.player.coords)
        assertEquals(0, f.player.capeEscapePending)
        f.player.statMap.setCurrentLevel("stat.hitpoints", 0)
        assertFalse(f.script.shouldEscape(f.player))
    }

    @Test fun `commune controls actual collection and a full inventory never drops junk`() {
        val f = Fixture()
        f.player.back = InvObj(MaxCapeOptions.WORN)
        f.player.capeNextCollection = 99
        f.script.onPostTick(f.player)
        assertTrue("obj.steel_arrow" in f.player.inv)
        val before = f.player.inv.objs.toList()
        f.action(CapeAction.Commune)
        f.player.currentMapClock += 100
        f.script.onPostTick(f.player)
        assertEquals(before, f.player.inv.objs.toList())
        f.action(CapeAction.Commune)
        for (i in 1..27) f.player.inv[i] = InvObj("obj.shark")
        val full = f.player.inv.objs.toList()
        f.player.currentMapClock += 100
        f.script.onPostTick(f.player)
        assertEquals(full, f.player.inv.objs.toList())
    }

    @Test fun `land teleports obey denial and chinchompa limit is charged only on success`() {
        val f = Fixture()
        val start = f.player.coords
        f.denial = "Blocked"
        f.action(CapeAction.Teleport(CapeDestination.Feldip))
        assertEquals(start, f.player.coords)
        assertEquals(0, f.player.capeHunterUses)
        f.denial = null
        for (d in CapeDestination.entries.filter { it != CapeDestination.BlackChins }) {
            f.action(CapeAction.Teleport(d))
            assertTrue(f.player.coords.chebyshevDistance(d.coords) <= 3, d.label)
        }
        assertEquals(1, f.player.capeHunterUses)
        f.player.capeHunterUses = 5
        val last = f.player.coords
        f.action(CapeAction.Teleport(CapeDestination.Feldip))
        assertEquals(last, f.player.coords)
    }

    private class Fixture {
        val events = EventBus()
        val clock = MapClock(100)
        var denial: String? = null
        val validator = PlayerTeleportValidator(setOf(PlayerTeleportValidateHook { _, _, _ -> denial }))
        val areas = mock(AreaChecker::class.java)
        val random = mock(GameRandom::class.java)
        val books = MagicSpellbookManager(mock(AutocastWeapons::class.java))
        val potions = PotionEffectService(mock(PotionSpecialEffectService::class.java), clock)
        val collision = CollisionFlagMap().apply {
            allocateIfAbsent(3204, 3204, 0)
            for (d in CapeDestination.entries) for (x in -8..8 step 8) for (z in -8..8 step 8)
                allocateIfAbsent(d.coords.x + x, d.coords.z + z, 0)
        }
        val player = Player().apply {
            inv = Inventory(ServerCacheManager.getInventory("inv.inv".asRSCM())!!, arrayOfNulls(28))
            worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14))
            inv[0] = InvObj(MaxCapeOptions.HELD)
            coords = CoordGrid(3204, 3204)
            currentMapClock = 100
            for (s in ServerCacheManager.getStats().values) {
                statMap.setBaseLevel(s.internalName, 99)
                statMap.setCurrentLevel(s.internalName, 99)
            }
        }
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { events }, getCollision = { collision },
            getAreaChecker = { areas }, getTeleportValidator = { validator }, getRandom = { random })
        val launcher = ProtectedAccessLauncher(mock(ProtectedAccessContextFactory::class.java).apply {
            `when`(create()).thenReturn(context)
        })
        val script = MaxCapeScript(books, potions, validator, areas, launcher, random)
        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { scripts.startup() }
            with(script) { scripts.startup() }
        }
        fun access(block: suspend ProtectedAccess.() -> Unit) { assertTrue(ProtectedAccessLauncher.withProtectedAccess(player, context, block = block)) }
        fun action(action: CapeAction) { with(script) { access { perform(action) } } }
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
