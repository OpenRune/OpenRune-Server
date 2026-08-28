package org.rsmod.content.skills.crafting.util

import dev.openrune.tables.skills.QuestReq
import dev.openrune.tables.skills.VarbitCompare
import org.rsmod.content.quest.manager.QuestRequirement
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.game.entity.Player

data class CraftingQuestReq(val quest: String, val requirement: QuestRequirement)

data class CraftingVarbitReq(val varbit: String, val compare: VarbitCompare, val value: Int)

fun craftingQuestReq(quest: String?, requirement: Int?): CraftingQuestReq? {
    if (quest.isNullOrBlank()) {
        return null
    }
    val parsed =
        when (QuestReq.of(requirement) ?: QuestReq.Completed) {
            QuestReq.Completed -> QuestRequirement.Completed
            QuestReq.InProgress -> QuestRequirement.InProgress
            QuestReq.NotCompleted -> QuestRequirement.NotCompleted
        }
    return CraftingQuestReq(quest, parsed)
}

fun craftingVarbitReq(varbit: String?, compare: Int?, value: Int?): CraftingVarbitReq? {
    if (varbit.isNullOrBlank()) {
        return null
    }
    return CraftingVarbitReq(varbit, VarbitCompare.of(compare) ?: VarbitCompare.GTE, value ?: 1)
}

fun Player.meetsUnlocks(product: CraftingProduct): Boolean =
    product.questReqs.all { QuestRequirements.satisfies(this, it.quest, it.requirement) } &&
        product.varbitReqs.all { req -> req.compare.passes(vars[req.varbit], req.value) }

fun Player.hasCompletedQuest(quest: String): Boolean = QuestRequirements.hasCompleted(this, quest)
