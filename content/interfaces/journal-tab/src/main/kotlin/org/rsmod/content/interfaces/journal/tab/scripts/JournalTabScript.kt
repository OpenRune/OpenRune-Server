package org.rsmod.content.interfaces.journal.tab.scripts

import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.interfaces.journal.tab.SideJournalTab
import org.rsmod.content.interfaces.journal.tab.openJournalTab
import org.rsmod.content.interfaces.journal.tab.sideJournalTab
import org.rsmod.content.interfaces.journal.tab.switchJournalTab
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class JournalTabScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen("interface.side_journal") { player.openActiveJournal() }

        onIfOverlayButton("component.side_journal:summary_list") {
            player.switchJournalTab(SideJournalTab.Summary)
        }

        onIfOverlayButton("component.side_journal:quest_list") {
            player.switchJournalTab(SideJournalTab.Quests)
        }

        onIfOverlayButton("component.side_journal:task_list") {
            player.switchJournalTab(SideJournalTab.Tasks)
        }

        onIfOverlayButton("component.toplevel_osrs_stretch:stone2") { player.selectStoneOp(it.op) }
    }

    private fun Player.selectStoneOp(op: IfButtonOp) {
        val tab =
            when (op) {
                IfButtonOp.Op2 -> SideJournalTab.Summary
                IfButtonOp.Op3 -> SideJournalTab.Quests
                IfButtonOp.Op4 -> SideJournalTab.Tasks
                else -> return
            }
        ClientScripts.toplevelSidebuttonSwitch(this, 2)
        soundSynth("synth.interface_select")
        switchJournalTab(tab)
    }

    private fun Player.openActiveJournal() = openJournalTab(sideJournalTab, eventBus)

    private fun Player.switchJournalTab(open: SideJournalTab) = switchJournalTab(open, eventBus)
}
