package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * A post-interaction 1-tick timer does the work until the player moves or clicks another station.
 * Same-tick re-clicks arm the sweet spot, miss an overworked action, or restart the heat ramp
 * before that tick's work is consumed.
 */
class RefiningScript
@Inject
constructor(
    private val random: GameRandom,
    private val clock: MapClock,
    private val worldRepo: WorldRepository,
    private val locRepo: LocRepository,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (tool in FoundryTool.entries) {
            onOpLoc1(tool.row.loc.internalName) { startTool(tool) }
        }
        onOpLoc1(LAVA_POOL) { startHeat(sign = 1, fast = false) }
        onOpLoc2(LAVA_POOL) { startHeat(sign = 1, fast = true) }
        onOpLoc1(WATERFALL) { startHeat(sign = -1, fast = false) }
        onOpLoc2(WATERFALL) { startHeat(sign = -1, fast = true) }
        onPlayerSoftTimer(WORK_TIMER) { tick(player) }
        onPlayerSoftTimer(SWEETSPOT_TIMER) { sweetSpotTick(player) }
        onPlayerSoftTimer(PREFORM_COOLING_TIMER) { passiveCooling(player) }
        onEvent<GameLifecycle.StartCycle> { foundryCycle() }
    }

    /**
     * The trip hammer swings on a world-wide cycle whether anyone works it or not; hammer actions
     * land on its swings. The foundry's ambient sounds play on their own cycle.
     */
    private fun foundryCycle() {
        if (clock.cycle % AMBIENT_PERIOD == 0) {
            worldRepo.soundArea(AMBIENT_1_COORD, AMBIENT_1_SYNTH, radius = AMBIENT_RADIUS)
            worldRepo.soundArea(AMBIENT_2_COORD, AMBIENT_2_SYNTH, radius = AMBIENT_RADIUS)
        }
        if (isHammerCycle(clock.cycle)) {
            for (coords in TRIP_HAMMER_COORDS) {
                locRepo.findExact(coords, LocShape.CentrepieceStraight)?.let {
                    worldRepo.locAnim(it, TRIP_HAMMER_SEQ)
                }
            }
            worldRepo.soundArea(TRIP_HAMMER_SOUND_COORD, TRIP_HAMMER_LOOP_SYNTH, radius = 15)
        }
    }

    private fun isHammerCycle(cycle: Int): Boolean =
        cycle % FoundryTool.Hammer.row.period == HAMMER_PHASE

    private suspend fun ProtectedAccess.canWork(): Boolean {
        if (!player.holdsPreform()) {
            // UNVERIFIED: the tools show the waterfall's box too (guess).
            mesbox(NEEDS_PREFORM)
            return false
        }
        if (player.preformQuality == 0) {
            mes(DAMAGED)
            return false
        }
        if (player.preformCompletion >= FoundryFormulas.SCALE) {
            if (player.makingQuestSword()) {
                questSwordFinished()
            } else {
                // UNVERIFIED: a commission sword gives the line shown when it was finished (guess).
                mes(FINISHED)
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.isWorking(station: Int): Boolean =
        player.workStation == station && player.workOrigin == player.coords

    private fun Player.workTool(): FoundryTool? =
        workStation.takeIf { it in 1 until LAVA_STATION }?.let { FoundryTool.forSeed(it - 1) }

    private suspend fun ProtectedAccess.startTool(tool: FoundryTool) {
        if (!canWork()) {
            return
        }
        val station = tool.row.seed + 1
        if (isWorking(station)) {
            onReclick()
            return
        }
        begin(station)
        player.workNextAction = firstAction(tool)
        player.workOverworked = false
        anim(tool.row.startSeq.internalName)
        if (player.sweetSpotUntil != 0) {
            player.workSweetSpotArmed = true
            player.workSweetSpotVoid = true
            player.showSweetSpot(SWEETSPOT_ARMED)
        }
    }

    private fun firstAction(tool: FoundryTool): Int {
        if (tool != FoundryTool.Hammer) {
            return clock.cycle + FIRST_ACTION_DELAY
        }
        var cycle = clock.cycle + 1
        while (!isHammerCycle(cycle)) {
            cycle++
        }
        return cycle
    }

    private suspend fun ProtectedAccess.startHeat(sign: Int, fast: Boolean) {
        arriveDelay()
        if (!canWork()) {
            return
        }
        val station = (if (sign > 0) LAVA_STATION else WATERFALL_STATION) + if (fast) 1 else 0
        if (isWorking(station)) {
            // The click's own step restarts at seven (slow) or the fast ramp's first step.
            // Live omits the animation only on this tick, then resumes it on the next step.
            player.workHeatTicks = if (fast) 0 else 1
            player.workNextAction = clock.cycle
            return
        }
        begin(station)
        player.workHeatTicks = 0
        player.workNextAction = -1
        heatTick(player)
    }

    private fun ProtectedAccess.begin(station: Int) {
        player.workStation = station
        player.workOrigin = player.coords
        softTimer(WORK_TIMER, 1, afterInteractions = true)
    }

    private fun stop(player: Player, resetAnim: Boolean = true) {
        player.workStation = NO_STATION
        player.clearSoftTimer(WORK_TIMER)
        if (resetAnim) {
            player.resetAnim()
        }
    }

    private fun tick(player: Player) {
        if (
            player.workStation == NO_STATION ||
                player.workOrigin != player.coords ||
                !player.holdsPreform()
        ) {
            stop(player)
            return
        }
        val tool = player.workTool()
        if (tool != null) {
            workTick(player, tool)
        } else {
            heatTick(player)
        }
    }

    private fun workTick(player: Player, tool: FoundryTool) {
        player.anim(tool.row.loopSeq.internalName)
        if (clock.cycle < player.workNextAction) {
            return
        }
        player.workNextAction = clock.cycle + tool.row.period
        if (player.preformQuality == 0) {
            player.mes(DAMAGED)
            stop(player)
            return
        }
        act(player, tool)
        if (player.preformCompletion >= FoundryFormulas.SCALE) {
            stop(player, resetAnim = false)
        }
    }

    /**
     * The slow ramp plays its anim and sound a tick before its first step; the fast one steps on
     * its first tick and starts the anim a tick later.
     */
    private fun heatTick(player: Player) {
        val heating = player.workStation < WATERFALL_STATION
        val fast = (player.workStation - LAVA_STATION) % 2 == 1
        val ticks = player.workHeatTicks
        player.workHeatTicks = (ticks + 1).coerceAtMost(MAX_HEAT_TICKS)
        if ((!fast || ticks > 0) && player.workNextAction != clock.cycle) {
            player.anim(if (heating) HEAT_SEQ else COOL_SEQ)
        }
        player.soundSynth(if (heating) LAVA_SYNTH else WATERFALL_SYNTH)
        val index = if (fast) ticks + FoundryFormulas.FAST_HEAT_OFFSET else ticks - 1
        if (index < 0) {
            return
        }
        val sign = if (heating) 1 else -1
        val temperature =
            (player.preformTemperature + sign * FoundryFormulas.heatStep(index))
                .coerceIn(0, FoundryFormulas.SCALE)
        player.preformTemperature = temperature
        if (temperature == 0 || temperature == FoundryFormulas.SCALE) {
            stop(player)
        }
    }

    private fun ProtectedAccess.onReclick() {
        if (player.workSweetSpotArmed) {
            return
        }
        if (player.sweetSpotUntil != 0) {
            player.workSweetSpotArmed = true
            player.showSweetSpot(SWEETSPOT_ARMED)
        } else {
            player.workOverworked = true
        }
    }

    /**
     * An offer clears 9 ticks after it shows unless it was armed. An armed one lasts while the
     * player keeps working and is lost a tick earlier once they walk away.
     */
    private fun sweetSpotTick(player: Player) {
        if (player.sweetSpotUntil == 0) {
            player.clearSoftTimer(SWEETSPOT_TIMER)
            return
        }
        val expired =
            if (player.workSweetSpotArmed) {
                player.workTool() == null && clock.cycle >= player.sweetSpotUntil
            } else {
                clock.cycle > player.sweetSpotUntil
            }
        if (expired) {
            clearSweetSpot(player)
            player.showSweetSpot(SWEETSPOT_NONE)
        }
    }

    private fun clearSweetSpot(player: Player) {
        player.sweetSpotUntil = 0
        player.workSweetSpotArmed = false
        player.workSweetSpotVoid = false
        player.clearSoftTimer(SWEETSPOT_TIMER)
    }

    private fun act(player: Player, tool: FoundryTool) {
        if (player.workOverworked) {
            player.workOverworked = false
            player.mes(OVERWORKED)
            return
        }
        val difficulty = player.preformDifficulty
        val completion = player.preformCompletion
        val section = FoundryFormulas.section(completion, difficulty)
        val needed = FoundryTool.forSeed(player.foundryVar(seedVarbit(section)))
        val window = FoundryFormulas.window(tool, difficulty)
        val mistake = tool != needed || player.preformTemperature !in window

        val armed = player.workSweetSpotArmed
        val bonus = armed && !player.workSweetSpotVoid
        val repairs = bonus && player.preformQuality < player.preformStartQuality
        if (armed) {
            clearSweetSpot(player)
        }
        if (bonus) {
            if (!mistake) {
                player.preformQuality =
                    (player.preformQuality + MISTAKE_QUALITY).coerceAtMost(player.preformStartQuality)
            }
            player.mes(if (repairs) REPAIRED else QUICK_REACTIONS)
        }
        var progress = 0
        var xpTenths = 0
        if (mistake) {
            player.preformQuality = (player.preformQuality - MISTAKE_QUALITY).coerceAtLeast(0)
            if (player.makingQuestSword()) {
                val prompt =
                    when {
                        tool != needed -> QuestPrompt.wrongTool(needed)
                        player.preformTemperature < window.first -> QuestPrompt.tooCold(tool)
                        else -> QuestPrompt.tooHot(tool)
                    }
                player.weakQueue(QUEST_PROMPT_QUEUE, 1, prompt)
                stop(player)
            }
        } else {
            progress = tool.row.progress + uniformBonus(player)
            xpTenths = tool.row.xpTenths
            if (bonus) {
                progress += SWEETSPOT_PROGRESS
                xpTenths *= 2
            }
        }
        player.preformCompletion = (completion + progress).coerceAtMost(FoundryFormulas.SCALE)
        val questFinish =
            player.preformCompletion >= FoundryFormulas.SCALE && player.makingQuestSword()
        if (player.preformCompletion >= FoundryFormulas.SCALE && !questFinish) {
            player.mes(FINISHED)
        }
        if (!questFinish) {
            player.preformTemperature =
                (player.preformTemperature + tool.row.heat).coerceIn(0, FoundryFormulas.SCALE)
        }
        if (xpTenths > 0) {
            player.statAdvance("stat.smithing", xpTenths / 10.0)
        }
        if (bonus) {
            player.soundSynth(SWEETSPOT_SYNTH)
        }
        when {
            mistake -> player.soundSynth(MISTAKE_SYNTH)
            !bonus -> player.soundSynth(tool.row.synth)
        }
        if (armed) {
            player.showSweetSpot(SWEETSPOT_NONE)
        }
        when {
            player.preformCompletion >= FoundryFormulas.SCALE -> swordFinished(player)
            !mistake -> maybeOfferSweetSpot(player, section, completion)
        }
    }

    private fun swordFinished(player: Player) {
        if (player.makingQuestSword()) {
            if (!protectedAccess.launch(player) { questSwordFinished() }) {
                player.weakQueue(QUEST_PROMPT_QUEUE, 1, QuestPrompt.SwordFinished)
            }
            return
        }
        player.resetAnim()
        player.anim(FINISH_SEQ)
        player.soundSynth(FINISH_SYNTH)
    }

    // Spots go to distinct random sections, each at a uniform action inside the section's window.
    // UNVERIFIED: the window's end at three quarters of the section and the slot size, fitted
    // to observed offers (plugin measurement).
    private fun maybeOfferSweetSpot(player: Player, section: Int, completion: Int) {
        if (player.sweetSpotUntil != 0) {
            return
        }
        val offered = player.sweetSpotsOffered
        val lastSection = player.sweetSpotsLastSection - 1
        val total = FoundryFormulas.sweetSpots(player.preformDifficulty)
        if (offered >= total || section <= lastSection) {
            return
        }
        val sections = FoundryFormulas.sections(player.preformDifficulty)
        val sectionStart = (section * FoundryFormulas.SCALE + sections - 1) / sections
        val offset = completion - sectionStart
        val windowEnd = FoundryFormulas.SCALE / sections * 3 / 4
        if (offset < SWEETSPOT_WINDOW_START || offset > windowEnd) {
            return
        }
        val needed = total - offered
        val left = sections - section
        val slots = (windowEnd - SWEETSPOT_WINDOW_START) / SWEETSPOT_SLOT + 1
        val passed = (offset - SWEETSPOT_WINDOW_START) / SWEETSPOT_SLOT
        val odds = (left * slots - passed * needed).coerceAtLeast(needed)
        if (random.of(1, odds) > needed) {
            return
        }
        player.sweetSpotsLastSection = section + 1
        player.sweetSpotsOffered = offered + 1
        player.sweetSpotUntil = clock.cycle + SWEETSPOT_TICKS
        player.softTimer(SWEETSPOT_TIMER, 1)
        player.showSweetSpot(SWEETSPOT_OFFERED)
    }

    private fun uniformBonus(player: Player): Int {
        val pieces =
            UNIFORM.count { (slot, objs) -> objs.any { player.worn[slot.slot]?.isType(it) == true } }
        return when {
            pieces == UNIFORM.size -> 1
            pieces > 0 && random.of(1, 5) <= pieces -> 1
            else -> 0
        }
    }

    private fun passiveCooling(player: Player) {
        if (!player.holdsPreform()) {
            player.clearSoftTimer(PREFORM_COOLING_TIMER)
            return
        }
        player.preformTemperature = (player.preformTemperature - 1).coerceAtLeast(0)
    }

    private companion object {
        const val WORK_TIMER = "timer.giants_foundry_work"
        const val SWEETSPOT_TIMER = "timer.giants_foundry_sweetspot"
        const val LAVA_POOL = "loc.giants_foundry_lava_pool"
        const val WATERFALL = "loc.giants_foundry_waterfall"
        const val FINISH_SEQ = "seq.giants_foundry_finish_sword"
        const val TRIP_HAMMER_SEQ = "seq.giants_foundry_hammer01"

        const val HEAT_SEQ = "seq.human_pickupfloor"
        const val COOL_SEQ = "seq.human_pickuptable"

        const val LAVA_SYNTH = "synth.giants_foundry_lava_pool"
        const val WATERFALL_SYNTH = "synth.giants_foundry_waterfall"
        const val MISTAKE_SYNTH = "synth.glassblowing"
        const val SWEETSPOT_SYNTH = "synth.cowbell_amulet_ring"
        const val FINISH_SYNTH = "synth.giants_foundry_sword_finished"
        const val TRIP_HAMMER_LOOP_SYNTH = "synth.giants_foundry_trip_hammer_loop"
        const val AMBIENT_1_SYNTH = "synth.giants_foundry_ambient_1"
        const val AMBIENT_2_SYNTH = "synth.giants_foundry_ambient_2"
        const val AMBIENT_PERIOD = 2
        const val AMBIENT_RADIUS = 12

        const val HAMMER_PHASE = 0
        const val FIRST_ACTION_DELAY = 3
        const val MAX_HEAT_TICKS = 255

        const val NO_STATION = 0
        const val LAVA_STATION = 4
        const val WATERFALL_STATION = 6

        const val MISTAKE_QUALITY = 5
        const val SWEETSPOT_PROGRESS = 50
        const val SWEETSPOT_TICKS = 8
        const val SWEETSPOT_WINDOW_START = 20
        const val SWEETSPOT_SLOT = 10

        const val SWEETSPOT_NONE = 0
        const val SWEETSPOT_OFFERED = 1
        const val SWEETSPOT_ARMED = 2

        const val DAMAGED =
            "<col=ff3045>This sword is too damaged to keep working. You should give it to Kovac " +
                "and try again.</col>"
        const val OVERWORKED =
            "<col=ff3045>Your attempt to overwork the preform causes you to miss the action " +
                "altogether."
        const val QUICK_REACTIONS =
            "<col=229628>Your quick reactions allow you to find the perfect position to work " +
                "the preform!"
        const val REPAIRED =
            "<col=229628>Your quick reactions allow you to repair some of the quality of your " +
                "preform!"
        const val NEEDS_PREFORM = "You need a giant sword preform equipped to use this tool."
        const val FINISHED =
            "<col=229628>You have successfully finished the sword. You should give it to Kovac " +
                "for a reward.</col>"

        val TRIP_HAMMER_COORDS = listOf(CoordGrid(3362, 11495, 0), CoordGrid(3363, 11496, 0))
        val TRIP_HAMMER_SOUND_COORD = CoordGrid(3367, 11497, 0)
        val AMBIENT_1_COORD = CoordGrid(3363, 11492, 0)
        val AMBIENT_2_COORD = CoordGrid(3364, 11485, 0)

        val UNIFORM =
            listOf(
                Wearpos.Torso to listOf("obj.smithing_uniform_torso"),
                Wearpos.Legs to listOf("obj.smithing_uniform_legs"),
                Wearpos.Feet to listOf("obj.smithing_uniform_boots"),
                Wearpos.Hands to
                    listOf("obj.smithing_uniform_gloves", "obj.smithing_uniform_gloves_ice"),
            )
    }
}
