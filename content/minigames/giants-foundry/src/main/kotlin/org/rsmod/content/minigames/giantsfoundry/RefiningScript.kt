package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType
import org.rsmod.game.loc.BoundLocInfo
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
) : PluginScript() {
    private val states = HashMap<Player, WorkState>()

    override fun ScriptContext.startup() {
        for (tool in FoundryTool.entries) {
            onOpLoc1(tool.loc) { startTool(tool, it.loc) }
        }
        onOpLoc1(LAVA_POOL) { startHeat(it.loc, sign = 1, fast = false) }
        onOpLoc2(LAVA_POOL) { startHeat(it.loc, sign = 1, fast = true) }
        onOpLoc1(WATERFALL) { startHeat(it.loc, sign = -1, fast = false) }
        onOpLoc2(WATERFALL) { startHeat(it.loc, sign = -1, fast = true) }
        onPlayerSoftTimer(WORK_TIMER) { tick(player) }
        onPlayerSoftTimer(PREFORM_COOLING_TIMER) { passiveCooling(player) }
        onPlayerLogout { states.remove(player) }
    }

    private class WorkState {
        /** What the player is working at: a tool, or a heat source and op. */
        var station: String? = null
        var tool: FoundryTool? = null
        var loc: BoundLocInfo? = null
        var origin: CoordGrid? = null
        var heatSign = 0
        var heatFast = false
        var heatTicks = 0
        var nextAction = 0
        var sweetSpotUntil = -1
        var sweetSpotArmed = false
        var overworked = false
    }

    private fun stateOf(player: Player): WorkState = states.getOrPut(player) { WorkState() }

    private fun ProtectedAccess.canWork(): Boolean {
        if (!player.holdsPreform()) {
            mes("You need to be holding a preform to do that.")
            return false
        }
        if (player.preformQuality == 0) {
            mes(DAMAGED, ChatType.Engine)
            return false
        }
        return true
    }

    private fun ProtectedAccess.isWorking(state: WorkState, station: String): Boolean =
        state.station == station && state.origin == player.coords

    private fun ProtectedAccess.startTool(tool: FoundryTool, loc: BoundLocInfo) {
        if (!canWork()) {
            return
        }
        if (player.preformCompletion >= FoundryFormulas.SCALE) {
            mes("The sword is finished. Hand it to Kovac.")
            return
        }
        val state = stateOf(player)
        if (isWorking(state, tool.loc)) {
            onReclick(state)
            return
        }
        begin(state, tool.loc, loc)
        state.tool = tool
        state.nextAction = clock.cycle + tool.period
        state.overworked = false
        anim(WORK_SEQ)
    }

    /**
     * Lava pool (+) and waterfall (-); the rate ramps up while the player stays, and clicking
     * the same option again restarts the ramp ("will reset the speed").
     * https://oldschool.runescape.wiki/w/Giants%27_Foundry/Strategies
     */
    private fun ProtectedAccess.startHeat(loc: BoundLocInfo, sign: Int, fast: Boolean) {
        if (!canWork()) {
            return
        }
        val state = stateOf(player)
        val station = "${loc.id}:$sign:$fast"
        if (!isWorking(state, station)) {
            begin(state, station, loc)
        }
        state.tool = null
        state.heatSign = sign
        state.heatFast = fast
        state.heatTicks = 0
        anim(if (sign > 0) HEAT_SEQ else COOL_SEQ)
    }

    private fun ProtectedAccess.begin(state: WorkState, station: String, loc: BoundLocInfo) {
        state.station = station
        state.loc = loc
        state.origin = player.coords
        softTimer(WORK_TIMER, 1)
    }

    private fun stop(player: Player, state: WorkState) {
        state.station = null
        state.tool = null
        player.clearSoftTimer(WORK_TIMER)
        player.resetAnim()
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
            heatTick(player, state)
        }
    }

    private fun workTick(player: Player, state: WorkState, tool: FoundryTool) {
        if (clock.cycle % WORK_ANIM_PERIOD == 0) {
            player.anim(WORK_SEQ)
            val loc = state.loc
            if (loc != null) {
                tool.locSeq?.let { worldRepo.locAnim(loc, it) }
            }
        }
        expireSweetSpot(player, state)
        if (clock.cycle >= state.nextAction) {
            act(player, tool, state)
            state.nextAction = clock.cycle + tool.period
        }
        if (player.preformQuality == 0 || player.preformCompletion >= FoundryFormulas.SCALE) {
            stop(player, state)
        }
    }

    private fun heatTick(player: Player, state: WorkState) {
        val offset = if (state.heatFast) FoundryFormulas.FAST_HEAT_OFFSET else 0
        val step = FoundryFormulas.heatStep(state.heatTicks + offset)
        state.heatTicks++
        val temperature =
            (player.preformTemperature + state.heatSign * step).coerceIn(0, FoundryFormulas.SCALE)
        player.preformTemperature = temperature
        if (temperature == 0 || temperature == FoundryFormulas.SCALE) {
            stop(player, state)
        }
    }

    /**
     * Clicking the station again while it works: during a sweet spot it arms the bonus (green
     * border); otherwise the next action makes no progress ("overworking").
     */
    private fun ProtectedAccess.onReclick(state: WorkState) {
        if (state.sweetSpotUntil >= clock.cycle && !state.sweetSpotArmed) {
            state.sweetSpotArmed = true
            player.showSweetSpot(SWEETSPOT_ARMED)
        } else if (!state.sweetSpotArmed) {
            state.overworked = true
        }
    }

    private fun expireSweetSpot(player: Player, state: WorkState) {
        if (state.sweetSpotUntil in 0 until clock.cycle && !state.sweetSpotArmed) {
            state.sweetSpotUntil = -1
            player.showSweetSpot(SWEETSPOT_NONE)
        }
    }

    /** One station action: progress, heat, quality and XP. */
    private fun act(player: Player, tool: FoundryTool, state: WorkState) {
        val difficulty = player.preformDifficulty
        val completion = player.preformCompletion
        val section = FoundryFormulas.section(completion, difficulty)
        val needed = FoundryTool.forSeed(player.foundryVar(seedVarbit(section)))
        val inZone = player.preformTemperature in FoundryFormulas.window(tool, difficulty)

        var progress = if (state.overworked) 0 else tool.progress + uniformBonus(player)
        state.overworked = false
        var xpTenths = tool.xpTenths
        // UNVERIFIED: a mistake still makes progress (the wiki only says it "will deal damage").
        if (tool != needed || !inZone) {
            player.preformQuality = (player.preformQuality - MISTAKE_QUALITY).coerceAtLeast(0)
            xpTenths = 0
        }
        if (state.sweetSpotArmed) {
            state.sweetSpotArmed = false
            state.sweetSpotUntil = -1
            player.showSweetSpot(SWEETSPOT_NONE)
            progress += SWEETSPOT_PROGRESS
            xpTenths *= 2
            // Since 7 May 2025 a sweet spot regains "a unit of lost quality" (one mistake's
            // worth). https://oldschool.runescape.wiki/w/Update:More_Poll_84_%26_Yama_Prep
            // UNVERIFIED: the update only regains it if clicked "shortly after it appears";
            // that window isn't published, so any armed sweet spot regains it here.
            player.preformQuality =
                (player.preformQuality + MISTAKE_QUALITY).coerceAtMost(player.preformStartQuality)
        }
        player.preformCompletion = (completion + progress).coerceAtMost(FoundryFormulas.SCALE)
        player.preformTemperature =
            (player.preformTemperature + tool.heat).coerceIn(0, FoundryFormulas.SCALE)
        if (xpTenths > 0) {
            player.statAdvance("stat.smithing", xpTenths / 10.0)
        }
        when {
            player.preformQuality == 0 -> player.mes(DAMAGED, ChatType.Engine)
            player.preformCompletion >= FoundryFormulas.SCALE ->
                player.mes("The sword is finished. Hand it to Kovac.")
            else -> maybeOfferSweetSpot(player, state, section)
        }
    }

    /**
     * Offers a sweet spot right after an action: 2, or 3 with six or more sections, at most one
     * per section, spread over the sections still to come ("more evenly distributed" since
     * 29 June 2022). https://oldschool.runescape.wiki/w/Giants%27_Foundry#Sweet_Spot
     */
    // UNVERIFIED: which action gets one isn't published; this spreads them evenly at random.
    private fun maybeOfferSweetSpot(player: Player, state: WorkState, section: Int) {
        if (state.sweetSpotUntil >= clock.cycle) {
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
        player.showSweetSpot(SWEETSPOT_OFFERED)
    }

    /**
     * Smiths' Uniform: each piece gives a 20% chance per action of +1 progress (+5% hammer,
     * +10% grindstone and polishing wheel). https://oldschool.runescape.wiki/w/Smiths%27_Uniform
     */
    private fun uniformBonus(player: Player): Int {
        val pieces =
            UNIFORM.count { (slot, objs) -> objs.any { player.worn[slot.slot]?.isType(it) == true } }
        return if (pieces > 0 && random.of(1, 5) <= pieces) 1 else 0
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
        const val LAVA_POOL = "loc.giants_foundry_lava_pool"
        const val WATERFALL = "loc.giants_foundry_waterfall"
        const val WORK_SEQ = "seq.giants_foundry_work_preform_loop"

        /** Heating 827 / cooling 832, as logged by the Easy Giants' Foundry plugin. */
        const val HEAT_SEQ = "seq.human_pickupfloor"
        const val COOL_SEQ = "seq.human_pickuptable"
        const val WORK_ANIM_PERIOD = 4

        // UNVERIFIED: the wiki said 5 per mistake at launch (8 June 2022 revision, still in its
        // reputation section); an uncited July 2022 edit changed the mechanic text to 10.
        const val MISTAKE_QUALITY = 10

        // UNVERIFIED: the wiki gives "around 5%" progress for a sweet spot.
        const val SWEETSPOT_PROGRESS = 50

        // UNVERIFIED: the wiki gives "about 10 ticks".
        const val SWEETSPOT_TICKS = 10

        const val SWEETSPOT_NONE = 0
        const val SWEETSPOT_OFFERED = 1
        const val SWEETSPOT_ARMED = 2

        const val DAMAGED =
            "<col=ff0000>This sword is too damaged to keep working. You should give it to Kovac " +
                "and try again.</col>"

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
