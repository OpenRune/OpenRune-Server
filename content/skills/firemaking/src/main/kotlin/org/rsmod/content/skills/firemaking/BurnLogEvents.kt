package org.rsmod.content.skills.firemaking

import dev.openrune.map.MapSingletons.collision
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.events.skilling.LogBurnedEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpObj4
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.FiremakingColoredLogsRow
import org.rsmod.api.table.FiremakingLogsRow
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj
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
            onOpHeldU("obj.tinderbox", log.input) { startBurn(log, method = BurnMethod.Tinderbox) }
            onOpHeldU(log.input) {
                val barbarianAnim = it.second.paramOrNull(params.barbarian_firemaking_anim)
                    ?: return@onOpHeldU
                val animName = RSCM.getReverseMapping(RSCMType.SEQ, barbarianAnim.id)
                if (animName.isBlank()) {
                    return@onOpHeldU
                }
                startBurn(log, method = BurnMethod.Bow, burnAnim = animName)
            }
            onOpObj4(log.input) { startBurn(log, it.obj, BurnMethod.Tinderbox) }
        }

        val ordinary = FiremakingLogsRow.all().first { it.input.internalName == "obj.logs" }
        onOpHeldU(GnomishFirelighter.CHARGED, "obj.logs") {
            if (inv[it.firstSlot]?.id != GnomishFirelighter.CHARGED.asRSCM() || inv[it.secondSlot]?.id != ordinary.input.id) return@onOpHeldU
            startBurn(ordinary, method = BurnMethod.Firelighter, firelighterSlot = it.firstSlot)
        }
        onPlayerQueueWithArgs("queue.firemaking_light") { processBurnTick(it.args) }
    }

    private fun ProtectedAccess.startBurn(
        log: FiremakingLogsRow,
        groundObj: Obj? = null,
        method: BurnMethod,
        burnAnim: String = "seq.human_createfire",
        firelighterSlot: Int? = null,
    ) {
        if (!canBurn(log, groundObj, method, firelighterSlot)) {
            resetAnim()
            return
        }

        var obj = groundObj

        if (obj == null) {
            if (invDel(player.inv, log.input.internalName).failure) return
            obj = Obj.fromServer(worldClock, coords, log.input.internalName, 1)
            objRegistry.add(obj)
        }

        stopAction()
        anim(burnAnim)
        player.mes(
            when (method) {
                BurnMethod.Tinderbox, BurnMethod.Firelighter -> "You attempt to light the logs."
                BurnMethod.Bow -> "You attempt to light the logs with your bow."
            },
        )

        weakQueue("queue.firemaking_light", 4, BurnTask(log, obj, method, firelighterSlot, firelighterSlot?.let { inv[it] }))
    }

    private fun ProtectedAccess.canBurn(
        log: FiremakingLogsRow,
        obj: Obj?,
        method: BurnMethod,
        firelighterSlot: Int? = null,
    ): Boolean {
        if (obj != null && !objRepo.isValid(player, obj)) {
            return false
        }

        if (method == BurnMethod.Tinderbox && !inv.contains("obj.tinderbox")) {
            player.mes("You do not have any fire source to light this.")
            return false
        }

        if (method == BurnMethod.Firelighter && (firelighterSlot == null || inv[firelighterSlot]?.let { GnomishFirelighter.count(it) > 0 } != true)) return false
        val reqLevel = when (method) {
            BurnMethod.Tinderbox, BurnMethod.Firelighter -> log.statReq.first().t1
            BurnMethod.Bow -> (log.statReq.first().t1 + 20).coerceAtMost(99)
        }
        if (player.firemakingLvl < reqLevel) {
            player.mes("You need a Firemaking level of $reqLevel to burn ${log.input.name} this way.")
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
        if (task.method == BurnMethod.Firelighter && (player.coords != task.obj.coords || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut)) { resetAnim(); return }
        if ((task.firelighterSlot != null && inv[task.firelighterSlot] !== task.firelighter) || !canBurn(task.log, task.obj, task.method, task.firelighterSlot)) {
            resetAnim()
            return
        }

        val success = task.method == BurnMethod.Firelighter || coloredLogs.contains(task.log.input.internalName) ||
            skillSuccess(64, 512, player.firemakingLvl)

        if (!success) {
            weakQueue("queue.firemaking_light", 4, task)
            return
        }

        completeBurn(task)
    }

    private fun ProtectedAccess.completeBurn(task: BurnTask) {
        val firelighterColour = task.firelighter?.let { GnomishFirelighter.colour(it) }
        if (task.firelighterSlot != null && (task.firelighter == null || !GnomishFirelighter.consume(player, task.firelighterSlot, task.firelighter))) { resetAnim(); return }
        objRepo.del(task.obj)

        val fireCoords = task.obj.coords
        val fireId = firelighterColour?.fireObject?.internalName ?: coloredFire[task.log.input.internalName] ?: "loc.fire"

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
        val xp = (task.log.xp * task.method.xpMultiplier(task.log.input.internalName) + if (task.method == BurnMethod.Firelighter) 10.0 else 0.0) * xpModifier
        statAdvance("stat.firemaking", xp)
        publish(LogBurnedEvent(player, task.log.input.internalName))
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
        val firelighterSlot: Int? = null,
        val firelighter: InvObj? = null,
    )

    enum class BurnMethod {
        Tinderbox,
        Firelighter,
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
