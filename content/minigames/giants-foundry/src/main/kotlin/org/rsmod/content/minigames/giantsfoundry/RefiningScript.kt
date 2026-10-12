package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
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
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Working the preform: the trip hammer, grindstone and polishing wheel, the lava pool and the
 * waterfall, sweet spots and passive cooling.
 * https://oldschool.runescape.wiki/w/Giants%27_Foundry#Refining_the_sword
 *
 * Each station re-issues its own op every tick (like other skilling loops), so any other click
 * stops it.
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
            onOpLoc1(tool.loc) { work(tool, it.loc) }
        }
        onOpLoc1(LAVA_POOL) { heat(it.loc, sign = 1, fast = false, op = 1) }
        onOpLoc2(LAVA_POOL) { heat(it.loc, sign = 1, fast = true, op = 2) }
        onOpLoc1(WATERFALL) { heat(it.loc, sign = -1, fast = false, op = 1) }
        onOpLoc2(WATERFALL) { heat(it.loc, sign = -1, fast = true, op = 2) }
        onPlayerSoftTimer(PREFORM_COOLING_TIMER) { passiveCooling(player) }
        onPlayerLogout { states.remove(player) }
    }

    private class WorkState {
        var station: String? = null
        var continuing = false
        var nextAction = 0
        var heatTicks = 0
        var sweetSpotUntil = -1
        var sweetSpotArmed = false
        var overworked = false
    }

    private fun ProtectedAccess.state(): WorkState = states.getOrPut(player) { WorkState() }

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

    private fun ProtectedAccess.work(tool: FoundryTool, loc: BoundLocInfo) {
        if (!canWork()) {
            return
        }
        if (player.preformCompletion >= FoundryFormulas.SCALE) {
            mes("The sword is finished. Hand it to Kovac.")
            return
        }
        val state = state()
        val clicked = !state.continuing
        state.continuing = false
        if (state.station != tool.loc) {
            state.station = tool.loc
            state.nextAction = clock.cycle + tool.period
            state.overworked = false
        } else if (clicked) {
            onReclick(state)
        }
        if (clock.cycle % WORK_ANIM_PERIOD == 0 || clicked) {
            anim(WORK_SEQ)
            tool.locSeq?.let { worldRepo.locAnim(loc, it) }
        }
        expireSweetSpot(state)
        if (clock.cycle >= state.nextAction) {
            act(tool, state)
            state.nextAction = clock.cycle + tool.period
        }
        if (player.preformQuality > 0 && player.preformCompletion < FoundryFormulas.SCALE) {
            state.continuing = true
            opLoc1(loc)
        } else {
            resetAnim()
            state.station = null
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

    private fun ProtectedAccess.expireSweetSpot(state: WorkState) {
        if (state.sweetSpotUntil in 0 until clock.cycle && !state.sweetSpotArmed) {
            state.sweetSpotUntil = -1
            player.showSweetSpot(SWEETSPOT_NONE)
        }
    }

    /** One station action: progress, heat, quality and XP. */
    private fun ProtectedAccess.act(tool: FoundryTool, state: WorkState) {
        val difficulty = player.preformDifficulty
        val completion = player.preformCompletion
        val section = FoundryFormulas.section(completion, difficulty)
        val needed = FoundryTool.forSeed(player.foundryVar(seedVarbit(section)))
        val inZone = player.preformTemperature in FoundryFormulas.window(tool, difficulty)

        var progress = if (state.overworked) 0 else tool.progress + uniformBonus()
        state.overworked = false
        var xpTenths = tool.xpTenths
        // UNVERIFIED: a mistake still makes progress (the wiki only says it costs quality).
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
            if (player.preformQuality < player.preformStartQuality) {
                player.preformQuality += 1
            }
        }
        player.preformCompletion = (completion + progress).coerceAtMost(FoundryFormulas.SCALE)
        player.preformTemperature =
            (player.preformTemperature + tool.heat).coerceIn(0, FoundryFormulas.SCALE)
        if (xpTenths > 0) {
            statAdvance("stat.smithing", xpTenths / 10.0)
        }
        when {
            player.preformQuality == 0 -> mes(DAMAGED, ChatType.Engine)
            player.preformCompletion >= FoundryFormulas.SCALE ->
                mes("The sword is finished. Hand it to Kovac.")
            else -> maybeOfferSweetSpot(state, section)
        }
    }

    /**
     * Offers a sweet spot right after an action: 2 or 3 per sword, at most one per section,
     * spread over the sections still to come.
     * https://oldschool.runescape.wiki/w/Giants%27_Foundry#Sweet_spot
     */
    // UNVERIFIED: placement rule is a guess; the wiki says only "more evenly distributed".
    private fun ProtectedAccess.maybeOfferSweetSpot(state: WorkState, section: Int) {
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
    private fun ProtectedAccess.uniformBonus(): Int {
        val pieces = UNIFORM.count { (slot, objs) -> objs.any { player.worn[slot.slot]?.isType(it) == true } }
        return if (pieces > 0 && random.of(1, 5) <= pieces) 1 else 0
    }

    /** Lava pool (+) and waterfall (-); the rate ramps up while the player stays. */
    private fun ProtectedAccess.heat(loc: BoundLocInfo, sign: Int, fast: Boolean, op: Int) {
        if (!canWork()) {
            return
        }
        val state = state()
        val station = "${loc.id}:$op"
        if (state.station != station || !state.continuing) {
            state.station = station
            state.heatTicks = 0
            anim(if (sign > 0) HEAT_SEQ else COOL_SEQ)
        }
        state.continuing = false
        val step = FoundryFormulas.heatStep(state.heatTicks + if (fast) FoundryFormulas.FAST_HEAT_OFFSET else 0)
        state.heatTicks++
        val temperature = (player.preformTemperature + sign * step).coerceIn(0, FoundryFormulas.SCALE)
        player.preformTemperature = temperature
        if (temperature in 1 until FoundryFormulas.SCALE) {
            state.continuing = true
            if (op == 1) opLoc1(loc) else opLoc2(loc)
        } else {
            resetAnim()
            state.station = null
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
        const val LAVA_POOL = "loc.giants_foundry_lava_pool"
        const val WATERFALL = "loc.giants_foundry_waterfall"
        const val WORK_SEQ = "seq.giants_foundry_work_preform_loop"
        const val HEAT_SEQ = "seq.human_pickupfloor"
        const val COOL_SEQ = "seq.human_pickuptable"
        const val WORK_ANIM_PERIOD = 4

        // UNVERIFIED: the wiki's quality section gives 10 per mistake, its reputation section 5.
        const val MISTAKE_QUALITY = 10

        // UNVERIFIED: the wiki gives "about +5%" progress for a sweet spot.
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
                Wearpos.Torso to listOf("obj.smiths_tunic"),
                Wearpos.Legs to listOf("obj.smiths_trousers"),
                Wearpos.Feet to listOf("obj.smiths_boots"),
                Wearpos.Hands to listOf("obj.smiths_gloves", "obj.smiths_gloves_i"),
            )
    }
}
