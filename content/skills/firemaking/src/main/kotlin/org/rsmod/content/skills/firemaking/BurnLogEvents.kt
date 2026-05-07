package org.rsmod.content.skills.firemaking

import dev.openrune.map.MapSingletons.collision
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpObj4
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.FiremakingColoredLogsRow
import org.rsmod.api.table.FiremakingLogsRow
import org.rsmod.game.MapClock
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.firstStepDestination
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import skillSuccess


public class BurnLogEvents @Inject constructor(
    private val objRepo: ObjRegistry,
    private val objRegistry: ObjRegistry,
    private val locRepo: LocRepository,
    private val worldClock: MapClock,
    private val xpMods: XpModifiers,
) : PluginScript() {

    private val walkDirections = listOf(
        Direction.West,
        Direction.East,
        Direction.South,
        Direction.North,
    )

    private val coloredLogs = FiremakingColoredLogsRow.all().map { it.logItem.internalName }.toSet()

    private val coloredFire: Map<String, String> = FiremakingColoredLogsRow.all()
        .associate { it.logItem.internalName to it.fireObject.internalName }

    override fun ScriptContext.startup() {
        FiremakingLogsRow.all().forEach { log ->
            onOpHeldU("obj.tinderbox", log.item) { startBurn(log, method = BurnMethod.Tinderbox) }
            onOpHeldU(log.item) {
                val barbarianAnim = it.second.paramOrNull(params.barbarian_firemaking_anim)
                    ?: return@onOpHeldU
                val animName = RSCM.getReverseMapping(RSCMType.SEQ, barbarianAnim.id)
                if (animName.isBlank()) {
                    return@onOpHeldU
                }
                startBurn(log, method = BurnMethod.Bow, burnAnim = animName)
            }
            onOpObj4(log.item) { startBurn(log, it.obj, BurnMethod.Tinderbox) }
        }

        onPlayerQueueWithArgs("queue.firemaking_light") { processBurnTick(it.args) }
    }

    private fun ProtectedAccess.startBurn(
        log: FiremakingLogsRow,
        groundObj: Obj? = null,
        method: BurnMethod,
        burnAnim: String = "seq.human_createfire",
    ) {
        if (!canBurn(log, groundObj, method)) {
            resetAnim()
            return
        }

        var obj = groundObj

        if (obj == null) {
            invDel(player.inv, log.item.internalName)
            obj = Obj.fromServer(worldClock, coords, log.item.internalName, 1)
            objRegistry.add(obj)
        }

        stopAction()
        anim(burnAnim)
        player.mes(
            when (method) {
                BurnMethod.Tinderbox -> "You attempt to light the logs."
                BurnMethod.Bow -> "You attempt to light the logs with your bow."
            },
        )

        weakQueue("queue.firemaking_light", 4, BurnTask(log, obj, method))
    }

    private fun ProtectedAccess.canBurn(
        log: FiremakingLogsRow,
        obj: Obj?,
        method: BurnMethod,
    ): Boolean {
        if (obj != null && !objRepo.isValid(player, obj)) {
            return false
        }

        if (method == BurnMethod.Tinderbox && !inv.contains("obj.tinderbox")) {
            player.mes("You do not have any fire source to light this.")
            return false
        }

        val reqLevel = when (method) {
            BurnMethod.Tinderbox -> log.level
            BurnMethod.Bow -> (log.level + 20).coerceAtMost(99)
        }
        if (player.firemakingLvl < reqLevel) {
            player.mes("You need a Firemaking level of $reqLevel to burn ${log.item.name} logs this way.")
            return false
        }

        val fireTile = obj?.coords ?: coords
        if (tileHasCentrepieceLoc(fireTile)) {
            player.mes("You can't light a fire here.")
            return false
        }

        return true
    }

    private fun tileHasCentrepieceLoc(tile: CoordGrid): Boolean =
        locRepo.findExact(tile, LocShape.CentrepieceStraight) != null ||
            locRepo.findExact(tile, LocShape.CentrepieceDiagonal) != null

    private fun ProtectedAccess.processBurnTick(task: BurnTask) {
        if (!canBurn(task.log, task.obj, task.method)) {
            resetAnim()
            return
        }

        val success = coloredLogs.contains(task.log.item.internalName) ||
            skillSuccess(64, 512, player.firemakingLvl)

        if (!success) {
            weakQueue("queue.firemaking_light", 4, task)
            return
        }

        completeBurn(task)
    }

    private fun ProtectedAccess.completeBurn(task: BurnTask) {
        objRepo.del(task.obj)

        val fireCoords = task.obj.coords
        val fireId = coloredFire[task.log.item.internalName] ?: "loc.fire"

        locRepo.add(
            fireCoords,
            fireId,
            (100..200).random(),
            LocAngle.West,
            LocShape.CentrepieceStraight,
            onDespawn = {
                val ashes = Obj.fromServer(worldClock, fireCoords, "obj.ashes", 1)
                objRegistry.add(ashes)
            },
        )

        resetAnim()
        val xpModifier = xpMods.get(player, "stat.firemaking")
        val xp = task.log.xp * task.method.xpMultiplier(task.log.item.internalName) * xpModifier
        statAdvance("stat.firemaking", xp)
        println("[DEBUG] Firemaking XP mod: x$xpModifier (awarded $xp xp)")
        mes("The fire catches and the logs begin to burn.")

        moveAwayFromFire(fireCoords)
        faceSquare(fireCoords)
    }

    private fun ProtectedAccess.moveAwayFromFire(coords: CoordGrid) {
        val dest = collision.firstStepDestination(coords, walkDirections) ?: return
        walk(dest)
    }

    data class BurnTask(
        val log: FiremakingLogsRow,
        val obj: Obj,
        val method: BurnMethod,
    )

    enum class BurnMethod {
        Tinderbox,
        Bow,
        ;

        fun xpMultiplier(logInternalName: String): Double {
            if (this != Bow) {
                return 1.0
            }
            return when (logInternalName) {
                "obj.camphor_logs" -> 1.361
                "obj.ironwood_logs" -> 1.451
                "obj.rosewood_logs" -> 1.567
                else -> 1.0
            }
        }
    }
}
