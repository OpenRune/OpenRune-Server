package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.quest.manager.QuestRequirements

object HunterClues {
    internal const val X_MARKS_THE_SPOT = "quest_xmarksthespot"
    internal const val BANK = "inv.bank"
    internal const val BASE_STACK_LIMIT = 2

    internal fun scrollBox(tier: String): String = "obj.league_clue_box_$tier"

    internal fun ProtectedAccess.scrollBoxes(): Boolean =
        QuestRequirements.hasCompleted(player, X_MARKS_THE_SPOT)

    internal fun ProtectedAccess.heldClues(tier: String): Int {
        val names = setOf("clue scroll ($tier)", "challenge scroll ($tier)")
        val box = scrollBox(tier)
        return listOf(inv, inv(BANK)).sumOf { held ->
            held.count(box) +
                held.objs.count { obj ->
                    obj != null && ServerCacheManager.getItem(obj.id)?.name?.lowercase() in names
                }
        }
    }

    internal fun ProtectedAccess.stackLimit(tier: String): Int =
        BASE_STACK_LIMIT +
            vars["varbit.scroll_case_${tier}_minor"] +
            vars["varbit.scroll_case_${tier}_major"] +
            vars["varbit.scroll_case_mimic"]

    internal fun ProtectedAccess.canReceiveClue(tier: String): Boolean =
        if (scrollBoxes()) heldClues(tier) < stackLimit(tier) else heldClues(tier) == 0

    internal fun ProtectedAccess.clueReward(tier: String, clue: String): String? {
        if (!canReceiveClue(tier)) {
            val item = if (scrollBoxes()) "scroll box" else "clue scroll"
            mes("You have a sneaking suspicion that you would have received ${HunterRumours.article(tier)} $tier $item.")
            return null
        }
        return if (scrollBoxes()) scrollBox(tier) else clue
    }
}
