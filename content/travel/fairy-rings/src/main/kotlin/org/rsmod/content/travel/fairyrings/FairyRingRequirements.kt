package org.rsmod.content.travel.fairyrings

import org.rsmod.api.attr.AttributeKey
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player

/** Keep virtual quest completion separate from the real native permission/progress bits. */
internal object FairyRingRequirements {
    fun permission(player: Player): Boolean =
        QuestRequirements.hasCompleted(player, "quest_fairytale2") ||
            player.vars["varbit.fairyring_permission"] != 0

    fun destinationDenial(player: Player, code: String): String? {
        val requirement = quests[code]
        if (requirement != null && !QuestRequirements.hasCompleted(player, requirement.first) &&
            !(code in partialQuests && QuestRequirements.isOnQuest(player, requirement.first))) {
            return "You need to ${if (code in partialQuests) "progress further in" else "complete"} ${requirement.second} before using this fairy ring."
        }
        // The fork's assume-completed policy keeps its accepted free world-travel behaviour.
        if (code in varlamore && QuestRequirements.activePolicy().mode != QuestRequirementMode.AssumeCompleted &&
            player.vars["varbit.varlamore_visited"] == 0) {
            return "Travel to Varlamore with Regulus Cento before using this fairy ring."
        }
        if (QuestRequirements.activePolicy().mode != QuestRequirementMode.AssumeCompleted) {
            if (code == "DLP" && player.vars["varbit.grimstone_visited"] == 0) return "Visit Grimstone before using this fairy ring."
            if (code == "CJQ" && player.attr[GREAT_CONCH_VISITED] != true && player.vars["varp.tt_primary"] < 44 && !QuestRequirements.hasCompleted(player, "quest_troubledtortugans")) return "Reach the Great Conch during Troubled Tortugans before using this fairy ring."
        }
        return null
    }

    val GREAT_CONCH_VISITED = AttributeKey<Boolean>(persistenceKey = "fairyring_great_conch_visited")

    private val partialQuests = setOf("BJR", "BLQ")
    private val varlamore = setOf("AIS", "AJP", "CKQ")
    private val quests = mapOf(
        "AIS" to ("quest_childrenofthesun" to "Children of the Sun"),
        "AJP" to ("quest_childrenofthesun" to "Children of the Sun"),
        "CKQ" to ("quest_childrenofthesun" to "Children of the Sun"),
        "AJQ" to ("quest_deathtothedorgeshuun" to "Death to the Dorgeshuun"),
        "BJR" to ("quest_holygrail" to "Holy Grail"),
        "BJS" to ("quest_regicide" to "Regicide"),
        "BLQ" to ("quest_landofthegoblins" to "Land of the Goblins"),
        "CIP" to ("quest_fremenniktrials" to "The Fremennik Trials"),
        "CLR" to ("quest_monkeymadness1" to "Monkey Madness I"),
        "DLS" to ("quest_insearchofthemyreque" to "In Search of the Myreque"),
    )
}
