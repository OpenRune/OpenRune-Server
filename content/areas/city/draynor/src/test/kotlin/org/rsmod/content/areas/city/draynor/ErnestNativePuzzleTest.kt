package org.rsmod.content.areas.city.draynor

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocDefinition
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.loc.MapLocListDefinition
import dev.openrune.map.obj.MapObjDefinition
import dev.openrune.map.obj.MapObjListDecoder
import dev.openrune.map.obj.MapObjListDefinition
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.tile.MapTileSimpleDefinition
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.content.areas.city.draynor.ErnestLeverPuzzle.Doors
import org.rsmod.content.areas.city.draynor.ErnestLeverPuzzle.LeverState
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.loc.LocLayerConstants

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ErnestNativePuzzleTest {
    @Test
    fun `all nine native gates and six levers use the script's varbits and morph states`() {
        val f = Fixture()
        for ((name, gate) in f.gates) {
            val type = checkNotNull(ServerCacheManager.getObject(gate.id))
            assertEquals("varbit.ernestdoor_$name".asRSCM(), type.multiVarBit)
            assertEquals("loc.ernest_doorclosed".asRSCM(), type.multiLoc[0])
            assertEquals("loc.ernest_doorajar".asRSCM(), type.multiLoc[1])
        }
        for (letter in 'a'..'f') {
            val lever = f.loc("loc.lever$letter")
            val type = checkNotNull(ServerCacheManager.getObject(lever.id))
            assertEquals("varbit.ernestlever_$letter".asRSCM(), type.multiVarBit)
            assertEquals("loc.lever${letter}_up".asRSCM(), type.multiLoc[0])
            assertEquals("loc.lever${letter}_down".asRSCM(), type.multiLoc[1])
        }
    }

    @Test
    fun `the lever solution traverses every native gate reaches the oil can and returns`() {
        val f = Fixture()
        assertFalse(f.canWalkTo(f.oilCan))
        f.pull('a')
        f.pull('b')
        f.cross("4to7")
        f.pull('d')
        f.cross("4to5")
        f.cross("4to5")
        f.cross("4to7")
        f.pull('a')
        f.pull('b')
        f.cross("5to8")
        f.cross("5to6")
        f.cross("3to6")
        f.pull('e')
        f.pull('f')
        f.cross("2to3")
        f.cross("1to2")
        f.pull('c')
        f.cross("1to2")
        f.cross("2to3")
        f.pull('e')
        f.cross("2to3")
        f.cross("2to5")
        f.cross("5to8")
        f.cross("8to9")
        f.walkTo(f.oilCan)
        assertEquals(f.gates.keys, f.crossed)

        f.cross("8to9")
        f.walkTo(Start)
        assertEquals(Start, f.position)
    }

    private class Fixture {
        private val collision = CollisionFlagMap()
        private val steps = StepValidator(collision)
        private val spawns = mapLocs.spawns.map(::MapLocDefinition).filter { it.level == 0 }
        private var down = LeverState(false, false, false, false, false, false)
        val gates = GateRules.keys.associateWith { loc("loc.$it") }
        val crossed = mutableSetOf<String>()
        var position = Start
            private set
        val oilCan: CoordGrid =
            mapObjs.packedSpawns.map(::MapObjDefinition)
                .single { it.id == "obj.oil_can".asRSCM() }
                .let { Square.toCoords(it.level).translate(it.localX, it.localZ) }

        init {
            val base = Square.toCoords(0)
            for (x in base.x until base.x + 64 step 8) {
                for (z in base.z until base.z + 64 step 8) {
                    collision.allocateIfAbsent(x, z, 0)
                }
            }
            GameMapDecoder.putMaps(collision, Square, mapTiles)
            GameMapDecoder.putLocs(GameMapBuilder(), collision, Square, mapTiles, mapLocs)
        }

        fun loc(name: String): BoundLocInfo {
            val spawn = spawns.single { it.id == name.asRSCM() }
            val type = checkNotNull(ServerCacheManager.getObject(spawn.id))
            return BoundLocInfo(
                coords = Square.toCoords(spawn.level).translate(spawn.localX, spawn.localZ),
                entity = LocEntity(spawn.id, spawn.shape, spawn.angle),
                layer = LocLayerConstants.of(spawn.shape),
                width = type.width,
                length = type.length,
                forceApproachFlags = type.forceApproachFlags,
            )
        }

        fun pull(letter: Char) {
            walkTo(loc("loc.lever$letter").coords)
            down = when (letter) {
                'a' -> down.copy(a = !down.a)
                'b' -> down.copy(b = !down.b)
                'c' -> down.copy(c = !down.c)
                'd' -> down.copy(d = !down.d)
                'e' -> down.copy(e = !down.e)
                'f' -> down.copy(f = !down.f)
                else -> error("Unknown lever $letter")
            }
        }

        fun cross(name: String) {
            val gate = gates.getValue(name)
            val state = if (GateRules.getValue(name)(down)) 1 else 0
            val type = checkNotNull(ServerCacheManager.getObject(gate.id))
            assertEquals("loc.ernest_doorajar".asRSCM(), type.multiLoc[state], "$name is locked")
            val approach = listOf(gate.acrossTile(), gate.behindTile()).firstOrNull(::canWalkTo)
            checkNotNull(approach) { "Cannot reach $name from $position with $down" }
            position = crossingRoute(approach, gate).last()
            crossed += name
        }

        fun walkTo(destination: CoordGrid) {
            assertTrue(canWalkTo(destination), "Cannot walk from $position to $destination with $down")
            position = destination
        }

        fun canWalkTo(destination: CoordGrid): Boolean {
            val visited = mutableSetOf(position)
            val pending = ArrayDeque<CoordGrid>()
            pending.add(position)
            while (pending.isNotEmpty()) {
                val tile = pending.removeFirst()
                if (tile == destination) return true
                for ((dx, dz) in Offsets) {
                    val next = tile.translate(dx, dz)
                    if (MapSquareKey.from(next) != Square || next in visited) continue
                    if (!steps.canTravel(tile.level, tile.x, tile.z, dx, dz)) continue
                    visited += next
                    pending.add(next)
                }
            }
            return false
        }
    }

    companion object {
        private val Start = CoordGrid(3117, 9753)
        private val Square = MapSquareKey.from(Start)
        private val Offsets = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        private val GateRules: Map<String, (LeverState) -> Boolean> =
            mapOf(
                "1to2" to Doors::oneToTwo,
                "2to3" to Doors::twoToThree,
                "2to5" to Doors::twoToFive,
                "3to6" to Doors::threeToSix,
                "4to5" to Doors::fourToFive,
                "4to7" to Doors::fourToSeven,
                "5to6" to Doors::fiveToSix,
                "5to8" to Doors::fiveToEight,
                "8to9" to Doors::eightToNine,
            )
        private lateinit var mapTiles: MapTileSimpleDefinition
        private lateinit var mapLocs: MapLocListDefinition
        private lateinit var mapObjs: MapObjListDefinition

        @JvmStatic
        @BeforeAll
        fun cache() {
            val cache = ServerCacheManager.init(240)
            try {
                mapTiles = MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, Square.id, 0))))
                mapLocs = MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, Square.id, 1))))
                mapObjs = MapObjListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, Square.id, 6))))
            } finally {
                cache.close()
            }
        }
    }
}
