package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.ClientScripts.invHighlightClearAll
import org.rsmod.api.player.output.ClientScripts.invHighlightItemOn
import org.rsmod.api.player.output.ClientScripts.toplevelSidebuttonSwitch
import org.rsmod.api.player.output.ClientScripts.uiHighlightClear
import org.rsmod.api.player.output.ClientScripts.uiHighlightClearAll
import org.rsmod.api.player.output.ClientScripts.uiHighlightUpdate
import org.rsmod.game.entity.Player

internal object TutorialHighlights {
    private val STYLE_CTA = "dbrow.ui_highlighting_style_tutorial_cta".asRSCM(RSCMType.DBROW)
    private val STYLE_ALERT = "dbrow.ui_highlighting_style_tutorial_alert".asRSCM(RSCMType.DBROW)
    private const val INVENTORY_TAB = 3
    private val SMITHING_DAGGER = "component.smithing:dagger".asRSCM(RSCMType.COMPONENT)
    private val WIND_STRIKE = "component.magic_spellbook:wind_strike".asRSCM(RSCMType.COMPONENT)
    private val HOME_TELEPORT =
        "component.magic_spellbook:teleport_home_standard".asRSCM(RSCMType.COMPONENT)

    fun item(player: Player, obj: String, alert: Boolean = false) {
        toplevelSidebuttonSwitch(player, INVENTORY_TAB)
        invHighlightItemOn(
            player,
            obj.asRSCM(RSCMType.OBJ),
            if (alert) STYLE_ALERT else STYLE_CTA,
            if (alert) 100 else 0,
        )
    }

    fun clearItems(player: Player) {
        invHighlightClearAll(player)
    }

    fun smithingDagger(player: Player) {
        uiHighlightUpdate(player, 3, 1, SMITHING_DAGGER, -1, STYLE_CTA, 0)
    }

    fun clearSmithingDagger(player: Player) {
        uiHighlightClear(player, 3, 1)
    }

    fun windStrike(player: Player) {
        uiHighlightUpdate(player, 3, 3, WIND_STRIKE, -1, STYLE_CTA, 0)
    }

    fun clearWindStrike(player: Player) {
        uiHighlightClear(player, 3, 3)
    }

    fun homeTeleport(player: Player) {
        uiHighlightUpdate(player, 3, 3, HOME_TELEPORT, -1, STYLE_CTA, 0)
    }

    fun clearAll(player: Player) {
        invHighlightClearAll(player)
        uiHighlightClearAll(player, 3)
    }
}
