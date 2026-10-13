package org.rsmod.api.game.process.player

import net.rsprot.protocol.api.util.ZonePartialEnclosedCacheBuffer
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZoneFullFollows
import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialEnclosed
import net.rsprot.protocol.game.outgoing.zone.payload.LocAddChangeV2
import net.rsprot.protocol.message.ZoneProt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.game.process.player.PlayerZoneUpdateProcessor.Companion.ZONE_VIEW_RADIUS
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.capture.attachClientCapture
import org.rsmod.api.testing.factory.collisionFactory
import org.rsmod.api.testing.factory.itemType
import org.rsmod.api.testing.factory.locTypeFactory
import org.rsmod.api.testing.factory.playerFactory
import org.rsmod.api.testing.scope.BasicGameTestScope
import org.rsmod.api.utils.zone.SharedZoneEnclosedBuffers
import org.rsmod.events.EventBus
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.obj.Obj
import org.rsmod.game.obj.ObjEntity
import org.rsmod.game.obj.ObjScope
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.game.region.RegionListWorldEntity
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap

// TODO: Refactor this into `runGameTest` which will give us access to the appropriate dependencies.
//  Can then get rid of all the intricate constructors for the required registries and processors.
class PlayerZoneUpdateProcessorTest {
    @Test
    fun GameTestState.`process zones in a new build area`() = runAdvancedGameTest {
        val parameters = createZoneProcess(eventBus)
        val buildProcessor = PlayerBuildAreaProcessor()
        val zoneProcessor = parameters.zoneProcessor
        val locZones = parameters.locZones
        val locRegistry = parameters.normalLocReg
        val objRegistry = parameters.objRegistry
        val standardLoc = locTypeFactory.create().id
        val standardObj = itemType("obj.abyssal_whip").id
        val startCoords = CoordGrid(0, 1, 1, 32, 32)
        withPlayer(startCoords) {
            val captured = attachClientCapture()

            // Set player observer uuid to view `obj`.
            val observer = 1L
            uuid = observer
            observerUUID = observer

            // NOTE: This may not always be the case. Was told that players' personal zone radius
            // can vary. (i.e., enlarges while inside Tombs of Amascut)
            val zoneRadius = ZONE_VIEW_RADIUS
            val zoneRange = -zoneRadius..zoneRadius

            fun process() {
                // Clear zone updates from previous cycle.
                zoneProcessor.clearPendingZoneUpdates()
                // Clear zone prots from previous cycle.
                client.flush()
                captured.clearOutgoing()
                buildProcessor.process(this)
                zoneProcessor.process(this)
            }

            val startZone = ZoneKey.from(startCoords)
            val expectedZoneKeys =
                zoneRange.flatMap { x -> zoneRange.map { z -> startZone.translate(x, z).packed } }

            // Set a static map loc in loc registry; this loc should not be sent as a zone
            // update, or so we will verify.
            val mapLoc =
                LocInfo(
                    layer = 3,
                    coords = startCoords.translate(0, 1),
                    entity = LocEntity(standardLoc, 10, 0),
                )
            mapLoc.let {
                val zoneKey = ZoneKey.from(it.coords)
                val zoneGrid = ZoneGrid.from(it.coords)
                val locZoneKey = LocZoneKey(zoneGrid.x, zoneGrid.z, it.layer)
                locZones.mapLocs[zoneKey, locZoneKey] = mapLoc.entity
            }

            // Spawn a loc in loc registry to be sent as a zone update.
            val loc =
                LocInfo(
                    layer = 0,
                    coords = startCoords.translate(2, 2),
                    entity = LocEntity(standardLoc, 10, 0),
                )
            locRegistry.add(loc)

            // Spawn an obj in obj registry to be sent as a zone update.
            val obj =
                Obj(
                    coords = startCoords.translate(1, 2),
                    entity = ObjEntity(standardObj, 1, ObjScope.Private.id),
                    creationCycle = 0,
                    receiverId = observer,
                )
            objRegistry.add(obj)

            process()

            // `processNewVisibleZones`
            assertEquals(49, captured.countOf<UpdateZoneFullFollows>())

            // `refreshVisibleZoneKeys`
            assertEquals(expectedZoneKeys.toSet(), visibleZoneKeys.toSet())
            assertEquals(startZone, lastProcessedZone)

            // `processVisibleZoneUpdates`
            assertEquals(1, captured.count { it is ZoneProt })
            assertEquals(1, captured.countOf<LocAddChangeV2>())
            assertEquals(standardLoc, captured.singleMapOf(LocAddChangeV2::id))
            assertEquals(1, captured.countOf<UpdateZonePartialEnclosed>())

            process()

            // Should no longer send update follows.
            assertEquals(0, captured.countOf<UpdateZoneFullFollows>())

            // Visible zones should have stayed the same.
            assertEquals(expectedZoneKeys.toSet(), visibleZoneKeys.toSet())

            // No zone update should have been sent.
            assertEquals(0, captured.count())
        }
    }

