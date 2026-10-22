package org.rsmod.content.quest.manager

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.util.Locale
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.table.QuestRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

const val QUEST_COMPLETE_JINGLE = "jingle.gaol"

internal const val QUEST_DIALOGUE_CLOSE_TIMER = "timer.quest_dialogue_close"
internal const val QUEST_SCROLL_CLOSE_TIMER = "timer.quest_scroll_close"

private fun scheduleClose(
    access: ProtectedAccess,
    behaviour: QuestClose,
    timer: String,
    closeNow: ProtectedAccess.() -> Unit,
) {
    val delay = behaviour.delayCycles ?: return
    if (delay <= 0) access.closeNow() else access.softTimer(timer, delay)
}

val QUEST_STAGE_MAP_ATTR = AttributeKey<MutableMap<String, Int>>("quest_stages")

data class ItemRewardDisplay(val item: String, val zoom: Int = 10)

data class Quest(
    val id: Int,
    val key: String,
    val rowID: Int,
    val displayName: String,
    val mapElement: Int?,
    val startCoord: CoordGrid?,
    val maxSteps: Int,
    val questPoints: Int,
    val questVarp: String,
    val rewards: QuestReward,
    val itemDisplay: ItemRewardDisplay,
    val questVarbit: String? = null,
    val closeDialogue: QuestClose = QuestClose.OnFinished,
    val closeScroll: QuestClose = QuestClose.Never,
    val completeJingle: String = QUEST_COMPLETE_JINGLE,
) {

    private var Player.questState: Int
        get() = vars[questVarbit ?: questVarp]
        set(value) { VarPlayerIntMapSetter.set(this, questVarbit ?: questVarp, value) }
    private var Player.questPoints by intVarp("varp.qp")
    private var Player.questsCompleted by intVarBit("varbit.quests_completed_count")

    private val attributeRegistry = mutableMapOf<String, QuestAttribute<*>>()
    private val itemRewards = rewards.items.map { (item, amount) -> InvObj(item, amount) }

    companion object {
        private val logger = InlineLogger()
        private val questsByKey = mutableMapOf<String, Quest>()

        fun get(key: String): Quest? = questsByKey[key.normalizedQuestKey()]

        fun getById(id: Int): Quest? = questsByKey.values.find { it.id == id }

        fun all(): Collection<Quest> = questsByKey.values

        fun register(
            rowKey: String,
            varp: String,
            itemDisplay: ItemRewardDisplay,
            rewards: QuestReward,
            varbit: String? = null,
            closeDialogue: QuestClose = QuestClose.OnFinished,
            closeScroll: QuestClose = QuestClose.Never,
            completeJingle: String = QUEST_COMPLETE_JINGLE,
        ): Quest {

            val rowKeyID = "dbrow.${rowKey}".asRSCM()
            val questRow = QuestRow.getRow(rowKeyID)
            val quest = Quest(
                id = questRow.id,
                rowID = rowKeyID,
                key = rowKey,
                displayName = questRow.displayname,
                mapElement = questRow.mapelement,
                startCoord = questRow.startcoord,
                maxSteps = questRow.endstate,
                questPoints = questRow.questpoints,
                questVarp = varp,
                questVarbit = varbit,
                itemDisplay = itemDisplay,
                rewards = rewards,
                closeDialogue = closeDialogue,
                closeScroll = closeScroll,
                completeJingle = completeJingle,
            )
            questsByKey[rowKey.normalizedQuestKey()] = quest
            return quest
        }
    }

    fun getQuestStage(access: Player): Int {
        if (questVarbit != null) return access.questState
        val stages = access.attr.getOrPut(QUEST_STAGE_MAP_ATTR) { mutableMapOf() }
        return stages[key] ?: 0
    }

    fun migrateLegacyStage(player: Player) {
        if (questVarbit == null) return
        val legacy = player.attr[QUEST_STAGE_MAP_ATTR]?.remove(key) ?: return
        if (player.questState == 0) {
            player.questState = legacy.coerceIn(0, maxSteps)
        }
    }

    private fun storeQuestStage(access: ProtectedAccess, stage: Int) {
        if (questVarbit != null) return
        val clampedStage = stage.coerceIn(0, maxSteps)
        val stages = access.player.attr.getOrPut(QUEST_STAGE_MAP_ATTR) { mutableMapOf() }
        stages[key] = clampedStage
    }

    fun questState(player: Player): QuestProgressState =
        QuestProgressState.fromStage(getQuestStage(player), maxSteps)

    fun isQuestNotStarted(player: Player): Boolean = questState(player).isNotStarted

    fun isQuestInProgress(player: Player): Boolean = questState(player).isInProgress

    fun isQuestCompleted(player: Player): Boolean = questState(player).isCompleted

    fun advanceQuestStage(access: ProtectedAccess, amount: Int = 1): Boolean {
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

        return setQuestStage(access, attemptedStage.coerceIn(0, maxSteps))
    }

    fun completeQuest(
        access: ProtectedAccess,
        additionalRewards: List<InvObj> = emptyList(),
        onCompleted: ProtectedAccess.() -> Unit = {},
    ): Boolean = setQuestStage(access, maxSteps, additionalRewards, onCompleted)

    fun setQuestStage(
        access: ProtectedAccess,
        newStage: Int,
        additionalRewards: List<InvObj> = emptyList(),
        onCompleted: ProtectedAccess.() -> Unit = {},
    ): Boolean {
        require(newStage in 0..maxSteps) { "Quest '$key' stage must be within 0..$maxSteps." }
        val oldStage = getQuestStage(access.player)
        val wasCompleted = oldStage >= maxSteps
        if (!wasCompleted && newStage == maxSteps && !grantItemRewards(access, additionalRewards)) {
            return false
        }
        storeQuestStage(access, newStage)

        // Quest varps store the real stage (0..endstate). Multinpc / journal clients depend on
        // endstate (e.g. runemysteries=6) rather than a collapsed 0/1/2 progress flag.
        if (access.player.questState != newStage) {
            access.player.questState = newStage
        }

        if (oldStage == 0 && newStage > 0) {
            access.mes("You've started a new quest: @q_mes@$displayName</col>")
        }

        if (!wasCompleted && newStage >= maxSteps) {
            access.onCompleted()
            completedQuest(access)
        }

        return true
    }

    private fun grantItemRewards(access: ProtectedAccess, additionalRewards: List<InvObj>): Boolean {
        if (itemRewards.isEmpty() && additionalRewards.isEmpty()) return true
        val transaction = access.player.invTransaction(access.inv, autoCommit = false) {
            val inventory = select(access.inv)
            for (reward in additionalRewards) {
                add(inventory, reward.id, reward.count, vars = reward.vars)
            }
            for (reward in itemRewards) {
                add(inventory, reward.id, reward.count, vars = reward.vars)
            }
        }
        if (transaction.failure) return false
        transaction.commitAll()
        return true
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
        access.mes("Congratulations, you've completed a quest: @q_mes@$displayName</col>")

        access.player.midiJingle(completeJingle)

        scheduleClose(access, closeDialogue, QUEST_DIALOGUE_CLOSE_TIMER) { ifCloseChat() }
        access.ifOpenMain("interface.questscroll")
        scheduleClose(access, closeScroll, QUEST_SCROLL_CLOSE_TIMER) {
            ifCloseSub("interface.questscroll")
        }
        access.ifSetText("component.questscroll:quest_title", "You have completed ${displayName}!")
        val questPointLabel = if (questPoints == 1) "Quest Point" else "Quest Points"
        access.ifSetText("component.questscroll:quest_reward1", "$questPoints $questPointLabel")

        access.ifSetObj("component.questscroll:quest_model", obj = itemDisplay.item, zoom = itemDisplay.zoom)

        val rewardLines = mutableListOf<String>()

        rewards.xp.forEach { (skill, amount) ->
            val stat = ServerCacheManager.getStats(skill.asRSCM(RSCMType.STAT))
                ?: error("No stat found for $skill")

            access.statAdvance(skill, amount)
            val skillName = stat.displayName.replaceFirstChar { it.uppercase() }
            rewardLines.add("${"%,d".format(Locale.US, amount.toInt())} $skillName XP")
        }

        rewards.items.forEach { (item, amount) ->
            val type = ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ)) ?: error("No item found for $item")
            rewardLines.add(rewards.itemLabels[item] ?: "$amount x ${type.name}")
        }

        rewardLines += rewards.extraLines

        val linesToShow = rewardLines.take(6)

        for (i in 0 until 6) {
            val componentId = "component.questscroll:quest_reward${i + 2}"
            val text = linesToShow.getOrNull(i) ?: ""
            access.ifSetText(componentId, text)
        }
    }
}
