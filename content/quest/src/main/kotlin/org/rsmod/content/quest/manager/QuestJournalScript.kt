package org.rsmod.content.quest.manager

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.ui.ifCloseModals
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class QuestJournalScript : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton("component.questlist:list") { evt ->
            val quest = Quest.getById(evt.comsub) ?: return@onIfOverlayButton
            if (QuestJournalRegistry.get(quest) == null) return@onIfOverlayButton

            val journalState = if (quest.questState(player) == QuestProgressState.NOT_STARTED) {
                JournalState.OVERVIEW
            } else {
                JournalState.LOG
            }

            QuestJournalRegistry.openJournal(this, quest, journalState)
        }

        onIfModalButton("component.questjournal_overview:close") {
            player.ifCloseModals(eventBus)
        }

        onIfModalButton("component.questjournal:close") {
            player.ifCloseModals(eventBus)
        }

        onIfModalButton("component.questjournal_overview:content_inner") {
            val quest = QuestJournalRegistry.activeQuest(player) ?: return@onIfModalButton

            player.ifOpenOverlay("interface.worldmap", eventBus)
            player.ifSetEvents(
                "component.worldmap:close",
                0..1,
                IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4,
            )

            quest.startCoord?.let { coord ->
                quest.mapElement?.let { element ->
                    player.runClientScript(
                        3331,
                        RSCM.getRSCM("component.worldmap:map_noclick"),
                        coord.packed,
                        element,
                    )
                }
            }
        }

        onIfModalButton("component.questjournal:switch") {
            val quest = QuestJournalRegistry.activeQuest(player) ?: return@onIfModalButton
            QuestJournalRegistry.openJournal(this, quest, JournalState.OVERVIEW)
        }

        onIfModalButton("component.questjournal_overview:switch") {
            val quest = QuestJournalRegistry.activeQuest(player) ?: return@onIfModalButton
            QuestJournalRegistry.openJournal(this, quest, JournalState.LOG)
        }
    }
}
