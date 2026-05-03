package org.rsmod.content.travel.canoe.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.enums.CanoeShapingAxeEnums.canoe_shaping_axe_anims
import org.rsmod.api.enums.CanoeShapingAxeEnums.canoe_shaping_axe_rates
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CanoeShaping
@Inject
constructor(
    private val locRepo: LocRepository,
    private val invisibleLvls: InvisibleLevels,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.canoestation_fallen_tree") { walkToAndShapeCanoe(it.loc) }
        onOpLoc3("loc.canoestation_fallen_tree") { cutShape(it.loc, canoeType ?: return@onOpLoc3) }
        onIfModalButton("component.canoeing:log") { selectCanoe(Canoe.Log) }
        onIfModalButton("component.canoeing:dugout") { selectCanoe(Canoe.Dugout) }
        onIfModalButton("component.canoeing:stable_dugout") { selectCanoe(Canoe.StableDugout) }
        onIfModalButton("component.canoeing:waka") { selectCanoe(Canoe.Waka) }
    }

    private suspend fun ProtectedAccess.walkToAndShapeCanoe(loc: BoundLocInfo) {
        val preselectedType = if (confirmedCanoeType) canoeType else null
        stationCoords = loc.coords

        val locCentre = loc.adjustedCentre
        if (preselectedType == null) {
            walk(coords)
            delay(1)
            playerWalk(locCentre)
            faceSquare(locCentre)
            openShapingModal()
            return
        }

        val centre = resolveStation().centreFaceCoords()
        val squares = validatedLineOfWalkSquares(centre, minRadius = 0, maxRadius = 1)
        val dest = squares.minBy { if (it == coords) -1 else it.chebyshevDistance(coords) }

        if (coords != dest) {
            walk(coords)
            delay(1)
            playerWalk(dest)
            faceSquare(locCentre)
            delay(1)
            faceSquare(locCentre)
        }
        shapeCanoe(loc, preselectedType)
    }

    private fun ProtectedAccess.openShapingModal() {
        ifOpenMainModal("interface.canoeing", colour = 3612928, transparency = 0)
        ifSetEvents("component.canoeing:log", 0..0, IfEvent.Op1)
        ifSetEvents("component.canoeing:dugout", 0..0, IfEvent.Op1)
        ifSetEvents("component.canoeing:stable_dugout", 0..0, IfEvent.Op1)
        ifSetEvents("component.canoeing:waka", 0..0, IfEvent.Op1)
        ifSetEvents("component.canoeing:close", 0..0, IfEvent.Op1)
    }

    private fun ProtectedAccess.selectCanoe(canoe: Canoe) {
        ifClose()

        if (player.woodcuttingLvl < lcParam(ServerCacheManager.getObject(canoe.loc.asRSCM(RSCMType.LOC))!!, params.levelrequire)) {
            // Cs2 removes the "Make" option on canoes when the player does not meet their level
            // requirement. No need for a message as this is not meant to be possible.
            // Could in theory test what is _meant_ to happen by faking an if_button packet
            // and seeing if a message is sent, but it's a low-priority addition either way.
            return
        }

        val stationCoords = checkNotNull(stationCoords) { "Expected valid station coords." }
        val station = locRepo.findExact(stationCoords, LocShape.CentrepieceStraight)
        checkNotNull(station) { "Expected canoe station loc: coords=$stationCoords" }

        val loc = BoundLocInfo(station, ServerCacheManager.getObject(station.id)!!)
        canoeType = canoe
        confirmedCanoeType = true
        opLoc1(loc)
    }

    private fun ProtectedAccess.shapeCanoe(loc: BoundLocInfo, canoe: Canoe) {
        if (actionDelay < mapClock) {
            actionDelay = mapClock + 5
            refaceDelay = mapClock + 1
            skillAnimDelay = mapClock + 1
            opLoc1(loc)
            return
        }
        cutShape(loc, canoe)
    }

    private fun ProtectedAccess.cutShape(loc: BoundLocInfo, canoe: Canoe) {
        val axe = findAxe(player)
        if (axe == null) {
            mes(
                "You need an axe to shape a canoe.<br>" +
                    "You do not have an axe that you have the Woodcutting level to use."
            )
            canoeType = null
            confirmedCanoeType = false
            return
        }

        if (skillAnimDelay == mapClock && actionDelay >= mapClock) {
            val axeEnum = canoe_shaping_axe_anims
            val axeSeq =
                axeEnum.find { it.key.id == axe.id }?.value ?: error("Unable to find axe seq")
            anim(RSCM.getReverseMapping(RSCMType.SEQ, axeSeq.id))
        }

        skillAnimDelay = mapClock + 1

        if (refaceDelay < mapClock) {
            refaceDelay = mapClock + 5
        } else if (refaceDelay == mapClock) {
            faceSquare(loc.adjustedCentre)
        }

        var finishShape = false

        if (actionDelay < mapClock) {
            actionDelay = mapClock + 5
            faceSquare(loc.adjustedCentre)
        } else if (actionDelay == mapClock) {
            val (low, high) = axeSuccessRates(axe, canoe_shaping_axe_rates)
            finishShape = statRandom("stat.woodcutting", low, high, invisibleLvls)
        }

        if (finishShape) {
            val station = resolveStation()
            this[station, canoe] = CanoeState.Ready
            confirmedCanoeType = false

            val xp = lcParam(ServerCacheManager.getObject(canoe.loc.asRSCM(RSCMType.LOC))!!, params.skill_xp) * xpMods.get(player, "stat.woodcutting")
            resetAnim()
            statAdvance("stat.woodcutting", xp)

            // TODO(content): Degrade axe charges when applicable.

            return
        }

        opLoc3(loc)
    }

    private fun Station.centreFaceCoords(): CoordGrid =
        when (this) {
            Station.Lumbridge -> CoordGrid(0, 50, 50, 43, 37)
            Station.ChampionsGuild -> CoordGrid(0, 50, 52, 2, 15)
            Station.BarbarianVillage -> CoordGrid(0, 48, 53, 40, 19)
            Station.Edgeville -> CoordGrid(0, 48, 54, 60, 54)
            Station.FeroxEnclave -> CoordGrid(0, 49, 56, 18, 46)
        }
}
