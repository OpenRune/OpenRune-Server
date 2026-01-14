package org.alter

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.ItemServerType
import org.alter.api.CommonClientScripts
import org.alter.api.ext.runClientScript
import org.alter.game.model.container.ItemContainer
import org.alter.game.model.entity.Player
import org.alter.game.model.entity.UpdateInventory.resendSlot
import org.alter.game.model.inv.Inventory
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType
import kotlin.collections.get
import kotlin.text.get

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
    inv: Inventory,
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
        inv.type.id,
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

public fun tooltip(
    player: Player,
    text: String,
    container: String,
    tooltip: String,
) {
    player.runClientScript(CommonClientScripts.TOOLTIP, text, container.asRSCM(), tooltip.asRSCM())
}

public fun mesLayerClose(player: Player, layerMode: Int) {
    player.runClientScript(CommonClientScripts.MES_SLAYER_CLOSE, layerMode)
}

public fun examineItem(
    player: Player,
    obj: Int,
    count: Int,
    desc: String,
    market: Boolean,
    marketPrice: Int,
    alchable: Boolean,
    highAlch: Int,
    lowAlch: Int,
) {
    player.runClientScript(
        CommonClientScripts.EXAMINE_ITEM,
        obj,
        count,
        desc,
        if (market) 1 else 0,
        marketPrice,
        if (alchable) 1 else 0,
        highAlch,
        lowAlch,
    )
}

public fun objExamine(
    player: Player,
    inventory: Inventory,
    slot: Int
) {
    val obj = inventory[slot] ?: return resendSlot(inventory, 0)
    player.objExamine(obj.getDef(), obj.amount, 0)
}

public fun Player.objExamine(type: ItemServerType, count: Int, marketPrice: Int) {
    //TODO THIS
    examineItem(
        player = this,
        obj = type.id,
        count = count,
        desc = type.examine,
        market = false,
        marketPrice = marketPrice,
        alchable = type.alchable,
        lowAlch = 0,
        highAlch = 0,
    )
}