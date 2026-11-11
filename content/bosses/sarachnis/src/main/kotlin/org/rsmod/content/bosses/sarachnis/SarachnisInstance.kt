package org.rsmod.content.bosses.sarachnis

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class SarachnisInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val locRepo: LocRepository,
) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_sarachnis"

    override fun area(): InstanceArea = INSTANCE

    override fun runsPreludeOnFreshRun(): Boolean = true

    override fun destroyWhenEmpty(): Boolean = true

    override fun recordsKillTime(player: Player): Boolean = false

    override fun ScriptContext.configure() {
        onEnterObject { enterCrypt() }
        onOpLoc2(ENTER_LOC) { quickEnter() }
        onOpLoc3(ENTER_LOC) { privateEntry() }

        onExitObject { exitObject() }
        onOpLoc2(EXIT_LOC) { exitObject(it.loc) }

        onOpLoc1(INSTANCE_ENTER_LOC) { enterInstanceCrypt(it.loc) }
        onOpLoc2(INSTANCE_ENTER_LOC) { enterInstanceCrypt(it.loc) }
        onOpLoc3(INSTANCE_ENTER_LOC) { leaveInstance() }

        onEnterPrelude { result, enter ->
            val session =
                when (result) {
                    is InstanceManager.Result.Created -> result.session
                    is InstanceManager.Result.Joined -> result.session
                    else -> return@onEnterPrelude
                }
            val fresh = result.isFreshRun()
            if (fresh) {
                spawnInstanceWeb(session)
            }
            withInstanceEnterTransition(InstanceEnterTransition()) { enter() }
            if (fresh) {
                spawnSarachnis(player, session)
            }
        }
    }

    private suspend fun ProtectedAccess.enterCrypt() {
        if (manager.sessionForPlayer(player) != null) {
            leaveInstance()
            return
        }
        if (isInsidePublicLair()) {
            leavePublicLair()
            return
        }
        val web = findClosestWeb(null)
        if (web == null) {
            arriveDelay()
            mes(ENTER_MESSAGE)
            thickWebDoorAnimation()
            telejump(PUBLIC_ENTER, TeleportType.Exempt)
            return
        }
        if (!crossWeb(web, PUBLIC_ENTER) { mes(ENTER_MESSAGE) }) return
    }

    private fun ProtectedAccess.findClosestWeb(session: InstanceSession?): BoundLocInfo? {
        val webs = WEB_TILES.mapNotNull { findWeb(session, it) }
        if (webs.isEmpty()) return null
        val vertical = webs.first().angle == LocAngle.North || webs.first().angle == LocAngle.South
        val aligned =
            webs.filter { if (vertical) it.coords.x == coords.x else it.coords.z == coords.z }
        return (aligned.ifEmpty { webs }).minByOrNull { it.coords.chebyshevDistance(coords) }
    }

    private fun findWeb(session: InstanceSession?, tile: CoordGrid): BoundLocInfo? {
        val coords = session?.let { manager.resolveCoord(it, tile) } ?: tile
        val loc = locRepo.findExact(coords, LocShape.WallStraight) ?: return null
        val type = ServerCacheManager.getObject(loc.id) ?: return null
        return BoundLocInfo(loc, type)
    }

    private suspend fun ProtectedAccess.crossWeb(
        web: BoundLocInfo,
        destination: CoordGrid,
        mount: Boolean = true,
        onStart: () -> Unit = {},
    ): Boolean {
        arriveDelay()
        if (mount && !mountWeb(web)) return false
        onStart()
        val direction = directionTo(web, destination) ?: return false
        faceDirection(direction)
        thickWebDoorAnimation()
        val step = coords.translate(direction)
        if (step != coords) {
            forcedWalk(listOf(step), crossTiles = 1)
        }
        if (coords != destination) {
            val vertical = web.angle == LocAngle.North || web.angle == LocAngle.South
            val rest = cardinalRoute(coords, destination, vertical)
            if (rest.isNotEmpty()) forcedWalk(rest, crossTiles = rest.size)
        }
        return coords == destination
    }

    private suspend fun ProtectedAccess.mountWeb(web: BoundLocInfo): Boolean {
        if (coords == web.coords) return true
        val vertical = web.angle == LocAngle.North || web.angle == LocAngle.South
        val route = cardinalRoute(coords, web.coords, vertical)
        if (route.isEmpty()) return false
        forcedWalk(route, crossTiles = route.size)
        return coords == web.coords
    }

    private fun directionTo(web: BoundLocInfo, destination: CoordGrid): Direction? {
        val dx = destination.x - web.coords.x
        val dz = destination.z - web.coords.z
        val vertical = web.angle == LocAngle.North || web.angle == LocAngle.South
        return if (vertical) {
            when {
                dz > 0 -> Direction.North
                dz < 0 -> Direction.South
                dx > 0 -> Direction.East
                dx < 0 -> Direction.West
                else -> null
            }
        } else {
            when {
                dx > 0 -> Direction.East
                dx < 0 -> Direction.West
                dz > 0 -> Direction.North
                dz < 0 -> Direction.South
                else -> null
            }
        }
    }

    private fun cardinalRoute(from: CoordGrid, to: CoordGrid, vertical: Boolean): List<CoordGrid> {
        var cursor = from
        val route = mutableListOf<CoordGrid>()

        fun step(dx: Int, dz: Int) {
            cursor = cursor.translate(dx, dz)
            route += cursor
        }

        if (vertical) {
            while (cursor.x != to.x) step(if (to.x > cursor.x) 1 else -1, 0)
            while (cursor.z != to.z) step(0, if (to.z > cursor.z) 1 else -1)
        } else {
            while (cursor.z != to.z) step(0, if (to.z > cursor.z) 1 else -1)
            while (cursor.x != to.x) step(if (to.x > cursor.x) 1 else -1, 0)
        }
        return route
    }

    private suspend fun ProtectedAccess.quickEnter() {
        if (manager.sessionForPlayer(player) != null) {
            leaveInstance()
            return
        }
        if (isInsidePublicLair()) {
            leavePublicLair()
            return
        }
        val session = player.uuid?.let { manager.sessionOwnedBy(key, it) }
        if (session == null) {
            enterCrypt()
            return
        }
        completeInstanceEntry(
            manager.join(player, session, worldClock.cycle, code = null, forceAccess = true)
        )
    }

    private suspend fun ProtectedAccess.privateEntry() {
        if (isInsidePublicLair()) {
            leavePublicLair()
            return
        }
        defaultInstanceEntry()
    }

    private suspend fun ProtectedAccess.enterInstanceCrypt(web: BoundLocInfo) {
        val session = manager.sessionForPlayer(player) ?: return
        if (isInsideInstanceArena(session)) {
            exitInstanceCrypt(session)
            return
        }
        val destination = manager.resolveCoord(session, ARENA_ENTER) ?: return
        if (!crossWeb(web, destination) { mes(ENTER_MESSAGE) }) return
        swapInstanceWeb(session, EXIT_LOC)
    }

    private suspend fun ProtectedAccess.exitInstanceCrypt(
        session: InstanceSession,
        clickedWeb: BoundLocInfo? = null,
    ) {
        val destination = manager.resolveCoord(session, LOBBY) ?: return
        val web = clickedWeb ?: findClosestWeb(session)
        if (web != null) {
            crossWeb(web, destination, mount = false)
        } else {
            arriveDelay()
            thickWebDoorAnimation()
            telejump(destination, TeleportType.Exempt)
        }
        swapInstanceWeb(session, INSTANCE_ENTER_LOC)
    }

    private suspend fun ProtectedAccess.exitObject(clickedWeb: BoundLocInfo? = null) {
        val session = manager.sessionForPlayer(player)
        if (session != null) {
            exitInstanceCrypt(session, clickedWeb)
            return
        }
        leavePublicLair()
    }

    private suspend fun ProtectedAccess.leaveInstance() {
        if (manager.sessionForPlayer(player) == null) {
            return
        }
        defaultLeaveFlow()
    }

    private suspend fun ProtectedAccess.leavePublicLair() {
        val web = findClosestWeb(null)
        if (web != null) {
            crossWeb(web, PUBLIC_EXIT, mount = false)
        } else {
            arriveDelay()
            thickWebDoorAnimation()
            telejump(PUBLIC_EXIT, TeleportType.Exempt)
        }
    }

    private suspend fun ProtectedAccess.thickWebDoorAnimation() {
        anim(CHOP_SEQ)
        delay(2)
    }

    private fun ProtectedAccess.isInsidePublicLair(): Boolean {
        if (coords.level != LAIR_SW.level) return false
        return isWithinArea(LAIR_SW, LAIR_NE)
    }

    private fun ProtectedAccess.isInsideInstanceArena(session: InstanceSession): Boolean {
        val spawn = manager.resolveCoord(session, SPAWN) ?: return false
        if (coords.level != spawn.level) return false
        return isWithinArea(
            spawn.translate(ARENA_MIN_OFFSET, ARENA_MIN_OFFSET),
            spawn.translate(ARENA_MAX_OFFSET, ARENA_MAX_OFFSET),
        )
    }

    private fun spawnInstanceWeb(session: InstanceSession) {
        for (tile in WEB_TILES) {
            val coords = manager.resolveCoord(session, tile) ?: continue
            locRepo.add(coords, INSTANCE_ENTER_LOC, Int.MAX_VALUE, LocAngle.South, LocShape.WallStraight)
        }
    }

    private fun swapInstanceWeb(session: InstanceSession, loc: String) {
        val type = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)) ?: return
        for (tile in WEB_TILES) {
            val coords = manager.resolveCoord(session, tile) ?: continue
            val current = locRepo.findExact(coords, LocShape.WallStraight) ?: continue
            locRepo.change(current, type, Int.MAX_VALUE)
        }
    }

    private fun spawnSarachnis(player: Player, session: InstanceSession) {
        val coords = manager.resolveCoord(session, SPAWN) ?: return
        val type = ServerCacheManager.getNpc(SARACHNIS_ID) ?: return
        val npc = Npc(type, coords)
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        manager.registerSessionNpc(player, npc)
        npc.apPlayer2(player, aiPlayerInteractions)
    }

    private companion object {
        private val SARACHNIS_ID = "npc.sarachnis".asRSCM(RSCMType.NPC)

        private const val ENTER_LOC = "loc.hosdun_sarachnis_entrance"
        private const val EXIT_LOC = "loc.hosdun_sarachnis_exit"
        private const val INSTANCE_ENTER_LOC = "loc.hosdun_sarachnis_entrance_instance"
        private const val CHOP_SEQ = "seq.human_machette_chop"
        private const val ENTER_MESSAGE = "You enter deeper into the ancient burial crypt."

        private const val ARENA_MIN_OFFSET = -10
        private const val ARENA_MAX_OFFSET = 11

        private val SPAWN = CoordGrid(1840, 9900)
        private val LAIR_SW = SPAWN.translate(ARENA_MIN_OFFSET, ARENA_MIN_OFFSET)
        private val LAIR_NE = SPAWN.translate(ARENA_MAX_OFFSET, ARENA_MAX_OFFSET)
        private val ARENA_ENTER = CoordGrid(1842, 9911)
        private val LOBBY = CoordGrid(1842, 9913)
        private val PUBLIC_ENTER = CoordGrid(1842, 9911)
        private val PUBLIC_EXIT = CoordGrid(1842, 9913)
        private val WEB_TILES = listOf(CoordGrid(1841, 9912), CoordGrid(1842, 9912))

        private val INSTANCE = InstanceArea.copyRegions(centerRegionId = 7322)
    }
}
