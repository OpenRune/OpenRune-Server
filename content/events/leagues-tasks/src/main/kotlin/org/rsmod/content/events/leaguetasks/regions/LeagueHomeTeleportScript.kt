package org.rsmod.content.events.leaguetasks.regions

import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.events.leaguetasks.LeagueTasks
import org.rsmod.game.world.WorldType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** `magic_spellbook:league_home_teleport`: op1 Cast, op2 Yama's Lair, op3 Civitas. */
class LeagueHomeTeleportScript
@Inject
constructor(private val homeTeleport: LeagueHomeTeleport, private val tasks: LeagueTasks) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.magic_spellbook:league_home_teleport") {
            if (it.op == IfButtonOp.Op2) {
                homeTeleport.start(this, YAMAS_LAIR, YAMAS_LAIR_RADIUS)
                return@onIfOverlayButton
            }
            // UNVERIFIED: Cast usually lands in Civitas but sometimes in Yama's Lair; the rule is
            // unknown.
            val home = tasks.leagueOf(player)?.home ?: return@onIfOverlayButton
            homeTeleport.start(this, home, CIVITAS_RADIUS)
        }

        onPlayerQueueWithArgs<LeagueHomeTeleport.Pending>(LeagueHomeTeleport.QUEUE) {
            homeTeleport.process(this, it.args)
        }
    }

    private companion object {
        // UNVERIFIED: centre and radius are fitted to a handful of landings.
        val YAMAS_LAIR = CoordGrid(1502, 5592, 0)
        const val YAMAS_LAIR_RADIUS = 2
        const val CIVITAS_RADIUS = 1
    }
}
