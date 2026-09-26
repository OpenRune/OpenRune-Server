package org.rsmod.content.bosses.barrows

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.random.Random
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.rollCount
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invClear
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BarrowsScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val objRepo: ObjRepository,
    private val charges: ObjChargeManager,
    private val spawner: BarrowsNpcSpawner,
    private val doors: BarrowsDoors,
    private val dropRegistry: DropTableRegistry,
) : PluginScript() {
    private val puzzles = mutableMapOf<Int, BarrowsPuzzle>()

    override fun ScriptContext.startup() {
        onArea("area.barrows") { enterSurface() }
        onAreaExit("area.barrows") { leaveSurface() }
        onArea("area.barrows_crypt") { enterCrypt() }
        onAreaExit("area.barrows_crypt") { leaveCrypt() }
        onPlayerLogout {
            spawner.despawnAll(player)
            puzzles.remove(player.uid.packed)
            deliverRewards(player)
        }
        onIfClose(REWARD_INTERFACE) { deliverRewards(player) }

        onOpHeld1("obj.spade") { dig() }
        onOpHeld4(LOCKPICK) { inspectLockpick(it.slot) }
        onOpHeld4(LOCKPICK_FULL) { inspectLockpick(it.slot) }
        onPlayerQueueWithArgs<BarrowsBrother?>(DIG_QUEUE) { finishDig(it.args) }

        for (brother in BarrowsBrother.entries) {
            onOpLoc1(brother.sarcophagus) { searchSarcophagus(brother) }
            onOpLoc1(brother.staircase) {
                telejump(spawner.randomWalkableNear(brother.moundCenter, MOUND_RADIUS))
            }
        }
        onOpLoc1("loc.barrows_ladder") { telejump(player.hiddenBrother.bySarcophagus) }
        onOpLoc1("loc.barrows_door_unlocked_r") { openDoor(it.loc) }
        onOpLoc1("loc.barrows_door_unlocked_l") { openDoor(it.loc) }
        onOpLoc1("loc.barrows_stone_chest_closed") { openChest() }
        onOpLoc1("loc.barrows_stone_chest_open") { searchChest() }
        onOpLoc2("loc.barrows_stone_chest_open") { player.chestOpen = false }

        onPlayerTimer(CRYPT_TIMER) { cryptTick() }

        onIfModalButton("component.barrows_puzzle:a") { selectPuzzleOption(0) }
        onIfModalButton("component.barrows_puzzle:b") { selectPuzzleOption(1) }
        onIfModalButton("component.barrows_puzzle:c") { selectPuzzleOption(2) }
        onIfModalButton("component.barrows_reward:items") {
            if (it.op == IfButtonOp.Op10) objExamine(player.invMap.getOrPut(REWARD_INV), it.comsub)
        }
    }

    private fun ProtectedAccess.enterSurface() {
        if (!player.barrowsRolled || player.barrowsLooted) {
            player.resetBarrows()
        }
        player.ifOpenOverlay(OVERLAY, OVERLAY_TARGET, eventBus)
    }

    private fun ProtectedAccess.leaveSurface() {
        if (player.inBarrowsCrypt()) return
        player.ifCloseOverlay(OVERLAY, eventBus)
    }

    private fun ProtectedAccess.enterCrypt() {
        if (!player.barrowsLooted) {
            player.ifOpenOverlay(OVERLAY, OVERLAY_TARGET, eventBus)
        } else if (cameraEffectsEnabled()) {
            camShake(CamShakeAxis.LEFT_RIGHT, 5, 0, 0)
        }
        player.refreshDoors(hasLockpick())
        minimapHideMap()
        player.refreshLadder()
        player.clearTimer(CRYPT_TIMER)
        timer(CRYPT_TIMER, CRYPT_TICK)
    }

    private fun ProtectedAccess.leaveCrypt() {
        minimapReset()
        camReset()
        player.clearTimer(CRYPT_TIMER)
        spawner.despawnAll(player)
        if (!player.inBarrowsSurface()) {
            player.ifCloseOverlay(OVERLAY, eventBus)
        }
    }

    private fun ProtectedAccess.dig() {
        anim("seq.human_dig")
        soundSynth("synth.digspade")
        strongQueue(DIG_QUEUE, DIG_TICKS, args = BarrowsBrother.entries.firstOrNull { inMound(it) })
    }

    private fun ProtectedAccess.finishDig(mound: BarrowsBrother?) {
        resetAnim()
        if (mound == null) {
            mes("You dig a hole in the ground... but find nothing.")
            return
        }
        spam("You've broken into a crypt!")
        telejump(mound.chamber)
    }

    private fun ProtectedAccess.inMound(brother: BarrowsBrother): Boolean {
        val centre = brother.moundCenter
        return player.coords.level == 0 &&
            abs(player.coords.x - centre.x) <= MOUND_RADIUS &&
            abs(player.coords.z - centre.z) <= MOUND_RADIUS
    }

    private suspend fun ProtectedAccess.searchSarcophagus(brother: BarrowsBrother) {
        if (spawner.currentBrother(player) == null) {
            if (brother == player.hiddenBrother) {
                enterTunnel()
                return
            }
            if (!player.isSlain(brother)) {
                spawner.spawnBrother(player, brother, "You dare disturb my rest!")
            }
        }
        spam("You don't find anything.")
    }

    private suspend fun ProtectedAccess.enterTunnel() {
        var enter = false
        startDialogue {
            mesbox("You've found a hidden tunnel, do you want to enter?")
            enter = choice2("Yes, I'm fearless.", true, "No way, that looks scary!", false)
        }
        if (!enter) return
        telejump(spawner.randomWalkableNear(player.corner.ladder, LANDING_RADIUS))
        player.refreshDoors(hasLockpick())
    }

    private fun ProtectedAccess.openDoor(loc: BoundLocInfo) {
        val here = player.coords
        if (here in PUZZLE_DOOR_TILES && !player.puzzleSolved) {
            spam("The door is locked with a strange puzzle.")
            openPuzzle()
            return
        }
        val doorway = BarrowsDoorway.forLoc(loc.id)
        if (doorway != null && doorway in player.shutDoorways && !useLockpick()) {
            player.refreshDoors(false)
            return
        }
        doors.swingOpen(loc)
        teleport(crossingTile(here, loc.coords, loc.angleId))
        spawnFromDoor()
    }

    private fun ProtectedAccess.spawnFromDoor() {
        if (spawner.owned(player).count() >= MAX_ROOM_ENEMIES) return
        val alive = player.aliveBrothers
        val brotherRoll = player.barrowsLooted || Random.nextInt(DOOR_ROLL) < DOOR_BROTHER_WEIGHT
        if (brotherRoll && alive.isNotEmpty() && spawner.currentBrother(player) == null) {
            spawner.spawnBrother(player, alive.random(), null)
            return
        }
        val roll = Random.nextInt(DOOR_SKELETON_WEIGHT + DOOR_BLOODWORM_WEIGHT + DOOR_RAT_WEIGHT)
        val monster =
            when {
                roll < DOOR_SKELETON_WEIGHT -> SKELETONS.random()
                roll < DOOR_SKELETON_WEIGHT + DOOR_BLOODWORM_WEIGHT -> BLOODWORM
                else -> CRYPT_RAT
            }
        spawner.spawnMonster(player, monster)
    }

    private fun crossingTile(player: CoordGrid, door: CoordGrid, angle: Int): CoordGrid =
        when (angle) {
            0 -> CoordGrid(if (player.x < door.x) door.x else door.x - 1, player.z, player.level)
            2 -> CoordGrid(if (player.x <= door.x) door.x + 1 else door.x, player.z, player.level)
            1 -> CoordGrid(player.x, if (player.z <= door.z) door.z + 1 else door.z, player.level)
            else -> CoordGrid(player.x, if (player.z < door.z) door.z else door.z - 1, player.level)
        }

    private fun ProtectedAccess.hasLockpick(): Boolean =
        player.inv.any { it.isType(LOCKPICK) || it.isType(LOCKPICK_FULL) }

    private fun ProtectedAccess.useLockpick(): Boolean {
        val slot = player.inv.objs.indexOfFirst { it.isType(LOCKPICK) || it.isType(LOCKPICK_FULL) }
        if (slot == -1) return false
        val obj = player.inv[slot] ?: return false
        val remaining =
            if (obj.isType(LOCKPICK_FULL)) LOCKPICK_CHARGES - 1
            else charges.getCharges(obj, CHARGES_VAROBJ) - 1
        if (remaining <= 0) {
            invDel(inv, LOCKPICK, 1, slot = slot)
            mes("Your Strange old lockpick crumbles to dust.")
        } else {
            val used = ServerCacheManager.getItem(LOCKPICK.asRSCM(RSCMType.OBJ)) ?: return true
            invReplaceSlot(inv, slot, 1, used, vars = remaining)
        }
        return true
    }

    private fun ProtectedAccess.inspectLockpick(slot: Int) {
        val obj = player.inv[slot] ?: return
        val remaining =
            if (obj.isType(LOCKPICK_FULL)) LOCKPICK_CHARGES else charges.getCharges(obj, CHARGES_VAROBJ)
        val noun = if (remaining == 1) "charge" else "charges"
        mes("Your Strange old lockpick has $remaining $noun left.")
    }

    private fun ProtectedAccess.openChest() {
        player.chestOpen = true
        val hidden = player.hiddenBrother
        if (player.barrowsLooted || player.isSlain(hidden)) return
        val current = spawner.currentBrother(player)
        if (current == null) {
            spawner.spawnBrother(player, hidden, "You dare steal from us!")
        }
    }

    private fun ProtectedAccess.searchChest() {
        if (player.barrowsLooted) {
            mes("The chest is empty!")
            return
        }
        val drops =
            when (val result = dropRegistry.forLoc(BARROWS_CHEST)?.roll(player, ArgMap())?.flatten()) {
                is RollResult.Single -> listOf(result.result)
                is RollResult.ListOf -> result.results
                else -> emptyList()
            }
        player.barrowsLooted = true
        player.chestCount += 1
        mes("Your Barrows chest count is: <col=ff0000>${player.chestCount}</col>.")

        val rewardInv = player.invMap.getOrPut(REWARD_INV)
        invClear(rewardInv)
        for (drop in drops) {
            if (drop.isNothing || !drop.condition(player)) continue
            invAdd(rewardInv, drop.transformObj(player) ?: drop.obj, drop.rollCount(random))
        }
        invTransmit(rewardInv)
        player.midiJingle(CHEST_JINGLE)
        ifOpenMain(REWARD_INTERFACE)
        ifSetEvents("component.barrows_reward:items", 0 until rewardInv.size, IfEvent.Op10)
        if (cameraEffectsEnabled()) {
            camShake(CamShakeAxis.LEFT_RIGHT, 5, 0, 0)
        }
        player.ifCloseOverlay(OVERLAY, eventBus)
    }

    private fun deliverRewards(player: Player) {
        val rewardInv = player.invMap[REWARD_INV] ?: return
        for (obj in rewardInv.objs.filterNotNull()) {
            val name = RSCM.getReverseMapping(RSCMType.OBJ, obj.id)
            CollectionLog.grant(player, name, obj.count)
            player.invAddOrDrop(objRepo, name, obj.count)
        }
        player.invClear(rewardInv)
        player.stopInvTransmit(rewardInv)
    }

    private fun ProtectedAccess.openPuzzle() {
        val puzzle = BarrowsPuzzle.random()
        puzzles[player.uid.packed] = puzzle
        ifOpenMain(PUZZLE_INTERFACE)
        for (i in 0 until 3) {
            ifSetModel("component.barrows_puzzle:${i + 1}", puzzle.type.sequence[i])
            ifSetModel("component.barrows_puzzle:pic_${"abc"[i]}", puzzle.options[i])
        }
    }

    private fun ProtectedAccess.selectPuzzleOption(slot: Int) {
        val puzzle = puzzles.remove(player.uid.packed) ?: return
        if (slot == puzzle.correctSlot) {
            mes("You hear the doors' locking mechanism grind open.")
            player.puzzleSolved = true
        } else {
            mes("You hear the doors' locking mechanism shut down.")
            player.corner = BarrowsCorner.entries.filter { it != player.corner }.random()
            player.shiftDoorways()
            player.refreshDoors(hasLockpick())
        }
        ifClose()
    }

    private fun ProtectedAccess.cryptTick() {
        timer(CRYPT_TIMER, CRYPT_TICK)
        if (player.barrowsLooted) {
            if (player.coords.level == 0 && player.ui.modals.isEmpty()) {
                spotanim("spotanim.rockfall")
                takeInstantHit(HitType.Typeless, Random.nextInt(0, ROCKFALL_MAX_HIT + 1))
            }
            return
        }
        val slain = player.slainBrothers
        if (!wieldingGhommalsHilt(player)) {
            statSub("stat.prayer", constant = PRAYER_DRAIN + slain.size, percent = 0)
            mes("<col=ff3045>Your prayers have been drained!</col>", ChatType.GameMessage)
        }
        val brother = slain.randomOrNull() ?: return
        val model = if (player.coords.level == 0) brother.tunnelNodModel else brother.cryptNodModel
        val head = "component.barrows_overlay:${brother.name.lowercase()}".asRSCM(RSCMType.COMPONENT)
        runClientScript(NOD_SCRIPT, head, model)
    }

    private fun wieldingGhommalsHilt(player: Player): Boolean {
        val shield = player.lefthand ?: return false
        return GHOMMALS_HILTS.any { shield.isType(it) }
    }

    private fun ProtectedAccess.cameraEffectsEnabled(): Boolean =
        player.vars["varbit.option_camera_effect_barrows_disabled"] == 0

    private companion object {
        const val OVERLAY = "interface.barrows_overlay"
        const val OVERLAY_TARGET = "component.toplevel_osrs_stretch:overlay_atmosphere"
        const val REWARD_INTERFACE = "interface.barrows_reward"
        const val PUZZLE_INTERFACE = "interface.barrows_puzzle"
        const val REWARD_INV = "inv.trail_rewardinv"
        const val CRYPT_TIMER = "timer.barrows_crypt"
        const val CRYPT_TICK = 30
        const val NOD_SCRIPT = 894
        const val PRAYER_DRAIN = 8
        const val ROCKFALL_MAX_HIT = 6
        const val MOUND_RADIUS = 3
        const val DIG_QUEUE = "queue.barrows_dig"
        const val DIG_TICKS = 2
        const val LANDING_RADIUS = 3
        const val CHEST_JINGLE = 77
        const val MAX_ROOM_ENEMIES = 11
        const val DOOR_ROLL = 128
        const val DOOR_BROTHER_WEIGHT = 12
        const val DOOR_SKELETON_WEIGHT = 52
        const val DOOR_BLOODWORM_WEIGHT = 32
        const val DOOR_RAT_WEIGHT = 32
        const val LOCKPICK = "obj.strange_old_lockpick"
        const val LOCKPICK_FULL = "obj.strange_old_lockpick_full"
        const val LOCKPICK_CHARGES = 50
        const val CHARGES_VAROBJ = "varobj.charges_16383"

        val GHOMMALS_HILTS =
            listOf(
                "obj.ca_offhand_medium",
                "obj.ca_offhand_hard",
                "obj.ca_offhand_elite",
                "obj.ca_offhand_master",
                "obj.ca_offhand_grandmaster",
                "obj.infernal_defender_ghommal_5",
                "obj.infernal_defender_ghommal_5_trouver",
                "obj.infernal_defender_ghommal_6",
                "obj.infernal_defender_ghommal_6_trouver",
            )

        val PUZZLE_DOOR_TILES =
            setOf(
                CoordGrid(3551, 9683, 0),
                CoordGrid(3552, 9683, 0),
                CoordGrid(3540, 9695, 0),
                CoordGrid(3540, 9694, 0),
                CoordGrid(3552, 9706, 0),
                CoordGrid(3551, 9706, 0),
                CoordGrid(3563, 9694, 0),
                CoordGrid(3563, 9695, 0),
            )
    }
}
