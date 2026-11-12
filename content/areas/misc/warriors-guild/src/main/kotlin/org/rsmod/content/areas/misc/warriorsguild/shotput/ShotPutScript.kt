package org.rsmod.content.areas.misc.warriorsguild.shotput

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.enums.WarguildEnums
import org.rsmod.api.player.hands
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.baseStrengthLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.warriorsguild.GuildDoors
import org.rsmod.content.areas.misc.warriorsguild.TokenLedger
import org.rsmod.content.areas.misc.warriorsguild.shotDusted
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ShotPutScript
@Inject
constructor(
    private val doors: GuildDoors,
    private val worldRepo: WorldRepository,
    private val random: GameRandom,
) : PluginScript() {
    private val thresholds: List<Int> =
        WarguildEnums.warguild_shotput_distance.backing.entries
            .sortedBy { it.key }
            .mapNotNull { it.value }

    override fun ScriptContext.startup() {
        onOpLoc1("loc.war_strength_door") { heavyDoor(it.loc, "loc.war_strength_door_o", north = true) }
        onOpLoc1("loc.war_strength_door_m") {
            heavyDoor(it.loc, "loc.war_strength_door_om", north = false)
        }
        for (lane in LANES) onOpLoc1(lane.pile) { takeShot(lane) }
        onOpHeld1("obj.war_strength_fine_ashes") { dustHands(it.slot) }
    }

    private suspend fun ProtectedAccess.heavyDoor(door: BoundLocInfo, openLoc: String, north: Boolean) {
        val entering = doors.destination(this, door).x > door.coords.x
        if (entering && player.baseStrengthLvl < DOOR_STRENGTH) {
            mes("You need a Strength level of $DOOR_STRENGTH to open this door.")
            return
        }
        anim("seq.human_push")
        mes("The doors creak slowly open - you rush through before they close behind you.")
        val xp = if (north) northDoorXp() else southDoorXp()
        if (xp > 0) statAdvance("stat.strength", xp.toDouble())
        doors.pass(this, door, openLoc)
    }

    private fun northDoorXp(): Int =
        when (random.of(0, 3)) {
            0 -> 0
            1 -> 1
            else -> random.of(2, 11)
        }

    private fun southDoorXp(): Int = if (random.of(0, 99) < 57) 1 else 0

    private suspend fun ProtectedAccess.takeShot(lane: Lane) {
        if (player.righthand != null || player.lefthand != null || player.hands != null) {
            mes("You must have both your hands completely free to throw a shot.")
            return
        }
        if (coords != lane.throwFrom) playerWalk(lane.throwFrom)
        if (coords != lane.throwFrom) return
        if (player.runEnergy / ENERGY_PER_PERCENT < ShotPutFormula.MIN_ENERGY_PERCENT) {
            mes("You're too tired to throw the shot.")
            return
        }
        anim("seq.human_pickupfloor")
        faceSquare(lane.throwFrom.translateX(LANE_LENGTH))
        delay(1)
        var chosen: Style? = null
        startDialogue {
            chosen =
                choice3(
                    STYLES[0].name,
                    STYLES[0],
                    STYLES[1].name,
                    STYLES[1],
                    STYLES[2].name,
                    STYLES[2],
                    title = "Choose your style",
                )
        }
        val style = chosen ?: return
        if (coords != lane.throwFrom) return
        throwShot(lane, style)
    }

    private suspend fun ProtectedAccess.throwShot(lane: Lane, style: Style) {
        val dusted = player.shotDusted
        player.shotDusted = false
        val energy = player.runEnergy
        val power =
            ShotPutFormula.power(
                player.baseStrengthLvl,
                energy / ENERGY_PER_PERCENT,
                style.bonus,
                dusted,
                lane.weight,
            )
        faceSquare(lane.throwFrom.translateX(LANE_LENGTH))
        mes("You take a deep breath and prepare yourself.")
        delay(1)
        mes(style.message)
        anim(style.seq)
        delay(1)
        player.runEnergy = (energy - ShotPutFormula.energyCost(power)).coerceAtLeast(0)
        UpdateRun.energy(player, player.runEnergy)
        if (random.of(ShotPutFormula.SUCCESS_DIVISOR) >= ShotPutFormula.successNumerator(power)) {
            mes("You fumble and drop the shot onto your toe. Ow!")
            queueHit(1, HitType.Typeless, 1)
            return
        }
        val distance = ShotPutFormula.distance(power, thresholds)
        val landing = lane.throwFrom.translateX(distance)
        worldRepo.projAnim(ProjAnim.fromCoordToCoord(coords, landing, SHOT_SPOTANIM, SHOT_PROJANIM))
        delay(FLIGHT_TICKS)
        statAdvance("stat.strength", ShotPutFormula.xp(power))
        TokenLedger.credit(player, ShotPutFormula.tokens(distance, lane.baseTokens))
        val yards = if (distance == 1) "1 yard" else "$distance yards"
        startDialogue {
            chatNpcSpecific("Ref", lane.ref, happy, "Well done. You threw the shot $yards!")
        }
    }

    private fun ProtectedAccess.dustHands(slot: Int) {
        if (invDel(inv, "obj.war_strength_fine_ashes", 1, slot).failure) return
        anim("seq.dust_hands_play")
        spotanim("spotanim.dust_hands_spotanim")
        player.shotDusted = true
        mes("You dust your hands with the finely ground ash.")
    }

    private data class Lane(
        val pile: String,
        val throwFrom: CoordGrid,
        val weight: Int,
        val baseTokens: Int,
        val ref: String,
    )

    private data class Style(val name: String, val bonus: Int, val seq: String, val message: String)

    private companion object {
        const val DOOR_STRENGTH = 50
        const val ENERGY_PER_PERCENT = 100
        const val LANE_LENGTH = 14
        const val FLIGHT_TICKS = 2
        const val SHOT_PROJANIM = "projanim.thrown"
        val SHOT_SPOTANIM = "spotanim.shot_projanim".asRSCM(RSCMType.SPOTANIM)

        val LANES =
            listOf(
                Lane(
                    pile = "loc.war_strength_shot18",
                    throwFrom = CoordGrid(2861, 3553, 1),
                    weight = 18,
                    baseTokens = 1,
                    ref = "npc.strength_referree1",
                ),
                Lane(
                    pile = "loc.war_strength_shot22",
                    throwFrom = CoordGrid(2861, 3547, 1),
                    weight = 22,
                    baseTokens = 3,
                    ref = "npc.strength_referree2",
                ),
            )

        val STYLES =
            listOf(
                Style(
                    "Standing throw",
                    30,
                    "seq.standing_shot",
                    "You throw the shot as hard as you can.",
                ),
                Style(
                    "Step and throw",
                    20,
                    "seq.running_shot",
                    "You take a step and throw the shot as hard as you can.",
                ),
                Style(
                    "Spin and throw",
                    10,
                    "seq.spinning_shot",
                    "You spin around and release the shot.",
                ),
            )
    }
}
