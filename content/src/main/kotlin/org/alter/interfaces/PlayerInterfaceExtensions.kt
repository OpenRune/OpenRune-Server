package org.alter.interfaces

import net.rsprot.protocol.game.outgoing.interfaces.IfCloseSub
import net.rsprot.protocol.game.outgoing.interfaces.IfMoveSub
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenTop
import net.rsprot.protocol.game.outgoing.interfaces.IfSetAnim
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.interfaces.IfSetHide
import net.rsprot.protocol.game.outgoing.interfaces.IfSetNpcHead
import net.rsprot.protocol.game.outgoing.interfaces.IfSetNpcHeadActive
import net.rsprot.protocol.game.outgoing.interfaces.IfSetObject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetPlayerHead
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import net.rsprot.protocol.util.CombinedId
import org.alter.api.ChatMessageType
import org.alter.api.CommonClientScripts
import org.alter.api.ext.intVarBit
import org.alter.api.ext.message
import org.alter.api.ext.runClientScript
import org.alter.chatboxMultiInit
import org.alter.confirmDestroyInit
import org.alter.confirmOverlayInit
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.EventManager
import org.alter.game.pluginnew.event.impl.IfMoveTopEvent
import org.alter.game.ui.Component
import org.alter.game.ui.InternalApi
import org.alter.game.ui.UserInterface
import org.alter.game.ui.UserInterfaceMap
import org.alter.game.ui.type.IfEvent
import org.alter.ifSetTextAlign
import org.alter.game.ui.IfSubType
import org.alter.menu
import org.alter.objboxSetButtons
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.requireRSCM
import org.alter.rscm.RSCMType
import org.alter.topLevelChatboxResetBackground
import org.alter.topLevelMainModalBackground
import org.alter.topLevelMainModalOpen

private var Player.chatModalUnclamp: Int by intVarBit("varbits.chatmodal_unclamp")

public fun Player.ifOpenTop(topLevel: Int) {
    val userInterface = UserInterface(topLevel)
    ui.topLevel = userInterface
    write(IfOpenTop(topLevel))
}

public fun Player.ifSetEvents(component: String, range: IntRange, vararg event: IfEvent) {
    RSCM.requireRSCM(RSCMType.COMPONENTS, component)

    val target = CombinedId(component.asRSCM())

    val packed = event.fold(0L) { sum, element -> sum or element.bitmask }
    ui.events.add(target, range, packed)

    val packedHigh = (packed shr 32).toInt()
    val packedLow = packed.toInt()
    val prot =
        IfSetEventsV2(
            interfaceId = target.interfaceId,
            componentId = target.componentId,
            start = range.first,
            end = range.last,
            events1 = packedLow,
            events2 = packedHigh,
        )
    write(prot)
}


public fun Player.ifOpenSub(
    interf: String,
    target: String,
    type: IfSubType
): Unit =
    when (type) {
        IfSubType.Modal -> openModal(interf, target)
        IfSubType.Overlay -> openOverlay(interf, target)
    }


public fun Player.ifOpenOverlay(interf: String, target: String) {
    ifOpenSub(interf, target, IfSubType.Overlay)
}


public fun Player.ifMoveTop(dest: String) {
    check(ui.topLevel != UserInterface.NULL) {
        "This function can only be used after `ifOpenTop` has been called. " +
                "Use `ifOpenTop` instead."
    }
    EventManager.post(IfMoveTopEvent(dest, this))
}

public fun Player.ifOpenSide(interf: String) {
    openModal(interf, "components.toplevel_osrs_stretch:sidemodal")
}

public fun Player.ifOpenMainModal(
    interf: String,
    colour: Int = -1,
    transparency: Int = -1,
) {
    topLevelMainModalOpen(this, colour, transparency)
    ifOpenMain(interf)
}


public fun Player.ifOpenMain(interf: String) {
    openModal(interf, "components.toplevel_osrs_stretch:mainmodal")
}

public fun topLevelMainModalOpen(
    player: Player,
    colour: Int = -1,
    transparency: Int = -1,
): Unit = player.runClientScript(CommonClientScripts.MAIN_MODAL_OPEN, colour, transparency)


