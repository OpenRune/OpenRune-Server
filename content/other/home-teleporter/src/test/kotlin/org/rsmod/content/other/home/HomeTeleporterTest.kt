package org.rsmod.content.other.home

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.central.account.Rights
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.game.process.player.PlayerQueueProcessor
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.*
import org.rsmod.api.player.input.ResumePStringDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.RouteFinding
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
internal class HomeTeleporterTest {
    @Test fun `public home command uses Edgeville without items xp or admin exemption`() {
        val f = Fixture()
        assertEquals(Rights.NONE, f.player.modLevel)
        assertTrue(f.commands.execute(f.player, "home", emptyList()))
        f.finish()
        assertEquals(TeleportCatalogue.HOME, f.player.coords)
        assertTrue(f.types.all { it == TeleportType.Standard })
        assertTrue(f.player.inv.isEmpty())
        assertEquals(0, f.player.statMap.getFineXP("stat.magic"))
    }

    @Test fun `native wizard interaction and public command open the same menu`() {
        val f = Fixture()
        val npc = Npc(ServerCacheManager.getNpc(TeleportCatalogue.WIZARD_TYPE.asRSCM())!!, TeleportCatalogue.WIZARD).apply { slotId = 20; assignUid() }
        val producer = NpcInteractions(f.bus)
        producer.interact(f.player, npc, InteractionOp.Op1)
        val event = producer.opTrigger(f.player, npc, InteractionOp.Op1)!!
        f.launcher.launch(f.player) { assertTrue(f.bus.publish(this, event)) }
        assertTrue(f.player.ui.containsModal(HomeTeleportMenu.INTERFACE))
        assertTrue(f.commands.execute(f.player, "teleports", emptyList()))
        assertTrue(f.text("query").startsWith("Cities"))
    }

    @Test fun `native IF3 search selects destination and teleport button actually travels`() {
        val f = Fixture(); f.search("Zulrah")
        assertTrue(f.text("name_0").contains("Zulrah"))
        f.click("row_0"); assertTrue(f.text("detail").contains("boat"))
        f.click("teleport"); f.finish()
        assertEquals(CoordGrid(2196, 3056), f.player.coords)
    }

    @Test fun `all categories and pagination can be selected and empty searches stay bounded`() {
        val f = Fixture(); f.open()
        for (category in TeleportCategory.entries) {
            f.click("tab_${category.key}")
            assertTrue(f.text("query").startsWith(category.label))
            repeat(30) { f.click("next") }
            val page = f.text("page").split('/').map(String::toInt)
            assertEquals(page[0], page[1])
            repeat(30) { f.click("previous") }
            assertTrue(f.text("page").startsWith("1/"))
        }
        f.search("no-destination-has-this-name"); f.click("row_0"); f.click("teleport"); f.finish()
        assertEquals(f.origin, f.player.coords)
        assertEquals("1/1", f.text("page"))
    }

    @Test fun `planned raids cannot teleport and stale closed interface packets are ignored`() {
        val f = Fixture(); f.search("Chambers of Xeric"); f.click("name_0"); f.click("teleport"); f.finish()
        assertEquals(f.origin, f.player.coords)
        f.launcher.launchLenient(f.player) { ifClose() }
        f.click("teleport"); f.finish(); assertEquals(f.origin, f.player.coords)
    }

    @Test fun `Wilderness confirmation accepts cancels and rechecks restrictions`() {
        for (mode in listOf("accept", "cancel", "block", "move")) {
            val f = Fixture(); f.search("Revenant Caves"); f.click("row_0"); f.click("teleport")
            assertEquals(f.origin, f.player.coords)
            assertTrue(f.player.activeCoroutine != null)
            if (mode == "block") f.denial = "Teleport blocked"
            if (mode == "move") f.player.coords = f.origin.translateX(1)
            f.respond(mode != "cancel"); f.finish()
            assertEquals(if (mode == "accept") CoordGrid(3075, 3653) else if (mode == "move") f.origin.translateX(1) else f.origin, f.player.coords)
        }
    }

