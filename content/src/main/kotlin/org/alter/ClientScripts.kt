package org.alter

import org.alter.api.CommonClientScripts
import org.alter.api.ext.runClientScript
import org.alter.game.model.container.ItemContainer
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType

public fun ifSetTextAlign(
    player: Player,
    target: String,
    alignH: Int,
    alignV: Int,
    lineHeight: Int,
): Unit = player.runClientScript(CommonClientScripts.SET_TEXT_ALIGN, alignH, alignV, lineHeight, target.asRSCM())

public fun chatboxMultiInit(player: Player, title: String, joinedChoices: String): Unit =
    player.runClientScript(CommonClientScripts.CHATBOX_MULTI, title, joinedChoices)

public fun topLevelMainModalOpen(
    player: Player,
    colour: Int = -1,
    transparency: Int = -1,
): Unit = player.runClientScript(CommonClientScripts.MAIN_MODAL_OPEN, colour, transparency)

public fun topLevelMainModalBackground(
    player: Player,
    colour: Int = -1,
    transparency: Int = -1,
) {
    player.runClientScript(CommonClientScripts.MAIN_MODAL_BACKGROUND, colour, transparency)
}

public fun topLevelChatboxResetBackground(player: Player): Unit = player.runClientScript(CommonClientScripts.CHATBOX_RESET_BACKGROUND)

public fun menu(player: Player, title: String, joinedChoices: String, hotkeys: Boolean): Unit =
    player.runClientScript(CommonClientScripts.MENU, title, joinedChoices, if (hotkeys) 1 else 0)

public fun confirmOverlayInit(
    player: Player,
    target: String,
    title: String,
    text: String,
    cancel: String,
    confirm: String,
) {
    RSCM.requireRSCM(RSCMType.COMPONENTS, target)
    player.runClientScript(CommonClientScripts.CONFIRM, "$title|$text|$cancel|$confirm", target.asRSCM())
}

public fun confirmDestroyInit(
    player: Player,
    header: String,
    text: String,
    obj: Int,
    count: Int,
): Unit = player.runClientScript(CommonClientScripts.CONFIRM_DESTROY, obj, count, header, text)

public fun objboxSetButtons(player: Player, text: String): Unit =
    player.runClientScript(CommonClientScripts.SET_OPTIONS, text)

public fun interfaceInvInit(
    player: Player,
    inv: ItemContainer,
    target: String,
    objRowCount: Int,
    objColCount: Int,
    dragType: Int = 0,
    dragComponent: String? = null,
    op1: String? = null,
    op2: String? = null,
    op3: String? = null,
    op4: String? = null,
    op5: String? = null,
) {
    player.runClientScript(
        CommonClientScripts.INTERFACE_INV_INIT,
        target.asRSCM(),
        inv.key.internalID,
        objRowCount,
        objColCount,
        dragType,
        dragComponent?.asRSCM() ?: -1,
        op1 ?: "",
        op2 ?: "",
        op3 ?: "",
        op4 ?: "",
        op5 ?: "",
    )
}

public fun statGroupTooltip(
    player: Player,
    tooltip: String,
    container: String,
    text: String,
) {
    player.runClientScript(CommonClientScripts.STAT_GROUP, tooltip.asRSCM(), container.asRSCM(), text)
}