private fun Player.openModal(interf: String, target: String) {

    RSCM.requireRSCM(RSCMType.INTERFACES,interf)
    RSCM.requireRSCM(RSCMType.COMPONENTS,target)

    val idComponent = target.toIdComponent()
    val idInterface = interf.toIdInterface()

    triggerCloseSubs(target)
    ui.removeQueuedCloseSub(target)
    ui.modals[idComponent] = idInterface

    // Translate any gameframe target component when sent to the client. As far as the server is
    // aware, the interface is being opened on the "base" target component. (when applicable)
    val translated = ui.translate(target)
    write(IfOpenSub(translated.parent, translated.child, interf.asRSCM(), IfSubType.Modal.id))

    org.alter.game.pluginnew.event.impl.OpenSub(this, interf, target, IfSubType.Modal).post()
}

private fun Player.openOverlay(interf: String, target: String) {
    RSCM.requireRSCM(RSCMType.INTERFACES,interf)
    RSCM.requireRSCM(RSCMType.COMPONENTS,target)

    val idComponent = target.toIdComponent()
    val idInterface = interf.toIdInterface()
    triggerCloseSubs(target)
    ui.removeQueuedCloseSub(target)
    ui.overlays[idComponent] = idInterface

    // Translate any gameframe target component when sent to the client. As far as the server is
    // aware, the interface is being opened on the "base" target component. (when applicable)

    val translated = ui.translate(target)

    write(IfOpenSub(translated.parent, translated.child, interf.asRSCM(), IfSubType.Overlay.id))
    org.alter.game.pluginnew.event.impl.OpenSub(this, interf, target, IfSubType.Overlay).post()
}



private fun String.toIdInterface() = UserInterface(this)

private fun String.toIdComponent() = Component(this.asRSCM())


private fun UserInterfaceMap.translate(component: String): Component =
    gameframe.getOrNull(component) ?: Component(component.asRSCM())

public fun Player.ifSetObj(target: String, obj: String, zoomOrCount: Int) {
    requireRSCM(RSCMType.COMPONENTS, target)
    requireRSCM(RSCMType.OBJTYPES, obj)
    val combined = CombinedId(target.asRSCM())
    write(IfSetObject(Component(combined.interfaceId, combined.combinedId).packed, obj.asRSCM(), zoomOrCount))
}

public fun Player.ifSetAnim(target: String, seq: String?) {
    requireRSCM(RSCMType.COMPONENTS, target)
    if (seq != null) {
        requireRSCM(RSCMType.SEQTYPES, seq)
    }
    val combined = CombinedId(target.asRSCM())
    write(IfSetAnim(combined.interfaceId, combined.combinedId, seq?.asRSCM() ?: -1))
}

public fun Player.ifSetPlayerHead(target: String) {
    requireRSCM(RSCMType.COMPONENTS, target)
    val combined = CombinedId(target.asRSCM())
    write(IfSetPlayerHead(combined.interfaceId, combined.combinedId))
}

/** @see [IfSetNpcHead] */
public fun Player.ifSetNpcHead(target: String, npc: String) {
    requireRSCM(RSCMType.COMPONENTS, target)
    requireRSCM(RSCMType.NPCTYPES, npc)
    val combined = CombinedId(target.asRSCM())
    write(IfSetNpcHead(combined.interfaceId, combined.combinedId, npc.asRSCM()))
}

/** @see [IfSetNpcHeadActive] */
public fun Player.ifSetNpcHeadActive(target: String, npcSlotId: Int) {
    requireRSCM(RSCMType.COMPONENTS, target)
    val combined = CombinedId(target.asRSCM())
    write(IfSetNpcHeadActive(combined.interfaceId, combined.combinedId, npcSlotId))
}

public fun Player.ifOpenMainSidePair(
    main: String,
    side: String,
    colour: Int,
    transparency: Int,
) {
    topLevelMainModalBackground(this, colour, transparency)
    openModal(main, "components.toplevel_osrs_stretch:mainmodal")
    openModal(side, "components.toplevel_osrs_stretch:sidemodal")
}

public fun Player.ifOpenOverlay(interf: String) {
    ifOpenOverlay(interf, "components.toplevel_osrs_stretch:floater")
}

