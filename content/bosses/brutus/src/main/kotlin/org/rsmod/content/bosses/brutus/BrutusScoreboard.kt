package org.rsmod.content.bosses.brutus

import jakarta.inject.Inject
import java.text.NumberFormat
import java.util.Locale
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.instances.events.InstanceKillTimeEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.instances.timer.formatInstanceKillTime
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The scoreboard by the cow pen (https://oldschool.runescape.wiki/w/Brutus). Interface, components
 * and number formats: live capture, rev 241, 20261012T200152-df49f48@709 and @802.
 */
class BrutusScoreboard @Inject constructor(private val worldStats: BrutusWorldStats) :
    PluginScript() {
    override fun ScriptContext.startup() {
        worldStats.load()
        onOpLoc1(SCOREBOARD) { read() }
        onEvent<InstanceKillTimeEvent>(instanceEventId(CowbellAmulet.BRUTUS_FIELD)) {
            worldStats.addKillTime(ticks)
        }
    }

    private fun ProtectedAccess.read() {
        // UNVERIFIED: the best times shown before any kill; the capture only saw recorded ones.
        val precise = vars["varbit.option_precise_timing"] != 0
        val personalBest =
            vars[BEST_TIME].let { if (it == 0) NO_TIME else formatInstanceKillTime(it, precise) }
        val worldBest =
            worldStats.bestTicks.let { if (it == 0) NO_TIME else formatInstanceKillTime(it, precise) }
        ifOpenMainModal(INTERFACE)
        ifSetText("$INTERFACE_COM:pkc_content", number(vars[KILLS].toLong()))
        ifSetText("$INTERFACE_COM:pd_content", number(vars[DEATHS].toLong()))
        ifSetText("$INTERFACE_COM:pbt_content", personalBest)
        ifSetText("$INTERFACE_COM:gkc_content", number(worldStats.kills))
        ifSetText("$INTERFACE_COM:gd_content", number(worldStats.deaths))
        ifSetText("$INTERFACE_COM:gbt_content", worldBest)
    }

    private fun number(value: Long): String = NumberFormat.getIntegerInstance(Locale.UK).format(value)

    companion object {
        const val SCOREBOARD = "loc.cowboss_scoreboard_multi"
        const val SCOREBOARD_SHOWN = "varbit.cowboss_scoreboards"
        const val KILLS = "varp.total_cowboss_kills"
        const val DEATHS = "varp.brutus_deaths"
        const val BEST_TIME = "varp.brutus_best_time"
        private const val INTERFACE = "interface.cowboss_scoreboard"
        private const val INTERFACE_COM = "component.cowboss_scoreboard"
        private const val NO_TIME = "0:00.00"
    }
}

/**
 * Counts each kill after the quest towards the server-wide total and shows the player the
 * scoreboard. Live capture, rev 241, 20261012T180256-df49f48@1610.
 */
class BrutusKillTally @Inject constructor(private val worldStats: BrutusWorldStats) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc
        if (!npc.isType(Brutus.BRUTUS) || npc.vars["varn.skip_killcount"] != 0) {
            return
        }
        VarPlayerIntMapSetter.set(context.hero, BrutusScoreboard.SCOREBOARD_SHOWN, 1)
        worldStats.addKill()
    }
}
