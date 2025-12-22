package org.alter.api.ext

import dev.openrune.ServerCacheManager.getItem
import dev.openrune.ServerCacheManager.getNpc
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.game.outgoing.misc.player.TriggerOnDialogAbort
import org.alter.api.ClientScript
import org.alter.api.CommonClientScripts
import org.alter.game.model.attr.INTERACTING_NPC_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask


import org.alter.game.pluginnew.event.impl.DialogCloseAll
import org.alter.game.pluginnew.event.impl.DialogItem
import org.alter.game.pluginnew.event.impl.DialogItemDouble
import org.alter.game.pluginnew.event.impl.DialogMessageOpen
import org.alter.game.pluginnew.event.impl.DialogMessageOption
import org.alter.game.pluginnew.event.impl.DialogNpcOpen
import org.alter.game.pluginnew.event.impl.DialogPlayerOpen
import org.alter.game.pluginnew.event.impl.DialogSkillMulti
import org.alter.rscm.RSCM.asRSCM


/**
 * The child id of the chat box in the gameframe interface. This can change
 * with revision.
 */
const val CHATBOX_CHILD = 566


/**
 * The default action that will occur when interrupting or finishing a dialog.
 */
private fun closeDialog(player: Player): QueueTask.() -> Unit =
    {
        DialogCloseAll(player).post()
        player.write(TriggerOnDialogAbort)
    }

/**
 * Invoked when input dialog queues are interrupted.
 */
private fun closeInput(player: Player): QueueTask.() -> Unit = {
        player.closeInputDialog()
    }


/**
 * Prompts the player with options.
 *
 * @return
 * The id of the option chosen. The id can range from [1] inclusive to [options.size] inclusive.
 */
suspend fun QueueTask.options(
    player: Player,
    vararg options: String,
    title: String = "Select an Option",
    fullSize: Boolean = true
): Int {
    DialogMessageOption(title, options.joinToString("|"),player).post()
    terminateAction = closeDialog(player)
    waitReturnValue()
    terminateAction!!(this)
    return (requestReturnValue as? ResumePauseButton)?.sub ?: -1
}

/**
 * Prompts the player with an input dialog where they can only enter an integer.
 *
 * @return
 * The integer input.
 */
suspend fun QueueTask.inputInt(
    player: Player,
    description: String = "Enter amount"
): Int {
    player.runClientScript(ClientScript("meslayer_mode7"), description)
    terminateAction = closeInput(player)
    waitReturnValue()
    terminateAction!!(this)

    return requestReturnValue as? Int ?: -1
}

/**
 * Prompts the player with an input dialog where they can enter a string.
 *
 * @param description the title, or description, of the dialog box.
 *
 * @return the string input.
 */
suspend fun QueueTask.inputString(
    player: Player,
    description: String = "Enter text"
): String {
    player.runClientScript(ClientScript("meslayer_mode9"), description)
    terminateAction = closeInput(player)
    waitReturnValue()
    terminateAction!!(this)

    return requestReturnValue as? String ?: ""
}


/**
 * Prompts the player with a chatbox interface that allows them to search
 * for an item.
 *
 * @return
 * The selected item's id.
 */
suspend fun QueueTask.searchItemInput(
    player: Player,
    message: String
): Int {
    player.runClientScript(CommonClientScripts.GE_SEARCH_ITEMS, message, 1, -1)

    terminateAction = closeInput(player)
    waitReturnValue()

    return requestReturnValue as? Int ?: -1
}

/**
 * Prompts the player with a chatbox interface that allows them to search
 * for an item.
 * Difference from [QueueTask.searchItemInput] It let's you choose untradeable items too.
 *
 * @return
 * The selected item's id.
 */
suspend fun QueueTask.searchItemInputT(
    player: Player,
    message: String
): Int {
    player.runClientScript(CommonClientScripts.GE_SEARCH_ITEMS, message, 0, -1)

    terminateAction = closeInput(player)
    waitReturnValue()

    return requestReturnValue as? Int ?: -1
}

/**
 * Sends a normal message dialog.
 *
 * @message
 * The message to render on the dialog box.
 *
 * @lineSpacing
 * The spacing, in pixels, in between each line that will be rendered on the
 * dialog box.
 */
suspend fun QueueTask.messageBox(
    player: Player,
    message: String,
    continues: Boolean = true,
) {
    
    DialogMessageOpen(message, continues,player).post()

    terminateAction = closeDialog(player)
    waitReturnValue()
    terminateAction!!(this)
}

