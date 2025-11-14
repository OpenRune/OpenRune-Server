package org.alter.quest.manager

import org.alter.api.*
import org.alter.api.ext.InterfaceEvent
import org.alter.api.ext.closeInterface
import org.alter.api.ext.getVarp
import org.alter.api.ext.openInterface
import org.alter.api.ext.runClientScript
import org.alter.api.ext.setComponentText
import org.alter.api.ext.setInterfaceEvents
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.pluginnew.event.impl.LoginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.interfaces.WorldMapEvents.Companion.WORLD_MAP_INTERFACE_ID
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType
import toRs

/**
 * Base class for quest content scripts backed by dbrow definitions.
 *
 * Implementors provide the quest key and register event listeners inside [onRegister].
 */
enum class JournalState { OVERVIEW, LOG }

enum class QuestProgressState(val varp : Int) {
    NOT_STARTED(0),
    IN_PROGRESS(1),
    FINISHED(2),
}

@DslMarker
annotation class QuestJournalDsl

data class QuestReward(
    val xp: Map<Int, Int> = emptyMap(),
    val items: List<Pair<String, Int>> = emptyList(),
    val extraText: String? = null
)

@QuestJournalDsl
fun rewards(builder: QuestRewardBuilder.() -> Unit): QuestReward {
    return QuestRewardBuilder().apply(builder).build()
}

@QuestJournalDsl
class QuestRewardBuilder {
    private val _xp = mutableMapOf<Int, Int>()
    private val _items = mutableListOf<Pair<String, Int>>()
    private var _extraText: String? = null

    fun xp(skill: Int, amount: Int) {
        _xp[skill] = amount
    }

    fun item(id: String, amount: Int = 1) {
        _items.add(id to amount)
    }

    fun extra(text: String) {
        _extraText = text
    }

    fun build(): QuestReward = QuestReward(_xp, _items, _extraText)
}

abstract class QuestScript(questKey: String, val questVarp : String, val rewards: QuestReward) : PluginEvent() {

    protected val quest: Quest

    abstract fun subTitle(): String

    abstract fun questLog(player: Player): String

    abstract fun completedLog(player: Player): String




    init {
        RSCM.requireRSCM(RSCMType.ROWTYPES, questKey)

        quest = Quest.register(questKey,questVarp,rewards)

        on<LoginEvent> {
            then {
                val state = quest.getQuestStage(player)
                val prog = when(state) {
                    0 -> QuestProgressState.NOT_STARTED
                    quest.maxSteps -> QuestProgressState.FINISHED
                    else -> QuestProgressState.IN_PROGRESS
                }
                player.setVarp(questVarp,prog.varp)
             }

        }

        on<ButtonClickEvent> {
            where { component.combinedId == "components.questlist:list".asRSCM() && slot == quest.id }
            then {
                val journalState = if (player.getVarp(questVarp) == QuestProgressState.NOT_STARTED.varp) JournalState.OVERVIEW else JournalState.LOG
                openJournal(player, journalState)
            }
        }

        // Switch buttons
        onButton("components.questjournal:switch") { openJournal(player, JournalState.OVERVIEW) }
        onButton("components.questjournal_overview:switch") { openJournal(player, JournalState.LOG) }

        onButton("components.questjournal:close") {
            player.closeInterface(InterfaceDestination.MAIN_SCREEN)
        }

        onButton("components.questjournal_overview:close") {
            player.closeInterface(InterfaceDestination.MAIN_SCREEN)
        }

        onButton("components.questjournal_overview:content_inner") {
            player.openInterface(
                interfaceId = WORLD_MAP_INTERFACE_ID,
                dest = InterfaceDestination.WORLD_MAP,
                fullscreen = false
            )
            player.setInterfaceEvents(
                interfaceId = WORLD_MAP_INTERFACE_ID,
                component = 21,
                range = 0..4,
                setting = InterfaceEvent.ClickOp1
            )
            player.runClientScript(
                CommonClientScripts.WORLD_MAP_GOTO,
                RSCM.getRSCM("components.worldmap:map_noclick"),
                quest.startCoord,
                quest.mapElement
            )
        }
    }

    fun openJournal(player: Player, type: JournalState) {
        when (type) {
            JournalState.OVERVIEW -> openJournalOverview(player)
            JournalState.LOG -> openQuestLog(player)
        }
    }

    private fun openJournalOverview(player: Player) {
        player.openInterface("interfaces.questjournal_overview", InterfaceDestination.MAIN_SCREEN)
        player.setInterfaceEvents("components.questjournal_overview:content_inner", 0..27, InterfaceEvent.ClickOp1)
        player.setComponentText("components.questjournal_overview:title", "<col=7f0000>${quest.displayName}</col>")

        player.runClientScript(
            CommonClientScripts.QUEST_OVERVIEW,
            quest.rowID,
            subTitle(),
            RSCM.getRSCM("components.questjournal_overview:universe"),
            RSCM.getRSCM("components.questjournal_overview:content_inner"),
            RSCM.getRSCM("components.questjournal_overview:content_outer"),
            RSCM.getRSCM("components.questjournal_overview:scrollbar"),
            RSCM.getRSCM("components.questjournal_overview:inner"),
            RSCM.getRSCM("components.questjournal_overview:container"),
            RSCM.getRSCM("components.questjournal_overview:scroll"),
            player.combatLevel
        )
    }

    private fun openQuestLog(player: Player) {
        val lines = (if (quest.isQuestCompleted(player)) completedLog(player) else questLog(player))
            .lines()
            .flatMap { it.toRs(inheritPreviousTags = true, wrapAt = 64).split("<br>") }

        player.openInterface("interfaces.questjournal", InterfaceDestination.MAIN_SCREEN)
        player.runClientScript(CommonClientScripts.QUEST_JOURNAL_RESET)
        player.setComponentText("components.questjournal:title", "<col=7f0000>${quest.displayName}</col>")

        lines.forEachIndexed { index, line ->
            player.setComponentText("components.questjournal:qj${index + 1}", line)
        }
    }


    protected fun questJournal(
        player: Player,
        builder: QuestJournalBuilder.() -> Unit
    ): String = buildQuestJournal(player, quest, builder)

    protected fun completionJournal(
        player: Player,
        builder: QuestJournalBuilder.() -> Unit
    ): String = buildCompletionJournal(player, quest, builder)
}