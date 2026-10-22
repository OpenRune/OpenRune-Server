package org.rsmod.content.quest.manager

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

data class QuestJournalContent(
    val subTitle: () -> String,
    val questLog: (ProtectedAccess) -> String,
    val completedLog: (ProtectedAccess) -> String,
)

private val ACTIVE_QUEST_JOURNAL_ATTR = AttributeKey<String>("active_quest_journal")

private var Player.latestQuestJournal by intVarp("varp.latest_quest_journal")
private var Player.questJournalLines by intVarp("varp.qj_lines")

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
        access.ifCloseSub(QUEST_JOURNAL)
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
        val player = access.player
        val log = if (quest.isQuestCompleted(player)) content.completedLog(access) else content.questLog(access)
        val lines = journalLines(log)

        access.ifClose()
        player.latestQuestJournal = quest.rowID
        player.questJournalLines = 0
        repeat(lines.size) { player.questJournalLines++ }

        access.ifSetText("component.questjournal:title", "<col=7f0000>${quest.displayName}</col>")
        access.runClientScript(QUEST_JOURNAL_RESET.asRSCM(RSCMType.CLIENTSCRIPT))
        lines.forEachIndexed { index, line ->
            access.ifSetText("component.questjournal:qj${index + 1}", line)
        }
        access.ifOpenOverlay(QUEST_JOURNAL)
        player.runClientScript(
            QUEST_JOURNAL_LAYOUT.asRSCM(RSCMType.CLIENTSCRIPT),
            charArrayOf('1', 'i'),
            listOf(0, lines.size),
        )
    }

    private const val QUEST_JOURNAL = "interface.questjournal"
    private const val QUEST_JOURNAL_RESET = "clientscript.[clientscript,quest_journal_reset]"
    private const val QUEST_JOURNAL_LAYOUT = "clientscript.[clientscript,script2523]"
}
