package org.rsmod.content.minigames.giantsfoundry

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfSubType
import org.rsmod.api.config.Constants
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

/**
 * A chatbox box with a tool icon. [text] carries the box's own line breaks; the chat log gets it
 * on one line. With a [hint], a hint arrow points at that tile while the box is open and stays
 * until [MiscOutput.hintArrowReset].
 */
internal suspend fun ProtectedAccess.graphicbox(graphic: Int, text: String, hint: CoordGrid? = null) {
    if (hint != null) {
        vars[CURRENT_HINT_ARROW] = HINT_ARROW_TILE
    }
    mes(text.replace("<br>", " "), ChatType.Mesbox)
    vars[CHATBOX_UNCLAMP_VARBIT] = 0
    if (hint != null) {
        MiscOutput.hintArrowReset(player)
        MiscOutput.hintArrowTile(player, hint, HINT_ARROW_HEIGHT)
    }
    ClientScripts.topLevelChatboxResetBackground(player)
    ifOpenSub(GRAPHICBOX, CHATBOX_MODAL, IfSubType.Modal)
    player.runClientScript(GRAPHICBOX_SETBUTTONS.asRSCM(RSCMType.CLIENTSCRIPT), Constants.cm_pausebutton)
    ifSetEvents(GRAPHICBOX_UNIVERSE, 0..1, IfEvent.PauseButton)
    player.runClientScript(
        GRAPHICBOX_SET_GRAPHIC.asRSCM(RSCMType.CLIENTSCRIPT),
        graphic,
        GRAPHIC_SIZE,
        GRAPHIC_SIZE,
    )
    ifSetText(GRAPHICBOX_TEXT, text)
    pauseButton()
    if (hint != null) {
        vars[CURRENT_HINT_ARROW] = 0
    }
}

private const val GRAPHIC_SIZE = 64
private const val HINT_ARROW_HEIGHT = 60
private const val HINT_ARROW_TILE = 7
private const val CURRENT_HINT_ARROW = "varbit.current_hint_arrow"
private const val CHATBOX_UNCLAMP_VARBIT = "varbit.chatmodal_unclamp"
private const val CHATBOX_MODAL = "component.chatbox:chatmodal"
private const val GRAPHICBOX = "interface.graphicbox"
private const val GRAPHICBOX_UNIVERSE = "component.graphicbox:universe"
private const val GRAPHICBOX_TEXT = "component.graphicbox:text"
private const val GRAPHICBOX_SETBUTTONS = "clientscript.[clientscript,graphicbox_setbuttons]"
private const val GRAPHICBOX_SET_GRAPHIC = "clientscript.[clientscript,graphicbox_set_graphic]"
