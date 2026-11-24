package org.rsmod.content.travel.teleports

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.manager.QuestRequirements

/**
 * Quest gates on travel. Each check goes through [QuestRequirements], so it follows the server's
 * quest requirement policy: `assume-completed` lets everything through, `respect-progress` reads
 * the player's real quest state. A destination's unlock varbit (a memoirs page) must be set too.
 */
object TravelRequirements {
    fun ProtectedAccess.canReach(destination: TeleportDestination): Boolean =
        destination.quests.all { QuestRequirements.hasCompleted(player, it) } &&
            destination.questsStarted.all {
                QuestRequirements.hasCompleted(player, it) ||
                    QuestRequirements.isOnQuest(player, it)
            } &&
            destination.unlockVarbit.let { it == null || vars[it] != 0 }

    /** Tells the player which quest stands in the way and returns `false` when one does. */
    fun ProtectedAccess.checkReach(destination: TeleportDestination): Boolean {
        val missing =
            destination.quests.firstOrNull { !QuestRequirements.hasCompleted(player, it) }
        if (missing != null) {
            mes("You need to complete ${missing.questName()} to travel there.")
            return false
        }
        val unstarted =
            destination.questsStarted.firstOrNull {
                !QuestRequirements.hasCompleted(player, it) &&
                    !QuestRequirements.isOnQuest(player, it)
            }
        if (unstarted != null) {
            mes("You need to have started ${unstarted.questName()} to travel there.")
            return false
        }
        val unlock = destination.unlockVarbit
        if (unlock != null && vars[unlock] == 0) {
            // UNVERIFIED: wording for a memoirs page that has not been added
            mes("You need to add the page for ${destination.name} to your book first.")
            return false
        }
        return true
    }

    fun ProtectedAccess.checkQuest(quest: String): Boolean {
        if (QuestRequirements.hasCompleted(player, quest)) {
            return true
        }
        mes("You need to complete ${quest.questName()} to travel there.")
        return false
    }

    private fun String.questName(): String = QuestRow.getRow("dbrow.$this").displayname
}
