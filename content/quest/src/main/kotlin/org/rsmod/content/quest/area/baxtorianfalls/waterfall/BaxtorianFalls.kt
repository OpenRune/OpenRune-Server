package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onApLocT
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ENTERED_FALLS
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.HUDON_NPC
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ROPE
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.STARTED
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.geraldGreetsWashedUp
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.hudonFirstMeeting
import org.rsmod.content.quest.util.fadeFromBlack
import org.rsmod.content.quest.util.fadeToBlack
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BaxtorianFalls
@Inject
constructor(private val waterfall: WaterfallQuest, private val search: NpcSearch) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(RAFT) { boardRaft() }
        onOpLoc1(RIVER) { swim() }
        onApLoc1(ROCK) { apRock(it.loc) { swim() } }
        onOpLoc1(ROCK) { swim() }
        // Only the op step of a used item is bridged to onOpLocU, so approach handlers bind to the
        // inventory component.
        val inventory = ServerCacheManager.fromComponent(INVENTORY.asRSCM(RSCMType.COMPONENT))
        onApLocT(ROCK, inventory) {
            if (it.objType?.id == ROPE.asRSCM(RSCMType.OBJ)) {
                apRock(it.loc) { ropeAcross(it.loc) }
            } else {
                apRange(-1)
            }
        }
        onOpLocU(ROCK, ROPE) { ropeAcross(it.loc) }
        onApLoc1(TREE) { apTree(it.loc) { climbWithoutRope() } }
        onOpLoc1(TREE) { climbWithoutRope() }
        onApLocT(TREE, inventory) {
            if (it.objType?.id == ROPE.asRSCM(RSCMType.OBJ)) {
                apTree(it.loc) { ropeDown() }
            } else {
                apRange(-1)
            }
        }
        onOpLocU(TREE, ROPE) { ropeDown() }
        onOpLoc1(LEDGE_DOOR) { enterFalls() }
        onOpLoc1(BARREL) { rideBarrel() }
    }

    private suspend fun ProtectedAccess.boardRaft() {
        if (waterfall.stage(player) == 0) {
            mesbox("You're not sure if the raft is safe to use. Best to leave it alone.")
            return
        }
        mesbox("You board the small raft and push off down stream...")
        soundSynth(RIVER_SOUND)
        fadeToBlack()
        telejump(WaterfallCoords.RAFT_CRASH, TeleportType.Exempt)
        delay(1)
        fadeFromBlack()
        soundSynth(CRASH_SOUND)
        mesbox("...you crash into a small island.")
        if (waterfall.stage(player) == STARTED) {
            val hudon =
                npcFind(player.coords, HUDON_NPC, HUDON_SEARCH_RADIUS, HuntVis.Off, search) ?: return
            startDialogue(hudon) { hudonFirstMeeting(waterfall) }
        }
    }

    private suspend fun ProtectedAccess.swim() {
        anim(SWIM_SEQ)
        mesbox("You swim out into the water...")
        mesbox("...but the current is too strong, washing you downstream.")
        washDownstream(ouch = false)
        geraldGreetsWashedUp(waterfall, search)
    }

    private suspend fun ProtectedAccess.apRock(
        rock: BoundLocInfo,
        action: suspend ProtectedAccess.() -> Unit,
    ) {
        if (!WaterfallCoords.onHudonIsland(player.coords)) {
            apRange(-1)
            return
        }
        if (!isWithinApRange(rock, ROCK_RANGE)) {
            return
        }
        action()
    }

    private suspend fun ProtectedAccess.apTree(
        tree: BoundLocInfo,
        action: suspend ProtectedAccess.() -> Unit,
    ) {
        if (!WaterfallCoords.onTreeIsland(player.coords)) {
            apRange(-1)
            return
        }
        if (!isWithinApRange(tree, TREE_RANGE)) {
            return
        }
        action()
    }

    private suspend fun ProtectedAccess.ropeAcross(rock: BoundLocInfo) {
        faceSquare(rock.coords)
        anim(THROW_ROPE_SEQ)
        soundSynth(TIE_ROPE_SOUND)
        delay(2)
        mes("You tie the rope around the rock and pull yourself through the water towards it.")
        anim(SWIM_SEQ)
        soundSynth(SWIM_SOUND)
        exactMove(
            start = player.coords,
            end = WaterfallCoords.TREE_ISLAND,
            delay1 = 0,
            delay2 = SWIM_TICKS * CLIENT_CYCLES_PER_TICK,
            dir = constants.em_face_south,
            teleportType = TeleportType.Exempt,
        )
        delay(SWIM_TICKS)
        resetAnim()
    }

    private suspend fun ProtectedAccess.climbWithoutRope() {
        mesbox("You try to use the tree to climb down...")
        anim(CLIMB_DOWN_SEQ)
        delay(1)
        mesbox("...but you slip and fall into the water.")
        washDownstream(ouch = true)
        geraldGreetsWashedUp(waterfall, search)
    }

    private suspend fun ProtectedAccess.ropeDown() {
        soundSynth(TIE_ROPE_SOUND)
        mesbox("You tie the rope to the tree and climb down to the ledge below.")
        anim(CLIMB_DOWN_SEQ)
        soundSynth(ROPE_CLIMB_SOUND)
        exactMove(
            start = player.coords,
            end = WaterfallCoords.LEDGE,
            delay1 = 0,
            delay2 = CLIMB_TICKS * CLIENT_CYCLES_PER_TICK,
            dir = constants.em_face_north,
            teleportType = TeleportType.Exempt,
        )
        delay(CLIMB_TICKS)
        resetAnim()
    }

    private suspend fun ProtectedAccess.enterFalls() {
        if (!waterfall.isComplete(player) && !player.wearsAmulet()) {
            mesbox("You try to open the door, but the ledge is suddenly flooded with water...")
            mesbox("...you are pushed over the waterfall and into the river.")
            washDownstream(ouch = true)
            geraldGreetsWashedUp(waterfall, search)
            return
        }
        soundSynth(DOOR_SOUND)
        telejump(WaterfallCoords.FALLS_ENTRY, TeleportType.Exempt)
        waterfall.advanceTo(this, ENTERED_FALLS)
        mesbox("You enter the waterfall.")
    }

    private suspend fun ProtectedAccess.rideBarrel() {
        mesbox("You climb into the barrel and push off the edge. You are carried down the river.")
        anim(BARREL_SEQ)
        washDownstream(ouch = false)
        geraldGreetsWashedUp(waterfall, search)
    }

    private companion object {
        const val INVENTORY = "component.inventory:items"
        const val RAFT = "loc.lograft_waterfall_quest"
        const val RIVER = "loc.waterfall_swim_point"
        const val ROCK = "loc.crossing_rock_waterfall_quest"
        const val TREE = "loc.overhanging_tree1_waterfall_quest"
        const val LEDGE_DOOR = "loc.waterfall_ledge_door"
        const val BARREL = "loc.barrel_waterfall_quest"

        const val HUDON_SEARCH_RADIUS = 8
        const val ROCK_RANGE = 10
        const val TREE_RANGE = 3

        const val SWIM_SEQ = "seq.human_swim"
        const val THROW_ROPE_SEQ = "seq.human_throwrope"
        const val CLIMB_DOWN_SEQ = "seq.human_climbing_down"
        const val BARREL_SEQ = "seq.human_pickupfloor"
        const val SWIM_TICKS = 4
        const val CLIMB_TICKS = 2
        const val CLIENT_CYCLES_PER_TICK = 30

        const val RIVER_SOUND = "synth.splash_and_river"
        const val CRASH_SOUND = "synth.watersplash"
        const val SWIM_SOUND = "synth.cf_swim"
        const val TIE_ROPE_SOUND = "synth.cf_tierope"
        const val ROPE_CLIMB_SOUND = "synth.ropeclimb"
        const val DOOR_SOUND = "synth.door_open"
    }
}
