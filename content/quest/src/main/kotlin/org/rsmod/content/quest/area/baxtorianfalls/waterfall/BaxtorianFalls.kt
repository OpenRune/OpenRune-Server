package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.api.hunt.NpcSearch
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
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
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BaxtorianFalls
@Inject
constructor(
    private val waterfall: WaterfallQuest,
    private val search: NpcSearch,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(RAFT) { boardRaft() }
        onOpLoc1(RIVER) { swim() }
        onApLoc1(ROCK) { apRock(it.loc) { swim() } }
        onOpLoc1(ROCK) { rockOp() }
        val inventory = ServerCacheManager.fromComponent(INVENTORY.asRSCM(RSCMType.COMPONENT))
        onApLocT(ROCK, inventory) {
            if (it.objType?.id == ROPE.asRSCM(RSCMType.OBJ)) {
                apRock(it.loc) { ropeAcross(it.loc) }
            } else {
                apRange(-1)
            }
        }
        onOpLocU(ROCK, ROPE) { ropeAcross(it.loc) }
        onOpLoc1(TREE) { climbWithoutRope() }
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

    private suspend fun ProtectedAccess.ropeAcross(rock: BoundLocInfo) {
        if (player.coords != WaterfallCoords.ROPE_START) {
            playerWalkWithMinDelay(WaterfallCoords.ROPE_START)
            if (player.coords != WaterfallCoords.ROPE_START) {
                return
            }
        }
        faceSquare(rock.coords)
        anim(THROW_ROPE_SEQ)
        delay(1)
        spotanimMap(worldRepo, ROPE_PIECE_SPOTANIM, ROPE_PIECE_TILE, delay = ROPE_PIECE_DELAY)
        delay(1)
        val pull = WaterfallCoords.ROPE_START.z - rock.coords.z - 1
        locRepo.change(rock, ROPED_ROCK, pull)
        for (step in 1..pull) {
            val tile = WaterfallCoords.ROPE_START.translateZ(-step)
            locRepo.add(tile, CROSSING_ROPE, step, LocAngle.East, LocShape.CentrepieceStraight)
        }
        val ropeEnd = WaterfallCoords.ROPE_START.translateZ(-pull)
        setBas(WADING_BAS)
        try {
            forcedWalk(listOf(ropeEnd, WaterfallCoords.TREE_ISLAND), pull + 1)
        } finally {
            setBas(null)
        }
    }

    private suspend fun ProtectedAccess.rockOp() {
        if (WaterfallCoords.onTreeIsland(player.coords)) {
            mes("You don't see that being any help.")
            return
        }
        swim()
    }

    private suspend fun ProtectedAccess.climbWithoutRope() {
        arriveDelay()
        fallIntoRiver(
            "You try to use the tree to climb down...",
            "...but you slip and fall into the water.",
        )
        geraldGreetsWashedUp(waterfall, search)
    }

    private suspend fun ProtectedAccess.ropeDown() {
        arriveDelay()
        telejump(WaterfallCoords.LEDGE, TeleportType.Exempt)
        mesbox("You tie the rope to the tree and climb down to the ledge below.")
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
        const val ROPED_ROCK = "loc.crossing_rock_roped_waterfall_quest"
        const val CROSSING_ROPE = "loc.crossing_rope_waterfall_quest"
        const val ROPE_PIECE_SPOTANIM = "spotanim.rope_piece"
        const val ROPE_PIECE_DELAY = 3
        const val WADING_BAS = "bas.waterfall_wading"
        val ROPE_PIECE_TILE = WaterfallCoords.ROPE_START.translateZ(-2)
        const val LEDGE_DOOR = "loc.waterfall_ledge_door"
        const val BARREL = "loc.barrel_waterfall_quest"

        const val HUDON_SEARCH_RADIUS = 8
        const val ROCK_RANGE = 10

        const val SWIM_SEQ = "seq.human_swim"
        const val THROW_ROPE_SEQ = "seq.human_throwrope"
        const val BARREL_SEQ = "seq.human_pickupfloor"

        const val RIVER_SOUND = "synth.splash_and_river"
        const val CRASH_SOUND = "synth.watersplash"
        const val DOOR_SOUND = "synth.door_open"
    }
}
