package org.rsmod.content.travel.fairyrings

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import java.nio.file.Files
import java.nio.file.Path
import net.rsprot.protocol.game.incoming.buttons.If3Button
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.game.process.player.PlayerQueueProcessor
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.hook.*
import org.rsmod.api.player.interact.LocInteractions
import org.rsmod.api.player.protect.*
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.*
import org.rsmod.api.route.BoundValidator
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.*
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@ResourceLock("ServerCacheManager")
internal class FairyRingTravelTest {
    @Test fun `authoritative native centre landings have a walkable exit`() {
        val blocked = rings.all.filter { it.destination?.let { tile ->
            listOf(tile.translate(1, 0), tile.translate(-1, 0), tile.translate(0, 1), tile.translate(0, -1)).none { next -> map[next.x, next.z, next.level] and BLOCKED == 0 }
        } == true }
        Files.writeString(Path.of("../fairy-native-codes.tsv"), rings.all.joinToString("\n") { "${it.code}\t${it.destination}\t${it.noStaffReturn}\t${it.description}" })
        Files.writeString(Path.of("../fairy-arrival-flags.tsv"), rings.all.filter { it.destination != null }.joinToString("\n") { r ->
            val d = r.destination!!; "${r.code}\t" + (-3..3).flatMap { dx -> (-3..3).map { dz -> "$dx,$dz:${map[d.x + dx,d.z + dz,d.level].toString(16)}" } }.joinToString(";")
        })
        assertTrue(blocked.isEmpty(), "Blocked native rings: $blocked")
    }

    @Test fun `native ring click IF3 dials and confirm reach every native destination`() {
        val f = Fixture()
        for (r in rings.all) {
            f.player.coords = HUB
            f.open()
            f.setCode(r)
            f.click("component.fairyrings:confirm")
            f.finish()
            assertEquals(r.destination ?: HUB, f.player.coords, r.code)
            if (r.destination != null && r.destination != HUB) {
                r.visitedVarBit?.let { assertEquals(1, f.player.vars[it], r.code) }
                assertEquals(r.logIndex, f.player.vars["varbit.fairyring_lastloc"], r.code)
            }
        }
    }

    @Test fun `favorite native log buttons and last destination survive reopening`() {
        val f = Fixture(); val ring = rings.byCode("AIQ")!!
        f.open(); f.setCode(ring); f.click("component.fairyrings:confirm"); f.finish()
        f.player.coords = HUB; f.open()
        f.click(ring.logComponent, 2)
        assertEquals(1, f.player.vars["varbit.fairyring_fave_set_1"])
        f.click("component.fairyrings_log:fave_1")
        assertEquals(ring.dials, (1..3).map { f.player.vars["varbit.fairyring_$it"] })
        f.click("component.fairyrings_log:fave_1", 2)
        assertEquals(0, f.player.vars["varbit.fairyring_fave_set_1"])
        f.loc(InteractionOp.Op3); f.finish()
        assertEquals(ring.destination, f.player.coords)
    }

