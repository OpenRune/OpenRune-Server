package org.rsmod.content.events.leaguetasks.pacts

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfScriptArgs
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onIfScriptTrigger
import org.rsmod.content.events.leaguetasks.LeagueTaskCompletedEvent
import org.rsmod.content.events.leaguetasks.tutorial.LeagueMenu
import org.rsmod.content.events.leaguetasks.tutorial.LeagueTutorial
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Pact points and the pact tree as captured in `rsprox-1098` and `rsprox-1102`: each task in
 * `enum.league_6_pact_tasks` earns a point, and a spend arrives as the node list the client sends
 * from `talent_tree:data_layer` (`[proc,talent_tree_apply_changes]`).
 */
class LeaguePactsScript
@Inject
constructor(
    private val pacts: LeaguePacts,
    private val tutorial: LeagueTutorial,
    private val eventBus: EventBus,
) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        onEvent<LeagueTaskCompletedEvent> {
            if (pacts.awardsPact(task)) {
                player.awardPact()
            }
        }

        onIfOverlayButton("component.league_side_panel:mastery_button") {
            if (!tutorial.onMenuButton(player, LeagueMenu.Pacts)) {
                pacts.openTree(player)
            }
        }

        onIfOverlayButton("component.talent_tree:frame") {
            if (it.comsub == CLOSE_SUB) {
                player.ifCloseOverlay("interface.talent_tree", eventBus)
            }
        }

        onIfScriptTrigger<SpendArgs>("component.talent_tree:data_layer") { spend(it.nodes) }
    }

    private fun Player.awardPact() {
        pacts.award(this)
        // Jagex pluralises on points earned, not points left: "1 Demonic Pacts" after the first.
        val noun = if (pacts.earned(this) > 1) "Demonic Pacts" else "Demonic Pact"
        mes(
            "<col=229628>Congratulations, you've earned a Demonic Pact! " +
                "You now have ${pacts.available(this)} $noun to spend.</col>"
        )
    }

    private suspend fun ProtectedAccess.spend(nodes: IntArray) {
        if (!pacts.spend(player, nodes)) {
            return
        }
        vars[BUSY] = 1
        val female = player.appearance.bodyType == constants.bodytype_b
        anim(if (female) "seq.human_relic_unlock_female" else "seq.human_relic_unlock")
        spotanim("spotanim.leagues_6_mastery_unlock", height = UNLOCK_SPOTANIM_HEIGHT)
        player.ifCloseOverlay("interface.talent_tree", eventBus)
        soundSynth("synth.league_unlock_1", delay = 8)
        soundSynth("synth.league_unlock_2", delay = 44)
        soundSynth("synth.league_unlock_3", delay = 80)
        soundSynth("synth.league_unlock_4", delay = 104)
        delay(UNLOCK_DELAY)
        vars[BUSY] = 0
        publish(LeaguePactsSpentEvent(player, nodes))
    }

    internal class SpendArgs(val nodes: IntArray) : IfScriptArgs

    private companion object {
        const val CLOSE_SUB = 11
        const val UNLOCK_SPOTANIM_HEIGHT = 92
        const val UNLOCK_DELAY = 5
        const val BUSY = "varbit.busy"
    }
}