    @Test
    fun GameTestState.`only send private obj to receiver`() = runAdvancedGameTest {
        val parameters = createZoneProcess(eventBus)
        val buildProcessor = PlayerBuildAreaProcessor()
        val zoneProcessor = parameters.zoneProcessor
        val zoneUpdateMap = parameters.zoneUpdateMap
        val objRegistry = parameters.objRegistry
        val stackableObj = itemType("obj.rune_arrow").id
        val startCoords = CoordGrid(0, 1, 1, 32, 32)

        val player1 =
            playerFactory.create(startCoords) {
                uuid = 1
                observerUUID = 1
            }

        val player2 =
            playerFactory.create(startCoords) {
                uuid = 2
                observerUUID = 2
            }

        val client1 = player1.attachClientCapture()
        val client2 = player2.attachClientCapture()

        fun process() {
            buildProcessor.process(player1)
            buildProcessor.process(player2)
            zoneProcessor.process(player1)
            zoneProcessor.process(player2)
        }

        fun endCycle() {
            zoneProcessor.clearPendingZoneUpdates()
            client1.clearOutgoing()
            client2.clearOutgoing()
        }

        // Skip the initial set-up phase for new build area.
        process()
        endCycle()

        // Add a new obj to be sent as an `ObjAdd` update.
        val obj =
            Obj(
                coords = startCoords.translate(1, 0),
                entity = ObjEntity(stackableObj, 1, ObjScope.Private.id),
                creationCycle = 0,
                receiverId = checkNotNull(player2.observerUUID),
            )
        objRegistry.add(obj)

        process()
        check(zoneUpdateMap.updatedZones.size == 1)

        // Obj should be sent to `client2`.
        assertEquals(1, client2.countOf<UpdateZonePartialEnclosed>())

        // `client1` should be oblivious to the Obj.
        assertEquals(0, client1.count { it is ZoneProt })
        assertEquals(0, client1.count())

        endCycle()

        // Add the same type of obj on top of the existing stack, this should trigger an
        // `ObjCount`.
        val countObj =
            Obj(
                coords = startCoords.translate(1, 0),
                entity = ObjEntity(stackableObj, 4, ObjScope.Private.id),
                creationCycle = 0,
                receiverId = checkNotNull(player2.observerUUID),
            )
        objRegistry.add(countObj)

        process()

        // Obj count should be updated for `client2`.
        assertEquals(1, client2.countOf<UpdateZonePartialEnclosed>())

        // `client1` should be oblivious to the Obj.
        assertEquals(0, client1.count { it is ZoneProt })
        assertEquals(0, client1.count())

        endCycle()

        // Finally, delete the obj.
        val deleteObj = objRegistry.findAll(obj.coords).single()
        objRegistry.del(deleteObj)

        process()

        // Obj should be deleted for `client2`.
        assertEquals(1, client2.countOf<UpdateZonePartialEnclosed>())

        // `client1` should be oblivious to the Obj.
        assertEquals(0, client1.count { it is ZoneProt })
        assertEquals(0, client1.count())

        endCycle()
    }

    private fun BasicGameTestScope.createZoneProcess(eventBus: EventBus): ZoneProcessParams {
        val zoneUpdates = ZoneUpdateMap()
        val locZones = LocZoneStorage()
        val collision = collisionFactory.borrowSharedMap()
        val objRegistry = ObjRegistry(zoneUpdates)
        val normalLocReg = LocRegistryNormal(zoneUpdates, collision, locZones)
        val regionRegistry =
            RegionRegistry(
                RegionListSmall(),
                RegionListLarge(),
                RegionListWorldEntity(),
                normalLocReg,
                collision,
                locZones,
                NpcRegistry(npcList, collision, eventBus),
                ControllerRegistry(mapClock, ControllerList()),
                ZonePlayerActivityBitSet(),
            )
        val regionLocReg = LocRegistryRegion(zoneUpdates, collision, locZones, regionRegistry)
        val enclosedCache = ZonePartialEnclosedCacheBuffer()
        val sharedEnclosed = SharedZoneEnclosedBuffers(zoneUpdates, enclosedCache)
        val locRegistry = LocRegistry(locZones, normalLocReg, regionLocReg)
        val processor =
            PlayerZoneUpdateProcessor(zoneUpdates, locRegistry, objRegistry, sharedEnclosed)
        return ZoneProcessParams(
            collision,
            locZones,
            normalLocReg,
            objRegistry,
            zoneUpdates,
            processor,
        )
    }

    private data class ZoneProcessParams(
        val collision: CollisionFlagMap,
        val locZones: LocZoneStorage,
        val normalLocReg: LocRegistryNormal,
        val objRegistry: ObjRegistry,
        val zoneUpdateMap: ZoneUpdateMap,
        val zoneProcessor: PlayerZoneUpdateProcessor,
    )
}