    @Test fun `late denial movement logout and blocked arrival cancel outgoing animation`() {
        for (mode in listOf("block", "move", "logout", "landing")) {
            val f = Fixture(); f.commands.execute(f.player, "home", emptyList())
            when (mode) {
                "block" -> f.denial = "Blocked"
                "move" -> f.player.coords = f.origin.translateX(1)
                "logout" -> f.player.pendingLogout = true
                "landing" -> sharedCollision.add(TeleportCatalogue.HOME.x, TeleportCatalogue.HOME.z, 0, HomeTravel.BLOCKED)
            }
            f.finish()
            assertNotEquals(TeleportCatalogue.HOME, f.player.coords)
            assertEquals(65535, org.rsmod.game.spot.EntitySpotanim(f.player.pendingSpotanims.single()).id)
            sharedCollision.remove(TeleportCatalogue.HOME.x, TeleportCatalogue.HOME.z, 0, HomeTravel.BLOCKED)
        }
    }

    @Test fun `duplicate requests cast denial and death cannot force a teleport`() {
        val denied = Fixture(); denied.denial = "A teleport block has been cast on you."
        denied.commands.execute(denied.player, "home", emptyList()); denied.finish()
        assertEquals(denied.origin, denied.player.coords)
        val duplicate = Fixture()
        repeat(3) { duplicate.commands.execute(duplicate.player, "home", emptyList()) }
        duplicate.finish(); assertEquals(TeleportCatalogue.HOME, duplicate.player.coords)
        val dead = Fixture(); dead.commands.execute(dead.player, "home", emptyList())
        dead.player.statMap.setCurrentLevel("stat.hitpoints", 0); dead.finish()
        assertEquals(dead.origin, dead.player.coords)
    }

    @Test fun `owned wizard startup and unload only change its own NPC`() {
        val f = Fixture()
        f.bus.publish(GameLifecycle.Startup)
        val captor = org.mockito.ArgumentCaptor.forClass(Npc::class.java)
        NpcRepository::class.java.getMethod("add", Npc::class.java, Int::class.javaPrimitiveType).invoke(verify(f.npcs), captor.capture(), eq(Int.MAX_VALUE))
        assertEquals(TeleportCatalogue.WIZARD, captor.value.coords)
        captor.value.slotId = 1
        with(f.script) { f.ctx.shutdown() }
        verify(f.npcs).del(captor.value, Int.MAX_VALUE)
    }

    @Test fun `native components fit fixed and resizable modal frame and symbols exist`() {
        val ui = ServerCacheManager.getInterface(HomeTeleportMenu.INTERFACE.asRSCM())!!
        for (c in ui.components.values) {
            assertTrue(c.x >= 0 && c.y >= 0 && c.x + c.width <= 512 && c.y + c.height <= 334, c.internalName)
        }
        for (category in TeleportCategory.entries) assertTrue(ServerCacheManager.getItem(category.icon.asRSCM()) != null, category.label)
        assertTrue(ServerCacheManager.getNpc(TeleportCatalogue.WIZARD_TYPE.asRSCM()) != null)
        val wizard = ServerCacheManager.getNpc(TeleportCatalogue.WIZARD_TYPE.asRSCM())!!
        assertEquals(0, wizard.huntRange)
        assertTrue(wizard.huntMode == null)
        assertEquals(61, HomeTravel.QUEUE.asRSCM())
        assertTrue(TeleportCatalogue.all.size >= 140)
        assertTrue(TeleportCatalogue.all.filter { it.category == TeleportCategory.Raids }.none { it.available })
    }

