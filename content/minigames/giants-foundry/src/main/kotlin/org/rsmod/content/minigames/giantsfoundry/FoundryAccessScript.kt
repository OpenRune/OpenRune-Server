package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpWorn1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Getting in and out of the foundry, its bank chest and the preform storage. */
class FoundryAccessScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ENTRANCE) { enter() }
        onOpLoc1(EXIT) { leave() }
        onOpLoc1(BANK_CHEST) { tryOpenBank() }
        onOpLoc1(STORAGE_EMPTY) { store() }
        onOpLoc1(STORAGE_FULL) { take() }
        onOpWorn1(PREFORM) { mes("You can only put the preform down in the preform storage.") }
        onPlayerLogin { login(player) }
        onPlayerLogout { autoStore(player) }
    }

    private fun ProtectedAccess.enter() {
        if (!QuestRequirements.hasCompleted(player, SLEEPING_GIANTS)) {
            mes("You need to complete Sleeping Giants to enter the foundry.")
            return
        }
        telejump(INSIDE)
    }

    private fun ProtectedAccess.leave() {
        if (player.holdsPreform()) {
            stashPreform()
            mes("You leave your preform in the preform storage.")
        }
        telejump(OUTSIDE)
    }

    private fun ProtectedAccess.store() {
        if (!player.holdsPreform()) {
            mes("You don't have a preform to store.")
            return
        }
        stashPreform()
    }

    private fun ProtectedAccess.stashPreform() {
        player.removePreform()
        player.preformStored = true
        closeHud()
    }

    private fun ProtectedAccess.take() {
        if (!player.preformStored) {
            return
        }
        if (equipPreform()) {
            player.preformStored = false
        }
    }

    /**
     * With quests treated as complete (game.yml `quest-requirements`), Sleeping Giants counts as
     * done, so its varbit must read finished too: Kovac's multinpc only shows his foundry ops at
     * 30. With real quest progress the varbit is left to the quest.
     */
    private fun login(player: Player) {
        if (QuestRequirements.hasCompleted(player, SLEEPING_GIANTS) && player.sleepingGiants < QUEST_COMPLETE) {
            player.sleepingGiants = QUEST_COMPLETE
        }
        if (player.preformStored) {
            player.mes("You notice your current commission is being kept warm in the preform storage.")
        }
    }

    /** A held preform goes back into storage when the player logs out. */
    private fun autoStore(player: Player) {
        if (player.holdsPreform()) {
            player.removePreform()
            player.preformStored = true
        }
    }

    private companion object {
        const val SLEEPING_GIANTS = "quest_sleepinggiants"
        const val QUEST_COMPLETE = 30
        const val ENTRANCE = "loc.giants_foundry_entrance"
        const val EXIT = "loc.giants_foundry_exit"
        const val BANK_CHEST = "loc.giants_foundry_chest"
        const val STORAGE_EMPTY = "loc.giants_foundry_preform_storage_empty"
        const val STORAGE_FULL = "loc.giants_foundry_preform_storage_full"

        // UNVERIFIED: no transport data has these. Inside matches the Sleeping Giants instance's
        // arrival (MRobertEvers/oldschool-clientc notes); outside is the Minigame Teleport tile.
        val INSIDE = CoordGrid(3366, 11484, 0)
        val OUTSIDE = CoordGrid(3361, 3149, 0)
    }
}
