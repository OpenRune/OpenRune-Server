package org.alter.quest.manager

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.InterfaceDestination
import org.alter.api.Skills
import org.alter.api.ext.getVarp
import org.alter.api.ext.setComponentText
import org.alter.api.ext.setVarp
import org.alter.api.ext.toItem
import org.alter.game.model.Tile
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.QUEST_STAGE_MAP_ATTR
import org.alter.game.model.entity.GroundItem
import org.alter.game.model.entity.Player
import org.alter.interfaces.ifOpenMain
import org.alter.rscm.RSCM.asRSCM
import org.generated.tables.QuestRow

data class Quest(
    val id: Int,
    val key: String,
    val rowID : Int,
    val displayName: String,
    val mapElement: Int?,
    val startCoord: Tile?,
    val maxSteps: Int,
    val questPoints: Int,
    val questVarp: String,
    val rewards : QuestReward
) {

    private val attributeRegistry = mutableMapOf<String, QuestAttribute<*>>()

    companion object {
        private val logger = KotlinLogging.logger {}

        fun register(rowKey: String,varp: String, rewards : QuestReward): Quest {

            val rowKeyID = rowKey.asRSCM()
            val questRow = QuestRow.getRow(rowKeyID)
            return Quest(
                id = questRow.id,
                rowID = rowKeyID,
                key = rowKey.replace("dbrows.",""),
                displayName = questRow.displayname,
                mapElement = questRow.mapelement,
                startCoord = questRow.startcoord,
                maxSteps = questRow.endstate,
                questPoints = questRow.questpoints,
                questVarp = varp,
                rewards = rewards
            )
        }
    }

    fun getQuestStage(player: Player): Int {
        val stages = player.attr.getOrPut(QUEST_STAGE_MAP_ATTR) { mutableMapOf() }
        return stages[key] ?: 0
    }

    private fun setQuestStage(player: Player, stage: Int) {
        val clampedStage = stage.coerceIn(0, maxSteps)
        val stages = player.attr.getOrPut(QUEST_STAGE_MAP_ATTR) { mutableMapOf() }
        stages[key] = clampedStage
    }

    fun questState(player: Player): QuestProgressState =
        QuestProgressState.entries.find { player.getVarp(questVarp) == it.varp }
            ?: QuestProgressState.NOT_STARTED

    fun isQuestCompleted(player: Player): Boolean =
        questState(player) == QuestProgressState.FINISHED

    fun advanceQuestStage(player: Player, amount: Int = 1): Int {
        val currentStage = getQuestStage(player)
        val attemptedStage = currentStage + amount

        if (attemptedStage > maxSteps) {
            val playerName = player.username.ifEmpty { "unknown" }
            logger.error {
                "Attempted to advance quest '$key' for player '$playerName' " +
                        "from stage $currentStage by $amount (max=$maxSteps)."
            }
            throw IllegalStateException("Quest '$key' cannot advance past stage $maxSteps.")
        }

        val newStage = attemptedStage.coerceIn(0, maxSteps)
        setQuestStage(player, newStage)

        val newState = when {
            newStage <= 0 -> QuestProgressState.NOT_STARTED
            newStage >= maxSteps -> QuestProgressState.FINISHED
            else -> QuestProgressState.IN_PROGRESS
        }

        if (player.getVarp(questVarp) != newState.varp) {
            player.setVarp(questVarp, newState.varp)
        }

        if (newState == QuestProgressState.FINISHED) {
            completedQuest(player)
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

    private fun completedQuest(player: Player) {
        player.ifOpenMain("interfaces.questscroll")
        player.setComponentText("components.questscroll:quest_title", "You have completed ${displayName}!")
        player.setComponentText("components.questscroll:quest_reward1", "$questPoints Quest Point")

        val rewardLines = mutableListOf<String>()

        rewards.xp.forEach { (skill, amount) ->
            player.addXp(skill,amount)
            rewardLines.add("$amount ${Skills.getSkillName(skill)} XP")
        }

        rewards.items.forEach { (id, amount) ->
            val addItem = player.inventory.add(id.asRSCM(),amount)
            rewardLines.add("$amount x ${id.asRSCM().toItem().getName()}")
            if (addItem.hasFailed()) {
                player.world.spawn(GroundItem(id.asRSCM(),amount,player.tile,player))
            }
        }

        rewards.extraText?.let {
            rewardLines.add(it)
        }

        val linesToShow = rewardLines.take(6)

        for (i in 0 until 6) {
            val componentId = "components.questscroll:quest_reward${i + 2}"
            val text = linesToShow.getOrNull(i) ?: ""
            player.setComponentText(componentId, text)
        }

    }

}