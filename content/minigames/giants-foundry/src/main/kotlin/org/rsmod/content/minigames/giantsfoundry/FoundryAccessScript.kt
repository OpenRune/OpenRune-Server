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

// UNVERIFIED: no transport data has the arrival tile; MRobertEvers/oldschool-clientc's notes
// give it for the Sleeping Giants instance.
internal val FOUNDRY_INSIDE = CoordGrid(3366, 11484, 0)

internal fun ProtectedAccess.enterFoundry() = telejump(FOUNDRY_INSIDE)

/** Getting in and out of the foundry, its bank chest and the preform storage. */
class FoundryAccessScript @Inject constructor(private val room: SleepingGiantsRoom) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(EXIT) { leave() }
        onOpLoc1(BANK_CHEST) { tryOpenBank() }
        onOpLoc1(STORAGE_EMPTY) { store() }
        onOpLoc1(STORAGE_FULL) { take() }
        onOpWorn1(PREFORM) { mes("You can only put the preform down in the preform storage.") }
        onPlayerLogin { login(player) }
        onPlayerLogout { autoStore(player) }
    }

    /** The cave entrance is handled by [SleepingGiantsQuest], which routes by quest stage. */
    private fun ProtectedAccess.leave() {
        if (player.holdsPreform()) {
            stashPreform()
            mes("You leave your preform in the preform storage.")
        }
        telejump(OUTSIDE)
        room.left(player)
    }

    private fun ProtectedAccess.store() {
        if (!player.holdsPreform()) {
            mes("You don't have a preform to store.")
            return
        }
        player.preformStored = true
        mes(
            "<col=229628>You place the preform in the handy storage area. It should stay nice " +
                "and warm there."
        )
        anim(STORAGE_SEQ)
        player.removePreform()
        closeHud()
    }

    private fun ProtectedAccess.stashPreform() {
        player.removePreform()
        player.preformStored = true
        closeHud()
    }

    private suspend fun ProtectedAccess.take() {
        if (!player.preformStored || !handsFreeForPreform()) {
            return
        }
        player.preformStored = false
        mes("You take the preform from the handy storage area. It will start to get cold now.")
        anim(STORAGE_SEQ)
        equipPreform()
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
        const val EXIT = "loc.giants_foundry_exit"
        const val BANK_CHEST = "loc.giants_foundry_chest"
        const val STORAGE_EMPTY = "loc.giants_foundry_preform_storage_empty"
        const val STORAGE_FULL = "loc.giants_foundry_preform_storage_full"
        const val STORAGE_SEQ = "seq.human_furnace"

        // UNVERIFIED: no transport data has this; it's the Minigame Teleport tile.
        val OUTSIDE = CoordGrid(3361, 3149, 0)
    }
}