public fun Player.ifOpenFullOverlay(interf: String) {
    ifOpenOverlay(interf, "components.toplevel_osrs_stretch:overlay_atmosphere")
}

/**
 * Difference from [ifCloseModals]: this function clears all weak queues for the player and closes
 * any active dialog.
 *
 * @see [cancelActiveDialog]
 */
public fun Player.ifClose() {
    //cancelActiveDialog()
    //weakQueueList.clear()
    ifCloseModals()
}


public fun Player.ifCloseModals() {
    // This gives us an iterable copy of the entries, so we are safe to modify `ui.modals` while
    // closing them.
    val modalEntries = ui.modals.entries()
    for ((key, value) in modalEntries) {
        closeModal(value, key)
    }
    // Make sure _all_ modals were closed. If not, then something is wrong, and we'd rather force
    // the player to disconnect than to allow them to keep modals open when they shouldn't.
    check(ui.modals.isEmpty()) {
        "Could not close all modals for player `$this`. (modals=${ui.modals})"
    }
}

public fun Player.ifSetText(target: String, text: String) {
    requireRSCM(RSCMType.COMPONENTS, target)
    val combined = CombinedId(target.asRSCM())
    write(IfSetText(combined.interfaceId, combined.combinedId, text))
}

public fun Player.ifSetHide(target: String, hide: Boolean) {
    requireRSCM(RSCMType.COMPONENTS, target)
    val combined = CombinedId(target.asRSCM())
    write(IfSetHide(combined.interfaceId, combined.combinedId, hide))
}

public fun Player.ifOpenTop(topLevel: String) {
    requireRSCM(RSCMType.INTERFACES, topLevel)
    val interfaceId = topLevel.asRSCM()
    val userInterface = UserInterface(interfaceId)
    ui.topLevel = userInterface

    println("Open Top: $interfaceId")

    write(IfOpenTop(interfaceId))
}


public fun Player.ifCloseSub(interf: String) {
    closeModal(interf)
    closeOverlay(interf)
}

public fun Player.ifCloseModal(interf: String) {
    closeModal(interf)
}

public fun Player.ifCloseOverlay(interf: String) {
    closeOverlay(interf)
}

private fun Player.closeModal(interf: String) {
    requireRSCM(RSCMType.INTERFACES, interf)
    val target = ui.modals.getComponentString(interf)
    if (target != null) {
        closeModal(interf, target)
    }
}

private fun Player.closeModal(interf: String, target: String) {
    ui.modals.remove(target)
    ui.events.clear(interf)

    // Translate any gameframe target component when sent to the client. As far as the server
    // is aware, the interface was open on the "base" target component. (when applicable)
    val translated = ui.translate(target)
    write(IfCloseSub(translated.parent, translated.child))

    closeOverlayChildren(interf)
}

private fun Player.closeOverlay(interf: String) {
    requireRSCM(RSCMType.INTERFACES, interf)
    val target = ui.overlays.getComponentString(interf)
    if (target != null) {
        closeOverlay(interf, target)
    }
}

private fun Player.closeOverlay(interf: String, target: String) {
    ui.overlays.remove(target)
    ui.events.clear(interf)

    // Translate any gameframe target component when sent to the client. As far as the server
    // is aware, the interface was open on the "base" target component. (when applicable)
    val translated = ui.translate(target)
    write(IfCloseSub(translated.parent, translated.child))

    closeOverlayChildren(interf)
}

private fun Player.closeOverlayChildren(parent: String) {
    // This gives us an iterable copy of the entries, so we are safe to modify `ui.overlays` while
    // closing them.
    val overlayEntries = ui.overlays.entries()
    for ((key, value) in overlayEntries) {
        if (key == parent) {
            closeOverlay(value, key)
        }
    }
}

@InternalApi("Usage of this function should only be used internally")
public fun Player.closeSubs(from: String) {
    val remove = ui.modals.remove(from) ?: ui.overlays.remove(from)
    if (remove != null) {
        ui.events.clear(remove)

        // Translate any gameframe target component when sent to the client. As far as the server
        // is aware, the interface was open on the "base" target component. (when applicable)
        val translated = ui.translate(from)
        write(IfCloseSub(translated.parent, translated.child))

        closeOverlayChildren(remove)
    }
}

