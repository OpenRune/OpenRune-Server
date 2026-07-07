package org.rsmod.content.quest.manager

import dev.openrune.rscm.RSCM
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import toRs

data class QuestJournalContent(
    val subTitle: () -> String,
    val questLog: (ProtectedAccess) -> String,
    val completedLog: (ProtectedAccess) -> String,
)

private val ACTIVE_QUEST_JOURNAL_ATTR = AttributeKey<String>("active_quest_journal")

object QuestJournalRegistry {
    private val journals = mutableMapOf<String, QuestJournalContent>()

    fun register(quest: Quest, content: QuestJournalContent) {
        journals[quest.key.normalizedQuestKey()] = content
    }

    fun get(quest: Quest): QuestJournalContent? = journals[quest.key.normalizedQuestKey()]

    fun getById(id: Int): QuestJournalContent? {
        val quest = Quest.getById(id) ?: return null
        return get(quest)
    }

    fun openJournal(access: ProtectedAccess, quest: Quest, type: JournalState) {
        val content = get(quest) ?: return
        access.player.attr[ACTIVE_QUEST_JOURNAL_ATTR] = quest.key
        when (type) {
            JournalState.OVERVIEW -> openJournalOverview(access, quest, content)
            JournalState.LOG -> openQuestLog(access, quest, content)
        }
    }

    fun activeQuest(player: Player): Quest? {
        val key = player.attr[ACTIVE_QUEST_JOURNAL_ATTR] ?: return null
        return Quest.get(key)
    }

    private fun openJournalOverview(access: ProtectedAccess, quest: Quest, content: QuestJournalContent) {
        access.ifOpenMain("interface.questjournal_overview")
        access.ifSetText("component.questjournal_overview:title", "<col=7f0000>${quest.displayName}</col>")

        access.runClientScript(
            6821,
            quest.rowID,
            content.subTitle(),
            RSCM.getRSCM("component.questjournal_overview:universe"),
            RSCM.getRSCM("component.questjournal_overview:content_inner"),
            RSCM.getRSCM("component.questjournal_overview:content_outer"),
            RSCM.getRSCM("component.questjournal_overview:scrollbar"),
            RSCM.getRSCM("component.questjournal_overview:inner"),
            RSCM.getRSCM("component.questjournal_overview:container"),
            RSCM.getRSCM("component.questjournal_overview:scroll"),
            access.player.combatLevel,
        )
    }

    private fun openQuestLog(access: ProtectedAccess, quest: Quest, content: QuestJournalContent) {
        val lines = (if (quest.isQuestCompleted(access.player)) content.completedLog(access) else content.questLog(access))
            .lines()
            .flatMap { it.toRs(inheritPreviousTags = true, wrapAt = 64).split("<br>") }

        access.ifOpenMain("interface.questjournal")
        access.runClientScript(5240)
        access.ifSetText("component.questjournal:title", "<col=7f0000>${quest.displayName}</col>")

        lines.forEachIndexed { index, line ->
            access.ifSetText("component.questjournal:qj${index + 1}", line)
        }
    }
}
