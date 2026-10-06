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
import org.rsmod.content.quest.area.ardougne.fadeFromBlack
import org.rsmod.content.quest.area.ardougne.fadeToBlack
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.EnteredFalls
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.HudonNpc
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Rope
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Started
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.geraldGreetsWashedUp
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs.hudonFirstMeeting
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The way down Baxtorian Falls to the waterfall door: the log raft behind Almera's house runs
 * aground on Hudon's island, a rope tied to the rock pulls the player across to the dead tree,
 * and a rope tied to the tree lowers them onto the ledge. Every shortcut (swimming, climbing
 * without a rope, forcing the door without Glarial's amulet) sweeps them down the river to
 * Gerald's bank; the barrel on the ledge does the same, but gently.
 *
 * The rock and the tree stand in the water, so both are worked from a distance. A used item
 * arrives as a `LocT` interaction and only its op step is bridged to `onOpLocU`, so the rope's
 * approach handlers are registered on the inventory component directly.
 */
class BaxtorianFalls
@Inject
constructor(private val waterfall: WaterfallQuest, private val search: NpcSearch) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(Raft) { boardRaft() }
        onOpLoc1(River) { swim() }
        onApLoc1(Rock) { apRock(it.loc) { swim() } }
        onOpLoc1(Rock) { swim() }
        val inventory = ServerCacheManager.fromComponent(Inventory.asRSCM(RSCMType.COMPONENT))
        onApLocT(Rock, inventory) {
            if (it.objType?.id == Rope.asRSCM(RSCMType.OBJ)) {
                apRock(it.loc) { ropeAcross(it.loc) }
            } else {
                apRange(-1)
            }
        }
        onOpLocU(Rock, Rope) { ropeAcross(it.loc) }
        onApLoc1(Tree) { apTree(it.loc) { climbWithoutRope() } }
        onOpLoc1(Tree) { climbWithoutRope() }
        onApLocT(Tree, inventory) {
            if (it.objType?.id == Rope.asRSCM(RSCMType.OBJ)) {
                apTree(it.loc) { ropeDown() }
            } else {
                apRange(-1)
            }
        }
        onOpLocU(Tree, Rope) { ropeDown() }
        onOpLoc1(LedgeDoor) { enterFalls() }
        onOpLoc1(Barrel) { rideBarrel() }
    }

    private suspend fun ProtectedAccess.boardRaft() {
        if (waterfall.stage(player) == 0) {
            mesbox("You're not sure if the raft is safe to use. Best to leave it alone.")
            return
        }
        mesbox("You board the small raft and push off down stream...")
        soundSynth(RiverSound)
        fadeToBlack()
        telejump(WaterfallCoords.RAFT_CRASH, TeleportType.Exempt)
        delay(1)
        fadeFromBlack()
        soundSynth(CrashSound)
        mesbox("...you crash into a small island.")
        if (waterfall.stage(player) == Started) {
            val hudon =
                npcFind(player.coords, HudonNpc, HudonSearchRadius, HuntVis.Off, search) ?: return
            startDialogue(hudon) { hudonFirstMeeting(waterfall) }
        }
    }

    private suspend fun ProtectedAccess.swim() {
        anim(SwimSeq)
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
        if (!isWithinApRange(rock, RockRange)) {
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
        if (!isWithinApRange(tree, TreeRange)) {
            return
        }
        action()
    }

    private suspend fun ProtectedAccess.ropeAcross(rock: BoundLocInfo) {
        faceSquare(rock.coords)
        anim(ThrowRopeSeq)
        soundSynth(TieRopeSound)
        delay(2)
        mes("You tie the rope around the rock and pull yourself through the water towards it.")
        anim(SwimSeq)
        soundSynth(SwimSound)
        exactMove(
            start = player.coords,
            end = WaterfallCoords.TREE_ISLAND,
            delay1 = 0,
            delay2 = SwimTicks * ClientCyclesPerTick,
            dir = constants.em_face_south,
            teleportType = TeleportType.Exempt,
        )
        delay(SwimTicks)
        resetAnim()
    }

    private suspend fun ProtectedAccess.climbWithoutRope() {
        mesbox("You try to use the tree to climb down...")
        anim(ClimbDownSeq)
        delay(1)
        mesbox("...but you slip and fall into the water.")
        washDownstream(ouch = true)
        geraldGreetsWashedUp(waterfall, search)
    }

    private suspend fun ProtectedAccess.ropeDown() {
        soundSynth(TieRopeSound)
        mesbox("You tie the rope to the tree and climb down to the ledge below.")
        anim(ClimbDownSeq)
        soundSynth(RopeClimbSound)
        exactMove(
            start = player.coords,
            end = WaterfallCoords.LEDGE,
            delay1 = 0,
            delay2 = ClimbTicks * ClientCyclesPerTick,
            dir = constants.em_face_north,
            teleportType = TeleportType.Exempt,
        )
        delay(ClimbTicks)
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
        soundSynth(DoorSound)
        mes("You enter the waterfall.")
        telejump(WaterfallCoords.FALLS_ENTRY, TeleportType.Exempt)
        waterfall.advanceTo(this, EnteredFalls)
    }

    private suspend fun ProtectedAccess.rideBarrel() {
        mesbox("You climb into the barrel and push off the edge. You are carried down the river.")
        anim(BarrelSeq)
        washDownstream(ouch = false)
        geraldGreetsWashedUp(waterfall, search)
    }

    private companion object {
        const val Inventory = "component.inventory:items"
        const val Raft = "loc.lograft_waterfall_quest"
        const val River = "loc.waterfall_swim_point"
        const val Rock = "loc.crossing_rock_waterfall_quest"
        const val Tree = "loc.overhanging_tree1_waterfall_quest"
        const val LedgeDoor = "loc.waterfall_ledge_door"
        const val Barrel = "loc.barrel_waterfall_quest"

        const val HudonSearchRadius = 8
        const val RockRange = 10
        const val TreeRange = 3

        const val SwimSeq = "seq.human_swim"
        const val ThrowRopeSeq = "seq.human_throwrope"
        const val ClimbDownSeq = "seq.human_climbing_down"
        const val BarrelSeq = "seq.human_pickupfloor"
        const val SwimTicks = 4
        const val ClimbTicks = 2
        const val ClientCyclesPerTick = 30

        const val RiverSound = "synth.splash_and_river"
        const val CrashSound = "synth.watersplash"
        const val SwimSound = "synth.cf_swim"
        const val TieRopeSound = "synth.cf_tierope"
        const val RopeClimbSound = "synth.ropeclimb"
        const val DoorSound = "synth.door_open"
    }
}
