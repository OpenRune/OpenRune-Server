package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Working the preform: the trip hammer, grindstone and polishing wheel, the lava pool and the
 * waterfall, sweet spots and passive cooling.
 * https://oldschool.runescape.wiki/w/Giants%27_Foundry#Refining_the_sword
 *
 * Clicking a station starts it; a 1-tick timer then does the work until the player moves or
 * clicks another station. Clicking the same station again is a re-click (sweet spot, overwork,
 * or restarting the lava/waterfall ramp), so the op handler never re-issues itself.
 */
class RefiningScript
@Inject
constructor(
    private val random: GameRandom,
    private val clock: MapClock,
    private val worldRepo: WorldRepository,
    private val locRepo: LocRepository,
) : PluginScript() {
    private val states = HashMap<Player, WorkState>()

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
        onPlayerLogout { states.remove(player) }
        onEvent<GameLifecycle.LateCycle> { foundryCycle() }
    }

    private class WorkState {
        /** What the player is working at: a tool, or a heat source and op. */
        var station: String? = null
        var tool: FoundryTool? = null
        var origin: CoordGrid? = null
        var heatSign = 0
        var heatFast = false
        var heatTicks = 0
        var nextAction = 0
        var sweetSpotUntil = 0
        var sweetSpotArmed = false

        /** Armed by the click that started the work: the first action pays no bonus. */
        var sweetSpotVoid = false
        var overworked = false
    }

    private fun stateOf(player: Player): WorkState = states.getOrPut(player) { WorkState() }

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

    private fun ProtectedAccess.canWork(): Boolean {
        if (!player.holdsPreform()) {
            // sigmazx's Sleeping Giants video (youtube.com/watch?v=WnGuNtTJW0M), using a tool.
            // UNVERIFIED: the lava pool and waterfall give the tools' message (guess).
            mes("You need a giant sword preform equipped to use this tool.")
            return false
        }
        if (player.preformQuality == 0) {
            mes(DAMAGED)
            return false
        }
        return true
    }

    private fun ProtectedAccess.isWorking(state: WorkState, station: String): Boolean =
        state.station == station && state.origin == player.coords

    private suspend fun ProtectedAccess.startTool(tool: FoundryTool) {
        if (!canWork()) {
            return
        }
        if (player.preformCompletion >= FoundryFormulas.SCALE) {
            if (player.makingQuestSword()) {
                questSwordFinished()
            } else {
                // UNVERIFIED: the same line when a tool is used on a finished sword (guess).
                mes(FINISHED)
            }
            return
        }
        val state = stateOf(player)
        if (isWorking(state, tool.name)) {
            onReclick(state)
            return
        }
        begin(state, tool.name)
        state.tool = tool
        state.nextAction = firstAction(tool)
        state.overworked = false
        anim(tool.row.startSeq.internalName)
        if (state.sweetSpotUntil != 0) {
            state.sweetSpotArmed = true
            state.sweetSpotVoid = true
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

    /**
     * Lava pool (+) and waterfall (-); the rate ramps up while the player stays, and clicking
     * the same option again restarts the ramp ("will reset the speed").
     * https://oldschool.runescape.wiki/w/Giants%27_Foundry/Strategies
     */
    private fun ProtectedAccess.startHeat(sign: Int, fast: Boolean) {
        if (!canWork()) {
            return
        }
        val state = stateOf(player)
        val station = "$sign:$fast"
        val working = isWorking(state, station)
        if (!working) {
            begin(state, station)
        }
        state.tool = null
        state.heatSign = sign
        state.heatFast = fast
        // UNVERIFIED: a re-click restarts the ramp from the next tick (wiki description).
        state.heatTicks = 0
        if (!working) {
            heatTick(player, state)
        }
    }

    private fun ProtectedAccess.begin(state: WorkState, station: String) {
        state.station = station
        state.origin = player.coords
        softTimer(WORK_TIMER, 1)
    }

    private fun stop(player: Player, state: WorkState, resetAnim: Boolean = true) {
        state.station = null
        state.tool = null
        player.clearSoftTimer(WORK_TIMER)
        if (resetAnim) {
            player.resetAnim()
        }
    }

    private fun tick(player: Player) {
        val state = stateOf(player)
        if (state.station == null || state.origin != player.coords || !player.holdsPreform()) {
            stop(player, state)
            return
        }
        val tool = state.tool
        if (tool != null) {
            workTick(player, state, tool)
        } else {
            player.anim(if (state.heatSign > 0) HEAT_SEQ else COOL_SEQ)
            heatTick(player, state)
        }
    }

    private fun workTick(player: Player, state: WorkState, tool: FoundryTool) {
        player.anim(tool.row.loopSeq.internalName)
        if (clock.cycle < state.nextAction) {
            return
        }
        state.nextAction = clock.cycle + tool.row.period
        if (player.preformQuality == 0) {
            player.mes(DAMAGED)
            stop(player, state)
            return
        }
        act(player, tool, state)
        if (player.preformCompletion >= FoundryFormulas.SCALE) {
            stop(player, state, resetAnim = false)
        }
    }

    private fun heatTick(player: Player, state: WorkState) {
        val offset = if (state.heatFast) FoundryFormulas.FAST_HEAT_OFFSET else 0
        val step = FoundryFormulas.heatStep(state.heatTicks + offset)
        state.heatTicks++
        val temperature =
            (player.preformTemperature + state.heatSign * step).coerceIn(0, FoundryFormulas.SCALE)
        player.preformTemperature = temperature
        player.soundSynth(if (state.heatSign > 0) LAVA_SYNTH else WATERFALL_SYNTH)
        if (temperature == 0 || temperature == FoundryFormulas.SCALE) {
            stop(player, state)
        }
    }

    /**
     * Clicking the station again while it works: during a sweet spot it arms the bonus (green
     * border); otherwise the next action is missed altogether ("overworking").
     */
    private fun ProtectedAccess.onReclick(state: WorkState) {
        if (state.sweetSpotArmed) {
            return
        }
        if (state.sweetSpotUntil != 0) {
            state.sweetSpotArmed = true
            player.showSweetSpot(SWEETSPOT_ARMED)
        } else {
            state.overworked = true
        }
    }

    /**
     * An offer clears 9 ticks after it shows unless it was armed. An armed one lasts while the
     * player keeps working and is lost a tick earlier once they walk away.
     */
    private fun sweetSpotTick(player: Player) {
        val state = stateOf(player)
        if (state.sweetSpotUntil == 0) {
            player.clearSoftTimer(SWEETSPOT_TIMER)
            return
        }
        val expired =
            if (state.sweetSpotArmed) {
                state.tool == null && clock.cycle >= state.sweetSpotUntil
            } else {
                clock.cycle > state.sweetSpotUntil
            }
        if (expired) {
            clearSweetSpot(player, state)
            player.showSweetSpot(SWEETSPOT_NONE)
        }
    }

    private fun clearSweetSpot(player: Player, state: WorkState) {
        state.sweetSpotUntil = 0
        state.sweetSpotArmed = false
        state.sweetSpotVoid = false
        player.clearSoftTimer(SWEETSPOT_TIMER)
    }

    /** One station action: progress, heat, quality and XP. */
    private fun act(player: Player, tool: FoundryTool, state: WorkState) {
        if (state.overworked) {
            state.overworked = false
            player.mes(OVERWORKED)
            return
        }
        val difficulty = player.preformDifficulty
        val completion = player.preformCompletion
        val section = FoundryFormulas.section(completion, difficulty)
        val needed = FoundryTool.forSeed(player.foundryVar(seedVarbit(section)))
        val window = FoundryFormulas.window(tool, difficulty)
        val mistake = tool != needed || player.preformTemperature !in window

        val armed = state.sweetSpotArmed
        val bonus = armed && !state.sweetSpotVoid
        if (armed) {
            clearSweetSpot(player, state)
        }
        if (bonus) {
            player.mes(QUICK_REACTIONS)
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
                stop(player, state)
            }
        } else {
            progress = tool.row.progress + uniformBonus(player)
            xpTenths = tool.row.xpTenths
            if (bonus) {
                progress += SWEETSPOT_PROGRESS
                xpTenths *= 2
                // Since 7 May 2025 a sweet spot regains "a unit of lost quality" (one mistake's
                // worth). https://oldschool.runescape.wiki/w/Update:More_Poll_84_%26_Yama_Prep
                // UNVERIFIED: the update only regains it if clicked "shortly after it appears";
                // that window isn't published, so any armed sweet spot regains it here.
                player.preformQuality =
                    (player.preformQuality + MISTAKE_QUALITY).coerceAtMost(player.preformStartQuality)
            }
        }
        player.preformCompletion = (completion + progress).coerceAtMost(FoundryFormulas.SCALE)
        player.preformTemperature =
            (player.preformTemperature + tool.row.heat).coerceIn(0, FoundryFormulas.SCALE)
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
            !mistake -> maybeOfferSweetSpot(player, state, section)
        }
    }

    private fun swordFinished(player: Player) {
        if (player.makingQuestSword()) {
            player.weakQueue(QUEST_PROMPT_QUEUE, 1, QuestPrompt.SwordFinished)
        } else {
            player.mes(FINISHED)
        }
        player.anim(FINISH_SEQ)
        player.soundSynth(FINISH_SYNTH)
    }

    /**
     * Offers a sweet spot right after an action: 2, or 3 with six or more sections, at most one
     * per section, spread over the sections still to come ("more evenly distributed" since
     * 29 June 2022). https://oldschool.runescape.wiki/w/Giants%27_Foundry#Sweet_Spot
     */
    // UNVERIFIED: which action gets one isn't published; this spreads them evenly at random.
    private fun maybeOfferSweetSpot(player: Player, state: WorkState, section: Int) {
        if (state.sweetSpotUntil != 0) {
            return
        }
        val offered = player.sweetSpots and 0xFF
        val lastSection = (player.sweetSpots shr 8) - 1
        val total = FoundryFormulas.sweetSpots(player.preformDifficulty)
        if (offered >= total || section <= lastSection) {
            return
        }
        val sectionsLeft = FoundryFormulas.sections(player.preformDifficulty) - section
        if (random.of(1, sectionsLeft) > total - offered) {
            return
        }
        player.sweetSpots = ((section + 1) shl 8) or (offered + 1)
        state.sweetSpotUntil = clock.cycle + SWEETSPOT_TICKS
        player.softTimer(SWEETSPOT_TIMER, 1)
        player.showSweetSpot(SWEETSPOT_OFFERED)
    }

    /**
     * Smiths' Uniform: each piece gives a 20% chance per action of +1 progress (+5% hammer,
     * +10% grindstone and polishing wheel), and the full set gives it every action.
     * https://oldschool.runescape.wiki/w/Smiths%27_Uniform
     */
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

        /** Heating 827 / cooling 832, as logged by the Easy Giants' Foundry plugin. */
        const val HEAT_SEQ = "seq.human_pickupfloor"
        const val COOL_SEQ = "seq.human_pickuptable"

        const val LAVA_SYNTH = "synth.giants_foundry_lava_pool"
        const val WATERFALL_SYNTH = "synth.giants_foundry_waterfall"
        const val MISTAKE_SYNTH = "synth.glassblowing"
        const val SWEETSPOT_SYNTH = "synth.giants_foundry_sweetspot"
        const val FINISH_SYNTH = "synth.giants_foundry_sword_finished"
        const val TRIP_HAMMER_LOOP_SYNTH = "synth.giants_foundry_trip_hammer_loop"
        const val AMBIENT_1_SYNTH = "synth.giants_foundry_ambient_1"
        const val AMBIENT_2_SYNTH = "synth.giants_foundry_ambient_2"
        const val AMBIENT_PERIOD = 2
        const val AMBIENT_RADIUS = 12

        const val HAMMER_PHASE = 0
        const val FIRST_ACTION_DELAY = 3

        const val MISTAKE_QUALITY = 5
        const val SWEETSPOT_PROGRESS = 50
        const val SWEETSPOT_TICKS = 8

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
