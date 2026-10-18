package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpWorn1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal val FOUNDRY_INSIDE = CoordGrid(3366, 11483, 0)

internal val FOUNDRY_OUTSIDE = CoordGrid(3361, 3148, 0)

private const val ENTER_FADE_CYCLES = 50

internal suspend fun ProtectedAccess.enterFoundry() {
    fadeTeleport(
        FOUNDRY_INSIDE,
        afterMove = { if (player.preformStored) mes(KEPT_WARM) },
    )
}

private const val KEPT_WARM =
    "<col=ff3045>You notice your current commission is being kept warm in the preform storage."

private suspend fun ProtectedAccess.fadeTeleport(
    destination: CoordGrid,
    atFade: () -> Unit = {},
    beforeMove: () -> Unit = {},
    afterMove: () -> Unit = {},
) {
    arriveDelay()
    fadeOverlay(
        startColour = 0,
        startTransparency = 255,
        endColour = 0,
        endTransparency = 0,
        clientDuration = ENTER_FADE_CYCLES,
    )
    atFade()
    delay(2)
    beforeMove()
    telejump(destination)
    delay(1)
    afterMove()
    fadeOverlay(
        startColour = 0,
        startTransparency = 0,
        endColour = 0,
        endTransparency = 255,
        clientDuration = ENTER_FADE_CYCLES,
    )
    closeFadeOverlay(cycles = 2)
}

class FoundryAccessScript
@Inject
constructor(private val room: SleepingGiantsRoom, private val eventBus: EventBus) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(EXIT) { leave() }
        onOpLoc1(BANK_CHEST) { tryOpenBank() }
        onOpLoc1(STORAGE_EMPTY) { store() }
        onOpLoc1(STORAGE_FULL) { take() }
        onOpWorn1(PREFORM) {
            mes("You should place the preform into the preform storage if you want to unequip it.")
        }
        onPlayerLogin { login(player) }
    }

    private suspend fun ProtectedAccess.leave() {
        fadeTeleport(
            FOUNDRY_OUTSIDE,
            atFade = { if (player.holdsPreform()) closeHud() },
            beforeMove = { room.leave(player) },
            afterMove = { if (player.holdsPreform()) placePreform() },
        )
    }

    private fun ProtectedAccess.store() {
        if (!player.holdsPreform()) {
            mes("There is nothing in the storage area to take.")
            return
        }
        placePreform()
        anim(STORAGE_SEQ)
        closeHud()
    }

    private fun ProtectedAccess.placePreform() {
        player.preformStored = true
        mes(PREFORM_STASHED)
        player.removePreform()
    }

    private suspend fun ProtectedAccess.take() {
        if (!player.preformStored || !handsFreeForPreform()) {
            return
        }
        equipPreform()
        if (!player.holdsPreform()) {
            return
        }
        player.preformStored = false
        mes("You take the preform from the handy storage area. It will start to get cold now.")
        anim(STORAGE_SEQ)
    }

    /**
     * With quests treated as complete (game.yml `quest-requirements`), Sleeping Giants counts as
     * done, so its varbit must read finished too: Kovac's multinpc only shows his foundry ops at
     * 30. With real quest progress the varbit is left to the quest.
     */
    private fun login(player: Player) {
        if (
            QuestRequirements.hasCompleted(player, SleepingGiants.QUEST) &&
                player.sleepingGiants < SleepingGiants.COMPLETE
        ) {
            player.sleepingGiants = SleepingGiants.COMPLETE
        }
        if (player.holdsPreform() && player.preformStored) {
            player.preformStored = false
        }
        // UNVERIFIED: that logging in with a stored preform gives the kept-warm line (guess).
        if (player.preformStored) {
            player.mes(KEPT_WARM)
        }
        if (player.holdsPreform()) {
            player.resumePreform(eventBus)
        }
    }

    private companion object {
        const val EXIT = "loc.giants_foundry_exit"
        const val BANK_CHEST = "loc.giants_foundry_chest"
        const val STORAGE_EMPTY = "loc.giants_foundry_preform_storage_empty"
        const val STORAGE_FULL = "loc.giants_foundry_preform_storage_full"
        const val STORAGE_SEQ = "seq.human_furnace"
    }
}
