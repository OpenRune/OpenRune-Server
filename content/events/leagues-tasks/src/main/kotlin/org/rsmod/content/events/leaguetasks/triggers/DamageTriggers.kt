package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.entity.Npc
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Counts a single player hit on an npc; the damage is what landed, capped by its hitpoints. */
class DamageTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rows = progress.rows(LeagueTaskTrigger.Damage)

    override fun ScriptContext.startup() {
        onEvent<PlayerAttackEvents.HitImpact> {
            if (target !is Npc) {
                return@onEvent
            }
            val used = setOfNotNull(hit.righthandType()?.id, hit.secondaryType()?.id)
            val met =
                rows.filter { row ->
                    hit.damage >= row.value && (row.tools.isEmpty() || row.tools.any { it in used })
                }
            progress.record(player, met)
        }
    }
}
