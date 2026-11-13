package org.rsmod.content.minigames.sepulchre

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.enums.HallowedEnums.hallowed_obelisk_gods
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.hook.ItemTeleportCaster
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.getInvObj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Hallowed Sepulchre's way in and out and its floor timer
 * (https://oldschool.runescape.wiki/w/Hallowed_Sepulchre). The lobby stairs and each floor's stairs
 * fade the player down to the next floor's start; the obelisks at the end of a floor and the
 * stairs at its start fade them back to the lobby, as does a crystal shard from anywhere. While on
 * a floor the timer counts down a tick at a time, pauses once the floor's last obstacle is crossed
 * and seals the stairs and coffins when it runs out.
 */
class SepulchreScript
@Inject
constructor(
    private val runs: SepulchreRuns,
    private val eventBus: EventBus,
    private val pets: PetRewards,
    private val teleports: ItemTeleportCaster,
) : PluginScript() {
    private lateinit var floors: SepulchreFloors

    override fun ScriptContext.startup() {
        floors = SepulchreFloors.load()
        onOpLoc1(MAUSOLEUM_DOOR) { enterLobby() }
        onOpLoc1(LOBBY_EXIT) { leaveLobby() }
        onOpLoc1(LOBBY_STAIRS) { lobbyStairs() }
        onOpLoc1(PROGRESS_STAIRS) { progressStairs() }
        onOpLoc1(OBELISK) { activateObelisk() }
        onOpLoc2(OBELISK) { leaveFloor(confirm = true, obelisk = true) }
        onOpLoc3(OBELISK) { leaveFloor(confirm = false, obelisk = true) }
        onOpLoc1(RETURN_STAIRS) { leaveFloor(confirm = true, obelisk = false) }
        onOpLoc2(RETURN_STAIRS) { leaveFloor(confirm = false, obelisk = false) }
        onOpHeld1(SHARD) { useShard() }
        onPlayerQueue(EXPIRY_QUEUE) {
            mes("<col=ef1020>You hear a loud rumbling noise as the door to the next floor closes.")
        }
        onPlayerQueue(BARRIER_QUEUE) {
            mes("<col=ef1020>You hear the sound of a magical barrier activating.")
        }
        onPlayerLogin { runs.trackLogin(player) }
        onEvent<GameLifecycle.StartCycle> { runs.players(RunPhase.Start).forEach(::tickFloor) }
        onEvent<GameLifecycle.LateCycle> { runs.players(RunPhase.Late).forEach(::checkLeft) }
    }

    private suspend fun ProtectedAccess.enterLobby() {
        arriveDelay()
        if (!QuestRequirements.hasCompleted(player, SINS_OF_THE_FATHER)) {
            // UNVERIFIED: the wording; Darkmeyer itself is only open after Sins of the Father.
            mes("You need to complete Sins of the Father to enter the Hallowed Sepulchre.")
            return
        }
        fadeAcross(LOBBY_ARRIVAL)
    }

    // UNVERIFIED: no capture leaves the lobby this way; it mirrors the way in.
    private suspend fun ProtectedAccess.leaveLobby() {
        arriveDelay()
        fadeAcross(MAUSOLEUM_OUTSIDE)
    }

    private suspend fun ProtectedAccess.fadeAcross(dest: CoordGrid) {
        fadeOverlay(0, OPAQUE, 0, CLEAR, FADE_CYCLES)
        delay(2)
        telejump(dest)
        delay(1)
        fadeOverlay(0, CLEAR, 0, OPAQUE, FADE_CYCLES)
        delay(2)
        ifCloseSub(FADE)
    }

    private suspend fun ProtectedAccess.lobbyStairs() {
        arriveDelay()
        if (!player.tutorialDone) {
            startDialogue {
                chatNpcSpecific(
                    STRANGER_NAME,
                    STRANGER,
                    shifty,
                    "I wouldn't advise going down there before you know the risks, adventurer.",
                )
            }
            return
        }
        val first = floors.get(1)
        if (stat(AGILITY) < first.level) {
            // UNVERIFIED: the wording.
            mes("You need an Agility level of ${first.level} to enter the Hallowed Sepulchre.")
            return
        }
        if (!player.skipEntryWarning && !confirmEntry()) {
            return
        }
        descend(first, fromLobby = true)
    }

    private suspend fun ProtectedAccess.confirmEntry(): Boolean {
        mesbox(
            "You are about to enter the Hallowed Sepulchre. Dying during this activity will not " +
                "be considered a safe death. Are you sure you wish to enter?"
        )
        val answer =
            choice3(
                "Yes.",
                ENTER,
                "Yes, and don't ask again.",
                ENTER_ALWAYS,
                "No.",
                STAY,
                title = "Are you sure you wish to enter?",
            )
        if (answer == ENTER_ALWAYS) {
            player.skipEntryWarning = true
        }
        return answer != STAY
    }

    private suspend fun ProtectedAccess.progressStairs() {
        val floor = floors.at(coords) ?: return
        val next = floors.next(floor) ?: return
        arriveDelay()
        if (SepulchreClock.isExpired(player.timeRemaining)) {
            return
        }
        if (stat(AGILITY) < next.level) {
            // UNVERIFIED: the wording.
            mes("You need an Agility level of ${next.level} to continue to the next floor.")
            return
        }
        complete(floor)
        descend(next, fromLobby = false)
    }

    /**
     * Fades the player onto [floor]'s centre and then one of its starts. The timer starts with the
     * overlay two ticks after the arrival, counting from the tick after.
     */
    private suspend fun ProtectedAccess.descend(floor: SepulchreFloor, fromLobby: Boolean) {
        player.busy = true
        player.currentFloor = 0
        try {
            ifCloseSub(OVERLAY)
            fadeOverlay(0, OPAQUE, 0, CLEAR, FADE_CYCLES)
            delay(1)
            minimapHideMap()
            delay(1)
            telejump(floor.centre)
            delay(1)
            val start = random.of(0, floor.starts.lastIndex)
            player.startIndex = start
            telejump(floor.starts[start])
            delay(1)
            arrive(floor, fromLobby)
            delay(2)
            ifCloseSub(FADE)
            ifOpenOverlay(OVERLAY, OVERLAY_TARGET)
            player.currentFloor = floor.number
        } finally {
            player.busy = false
        }
    }

    private fun ProtectedAccess.arrive(floor: SepulchreFloor, fromLobby: Boolean) {
        for (varbit in FLOOR_STATE_VARBITS) {
            vars[varbit] = 0
        }
        for (varbit in floor.encounters) {
            vars[varbit] = 1
        }
        if (floor.encounterChoices.isNotEmpty()) {
            vars[random.pick(floor.encounterChoices)] = 1
        }
        if (fromLobby) {
            mes("You venture down into the Hallowed Sepulchre.")
            player.timeRemaining = SepulchreClock.FIRST_FLOOR
            player.timeSpent = 0
            player.floorTimeSpent = 0
        } else {
            mes("You venture further down into the Hallowed Sepulchre.")
            player.timeRemaining = SepulchreClock.descend(player.timeRemaining)
        }
        player.timerPaused = false
        player.checkpoint = 0
        fadeOverlay(0, CLEAR, 0, OPAQUE, FADE_CYCLES)
        minimapReset()
    }

    /**
     * Counts [floor] as completed and pays its xp. The floor time counts towards the personal
     * best only while time remains.
     */
    private fun ProtectedAccess.complete(floor: SepulchreFloor) {
        val count = vars[floor.completions] + 1
        VarPlayerIntMapSetter.set(player, floor.completions, count)
        mes(
            "You have completed Floor ${floor.number} of the Hallowed Sepulchre! " +
                "Total completions: <col=ff0000>$count</col>."
        )
        if (!SepulchreClock.isExpired(player.timeRemaining)) {
            recordTime(floor)
        }
        player.floorTimeSpent = 0
        statAdvance(AGILITY, floor.xpTenths / XP_TENTHS)
    }

    private fun ProtectedAccess.recordTime(floor: SepulchreFloor) {
        val time = SepulchreClock.storedTime(player.floorTimeSpent)
        val best = vars[floor.pb]
        val shown = SepulchreClock.format(time)
        if (best == 0 || time < best) {
            vars[floor.pb] = time
            mes("Floor ${floor.number} time: <col=ff0000>$shown</col> (new personal best)")
        } else {
            val bestShown = SepulchreClock.format(best)
            mes("Floor ${floor.number} time: <col=ff0000>$shown</col>. Personal best: $bestShown")
        }
        if (floor.isLast) {
            val total = player.timeSpent
            // UNVERIFIED: that the whole-run best is the run's time spent on reaching the end.
            if (player.allFloorsBest == 0 || total < player.allFloorsBest) {
                player.allFloorsBest = total
            }
        }
    }

    private suspend fun ProtectedAccess.activateObelisk() {
        arriveDelay()
        anim(PRAY)
        delay(1)
        val loyalty = loyalty()
        if (loyalty == ANOTHER_GOD) {
            queueHit(delay = 1, type = HitType.Typeless, damage = OBELISK_DAMAGE)
        }
        delay(1)
        when (loyalty) {
            SARADOMIN -> {
                player.runEnergy = Constants.run_max_energy
                mes(
                    "<col=229628>The obelisk senses your loyalty to Saradomin and fully restores " +
                        "your energy."
                )
                spotanim(OBELISK_SPOTANIM, height = OBELISK_SPOTANIM_HEIGHT)
                soundSynth(OBELISK_SYNTH)
            }
            ANOTHER_GOD -> {
                // UNVERIFIED: how much energy is taken; the capture holds no energy update.
                player.runEnergy = (player.runEnergy - OBELISK_DRAIN).coerceAtLeast(0)
                mes(
                    "<col=e00a19>The obelisk senses your loyalty to another god and steals some " +
                        "of your energy from you."
                )
                spotanim(DRAIN_SPOTANIM, height = DRAIN_SPOTANIM_HEIGHT)
                soundSynth(DRAIN_SYNTH)
                soundSynth(DRAIN_HIT_SYNTH, delay = DRAIN_HIT_SYNTH_DELAY)
            }
            else -> mes("The obelisk doesn't respond to your lack of faith.")
        }
    }

    // UNVERIFIED: that another god's item outweighs a Saradomin one worn with it.
    private fun ProtectedAccess.loyalty(): Int? {
        val gods = worn.mapNotNull { obj -> obj?.let { hallowed_obelisk_gods[getInvObj(it)] } }
        return when {
            ANOTHER_GOD in gods -> ANOTHER_GOD
            SARADOMIN in gods -> SARADOMIN
            else -> null
        }
    }

    private suspend fun ProtectedAccess.useShard() {
        with(teleports) {
            if (!canTeleport(TeleportType.Standard)) {
                return
            }
        }
        if (invDel(inv, SHARD, 1).failure) {
            return
        }
        anim(CAST_TELEPORT)
        spotanim(TELEPORT_CASTING, height = TELEPORT_CASTING_HEIGHT)
        soundSynth(TELEPORT_SYNTH)
        delay(SHARD_TELEPORT_TICKS)
        telejump(SHARD_ARRIVAL)
        resetAnim()
    }

    private suspend fun ProtectedAccess.leaveFloor(confirm: Boolean, obelisk: Boolean) {
        val floor = floors.at(coords) ?: return
        arriveDelay()
        if (confirm && !confirmLeave()) {
            return
        }
        player.busy = true
        player.currentFloor = 0
        try {
            if (obelisk && player.timerPaused) {
                complete(floor)
                pets.rollSkillingPet(player, GIANT_SQUIRREL, AGILITY, floor.petChance)
            }
            ifCloseSub(OVERLAY)
            fadeOverlay(0, OPAQUE, 0, CLEAR, FADE_CYCLES)
            delay(1)
            minimapHideMap()
            delay(1)
            if (obelisk) {
                mes("The obelisk teleports you back to the lobby of the Hallowed Sepulchre.")
            }
            resetFloorState(player)
            telejump(LOBBY_RETURN)
            soundSynth(OBELISK_SYNTH)
            soundSynth(TELEPORT_SYNTH)
            delay(1)
            fadeOverlay(0, CLEAR, 0, OPAQUE, FADE_CYCLES)
            minimapReset()
            delay(2)
            ifCloseSub(FADE)
        } finally {
            player.busy = false
        }
    }

    private suspend fun ProtectedAccess.confirmLeave(): Boolean =
        choice2("Yes.", true, "No.", false, title = "Are you sure you wish to leave?")

    private fun tickFloor(player: Player) {
        val number = player.currentFloor
        if (number == 0) {
            return
        }
        val floor = floors.get(number)
        val reached = floor.checkpoints.indexOfFirst { it.contains(player.coords) }
        if (reached >= 0) {
            player.checkpoint = reached + 1
        }
        tickTimer(player, floor)
    }

    private fun checkLeft(player: Player) {
        val number = player.currentFloor
        if (number == 0 || floors.get(number).contains(player.coords)) {
            return
        }
        val timeLeft = !SepulchreClock.isExpired(player.timeRemaining)
        resetFloorState(player)
        runs.release(player)
        player.ifCloseOverlay(OVERLAY, eventBus)
        if (timeLeft) {
            // UNVERIFIED: that floor 5 says the same; the captured teleports leave earlier floors.
            player.queue(EXPIRY_QUEUE, 1)
        }
    }

    private fun tickTimer(player: Player, floor: SepulchreFloor) {
        if (player.timerPaused) {
            return
        }
        val remaining = player.timeRemaining
        if (SepulchreClock.isExpired(remaining)) {
            return
        }
        val left = remaining - 1
        player.timeRemaining = left
        player.timeSpent++
        player.floorTimeSpent++
        player.runClientScript(TIMER_UPDATE.asRSCM(RSCMType.CLIENTSCRIPT), left)
        SepulchreClock.warning(remaining)?.let {
            player.mes("<col=ef1020>Time remaining: $it", ChatType.GameMessage)
        }
        if (SepulchreClock.isExpired(left)) {
            player.ifCloseOverlay(OVERLAY, eventBus)
            player.strongQueue(if (floor.isLast) BARRIER_QUEUE else EXPIRY_QUEUE, 1)
        }
    }

    private companion object {
        const val MAUSOLEUM_DOOR = "loc.sepulchre_ow_entrance"
        const val LOBBY_EXIT = "loc.sepulchre_lobby_exit_3x3"
        const val LOBBY_STAIRS = "loc.hallowed_minigame_start"
        const val PROGRESS_STAIRS = "loc.hallowed_progress_stairs"
        const val OBELISK = "loc.hallowed_exit"
        const val RETURN_STAIRS = "loc.hallowed_stairs_return"
        const val STRANGER = "npc.hallowed_lobby_npc"
        const val STRANGER_NAME = "Mysterious Stranger"
        const val SINS_OF_THE_FATHER = "quest_sinsofthefather"
        const val GIANT_SQUIRREL = "obj.skillpetagility"
        const val FADE = "interface.fade_overlay"
        const val PRAY = "seq.human_pray"
        const val OBELISK_SPOTANIM = "spotanim.teleport_monastery"
        const val OBELISK_SPOTANIM_HEIGHT = 92
        const val OBELISK_SYNTH = "synth.found_gem"
        const val TELEPORT_SYNTH = "synth.teleport_all"
        const val DRAIN_SPOTANIM = "spotanim.saradomin_lightning"
        const val DRAIN_SPOTANIM_HEIGHT = 128
        const val DRAIN_SYNTH = "synth.saradomin_strike"
        const val DRAIN_HIT_SYNTH = "synth.human_hit_3"
        const val DRAIN_HIT_SYNTH_DELAY = 20
        const val OBELISK_DAMAGE = 1
        const val OBELISK_DRAIN = 1000
        const val SARADOMIN = 1
        const val ANOTHER_GOD = 2
        const val SHARD = "obj.hallowed_teleport"
        const val CAST_TELEPORT = "seq.human_castteleport"
        const val TELEPORT_CASTING = "spotanim.teleport_casting"
        const val TELEPORT_CASTING_HEIGHT = 92
        const val SHARD_TELEPORT_TICKS = 3
        const val EXPIRY_QUEUE = "queue.hallowed_timer_expired"
        const val BARRIER_QUEUE = "queue.hallowed_barrier_activated"
        const val OPAQUE = 255
        const val CLEAR = 0
        const val FADE_CYCLES = 50
        const val XP_TENTHS = 10.0
        const val ENTER = 1
        const val ENTER_ALWAYS = 2
        const val STAY = 3

        val LOBBY_ARRIVAL = CoordGrid(2400, 5969, 0)
        val LOBBY_RETURN = CoordGrid(2400, 5982, 0)
        val SHARD_ARRIVAL = CoordGrid(2401, 5981, 0)
        val MAUSOLEUM_OUTSIDE = CoordGrid(3654, 3384, 0)
    }
}

internal fun resetFloorState(player: Player) {
    player.currentFloor = 0
    player.timeRemaining = 0
    player.timeSpent = 0
    player.floorTimeSpent = 0
    player.timerPaused = false
    player.checkpoint = 0
    player.telepadImmunity = 0
    for (varbit in FLOOR_STATE_VARBITS) {
        VarPlayerIntMapSetter.set(player, varbit, 0)
    }
}