    @Test fun `closed UI packets death logout displacement and late denial cannot teleport or record visits`() {
        val ring = rings.byCode("AIQ")!!
        for (mode in 0..4) {
            val f = Fixture(); f.open(); f.setCode(ring)
            if (mode == 4) f.player.ui.modals.backing.clear()
            f.click("component.fairyrings:confirm")
            when (mode) {
                0 -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0)
                1 -> f.player.pendingLogout = true
                2 -> f.player.coords = HUB.translateX(1)
                3 -> f.denial = "A teleport cannot be used here."
            }
            f.finish()
            assertNotEquals(ring.destination, f.player.coords)
            assertEquals(0, f.player.vars[ring.visitedVarBit!!])
        }
    }

    @Test fun `staff requirement allows native no staff return and elite diary`() {
        val f = Fixture(); f.player.worn.fillNulls(); f.open()
        assertFalse(f.player.ui.containsModal("interface.fairyrings"))
        VarPlayerIntMapSetter.set(f.player, "varbit.lumbridge_diary_elite_complete", 1); f.open()
        assertTrue(f.player.ui.containsModal("interface.fairyrings"))
    }

    @Test fun `all six native dial buttons rotate and wrap and closed UI buttons cannot alter them`() {
        val f = Fixture(); f.open()
        for (dial in 1..3) {
            VarPlayerIntMapSetter.set(f.player, "varbit.fairyring_$dial", 0)
            f.click("component.fairyrings:${dial}_anticlockwise")
            assertEquals(3, f.player.vars["varbit.fairyring_$dial"])
            f.click("component.fairyrings:${dial}_clockwise")
            assertEquals(0, f.player.vars["varbit.fairyring_$dial"])
        }
        f.player.ifClose(f.bus)
        f.click("component.fairyrings:1_clockwise")
        assertEquals(0, f.player.vars["varbit.fairyring_1"])
    }

    @Test fun `native island return works without a staff and hideout requires the complete successful sequence`() {
        val f = Fixture(); f.open(); f.setCode(rings.byCode("AJS")!!); f.click("component.fairyrings:confirm"); f.finish()
        assertEquals(rings.byCode("AJS")!!.destination, f.player.coords)
        f.player.worn.fillNulls(); f.loc(InteractionOp.Op1); f.finish()
        assertEquals(HUB, f.player.coords)
        f.player.worn[3] = InvObj("obj.dramen_staff")
        for (code in listOf("AIR", "DLR", "DJQ", "AJS")) {
            f.open(); f.setCode(rings.byCode(code)!!); f.click("component.fairyrings:confirm"); f.finish()
        }
        assertEquals(CoordGrid(2328, 4426), f.player.coords)
        assertEquals(1, f.player.vars["varbit.fairyrings_log_hideout"])
        assertEquals(1, f.player.vars["varbit.fairyring_lastloc_set"])
        f.player.coords = HUB; f.loc(InteractionOp.Op3); f.finish()
        assertEquals(CoordGrid(2328, 4426), f.player.coords)
        f.player.coords = HUB; f.open(); f.click("component.fairyrings_log:hideout"); f.finish(32)
        assertEquals(CoordGrid(2328, 4426), f.player.coords)
    }

    private class Fixture {
        val bus = EventBus(); val ctx = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        val player = Player(client).apply {
            coords = HUB; currentMapClock = 100; processedMapClock = 100
            inv = Inventory.create("inv.inv"); worn = Inventory.create("inv.worn")
            worn[3] = InvObj("obj.dramen_staff")
            statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99)
        }
        var denial: String? = null
        private val areas = mock(AreaChecker::class.java)
        private val validator = PlayerTeleportValidator(setOf(PlayerTeleportValidateHook { _, _, _ -> denial }))
        val launcher: ProtectedAccessLauncher
        private val queues: PlayerQueueProcessor
        private val locProducer = LocInteractions(BoundValidator(map), bus)
        private val hub: BoundLocInfo get() {
            val symbol = if (player.coords == HUB) "loc.fairyring_homehub" else "loc.fairyring_minorhub"
            return BoundLocInfo(LocInfo(2, player.coords, LocEntity(symbol.asRSCM(), 10, 0)), ServerCacheManager.getObject(symbol.asRSCM())!!)
        }
        init {
            val context = ProtectedAccessContextFactory.empty().copy(getEventBus = { bus }, getCollision = { map }, getTeleportValidator = { validator }, getAreaChecker = { areas }, getAlignment = { TextAlignment() })
            val factory = mock(ProtectedAccessContextFactory::class.java).apply { `when`(create()).thenReturn(context) }
            launcher = ProtectedAccessLauncher(factory); queues = PlayerQueueProcessor(bus, launcher)
            with(FairyRingScript(validator, areas, map)) { ctx.startup() }
        }
        fun open() = loc(InteractionOp.Op2)
        fun loc(op: InteractionOp) {
            player.ifClose(bus)
            locProducer.interact(player, hub, op)
            val event = locProducer.opTrigger(player, hub, op)!!
            launcher.launch(player) { assertTrue(bus.publish(this, event)) }
        }
        fun setCode(ring: FairyRing) { for ((index, value)in ring.dials.withIndex())VarPlayerIntMapSetter.set(player, "varbit.fairyring_${index + 1}", value) }
        fun click(component: String, op: Int = 1) {
            val ctor = If3Button::class.java.declaredConstructors.single { it.parameterCount == 5 }
            val packet = ctor.newInstance(component.asRSCM(), -1, -1, op, null) as If3Button
            If3ButtonHandler(bus, launcher).handle(player, packet)
        }
        fun finish(ticks: Int = 6) { repeat(ticks) { player.currentMapClock++; player.processedMapClock = player.currentMapClock; player.activeCoroutine?.advance(); queues.process(player) } }
    }
    companion object {
        private val HUB = CoordGrid(2412, 4434)
        private val map = CollisionFlagMap()
        private lateinit var rings: FairyRings
        private const val BLOCKED = CollisionFlag.LOC or CollisionFlag.GROUND_DECOR or CollisionFlag.BLOCK_WALK
        private val client = object : Client<Any, Any> {
            override fun write(message: Any) = Unit; override fun close() = Unit; override fun read(player: Player) = Unit; override fun flush() = Unit; override fun flushHighPriority() = Unit; override fun unregister(service: Any, player: Player) = Unit
        }

        @JvmStatic @BeforeAll fun maps() {
            val cache = ServerCacheManager.init(240)
            try {
                rings = FairyRings.load()
                for (square in (rings.all.mapNotNull { it.destination } + HUB + CoordGrid(2328, 4426)).map { MapSquareKey(it.x shr 6, it.z shr 6) }.distinct()) {
                    val group = square.x shl 8 or square.z
                    val floor = MapTileDecoder.decode(InlineByteBuf(cache.data(MAPS, group, 0)!!))
                    val locs = MapLocListDecoder.decode(InlineByteBuf(cache.data(MAPS, group, 1)!!))
                    for (l in 0..3)for (x in square.x * 64..square.x * 64 + 63 step 8)for (z in square.z * 64..square.z * 64 + 63 step 8)map.allocateIfAbsent(x, z, l)
                    GameMapDecoder.putMaps(map, square, floor); GameMapDecoder.putLocs(GameMapBuilder(), map, square, floor, locs)
                }
            } finally { cache.close() }
        }
    }
}
