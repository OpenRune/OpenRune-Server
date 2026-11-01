package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionStaticTemplate
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.table.SleepingGiantsRoomLocsRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

internal const val DROP_TRIGGER = "droptrigger.sleeping_giants_room"

internal const val ROOM_KEY = "sleeping_giants"

/** The cache places none of the `_quest` locs on any map, so the room is built per player. */
@Singleton
class SleepingGiantsRoom
@Inject
constructor(
    private val manager: InstanceManager,
    private val clock: MapClock,
    private val regions: RegionRegistry,
    private val regionRepo: RegionRepository,
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
) {
    private val rooms = HashMap<InstanceId, Room>()

    private class Room(val region: Region) {
        var kovac: Npc? = null
    }

    private fun sessionOf(player: Player): InstanceSession? =
        manager.sessionForPlayer(player)?.takeIf { it.key == ROOM_KEY }

    private fun roomOf(player: Player): Room? = sessionOf(player)?.let { rooms[it.id] }

    fun kovacOf(player: Player): Npc? = roomOf(player)?.kovac

    fun isInside(player: Player): Boolean = sessionOf(player) != null

    suspend fun enter(access: ProtectedAccess, intro: Boolean, onArrival: () -> Unit): Boolean {
        val player = access.player
        val result = manager.create(player, ROOM_KEY, SPEC, InstanceAccess.Private, clock.cycle)
        val created =
            when (result) {
                is InstanceManager.Result.Created -> result
                is InstanceManager.Result.Failed -> {
                    access.mes(result.reason)
                    return false
                }
                is InstanceManager.Result.Joined -> return false
            }
        val region = checkNotNull(regions[created.enter]) { "No region at ${created.enter}." }
        val room = Room(region)
        rooms[created.session.id] = room
        regionRepo.protect(region)
        try {
            if (intro) {
                playIntro(access, created, room, onArrival)
            } else {
                arrive(access, created, room, onArrival)
            }
        } finally {
            regionRepo.unprotect(region)
            // Destroys the room if the player never got into it.
            manager.cancelPendingEntry(player, clock.cycle)
        }
        return true
    }

    private fun ProtectedAccess.moveIn(created: InstanceManager.Result.Created) {
        telejump(created.enter)
        manager.finalizeEntry(player, created.session, clock.cycle)
        armDropWarning(player)
    }

    private suspend fun arrive(
        access: ProtectedAccess,
        created: InstanceManager.Result.Created,
        room: Room,
        onArrival: () -> Unit,
    ) {
        val region = room.region
        for (swap in SleepingGiantsRoomLocsRow.all()) {
            swapLoc(region, swap.coords, swap.real, swap.quest)
        }
        addQuestLocs(region)
        room.kovac = spawnQuestKovac(region)
        access.fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        access.delay(2)
        onArrival()
        access.moveIn(created)
        access.delay(1)
        access.fadeIn()
    }

    private suspend fun playIntro(
        access: ProtectedAccess,
        created: InstanceManager.Result.Created,
        room: Room,
        onArrival: () -> Unit,
    ) {
        val region = room.region
        try {
            access.mesboxNp(" ")
            access.fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
            access.clearHealthHud()
            access.delay(2)
            onArrival()
            access.camModeFixed()
            access.hideTopLevel()
            access.hideEntityOps()
            access.minimapHideMap()
            access.hideHealthHud()
            access.closeTopLevelTabsLenient()
            access.moveIn(created)
            access.delay(1)
            room.kovac = spawnKovac(region, KOVAC_CUTSCENE, KOVAC_TILE, Direction.South)
            for (swap in SleepingGiantsRoomLocsRow.all()) {
                swapLoc(region, swap.coords, swap.real, swap.cutscene)
            }
            swapLoc(region, EXIT_TILE, EXIT, EXIT_CUTSCENE)
            for (tile in POWER_UNIT_TILES) {
                addLoc(region, tile, POWER_UNIT_QUEST)
            }
            access.camera(region, INTRO_SHOTS[0])
            access.delay(1)
            access.camera(region, INTRO_SHOTS[1])
            access.fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
            access.delay(1)
            access.walk(region.normal[INTRO_PLAYER_TILE])
            access.delay(4)
            access.camera(region, INTRO_SHOTS[2])
            access.delay(8)
            access.anim(THINK_SEQ)
            access.camera(region, INTRO_SHOTS[3])
            access.delay(8)
            access.anim(THINK_SEQ)
            access.camera(region, INTRO_SHOTS[4])
            access.delay(8)
        } catch (t: Throwable) {
            endIntroNow(access)
            throw t
        }
    }

    suspend fun introWalkBack(access: ProtectedAccess) {
        val region = roomOf(access.player)?.region ?: return
        access.walk(region.normal[INTRO_RETURN_TILE])
        access.camera(region, INTRO_SHOTS[5])
        access.mesboxNp(" ")
        access.delay(2)
    }

    fun kovacLooksAtTools(player: Player) {
        val room = roomOf(player) ?: return
        room.kovac?.faceSquare(room.region.normal[INTRO_TOOLS_TILE])
    }

    fun kovacToTripHammer(player: Player) {
        val room = roomOf(player) ?: return
        val kovac = room.kovac ?: return
        val region = room.region
        kovac.walk(KOVAC_TRIP_HAMMER_PATH.map { region.normal[it] }) {
            kovac.faceSquare(region.normal[TRIP_HAMMER_FACE_TILE])
        }
    }

    suspend fun endIntro(access: ProtectedAccess) {
        val room = roomOf(access.player) ?: return
        try {
            access.mesboxNp(" ")
            access.fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
            access.delay(2)
            access.restoreSceneState()
            swapToQuest(room)
            access.camReset()
            access.openTopLevelTabs()
            access.camForceAngle(INTRO_END_CAM_ANGLE, 0)
            access.delay(1)
            access.fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
            access.ifCloseSub(MESSAGEBOX)
            access.delay(2)
            access.closeFadeOverlayNow()
            room.kovac?.facePlayer(access.player)
        } catch (t: Throwable) {
            endIntroNow(access)
            throw t
        }
    }

    fun endIntroNow(access: ProtectedAccess) {
        roomOf(access.player)?.let { swapToQuest(it) }
        access.restoreSceneState()
        access.camReset()
        access.openTopLevelTabs()
        access.closeFadeOverlayNow()
    }

    private fun ProtectedAccess.restoreSceneState() {
        showEntityOps()
        minimapReset()
        camModeReset()
        showTopLevel()
        showHealthHud()
    }

    private fun swapToQuest(room: Room) {
        val current = room.kovac
        if (current != null && current.type.id == KOVAC_QUEST.asRSCM(RSCMType.NPC)) {
            return
        }
        for (swap in SleepingGiantsRoomLocsRow.all()) {
            swapLoc(room.region, swap.coords, swap.cutscene, swap.quest)
        }
        swapLoc(room.region, EXIT_TILE, EXIT_CUTSCENE, EXIT)
        for ((tile, loc) in QUEST_ONLY_LOCS) {
            addLoc(room.region, tile, loc)
        }
        current?.let { npcRepo.del(it, Int.MAX_VALUE) }
        room.kovac = spawnQuestKovac(room.region)
    }

    private fun addQuestLocs(region: Region) {
        for (tile in POWER_UNIT_TILES) {
            addLoc(region, tile, POWER_UNIT_QUEST)
        }
        for ((tile, loc) in QUEST_ONLY_LOCS) {
            addLoc(region, tile, loc)
        }
    }

    private fun spawnQuestKovac(region: Region): Npc {
        val kovac = spawnKovac(region, KOVAC_QUEST, KOVAC_TILE, Direction.North)
        kovac.faceSquare(region.normal[KOVAC_FACE_TILE])
        return kovac
    }

    private fun spawnKovac(region: Region, type: String, tile: CoordGrid, dir: Direction): Npc {
        val kovac = Npc(type, region.normal[tile])
        kovac.respawnDir = dir
        npcRepo.add(kovac, Int.MAX_VALUE)
        kovac.noneMode()
        return kovac
    }

    suspend fun moveToFoundry(access: ProtectedAccess) {
        val session = sessionOf(access.player) ?: return
        val room = rooms[session.id] ?: return
        access.mesboxNp(" ")
        access.fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        access.clearHealthHud()
        access.delay(1)
        access.minimapHideMap()
        access.delay(1)
        val target = room.realCoords(access.player.coords)
        manager.leave(access.player, session, clock.cycle)
        access.telejump(target)
        access.delay(1)
        access.minimapReset()
        access.fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        access.ifCloseSub(MESSAGEBOX)
        access.delay(2)
        access.closeFadeOverlayNow()
    }

    fun leave(player: Player) {
        val session = sessionOf(player) ?: return
        manager.leave(player, session, clock.cycle)
    }

    fun onLeft(player: Player) {
        if (player.dropTrigger == DROP_TRIGGER) {
            player.clearDropTrigger(DROP_TRIGGER)
        }
    }

    fun onEnded(instance: InstanceId) {
        val room = rooms.remove(instance) ?: return
        room.kovac?.let { npcRepo.del(it, Int.MAX_VALUE) }
        regionRepo.unprotect(room.region)
    }

    /*
     * Every drop is blocked by re-arming the trigger; the quest's drop prompt performs the drop
     * once confirmed, and the trigger re-arms for the next item.
     */

    fun armDropWarning(player: Player) {
        if (player.dropTrigger == null) {
            player.dropTrigger(DROP_TRIGGER)
        }
    }

    fun onDropped(player: Player) {
        if (isInside(player)) {
            armDropWarning(player)
        }
    }

    private fun Room.realCoords(coords: CoordGrid): CoordGrid {
        val origin = region.normal[COPY_ORIGIN]
        return COPY_ORIGIN.translate(coords.x - origin.x, coords.z - origin.z, coords.level)
    }

    private fun swapLoc(region: Region, tile: CoordGrid, from: String, into: String) {
        val coords = region.normal[tile]
        val fromId = from.asRSCM(RSCMType.LOC)
        val loc = locRepo.findAll(coords).firstOrNull { it.id == fromId } ?: return
        locRepo.add(coords, into, Int.MAX_VALUE, loc.angle, loc.shape)
    }

    private fun swapLoc(
        region: Region,
        tile: CoordGrid,
        from: ObjectServerType,
        into: ObjectServerType,
    ) {
        swapLoc(region, tile, from.internalName, into.internalName)
    }

    private fun addLoc(region: Region, tile: CoordGrid, loc: String) {
        locRepo.add(region.normal[tile], loc, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
    }

    private suspend fun ProtectedAccess.fadeIn() {
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        delay(FADE_TICKS)
        closeFadeOverlayNow()
    }

    private fun ProtectedAccess.camera(region: Region, shot: Shot) {
        camMoveToV3(region.normal[shot.eye], height = shot.eyeHeight, rate = shot.eyeRate, rate2 = shot.eyeRate)
        camLookAtV3(region.normal[shot.lookAt], height = shot.lookAtHeight, rate = shot.lookAtRate, rate2 = shot.lookAtRate)
    }

    private class Shot(
        val eye: CoordGrid,
        val eyeHeight: Int,
        val eyeRate: Int,
        val lookAt: CoordGrid,
        val lookAtHeight: Int,
        val lookAtRate: Int,
    )

    private companion object {
        const val KOVAC_QUEST = "npc.giants_foundry_kovac_quest"
        val KOVAC_TILE = CoordGrid(3368, 11483, 0)
        val KOVAC_FACE_TILE = CoordGrid(3369, 11487, 0)
        val KOVAC_TRIP_HAMMER_PATH = listOf(CoordGrid(3366, 11492, 0), CoordGrid(3365, 11493, 0))
        val TRIP_HAMMER_FACE_TILE = CoordGrid(3365, 11497, 0)

        // UNVERIFIED: whether logging out in the room always lands here or on the tile the
        // player entered from (one observation)
        val LOGIN_TILE = CoordGrid(3363, 3148, 0)
        val COPY_ORIGIN = CoordGrid(3328, 11456, 0)
        const val REGION_ZONE_OFFSET = 4
        const val ZONE_LENGTH = 8
        const val SQUARE_ZONES = 8

        val TEMPLATE: RegionStaticTemplate =
            RegionTemplate.create {
                copyAllLevels(COPY_ORIGIN.x / ZONE_LENGTH, COPY_ORIGIN.z / ZONE_LENGTH) {
                    regionZoneX = REGION_ZONE_OFFSET
                    regionZoneZ = REGION_ZONE_OFFSET
                    zoneWidth = SQUARE_ZONES
                    zoneLength = SQUARE_ZONES
                }
            }

        val SPEC =
            InstanceSpec(
                fee = 0,
                maxPlayers = 1,
                reclaimTicks = 0,
                graceTicks = 0,
                destroyWhenEmpty = true,
                area =
                    InstanceArea.template(
                        template = TEMPLATE,
                        enterCoord = FOUNDRY_INSIDE.run { RegionLocal(level, mx, mz, lx, lz) },
                        exitCoord = LOGIN_TILE,
                    ),
                settingsRowId = 0,
            )

        const val KOVAC_CUTSCENE = "npc.giants_foundry_kovac_quest_cutscene"
        const val EXIT = "loc.giants_foundry_exit"
        const val EXIT_CUTSCENE = "loc.giants_foundry_exit_cutscene"
        val EXIT_TILE = CoordGrid(3364, 11482, 0)
        const val MESSAGEBOX = "interface.messagebox"
        const val THINK_SEQ = "seq.emote_think"

        /** Set with the cutscene locs and left in place for the quest. */
        const val POWER_UNIT_QUEST = "loc.giants_foundry_tool_powerunit_quest"
        val POWER_UNIT_TILES = listOf(CoordGrid(3362, 11487, 0), CoordGrid(3361, 11494, 0))

        val QUEST_ONLY_LOCS =
            listOf(
                CoordGrid(3359, 11489, 0) to "loc.giants_foundry_waterfall_quest_invis",
                CoordGrid(3365, 11497, 0) to "loc.giants_foundry_trip_hammer_broken_quest_invis",
            )

        const val FADE_CYCLES = 50
        const val FADE_TICKS = 2
        const val CAM_SNAP = 232
        const val INTRO_END_CAM_ANGLE = 250

        val INTRO_PLAYER_TILE = CoordGrid(3367, 11489, 0)
        val INTRO_RETURN_TILE = CoordGrid(3367, 11485, 0)
        val INTRO_TOOLS_TILE = CoordGrid(3362, 11491, 0)

        val INTRO_SHOTS =
            listOf(
                Shot(CoordGrid(3367, 11487, 0), 500, CAM_SNAP, CoordGrid(3367, 11479, 0), 300, CAM_SNAP),
                Shot(CoordGrid(3367, 11492, 0), 500, 4, CoordGrid(3367, 11484, 0), 280, 3),
                Shot(CoordGrid(3369, 11477, 0), 700, 4, CoordGrid(3364, 11493, 0), 300, 4),
                Shot(CoordGrid(3384, 11490, 0), 700, 4, CoordGrid(3364, 11491, 0), 400, 4),
                Shot(CoordGrid(3380, 11504, 0), 900, 4, CoordGrid(3366, 11489, 0), 400, 2),
                Shot(CoordGrid(3371, 11493, 0), 700, CAM_SNAP, CoordGrid(3368, 11486, 0), 400, CAM_SNAP),
            )
    }
}
