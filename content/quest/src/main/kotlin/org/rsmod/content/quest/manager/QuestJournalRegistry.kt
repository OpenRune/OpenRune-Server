package org.rsmod.content.quest.manager

import dev.openrune.definition.type.widget.IfEvent
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
        val log = if (type == JournalState.LOG) {
            val source = if (quest.isQuestCompleted(access.player)) content.completedLog(access) else content.questLog(access)
            source.lines().joinToString("<br>") { it.toRs(inheritPreviousTags = true, wrapAt = 10_000) }
        } else ""
        access.ifOpenMain("interface.quest_guide")
        access.ifSetEvents("component.quest_guide:body", 0..200, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4)
        access.runClientScript(
            RSCM.getRSCM("clientscript.quest_guide_draw"), quest.rowID, content.subTitle(),
            quest.displayName, log, access.player.combatLevel, if (type == JournalState.OVERVIEW) 0 else 1,
        )
    }

    fun activeQuest(player: Player): Quest? {
        val key = player.attr[ACTIVE_QUEST_JOURNAL_ATTR] ?: return null
        return Quest.get(key)
    }
}
