package org.rsmod.content.other.toolbelt

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.table.ToolbeltSlotsRow
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

object ToolbeltSlotInfo {
    private const val HIGHLIGHT = "component.toolbelt:highlight"
    private const val SCREEN_HIGHLIGHT = "interface.screenhighlight"

    private const val CS_HIGHLIGHT_START = 3408
    private const val CS_HIGHLIGHT_SCREEN = 3409
    private const val CS_HIGHLIGHT_TEXTBOX = 3413
    private const val CS_HIGHLIGHT_HIDE = 3412

    private var Player.tutorialPage by intVarBit("varbit.hnt_hint_step")
    private var Player.tutorialTotalPages by intVarBit("varbit.hnt_hint_max_step")

    suspend fun show(access: ProtectedAccess, slot: ToolbeltSlotsRow, eventBus: EventBus) {
        val player = access.player
        val text = ToolbeltUnlocks.unlockMessage(slot)
        val highlight = HIGHLIGHT.asRSCM(RSCMType.COMPONENT)

        player.tutorialTotalPages = 1
        player.tutorialPage = 0

        player.runClientScript(CS_HIGHLIGHT_START, highlight)
        access.ifOpenOverlay(SCREEN_HIGHLIGHT, HIGHLIGHT)
        player.runClientScript(
            CS_HIGHLIGHT_SCREEN,
            highlight,
            0,
            0,
            0,
            0,
            constants.setpos_abs_right,
            constants.setpos_abs_bottom,
            100,
            1,
        )
        player.runClientScript(
            CS_HIGHLIGHT_TEXTBOX,
            text,
            -1,
            1,
            0,
            highlight,
        )
        access.ifSetEvents("component.screenhighlight:continue", 9..9, IfEvent.Op1)
        access.ifSetEvents("component.screenhighlight:previous", 9..9, IfEvent.Op1)
        access.ifSetEvents("component.screenhighlight:pausebutton", -1..-1, IfEvent.PauseButton)
        try {
            access.pauseButton()
        } finally {
            player.runClientScript(CS_HIGHLIGHT_HIDE, highlight)
            player.ifCloseOverlay(SCREEN_HIGHLIGHT, eventBus)
        }
    }
}
