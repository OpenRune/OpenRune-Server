package org.rsmod.content.events.demonicpacts

import dev.openrune.definition.type.widget.IfEvent
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfScriptArgs
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onIfScriptTrigger
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The game's own Demonic Pacts tree (`talent_tree`) and league side panel. */
class PactInterfaceScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    internal class PactApplyArg(val nodes: IntArray) : IfScriptArgs

    internal class PactResetArg : IfScriptArgs

    override fun ScriptContext.startup() {
        onIfOpen(PACT_TREE) { player.prepareTree() }
        onIfScriptTrigger<PactApplyArg>(APPLY_COMPONENT) { applyPacts(it.nodes) }
        onIfScriptTrigger<PactResetArg>(RESET_COMPONENT) { confirmReset() }

        onIfOverlayButton(JOURNAL_LEAGUE_BUTTON) { player.openSidePanel() }
        onIfOverlayButton(SIDE_PANEL_PACTS_BUTTON) { player.openPactTree() }
        onIfOverlayButton(SIDE_PANEL_RELICS_BUTTON) { player.openRelics() }
        onIfOverlayButton(SIDE_PANEL_TASKS_BUTTON) {
            player.mes("Type ::tasks to list your league tasks.")
        }
        onIfOverlayButton(SIDE_PANEL_AREAS_BUTTON) {
            player.mes("Type ::regions to see and unlock league regions.")
        }

        onCommand("pacts") {
            desc = "Open the Demonic Pacts tree"
            cheat { player.openPactTree() }
        }
        onCommand("leaguepanel") {
            desc = "Open the league side panel"
            cheat { player.openSidePanel() }
        }
    }

    private fun Player.openPactTree() {
        ifOpenOverlay(PACT_TREE, eventBus)
    }

    private fun Player.prepareTree() {
        onPactsChanged()
        ifSetEvents(APPLY_COMPONENT, -1..-1, IfEvent.ScriptTrigger)
        ifSetEvents(RESET_COMPONENT, -1..-1, IfEvent.ScriptTrigger)
        runClientScript(TALENT_TREE_INIT, CENTRE_TREE)
    }

    private fun Player.openSidePanel() {
        syncLeagueClientVars()
        ifOpenOverlay(SIDE_PANEL, JOURNAL_CONTAINER, eventBus)
    }

    private fun Player.openRelics() {
        syncLeagueClientVars()
        ifOpenOverlay(RELICS, eventBus)
    }

    private fun ProtectedAccess.applyPacts(nodes: IntArray) {
        when (val result = PactTree.apply(player, nodes)) {
            is PactApply.Applied -> {
                val noun = if (result.count == 1) "pact" else "pacts"
                mes("<col=ef1020>You make ${result.count} new $noun with Yama.")
                val unimplemented =
                    nodes.mapNotNull { DemonicPactsData.nodesByBit[it] }.count { !it.isImplemented }
                if (unimplemented > 0) {
                    mes("$unimplemented of them have no effect on this server yet. ::pactlist owned")
                }
                LeagueTasks.checkProgress(player)
            }
            is PactApply.Rejected -> mes("<col=ff0000>${result.reason}")
        }
        player.runClientScript(TALENT_TREE_INIT, KEEP_SCROLL)
    }

    private suspend fun ProtectedAccess.confirmReset() {
        val resets = player.pactResetsAvailable
        if (resets <= 0) {
            mes("You have no pact resets left.")
            return
        }
        val confirmed =
            choice2(
                "Yes, break all of my pacts. ($resets resets left)",
                true,
                "No, keep them.",
                false,
                title = "Reset every Demonic Pact?",
            )
        if (!confirmed) {
            return
        }
        if (PactTree.reset(player)) {
            mes("<col=ef1020>Your pacts are broken. Resets left: ${player.pactResetsAvailable}.")
            player.runClientScript(TALENT_TREE_INIT, KEEP_SCROLL)
        }
    }

    private companion object {
        const val PACT_TREE = "interface.talent_tree"
        const val RELICS = "interface.league_relics"
        const val SIDE_PANEL = "interface.league_side_panel"
        const val JOURNAL_CONTAINER = "component.side_journal:tab_container"
        const val JOURNAL_LEAGUE_BUTTON = "component.side_journal:league_list"
        const val APPLY_COMPONENT = "component.talent_tree:data_layer"
        const val RESET_COMPONENT = "component.talent_tree:perm_floating_panel_content"
        const val SIDE_PANEL_PACTS_BUTTON = "component.league_side_panel:mastery_button"
        const val SIDE_PANEL_RELICS_BUTTON = "component.league_side_panel:relics_button"
        const val SIDE_PANEL_TASKS_BUTTON = "component.league_side_panel:tasks_button"
        const val SIDE_PANEL_AREAS_BUTTON = "component.league_side_panel:areas_button"

        /** talent_tree_init(int): 1 centres the tree, 0 keeps the current scroll position. */
        const val TALENT_TREE_INIT = 7892
        const val CENTRE_TREE = 1
        const val KEEP_SCROLL = 0
    }
}
