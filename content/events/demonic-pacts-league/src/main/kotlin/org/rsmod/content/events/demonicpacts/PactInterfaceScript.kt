package org.rsmod.content.events.demonicpacts

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfScriptArgs
import org.rsmod.api.player.ui.ifCloseModal
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenMain
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfModalButton
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
        onIfOverlayButton(PACT_TREE_FRAME) { player.ifCloseSub(PACT_TREE, eventBus) }
        onIfOpen(RELICS) { player.prepareRelics() }
        onIfModalButton(RELICS_CLOSE) { player.ifCloseModal(RELICS, eventBus) }
        onIfModalButton(RELICS_CLICKZONES) { viewRelic(it.comsub) }
        onIfModalButton(RELICS_CONFIRM) { confirmRelic() }

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
        ifSetEvents(PACT_TREE_FRAME, FRAME_CHILDREN, IfEvent.Op1)
        runClientScript(TALENT_TREE_INIT, CENTRE_TREE)
    }

    private fun Player.openSidePanel() {
        syncLeagueClientVars()
        ifOpenOverlay(SIDE_PANEL, JOURNAL_CONTAINER, eventBus)
    }

    private fun Player.openRelics() {
        syncLeagueClientVars()
        ifOpenMain(RELICS, eventBus)
    }

    private fun Player.prepareRelics() {
        syncLeagueClientVars()
        VarPlayerIntMapSetter.set(this, RELIC_VIEWING, 0)
        ifSetEvents(RELICS_CLICKZONES, 0 until LeagueRelics.clickzoneCount, IfEvent.Op1)
        ifSetEvents(RELICS_CONFIRM, -1..-1, IfEvent.Op1)
        ifSetEvents(RELICS_CLOSE, -1..-1, IfEvent.Op1)
    }

    private fun ProtectedAccess.viewRelic(clickzone: Int) {
        val relic = LeagueRelics.byClickzone[clickzone] ?: return
        val tier = LeagueRelics.tiers[relic.tier]
        val state =
            when {
                LeagueRelics.selection(player, relic.tier) == relic.key -> RELIC_STATE_OWNED
                LeagueRelics.canSelect(player, relic) -> RELIC_STATE_SELECTABLE
                else -> RELIC_STATE_LOCKED
            }
        VarPlayerIntMapSetter.set(player, RELIC_VIEWING, clickzone + 1)
        val components = EXPANDED_VIEW_COMPONENTS.map { it.asRSCM(RSCMType.COMPONENT) }
        val passive = if (tier.passive.isNotEmpty()) tier.index else NO_PASSIVE
        val args = components + listOf(state, relic.struct, passive, relic.description, tier.passive)
        player.runClientScript(RELIC_EXPANDED_VIEW, args)
    }

    private fun ProtectedAccess.confirmRelic() {
        val viewing = player.vars[RELIC_VIEWING] - 1
        val relic = LeagueRelics.byClickzone[viewing] ?: return
        if (!LeagueRelics.canSelect(player, relic)) {
            mes("You can't unlock that relic right now.")
            return
        }
        LeagueRelics.select(player, relic)
        mes("<col=ef1020>You unlock the ${relic.name} relic.")
        mes("Relic effects aren't active on this server yet; your choice is saved for when they are.")
        player.openRelics()
    }

    private fun ProtectedAccess.applyPacts(nodes: IntArray) {
        when (val result = PactTree.apply(player, nodes)) {
            is PactApply.Applied -> {
                val noun = if (result.count == 1) "pact" else "pacts"
                mes("<col=ef1020>You make ${result.count} new $noun with Yama.")
                val bought = nodes.toList().mapNotNull { DemonicPactsData.nodesByBit[it] }
                val unimplemented = bought.count { !it.isImplemented }
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

        /** The steel border's close button is a child of the frame; nothing else on it has ops. */
        const val PACT_TREE_FRAME = "component.talent_tree:frame"
        val FRAME_CHILDREN = 0..20
        const val RELICS_CLOSE = "component.league_relics:close_button"
        const val RELICS_CLICKZONES = "component.league_relics:clickzones"
        const val RELICS_CONFIRM = "component.league_relics:confirm_button"
        const val RELIC_VIEWING = "varp.dp_relic_viewing"

        /** league_relic_expanded_view: the relic detail page, filled in for one relic. */
        const val RELIC_EXPANDED_VIEW = 3193
        const val RELIC_STATE_OWNED = 1
        const val RELIC_STATE_SELECTABLE = 2
        const val RELIC_STATE_LOCKED = 3
        const val NO_PASSIVE = -1
        val EXPANDED_VIEW_COMPONENTS =
            listOf(
                "component.league_relics:view_all",
                "component.league_relics:view_all_scrollbar",
                "component.league_relics:view_one",
                "component.league_relics:loading",
                "component.league_relics:icon",
                "component.league_relics:name",
                "component.league_relics:description_header",
                "component.league_relics:description",
                "component.league_relics:select_button",
                "component.league_relics:select_back",
                "component.league_relics:confirm",
                "component.league_relics:confirm_frame",
                "component.league_relics:confirm_text",
                "component.league_relics:confirm_button",
                "component.league_relics:confirm_cancel",
                "component.league_relics:passive_header",
                "component.league_relics:passive_description",
                "component.league_relics:relic_available_header",
            )
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