/**
 * Similar to [closeSubs], but only triggers "close sub" scripts and does _not_ send [IfCloseSub]
 * packet to the client.
 */
private fun Player.triggerCloseSubs(from: String) {
    val remove = ui.modals.remove(from) ?: ui.overlays.remove(from)
    if (remove != null) {
        ui.events.clear(remove)
        triggerCloseOverlayChildren(remove)
    }
}

/**
 * Similar to [closeOverlayChildren], but only triggers "close sub" scripts and does _not_ send
 * [IfCloseSub] packet to the client.
 */
private fun Player.triggerCloseOverlayChildren(parent: String) {
    // This gives us an iterable copy of the entries, so we are safe to modify `ui.overlays` while
    // closing them.
    val overlayEntries = ui.overlays.entries()
    for ((key, value) in overlayEntries) {
        if (key == parent) {
            triggerCloseOverlay(value, key)
        }
    }
}

/**
 * Similar to [closeOverlay], but only triggers "close sub" scripts and does _not_ send [IfCloseSub]
 * packet to the client.
 */
private fun Player.triggerCloseOverlay(
    interf: String,
    target: String,
) {
    ui.overlays.remove(target)
    ui.events.clear(interf)
    triggerCloseOverlayChildren(interf)
}

public fun Player.ifMoveSub(
    source: String,
    dest: String,
    base: String,
) {

    requireRSCM(RSCMType.COMPONENTS, source)
    requireRSCM(RSCMType.COMPONENTS, dest)

    write(IfMoveSub(source.asRSCM(), dest.asRSCM()))
}

/*
 * Dialogue helper functions
 *
 * These functions are intended to help with displaying various dialogue interfaces to the player.
 * However, they do _not_ properly handle state suspension or resuming from player input.
 *
 * Important: These functions should only be used internally within systems that properly manage
 * player state, input handling, and coroutine suspension. Direct usage in other contexts may result
 * in unwanted behavior.
 */

internal fun Player.ifMesbox(text: String, pauseText: String, lineHeight: Int) {
    message(text, ChatMessageType.Mesbox)
    openModal("interfaces.messagebox", "components.chatbox:chatmodal")
    ifSetText("components.messagebox:text", text)
    ifSetTextAlign(this, "components.messagebox:text", alignH = 1, alignV = 1, lineHeight)
    ifSetPauseText("components.messagebox:continue", pauseText)
    // TODO: Look into clientscript to name property and place in clientscript utility class.
    runClientScript(CommonClientScripts.SCRIPT_1508, "0")
}

internal fun Player.skillMulti() {
    openModal("interfaces.skillmulti", "components.chatbox:chatmodal")
}

internal fun Player.ifObjbox(
    text: String,
    obj: Int,
    zoom: Int,
    pauseText: String,
) {
    message(text, ChatMessageType.Mesbox)
    ifOpenChat("interfaces.objectbox", 1)
    objboxSetButtons(this, pauseText)
    if (pauseText.isNotBlank()) {
        ifSetEvents("components.objectbox:universe", 0..1, IfEvent.PauseButton)
    } else {
        // Note: This purposefully disables `if_events` for subcomponents -1 to -1.
        ifSetEvents("components.objectbox:universe", -1..-1)
    }
    ifSetObj("components.objectbox:item", obj, zoom)
    ifSetText("components.objectbox:text", text)
}

internal fun Player.ifDoubleobjbox(
    text: String,
    obj1: Int,
    zoom1: Int,
    obj2: Int,
    zoom2: Int,
    pauseText: String,
) {
    message(text, ChatMessageType.Mesbox)
    ifOpenChat("interfaces.objectbox_double", 1)
    ifSetPauseText("components.objectbox_double:pausebutton", pauseText)
    ifSetObj("components.objectbox_double:model1", obj1, zoom1)
    ifSetObj("components.objectbox_double:model2", obj2, zoom2)
    ifSetText("components.objectbox_double:text", text)
}

internal fun Player.ifConfirmDestroy(
    header: String,
    text: String,
    obj: Int,
    count: Int,
) {
    ifOpenChat("interfaces.confirmdestroy", 0)
    confirmDestroyInit(this, header, text, obj, count)
    ifSetEvents("components.confirmdestroy:universe", 0..1, IfEvent.PauseButton)
}

