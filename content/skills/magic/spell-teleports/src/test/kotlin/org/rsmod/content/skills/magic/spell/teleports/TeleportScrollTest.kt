package org.rsmod.content.skills.magic.spell.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.PlayerTeleportDelayHook
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.worn.HeldEquipOp
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class TeleportScrollTest {
    @Test fun `delayed native scroll retains its item until arrival and late cancellation consumes nothing`() {
        for (cancel in listOf(false, true)) {
            val f = Fixture(TeleportScroll.Nardah, delayed = true); val origin = f.player.coords
            f.click(); f.finish(); assertEquals(origin, f.player.coords); assertEquals(2, f.player.inv.count(f.scroll.item))
            f.release = true; if (cancel)f.denial = "Blocked"
            f.finish(); assertEquals(if (cancel)origin else f.scroll.destination, f.player.coords); assertEquals(if (cancel)2 else 1, f.player.inv.count(f.scroll.item))
        }
    }

    @Test fun `catalogue covers every real native teleport scroll and correct inventory option`() {
        val native = ServerCacheManager.getItems().values.filter {
            it.internalName.startsWith("obj.teleportscroll_") && "Teleport" in it.interfaceOptions
        }.map { it.id }.toSet()
        assertEquals(20, native.size)
        assertEquals(native, TeleportScroll.entries.map { it.item.asRSCM() }.toSet())
        for (scroll in TeleportScroll.entries) {
            val item = ServerCacheManager.getItem(scroll.item.asRSCM())!!
            assertEquals("Teleport", item.interfaceOptions[if (scroll == TeleportScroll.Revenants) 2 else 0])
        }
    }

    @Test fun `real held interactions teleport every scroll consume one and restore visible animation`() {
        for (scroll in TeleportScroll.entries) {
            val f = Fixture(scroll)
            f.click()
            if (scroll == TeleportScroll.Revenants) f.respond(true)
            assertEquals(2, f.player.inv.count(scroll.item), scroll.name)
            assertEquals(3864, f.player.pendingSequence.id, scroll.name)
            assertEquals(1039, EntitySpotanim(f.player.pendingSpotanims.last()).id, scroll.name)
            f.finish()
            assertEquals(scroll.destination, f.player.coords, scroll.name)
            assertEquals(1, f.player.inv.count(scroll.item), scroll.name)
            assertEquals(org.rsmod.game.seq.EntitySeq.ZERO, f.player.pendingSequence, scroll.name)
            assertEquals(65535, EntitySpotanim(f.player.pendingSpotanims.single()).id, scroll.name)
            assertEquals(0, f.player.statMap.getFineXP("stat.magic"))
        }
    }

    @Test fun `denial before or after animation preserves location and scroll`() {
        for (late in listOf(false, true)) {
            val f = Fixture(TeleportScroll.LunarIsle); val origin = f.player.coords
            if (!late) f.denial = "Blocked"
            f.click(); f.denial = "Blocked"; f.finish()
            assertEquals(origin, f.player.coords)
            assertEquals(2, f.player.inv.count(f.scroll.item))
            if (late) assertEquals(org.rsmod.game.seq.EntitySeq.ZERO, f.player.pendingSequence)
        }
    }

    @Test fun `logout death moved origin and stale inventory reference cannot consume or teleport`() {
        for (cause in listOf("logout", "death", "movement", "stale")) {
            val f = Fixture(TeleportScroll.Nardah); f.click()
            when (cause) {
                "logout" -> f.player.pendingLogout = true
                "death" -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0)
                "movement" -> f.player.coords = f.player.coords.translateX(1)
                "stale" -> f.player.inv[0] = InvObj(f.scroll.item, 2)
            }
            val origin = f.player.coords
            f.finish()
            assertEquals(origin, f.player.coords, cause)
            assertEquals(2, f.player.inv.count(f.scroll.item), cause)
        }
    }

    @Test fun `unloaded destination fails before animation and preserves inventory`() {
        val f = Fixture(TeleportScroll.Ardeaglais, destinationLoaded = false)
        val origin = f.player.coords
        f.click(); f.finish()
        assertEquals(origin, f.player.coords)
        assertEquals(2, f.player.inv.count(f.scroll.item))
    }

    @Test fun `Revenant danger confirmation can decline and Config does not teleport`() {
        val f = Fixture(TeleportScroll.Revenants); val origin = f.player.coords
        f.click(); f.respond(false); f.finish()
        assertEquals(origin, f.player.coords)
        assertEquals(2, f.player.inv.count(f.scroll.item))
        val config = Fixture(TeleportScroll.Revenants)
        config.click(HeldOp.Op4); config.finish()
        assertEquals(origin, config.player.coords)
        assertEquals(2, config.player.inv.count(config.scroll.item))
    }

    @Test fun `duplicate native clicks while scroll is pending cannot double consume`() {
        val f = Fixture(TeleportScroll.Digsite)
        f.click(); assertFalse(f.launcher.launch(f.player) { f.held.interact(this, f.player.inv, 0, HeldOp.Op1) })
        f.finish()
        assertEquals(1, f.player.inv.count(f.scroll.item))
    }

    @Test fun `Revenant native Teleport supports each of the three entrances`() {
        for ((option, destination) in listOf(1 to CoordGrid(3127, 3833), 2 to CoordGrid(3069, 3740), 3 to CoordGrid(3075, 3653))) {
            val f = Fixture(TeleportScroll.Revenants)
            f.click(); f.respond(true, option); f.finish()
            assertEquals(destination, f.player.coords)
            assertEquals(1, f.player.inv.count(f.scroll.item))
        }
    }

    private class Fixture(val scroll: TeleportScroll, destinationLoaded: Boolean = true, delayed: Boolean = false) {
        val bus = EventBus()
        val collision = TeleportTestCollision.map
        val areas = mock(AreaChecker::class.java)
        var denial: String? = null
        var release = false; val queues = WorldQueueList()
        val delays = if (delayed)setOf(object : PlayerTeleportDelayHook {
            override fun remaining(player: Player, type: TeleportType, areas: AreaChecker) = if (release)0 else 2
        }) else emptySet()
        val validator = PlayerTeleportValidator(setOf(PlayerTeleportValidateHook { _, _, _ -> denial }), delays, queues)
        val player = Player().apply {
            inv = Inventory(ServerCacheManager.getInventory("inv.inv".asRSCM())!!, arrayOfNulls(28))
            inv[0] = InvObj(scroll.item, 2)
            coords = CoordGrid(3222, 3218)
            currentMapClock = 100; processedMapClock = 100
        }
        val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getCollision = { collision },
            getTeleportValidator = { validator }, getAreaChecker = { areas }, getAlignment = { TextAlignment() })
        val factory = mock(ProtectedAccessContextFactory::class.java).apply { `when`(create()).thenReturn(context) }
        val launcher = ProtectedAccessLauncher(factory)
        val held: HeldInteractions
        init {
            collision.allocateIfAbsent(player.coords.x, player.coords.z, player.coords.level)
            for (coord in listOf(TeleportScrollScript.REVENANT_MIDDLE, TeleportScrollScript.REVENANT_SOUTH)) collision.allocateIfAbsent(coord.x, coord.z, coord.level)
            if (destinationLoaded) collision.allocateIfAbsent(scroll.destination.x, scroll.destination.z, scroll.destination.level)
            else collision.deallocateIfPresent(scroll.destination.x, scroll.destination.z, scroll.destination.level)
            val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) { scripts.startup() }
            with(TeleportScrollScript(validator, areas, collision)) { scripts.startup() }
            val ctor = HeldInteractions::class.java.declaredConstructors.single().apply { isAccessible = true }
            held = ctor.newInstance(bus, mock(ctor.parameterTypes[1]), mock(ctor.parameterTypes[2]), HeldEquipOp(bus)) as HeldInteractions
        }
        fun click(op: HeldOp = if (scroll == TeleportScroll.Revenants) HeldOp.Op3 else HeldOp.Op1) {
            assertTrue(launcher.launch(player) { held.interact(this, player.inv, 0, op) })
        }
        fun respond(accept: Boolean, destinationOption: Int = 1) {
            var menus = 0
            repeat(3) {
                val coroutine = player.activeCoroutine ?: return
                if (!coroutine.isAwaiting(ResumePauseButtonInput::class)) return
                val menu = player.ui.containsModal("interface.chatmenu")
                coroutine.resumeWith(ResumePauseButtonInput(if (menu) "component.chatmenu:options" else "component.messagebox:continue",
                    if (menu) { if (menus++ == 0) destinationOption else if (accept) 1 else 2 } else -1))
            }
        }
        fun finish() {
            repeat(6) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance()
                val it = queues.iterator(); while (it.hasNext()) { val q = it.next(); if (--q.remainingCycles <= 0) { it.remove(); q.action() } }; it.cleanUp()
            }
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
