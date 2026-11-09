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
        arriveDelay()
        mesboxNp("You board the small raft and push off down stream...")
        fadeOut()
        delay(RAFT_TICKS)
        telejump(WaterfallCoords.RAFT_CRASH, TeleportType.Exempt)
        soundSynth(CRASH_SOUND, delay = CRASH_SOUND_DELAY)
        delay(1)
        fadeIn()
        mesbox("...you crash into a small island.")
        if (waterfall.stage(player) == STARTED) {
            val hudon = npcFind(player.coords, HUDON_NPC, HUDON_SEARCH_RADIUS, HuntVis.Off, search)
            if (hudon != null) {
                startDialogue(hudon) { hudonFirstMeeting(waterfall) }
            }
        }
        closeFadeOverlayNow()
    }

    private suspend fun ProtectedAccess.swim() {
        arriveDelay()
        if (player.coords != WaterfallCoords.ROPE_START) {
            playerWalkWithMinDelay(WaterfallCoords.ROPE_START)
            if (player.coords != WaterfallCoords.ROPE_START) {
                return
            }
        }
        delay(1)
        mesboxNp("You swim out into the water...")
        soundSynth(SPLASH_SOUND)
        spotanimMap(worldRepo, SPLASH_SPOTANIM, SPLASH_TILE, delay = SPLASH_DELAY)
        setBas(SWIMMING_BAS)
        try {
            forcedWalk(WaterfallCoords.SWIM_END, SWIM_TILES)
            setBas(DROWNING_BAS)
            delay(1)
            mesboxNp(WASHED_DOWNSTREAM)
            fadeOut()
            delay(CARRIED_TICKS)
            telejump(WaterfallCoords.DOWNSTREAM, TeleportType.Exempt)
            delay(1)
        } finally {
            setBas(null)
        }
        fadeIn()
        mesbox(WASHED_DOWNSTREAM)
        geraldGreetsWashedUp(waterfall, search)
        closeFadeOverlayNow()
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
            soundSynth(DOOR_OPEN_SOUND)
            fallIntoRiver(
                "You try to open the door, but the ledge is suddenly flooded with water...",
                "...you are pushed over the waterfall and into the river.",
            )
            geraldGreetsWashedUp(waterfall, search)
            return
        }
        mesboxNp(ENTER_FALLS)
        soundSynth(DOOR_OPEN_SOUND)
        fadeOut()
        delay(CARRIED_TICKS)
        telejump(WaterfallCoords.FALLS_ENTRY, TeleportType.Exempt)
        waterfall.advanceTo(this, ENTERED_FALLS)
        delay(1)
        soundSynth(DOOR_CLOSE_SOUND)
        fadeIn()
        mesbox(ENTER_FALLS)
        closeFadeOverlayNow()
    }

    private suspend fun ProtectedAccess.rideBarrel() {
        mesboxNp(BARREL_RIDE)
        fadeOut()
        delay(CARRIED_TICKS)
        telejump(WaterfallCoords.DOWNSTREAM, TeleportType.Exempt)
        delay(1)
        fadeIn()
        mesbox(BARREL_RIDE)
        geraldGreetsWashedUp(waterfall, search)
        closeFadeOverlayNow()
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
        const val SWIMMING_BAS = "bas.waterfall_swimming"
        const val DROWNING_BAS = "bas.waterfall_drowning"
        const val SPLASH_SPOTANIM = "spotanim.watersplash"
        const val SPLASH_DELAY = 3
        val SPLASH_TILE = WaterfallCoords.ROPE_START.translateZ(-1)
        const val SWIM_TILES = 4
        const val RAFT_TICKS = 3
        const val CARRIED_TICKS = 2
        val ROPE_PIECE_TILE = WaterfallCoords.ROPE_START.translateZ(-2)
        const val LEDGE_DOOR = "loc.waterfall_ledge_door"
        const val BARREL = "loc.barrel_waterfall_quest"

        const val HUDON_SEARCH_RADIUS = 8
        const val ROCK_RANGE = 10

        const val THROW_ROPE_SEQ = "seq.human_throwrope"

        const val CRASH_SOUND = "synth.roof_collapse"
        const val CRASH_SOUND_DELAY = 10
        const val SPLASH_SOUND = "synth.watersplash"
        const val DOOR_OPEN_SOUND = "synth.big_wooden_door_open"
        const val DOOR_CLOSE_SOUND = "synth.big_wooden_door_close"

        const val WASHED_DOWNSTREAM = "...but the current is too strong, washing you downstream."
        const val ENTER_FALLS = "You enter the waterfall."
        const val BARREL_RIDE =
            "You climb into the barrel and push off the edge. You are carried down the river."
    }
}
