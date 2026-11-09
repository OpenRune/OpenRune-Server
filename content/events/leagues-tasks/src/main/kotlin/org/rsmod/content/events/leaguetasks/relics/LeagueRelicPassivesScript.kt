package org.rsmod.content.events.leaguetasks.relics

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSlayerTaskComplete
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.script.onPlayerXpModify
import org.rsmod.api.script.onVarChanged
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeagueRelicPassivesScript @Inject constructor(private val passives: LeagueRelicPassives) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        onPlayerLogin { player.applyPassives() }
        onVarChanged("varp.league_points_claimed") { player.applyPassives() }

        onPlayerXpModify {
            val percent = passives.of(player).combatXpPercent
            if (percent > 0 && stat in COMBAT_STATS) {
                xp = xp * percent / 100
            }
        }

        onPlayerSlayerTaskComplete {
            val multiplier = passives.of(player).slayerPointMultiplier
            if (multiplier > 0) {
                points = max(points, basePoints) * multiplier
            }
        }

        onPlayerSoftTimer("timer.league_infinite_run") { player.refillRunEnergy() }
    }

    private fun Player.applyPassives() {
        val active = passives.of(this)
        if (active.xpRate > 0) {
            xpRate = active.xpRate.toDouble()
        }
        VarPlayerIntMapSetter.set(this, "varp.drop_rate_multiplier", active.dropRatePercent)
        VarPlayerIntMapSetter.set(
            this,
            "varp.slayer_superior_rate_multiplier",
            active.superiorRatePercent,
        )
        if (active.freeBiggerAndBadder) {
            VarPlayerIntMapSetter.set(this, "varbit.slayer_unlock_superiormobs", 1)
        }
        if (active.infiniteRun) {
            softTimer("timer.league_infinite_run", cycles = 1)
        } else {
            clearSoftTimer("timer.league_infinite_run")
        }
    }

    /** The drain runs after timers, so the orb shows 98-99% while running (accepted, 2026-11-05). */
    private fun Player.refillRunEnergy() {
        if (runEnergy < constants.run_max_energy) {
            runEnergy = constants.run_max_energy
            UpdateRun.energy(this, runEnergy)
        }
    }

    private companion object {
        val COMBAT_STATS =
            setOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
                "stat.ranged",
                "stat.magic",
                "stat.hitpoints",
                "stat.prayer",
            )
    }
}