/**
 * Send a dialog with an npc's head model.
 *
 * @param message
 * The message to render on the dialog box.
 *
 * @npc
 * The npc id which represents the npc that will be drawn on the dialog box.
 * If set to -1, the npc id will be set to the player's interacting npc. If the
 * player is not interacting with an npc, a [RuntimeException] will be thrown.
 *
 * @animation
 * The animation id of the npc's head model.
 *
 * @title
 * The title of the dialog, if left as null, the npc's name will be used.
 */
suspend fun QueueTask.chatNpc(
    player: Player,
    message: String,
    npc: Int = -1,
    animation: String = "sequences.chatneu1",
    title: String? = null,
) {
    val npcId = when {
        npc != -1 -> npc
        else -> player.attr[INTERACTING_NPC_ATTR]?.get()?.getTransform(player)
            ?: error("Npc id must be manually set because the player is not interacting with an NPC.")
    }

    val dialogTitle = title ?: getNpc(npcId)?.name.orEmpty()
    DialogNpcOpen(message = message, npc = npcId,animation = animation, title = dialogTitle, player = player).post()
    terminateAction = closeDialog(player)
    waitReturnValue()
    terminateAction?.invoke(this)
}

/**
 * Send a dialog with your player's head model.
 *
 * @param message
 * The message to render on the dialog box.
*/

suspend fun QueueTask.chatPlayer(
    player: Player,
    message: String,
    animation: String = "sequences.chatneu1",
    title: String? = null,
) {
    val dialogTitle = title ?: player.username
    DialogPlayerOpen(message,animation,dialogTitle,player).post()
    terminateAction = closeDialog(player)
    waitReturnValue()
    terminateAction!!(this)
}

/**
 * Sends a single item dialog.
 *
 * @param message
 * The message to render on the dialog box.
 *
 * @param item
 * The id of the item to show on the dialog.
 *
 * @param amountOrZoom
 * The amount or zoom of the item to show on the dialog.
 *
 * @param options
 * Item dialog boxes can have multiple options be shown instead of the default
 * 'Click here to continue'.
 */
suspend fun QueueTask.itemMessageBox(
    player: Player,
    message: String,
    item: String,
    amountOrZoom: Int = 1,
    vararg options: String = arrayOf("Click here to continue"),
) {
    DialogItem(message,item.asRSCM(),amountOrZoom,player)

    terminateAction = closeDialog(player)
    waitReturnValue()
    terminateAction!!(this)
}

suspend fun QueueTask.doubleItemMessageBox(
    player: Player,
    message: String,
    item1: String,
    item2: String,
    amount1: Int = 1,
    amount2: Int = 1,
) {
    DialogItemDouble(message, item1.asRSCM(), item2.asRSCM(), amount1, amount2,player).post()

    terminateAction = closeDialog(player)
    waitReturnValue()
    terminateAction!!(this)
}


/**
 * Prompts the player with skill menu for making things.
 *
 * @param items
 * The possible [Item] products the menu presents as options.
 * Note| max is 10
 *
 * @param title
 * Title String to display atop the prompt.
 *
 * @param maxProducable
 * The possible number of products which could be made from the available input mats.
 * Note| defaults to full inventory as being possible
 *
 * @param logic
 * The logic to be executed upon response using selected [Item] from prompt
 * and quantity as indicated by response slot message.
 *
 * @return
 * The id of the option chosen. The id can range from [1] inclusive to [9] inclusive.
 */
suspend fun QueueTask.produceItemBox(
    player: Player,
    vararg items: Int,
    title: String = "What would you like to make?",
    maxProducable: Int = player.inventory.capacity,
    logic: Player.(Int, Int) -> Unit,
) {

    val itemDefs = items.map { getItem(it) }

    val baseChild = 15
    val itemArray = Array(15) { -1 }
    val nameArray = Array(15) { "|" }

    itemDefs.withIndex().forEach {
        val def = it.value
        itemArray[it.index] = def!!.id
        nameArray[it.index] = "|${def.name}"
    }

    player.runClientScript(CommonClientScripts.CHATBOX_RESET_BACKGROUND)
    player.sendTempVarbit("varbits.chatmodal_unclamp", 1)

    DialogSkillMulti(player).post()

    player.runClientScript(CommonClientScripts.SKILL_MULTI_SETUP, 0, "$title${nameArray.joinToString("")}", maxProducable, *itemArray)

    terminateAction = closeDialog(player)
    waitReturnValue()
    terminateAction!!(this)

    val msg = requestReturnValue as? ResumePauseButton ?: return

    val child = msg.componentId

    if (child < baseChild || child >= baseChild + items.size) {
        return
    }

    val item = items[child - baseChild]
    val qty = msg.sub

    logic(player, item, qty)
}
