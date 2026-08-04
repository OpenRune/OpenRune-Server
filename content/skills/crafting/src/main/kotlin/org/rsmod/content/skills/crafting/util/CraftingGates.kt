package org.rsmod.content.skills.crafting.util

import dev.openrune.tables.skills.QuestReq
import dev.openrune.tables.skills.VarbitCompare
import org.rsmod.content.quest.manager.QuestRequirement
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.game.entity.Player

data class CraftingQuestReq(val quest: String, val requirement: QuestRequirement)

data class CraftingVarbitReq(val varbit: String, val compare: VarbitCompare, val value: Int)

/** Reads a quest gate/requirement off a recipe row, defaulting to requiring completion. */
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

/** Reads a varbit gate off a recipe row, defaulting to at least the given value. */
fun craftingVarbitReq(varbit: String?, compare: Int?, value: Int?): CraftingVarbitReq? {
    if (varbit.isNullOrBlank()) {
        return null
    }
    return CraftingVarbitReq(varbit, VarbitCompare.of(compare) ?: VarbitCompare.GTE, value ?: 1)
}

/** Whether every gate/requirement on [product] passes. An unlock varbit missing from the cache counts as passed. */
fun Player.meetsUnlocks(product: CraftingProduct): Boolean =
    product.questReqs.all { QuestRequirements.satisfies(this, it.quest, it.requirement) } &&
        product.varbitReqs.all { req ->
            !CraftingGamevals.exists(req.varbit) || req.compare.passes(vars[req.varbit], req.value)
        }

/** Whether the quest manager considers [quest] complete for this player. */
fun Player.hasCompletedQuest(quest: String): Boolean = QuestRequirements.hasCompleted(this, quest)
