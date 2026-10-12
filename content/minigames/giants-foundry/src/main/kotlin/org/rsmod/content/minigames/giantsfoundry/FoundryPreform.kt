package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

internal const val PREFORM = "obj.giants_foundry_preform"
internal const val HUD = "interface.giants_foundry_hud"
internal const val PREFORM_COOLING_TIMER = "timer.giants_foundry_preform_cooling"
internal const val JIG_COOLING_TIMER = "timer.giants_foundry_jig_cooling"

private const val HUD_RESCALE = "clientscript.[clientscript,giants_foundry_hud_rescale]"

/** `giants_foundry_hud_set_sweetspot(int)` in the rev 241 dump: 0 none, 1 gold, 2 green. */
private const val HUD_SWEETSPOT = "clientscript.[clientscript,script6122]"

/** Passive cooling: -1 every 2 ticks (Easy Giants' Foundry plugin, which logs the heat varbit). */
internal const val PREFORM_COOLING_PERIOD = 2

internal fun Player.holdsPreform(): Boolean =
    worn[Wearpos.RightHand.slot]?.isType(PREFORM) == true

/** True while a sword is in progress: held, stored, or poured and waiting in the jig. */
internal fun Player.hasSwordInProgress(): Boolean =
    holdsPreform() || preformStored || mouldState >= 2

/** Puts the preform in the weapon slot, moving anything held into the inventory. */
internal fun ProtectedAccess.equipPreform(): Boolean {
    val held =
        listOf(Wearpos.RightHand.slot, Wearpos.LeftHand.slot).mapNotNull { slot ->
            player.worn[slot]?.let { slot to it }
        }
    if (inv.freeSpace() < held.size) {
        mes("You need to free up some inventory space to hold the preform.")
        return false
    }
    for ((slot, obj) in held) {
        player.invTransfer(from = player.worn, fromSlot = slot, into = player.inv, count = obj.count, untransform = true)
    }
    player.invAdd(player.worn, PREFORM, 1, slot = Wearpos.RightHand.slot)
    player.rebuildAppearance()
    openHud()
    softTimer(PREFORM_COOLING_TIMER, PREFORM_COOLING_PERIOD)
    return true
}

internal fun Player.removePreform() {
    worn[Wearpos.RightHand.slot]?.takeIf { it.isType(PREFORM) }?.let {
        invDel(worn, it.id, 1, slot = Wearpos.RightHand.slot)
        rebuildAppearance()
    }
}

internal fun ProtectedAccess.openHud() {
    ifOpenOverlay(HUD)
    player.runClientScript(HUD_RESCALE.asRSCM(RSCMType.CLIENTSCRIPT))
}

internal fun ProtectedAccess.closeHud() {
    ifCloseSub(HUD)
    clearSoftTimer(PREFORM_COOLING_TIMER)
}

internal fun Player.showSweetSpot(state: Int) {
    runClientScript(HUD_SWEETSPOT.asRSCM(RSCMType.CLIENTSCRIPT), state)
}

/** Resets everything about the current sword (after hand-in). */
internal fun Player.clearSword() {
    commissionWord1 = 0
    commissionWord2 = 0
    preformDifficulty = 0
    preformQuality = 0
    preformStartQuality = 0
    preformTemperature = 0
    preformCompletion = 0
    preformStored = false
    sweetSpots = 0
    commissionStart = 0
    for (section in 0..6) {
        setFoundryVar(seedVarbit(section), 0)
    }
}
