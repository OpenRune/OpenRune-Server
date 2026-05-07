package org.rsmod.content.skills.firemaking

import dev.openrune.internalName
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.aconverted.interf.IfSubType
import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.controller.vars.intVarCon
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.loc.locNearby
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.FiremakingColoredLogsRow
import org.rsmod.api.table.FiremakingLogsRow
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CampfireEvents @Inject constructor(
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val conRepo: ControllerRepository,
    private val mapClock: MapClock,
    private val objRegistry: ObjRegistry,
) : PluginScript() {

    private val coloredLogRows = FiremakingColoredLogsRow.all()

    private val campFireObjects =
        coloredLogRows.map { it.campfireObject.internalName }.toMutableSet().apply { add("loc.forestry_fire") }

    private val basicFires = coloredLogRows.map { it.fireObject.internalName }.toSet() + "loc.fire"

    private val campfireLocForColoredLog =
        coloredLogRows.associate { it.logItem.internalName to it.campfireObject.internalName }

    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs("queue.firemaking_campfire_tend") { processCampfireTendTick(it.args) }

        campFireObjects.forEach { camp ->
            onOpLoc1(camp) { tendCampfireMenu(it.vis) }
            onOpLoc2(camp) { showCampfireStatus(it.vis) }
            onOpLoc3(camp) { anim("seq.forestry_sitting_tea_loop") }

            FiremakingLogsRow.all().forEach { log ->
                onOpLocU(camp, log.item.internalName) {
                    tendCampfireWithLog(it.vis, log)
                }
            }
        }

        basicFires.forEach { fire ->
            FiremakingLogsRow.all().forEach { log ->
                onOpLocU(fire, log.item.internalName) {
                    lightCampfire(it, log)
                }
            }
        }
    }

    private fun ProtectedAccess.lightCampfire(event: LocUEvents.Op, log: FiremakingLogsRow) {
        if (!canLight(log)) return

        val fire = event.vis
        val tile = fire.coords

        if (locRepo.locNearby(tile, 5, tile, campFireObjects)) {
            player.mes("There's a Forester's Campfire nearby, help tend to that one or move further away.")
            return
        }

        val fireObject = campfireLocForColoredLog[log.item.internalName] ?: "loc.forestry_fire"

        locRepo.del(fire, Int.MAX_VALUE)
        invDel(player.inv, log.item.internalName, 1)

        spawnCampfire(
            fire,
            fireObject,
            log.foresterInitialTicks.coerceAtLeast(1)
        )

        spotanimMap(worldRepo, smokeSpotForAngle(fire.angleId), tile)
    }

    private suspend fun ProtectedAccess.tendCampfireMenu(camp: BoundLocInfo) {
        val log = bestAvailableLog() ?: return
        tendCampfireWithLog(camp, log)
    }

    private fun ProtectedAccess.tendCampfireWithLog(camp: BoundLocInfo, log: FiremakingLogsRow) {
        if (!canTend(log, camp)) {
            resetAnim()
            stopAction()
            return
        }
        weakQueue("queue.firemaking_campfire_tend", 1, CampfireTendTask(camp, log))
    }

    private fun ProtectedAccess.processCampfireTendTick(task: CampfireTendTask) {
        val camp = resolveCampfire(task.campfire) ?: return
        if (!canTend(task.log, task.campfire)) {
            resetAnim()
            stopAction()
            return
        }

        task.log.foresterAnimation?.internalName?.let(::anim)

        extendCampfire(camp, task.log.foresterLogTicks)

        statAdvance("stat.firemaking", task.log.xp.toDouble())
        invDel(player.inv, task.log.item.internalName, 1)

        spotanimMap(worldRepo, "spotanim.forestry_campfire_burning_spotanim", camp.coords)

        weakQueue("queue.firemaking_campfire_tend", 9, task)
    }

    private fun ProtectedAccess.showCampfireStatus(camp: BoundLocInfo) {
        resetAnim()

        val text = when (campfireRemainingTicks(camp)) {
            in 0..58 -> "The embers glow softly."
            in 59..118 -> "The flames flicker gently."
            in 119..178 -> "The fire burns steadily."
            in 179..238 -> "The fire burns brightly."
            else -> "The roaring fire crackles invitingly."
        }

        spam(text)
    }

    private fun ProtectedAccess.canLight(log: FiremakingLogsRow): Boolean =
        hasTinderboxOrMes() && hasFiremakingLevelOrMes(log)

    private fun ProtectedAccess.canTend(log: FiremakingLogsRow, camp: BoundLocInfo): Boolean {
        if (!hasTinderboxOrMes()) return false
        if (inv.count(log.item.internalName) <= 0) return false
        if (!locRepo.findLoc(camp.coords, camp.internalName)) return false
        return hasFiremakingLevelOrMes(log)
    }

    private fun ProtectedAccess.hasTinderboxOrMes(): Boolean {
        if (inv.contains("obj.tinderbox")) return true
        player.mes("You need a Tinderbox to do this.")
        return false
    }

    private fun ProtectedAccess.hasFiremakingLevelOrMes(log: FiremakingLogsRow): Boolean {
        if (player.firemakingLvl >= log.level) return true
        player.mes("You need a Firemaking level of ${log.level} to burn ${log.item.name} logs.")
        return false
    }

    private suspend fun ProtectedAccess.bestAvailableLog(): FiremakingLogsRow? {
        val logs = logsCarriedForTending()

        when (logs.size) {
            0 -> return null
            1 -> return logs.first()
        }

        var chosen: FiremakingLogsRow? = null

        openSkillMulti(SkillMultiConfig(verb = "burn", entries = logs.map {
          SkillMultiEntry(it.item.internalName)
        }),) { selection ->
            chosen = logs.firstOrNull { it.item.internalName == selection.entry.internal }
        }

        return chosen
    }

    private fun ProtectedAccess.logsCarriedForTending(): List<FiremakingLogsRow> =
        FiremakingLogsRow.all().filter {
            player.firemakingLvl >= it.level && inv.count(it.item.internalName) > 0
        }


    private fun spawnCampfire(template: BoundLocInfo, id: String, duration: Int) {
        val coords = template.coords

        ensureController(coords).apply {
            firemakingCampfireExpiryCycle = mapClock + duration
        }

        locRepo.add(
            coords,
            id,
            duration,
            template.angle,
            template.shape,
            onDespawn = {
                conRepo.findExact(coords, "controller.firemaking_campfire")?.let(conRepo::del)
                objRegistry.add(Obj.fromServer(mapClock, coords, "obj.ashes", 1))
            }
        )
    }

    private fun extendCampfire(camp: BoundLocInfo, add: Int) {
        val newDuration = (campfireRemainingTicks(camp) + add).coerceIn(1, 300)
        locRepo.del(camp, Int.MAX_VALUE)
        spawnCampfire(camp, camp.internalName, newDuration)
    }

    private fun campfireRemainingTicks(camp: BoundLocInfo): Int {
        val expiry = conRepo.findExact(camp.coords, "controller.firemaking_campfire")
            ?.firemakingCampfireExpiryCycle ?: return 300

        return (expiry - mapClock.cycle).coerceAtLeast(0)
    }

    private fun ensureController(coords: CoordGrid): Controller =
        conRepo.findExact(coords, "controller.firemaking_campfire")
            ?: Controller("controller.firemaking_campfire", coords).also {
                conRepo.add(it, 5000)
                it.firemakingCampfireExpiryCycle = 0
            }

    private fun resolveCampfire(camp: BoundLocInfo): BoundLocInfo? {
        val type = ServerCacheManager
            .getObject(camp.internalName.asRSCM(RSCMType.LOC))
            ?: return null

        return locRepo.findExact(camp.coords, type)?.let { BoundLocInfo(it, type) }
    }

    private val smokeByAngle =
        listOf(
            0 to "spotanim.forestry_campfire_smoke_01",
            90 to "spotanim.forestry_campfire_smoke_02",
            180 to "spotanim.forestry_campfire_smoke_03",
            270 to "spotanim.forestry_campfire_smoke_04",
        )

    private fun smokeSpotForAngle(angleId: Int): SpotanimType {
        val rot = LocAngle[angleId].ordinal * 90 % 360
        return SpotanimType(
            smokeByAngle.minBy { abs(it.first - rot) }
                .second.asRSCM(RSCMType.SPOTANIM)
        )
    }

    private data class CampfireTendTask(
        val campfire: BoundLocInfo,
        val log: FiremakingLogsRow
    )

    companion object {
        var Controller.firemakingCampfireExpiryCycle: Int by intVarCon("varcon.firemaking_campfire_expiry_cycle")
    }
}
