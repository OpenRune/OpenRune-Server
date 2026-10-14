package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

internal const val PREFORM = "obj.giants_foundry_preform"
internal const val HUD = "interface.giants_foundry_hud"
internal const val PREFORM_COOLING_TIMER = "timer.giants_foundry_preform_cooling"

/** `giants_foundry_hud_set_sweetspot(int)`: 0 none, 1 gold, 2 green. */
private const val HUD_SWEETSPOT = "clientscript.[clientscript,script6122]"

// UNVERIFIED: passive cooling of 1 every 2 ticks (plugin measurement).
internal const val PREFORM_COOLING_PERIOD = 2

internal fun Player.holdsPreform(): Boolean =
    worn[Wearpos.RightHand.slot]?.isType(PREFORM) == true

internal suspend fun ProtectedAccess.handsFreeForPreform(): Boolean {
    if (player.worn[Wearpos.RightHand.slot] != null || player.worn[Wearpos.LeftHand.slot] != null) {
        mesbox("You must free your hands so you can hold the preform.")
        return false
    }
    return true
}

internal fun ProtectedAccess.equipPreform() {
    player.invAdd(player.worn, PREFORM, 1, slot = Wearpos.RightHand.slot)
    player.rebuildAppearance()
    openHud()
    softTimer(PREFORM_COOLING_TIMER, PREFORM_COOLING_PERIOD)
}

internal fun Player.removePreform() {
    worn[Wearpos.RightHand.slot]?.takeIf { it.isType(PREFORM) }?.let {
        invDel(worn, it.id, 1, slot = Wearpos.RightHand.slot)
        rebuildAppearance()
    }
}

internal fun ProtectedAccess.openHud() {
    ifOpenOverlay(HUD)
}

internal fun ProtectedAccess.closeHud() {
    ifCloseSub(HUD)
    clearSoftTimer(PREFORM_COOLING_TIMER)
}

internal fun Player.showSweetSpot(state: Int) {
    runClientScript(HUD_SWEETSPOT.asRSCM(RSCMType.CLIENTSCRIPT), state)
}

/** The heat and seeds stay until the next pick-up. */
internal fun Player.clearSword() {
    preformDifficulty = 0
    preformQuality = 0
    preformStartQuality = 0
    preformCompletion = 0
    commissionWord1 = 0
    commissionWord2 = 0
    preformStored = false
    sweetSpotsOffered = 0
    sweetSpotsLastSection = 0
    commissionStart = 0
}
