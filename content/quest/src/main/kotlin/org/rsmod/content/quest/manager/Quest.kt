package org.rsmod.content.quest.manager

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.table.QuestRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

val QUEST_STAGE_MAP_ATTR = AttributeKey<MutableMap<String, Int>>("quest_stages")

data class ItemRewardDisplay(val item: String, val zoom: Int = 10)

data class Quest(
    val id: Int,
    val key: String,
    val rowID : Int,
    val displayName: String,
    val mapElement: Int?,
    val startCoord: CoordGrid?,
    val maxSteps: Int,
    val questPoints: Int,
    val questVarp: String,
    val rewards : QuestReward,
    val itemDisplay : ItemRewardDisplay
) {

    private var Player.questState by intVarp(questVarp)
    private var Player.questPoints by intVarp("varp.qp")
    private var Player.questsCompleted by intVarBit("varbit.quests_completed_count")

    private val attributeRegistry = mutableMapOf<String, QuestAttribute<*>>()

    companion object {
        private val logger = InlineLogger()

        fun register(
            rowKey: String,
            varp: String,
            itemDisplay : ItemRewardDisplay,
            rewards : QuestReward
        ): Quest {

            val rowKeyID = "dbrow.${rowKey}".asRSCM()
            val questRow = QuestRow.getRow(rowKeyID)
            return Quest(
                id = questRow.id,
                rowID = rowKeyID,
                key = rowKey,
                displayName = questRow.displayname,
                mapElement = questRow.mapelement,
                startCoord = questRow.startcoord,
                maxSteps = questRow.endstate,
                questPoints = questRow.questpoints,
                questVarp = varp,
                itemDisplay = itemDisplay,
                rewards = rewards
            )
        }
    }

    fun getQuestStage(access: Player): Int {
        val stages = access.attr.getOrPut(QUEST_STAGE_MAP_ATTR) { mutableMapOf() }
        return stages[key] ?: 0
    }

    private fun setQuestStage(access: ProtectedAccess, stage: Int) {
        val clampedStage = stage.coerceIn(0, maxSteps)
        val stages = access.player.attr.getOrPut(QUEST_STAGE_MAP_ATTR) { mutableMapOf() }
        stages[key] = clampedStage
    }

    fun questState(player: Player): QuestProgressState =
        QuestProgressState.entries.find { player.questState == it.varp }
            ?: QuestProgressState.NOT_STARTED

    fun isQuestCompleted(player: Player): Boolean =
        questState(player) == QuestProgressState.FINISHED

    fun advanceQuestStage(access: ProtectedAccess, amount: Int = 1): Int {
        val currentStage = getQuestStage(access.player)
        val attemptedStage = currentStage + amount

        if (attemptedStage > maxSteps) {
            val playerName = access.player.displayName.ifEmpty { "unknown" }
            logger.error {
                "Attempted to advance quest '$key' for player '$playerName' " +
                    "from stage $currentStage by $amount (max=$maxSteps)."
            }
            throw IllegalStateException("Quest '$key' cannot advance past stage $maxSteps.")
        }

        val newStage = attemptedStage.coerceIn(0, maxSteps)
        setQuestStage(access, newStage)

        val newState = when {
            newStage <= 0 -> QuestProgressState.NOT_STARTED
            newStage >= maxSteps -> QuestProgressState.FINISHED
            else -> QuestProgressState.IN_PROGRESS
        }

        if (access.player.questState != newState.varp) {
            access.player.questState = newState.varp
        }

        if (newState == QuestProgressState.FINISHED) {
            completedQuest(access)
        }


        return newStage
    }

    fun <T> attribute(
        name: String,
        default: T,
        resetOnDeath: Boolean = false,
        temp: Boolean = false
    ): QuestAttribute<T> = attribute(name, { default }, resetOnDeath, temp)

    fun <T> attribute(
        name: String,
        default: () -> T,
        resetOnDeath: Boolean = false,
        temp: Boolean = false
    ): QuestAttribute<T> {
        @Suppress("UNCHECKED_CAST")
        return attributeRegistry.getOrPut(name) {
            QuestAttribute(
                name = name,
                attributeKey = AttributeKey(
                    persistenceKey = "quest.$key.$name",
                    resetOnDeath = resetOnDeath,
                    temp = temp
                ),
                defaultProvider = default
            )
        } as QuestAttribute<T>
    }

    private fun completedQuest(access: ProtectedAccess) {

        access.player.questPoints += questPoints
        access.player.questsCompleted++

        access.ifOpenMain("interface.questscroll")
        access.ifSetText("component.questscroll:quest_title", "You have completed ${displayName}!")
        access.ifSetText("component.questscroll:quest_reward1", "$questPoints Quest Point")

        access.ifSetObj("component.questscroll:quest_model", obj = itemDisplay.item, zoom = itemDisplay.zoom)

        val rewardLines = mutableListOf<String>()

        rewards.xp.forEach { (skill, amount) ->
            val stat = ServerCacheManager.getStats(skill.asRSCM(RSCMType.STAT))
                ?: error("No stat found for $skill")

            access.statAdvance(skill,amount)
            rewardLines.add("${amount.toInt()} ${stat.displayName} XP")
        }

        rewards.items.forEach { (item, amount) ->
            access.invAdd(access.inv,item,amount)
            val type = ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ))?: error("No item found for $item")
            rewardLines.add("$amount x ${type.name}")
        }

        rewards.extraText?.let {
            rewardLines.add(it)
        }

        val linesToShow = rewardLines.take(6)

        for (i in 0 until 6) {
            val componentId = "component.questscroll:quest_reward${i + 2}"
            val text = linesToShow.getOrNull(i) ?: ""
            access.ifSetText(componentId, text)
        }

    }

}