    @Test fun `all available destinations use actual walkable native maps and home can reach wizard`() {
        val tiles = TeleportCatalogue.all.mapNotNull { it.destination } + TeleportCatalogue.WIZARD
        val map = CollisionFlagMap()
        val cache = ServerCacheManager.init(240)
        try {
            for (square in tiles.map { MapSquareKey(it.x shr 6, it.z shr 6) }.distinct()) {
                val group = (square.x shl 8) or square.z
                val floor = MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 0))))
                val locs = MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 1))))
                for (level in 0..3) for (x in square.x * 64..square.x * 64 + 63 step 8) for (z in square.z * 64..square.z * 64 + 63 step 8) map.allocateIfAbsent(x, z, level)
                GameMapDecoder.putMaps(map, square, floor)
                GameMapDecoder.putLocs(GameMapBuilder(), map, square, floor, locs)
            }
        } finally { cache.close() }
        val blocked = TeleportCatalogue.all.filter { it.destination != null && map[it.destination.x, it.destination.z, it.destination.level] and HomeTravel.BLOCKED != 0 }
        val alternatives = blocked.map { row -> row.name to buildList { val t = row.destination!!; for (dx in -3..3) for (dz in -3..3) if (map[t.x + dx, t.z + dz, t.level] and HomeTravel.BLOCKED == 0) add("${t.x + dx},${t.z + dz},${t.level}") } }
        assertTrue(blocked.isEmpty(), "Blocked landings: $alternatives")
        val route = RouteFinding(map).findRoute(level = 0, srcX = TeleportCatalogue.HOME.x, srcZ = TeleportCatalogue.HOME.z,
            destX = TeleportCatalogue.WIZARD.x, destZ = TeleportCatalogue.WIZARD.z, destWidth = 1, destLength = 1, moveNear = false)
        assertTrue(route.success)
    }

    private class Fixture {
        val bus = EventBus(); val commands = CheatCommandMap(); val ctx = ScriptContext(bus, commands, EngineQueueCache())
        val client = RecordingClient(); val origin = CoordGrid(3212, 3424)
        val player = Player(client).apply { username = "home-test"; coords = origin; currentMapClock = 100; processedMapClock = 100; inv = Inventory(ServerCacheManager.getInventory("inv.inv".asRSCM())!!, arrayOfNulls(28)) }
        val types = mutableListOf<TeleportType>(); var denial: String? = null
        val areas = mock(AreaChecker::class.java)
        val validator = PlayerTeleportValidator(setOf(PlayerTeleportValidateHook { _, type, _ -> types += type; denial }))
        val launcher: ProtectedAccessLauncher
        val travel = HomeTravel(validator, areas, sharedCollision)
        val menu = HomeTeleportMenu(travel)
        val npcs = mock(NpcRepository::class.java)
        val script: HomeTeleporterScript
        val queues: PlayerQueueProcessor
        init {
            sharedCollision.allocateIfAbsent(origin.x, origin.z, origin.level)
            TeleportCatalogue.all.mapNotNull { it.destination }.forEach { sharedCollision.allocateIfAbsent(it.x, it.z, it.level) }
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getCollision = { sharedCollision }, getTeleportValidator = { validator }, getAreaChecker = { areas }, getAlignment = { TextAlignment() })
            val factory = mock(ProtectedAccessContextFactory::class.java).apply { `when`(create()).thenReturn(context) }
            launcher = ProtectedAccessLauncher(factory)
            queues = PlayerQueueProcessor(bus, launcher)
            script = HomeTeleporterScript(npcs, menu, travel, launcher)
            with(travel) { ctx.startup() }; with(menu) { ctx.startup() }; with(script) { ctx.startup() }
        }
        fun open() { commands.execute(player, "teleports", emptyList()) }
        fun search(query: String) { open(); click("search"); player.resumeActiveCoroutine(ResumePStringDialogInput(query)) }
        fun click(name: String) {
            val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }
            val packet = ctor.newInstance(HomeTeleportMenu.comp(name).asRSCM(), -1, -1, 1, null) as If3Button
            If3ButtonHandler(bus, launcher).handle(player, packet)
        }
        fun text(name: String) = client.messages.filterIsInstance<IfSetText>().last { it.combinedId == HomeTeleportMenu.comp(name).asRSCM() }.text
        fun respond(accept: Boolean) { player.resumeActiveCoroutine(ResumePauseButtonInput("component.chatmenu:options", if (accept) 1 else 2)) }
        fun finish() { repeat(6) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; queues.process(player) } }
    }
    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()
        override fun write(message: Any) { messages += message }
        override fun close() = Unit
        override fun read(player: Player) = Unit
        override fun flush() = Unit
        override fun flushHighPriority() = Unit
        override fun unregister(service: Any, player: Player) = Unit
    }
    companion object {
        private val sharedCollision = CollisionFlagMap()

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