internal fun Player.ifConfirmOverlay(
    target: String,
    title: String,
    text: String,
    cancel: String,
    confirm: String,
) {
    ifOpenSub("interfaces.popupoverlay", target, IfSubType.Overlay)
    confirmOverlayInit(this, target, title, text, cancel, confirm)
}

internal fun Player.ifConfirmOverlayClose(): Unit =
    ifCloseOverlay("interfaces.popupoverlay")

internal fun Player.ifMenu(
    title: String,
    joinedChoices: String,
    hotkeys: Boolean,
) {
    ifOpenMainModal("interfaces.menu")
    menu(this, title, joinedChoices, hotkeys)
    ifSetEvents("components.menu:lj_layer1", 0..127, IfEvent.PauseButton)
}

/** @see [chatboxMultiInit] */
internal fun Player.ifChoice(
    title: String,
    joinedChoices: String,
    choiceCountInclusive: Int,
) {
    ifOpenChat("interfaces.chatmenu", 1)
    chatboxMultiInit(this, title, joinedChoices)
    ifSetEvents("components.chatmenu:options", 1..choiceCountInclusive, IfEvent.PauseButton)
}

internal fun Player.ifChatPlayer(
    title: String,
    text: String,
    expression: String?,
    pauseText: String,
    lineHeight: Int,
) {
    message("$title|$text", ChatMessageType.Dialogue)
    ifOpenChat("interfaces.chat_right", 0)
    ifSetPlayerHead("components.chat_right:head")
    ifSetAnim("components.chat_right:head", expression)
    ifSetText("components.chat_right:name", title)
    ifSetText("components.chat_right:text", text)
    ifSetTextAlign(this, "components.chat_right:text", alignH = 1, alignV = 1, lineHeight)
    ifSetPauseText("components.chat_right:continue", pauseText)
}

internal fun Player.ifChatNpcActive(
    title: String,
    npcSlotId: Int,
    text: String,
    chatanim: String?,
    pauseText: String,
    lineHeight: Int,
) {
    message("$title|$text", ChatMessageType.Dialogue)
    ifOpenChat("interfaces.chat_left", 0)
    ifSetNpcHeadActive("components.chat_left:head", npcSlotId)
    ifSetAnim("components.chat_left:head", chatanim)
    ifSetText("components.chat_left:name", title)
    ifSetText("components.chat_left:text", text)
    ifSetTextAlign(this, "components.chat_left:text", alignH = 1, alignV = 1, lineHeight)
    ifSetPauseText("components.chat_left:continue", pauseText)
}

internal fun Player.ifChatNpcSpecific(
    title: String,
    type: String,
    text: String,
    chatanim: String?,
    pauseText: String,
    lineHeight: Int,
) {

    message("$title|$text", ChatMessageType.Dialogue)
    ifOpenChat("interfaces.chat_left", 1)
    ifSetNpcHead("components.chat_left:head", type)
    ifSetAnim("components.chat_left:head", chatanim)
    ifSetText("components.chat_left:name", title)
    ifSetText("components.chat_left:text", text)
    ifSetTextAlign(this, "components.chat_left:text", alignH = 1, alignV = 1, lineHeight)
    ifSetPauseText("components.chat_left:continue", pauseText)
}

internal fun Player.ifOpenChat(interf: String, widthAndHeightMode: Int) {
    chatModalUnclamp = widthAndHeightMode
    topLevelChatboxResetBackground(this)
    openModal(interf, "components.chatbox:chatmodal")
}

private fun Player.ifSetPauseText(component: String, text: String) {
    if (text.isNotBlank()) {
        ifSetEvents(component, -1..-1, IfEvent.PauseButton)
    } else {
        ifSetEvents(component, -1..-1)
    }
    ifSetText(component, text)
}

private fun Player.ifSetObj(target: String, obj: Int, zoomOrCount: Int) {
    requireRSCM(RSCMType.COMPONENTS, target)
    val combined = CombinedId(target.asRSCM())
    write(IfSetObject(Component(combined.interfaceId, combined.combinedId).packed, obj, zoomOrCount))
}

