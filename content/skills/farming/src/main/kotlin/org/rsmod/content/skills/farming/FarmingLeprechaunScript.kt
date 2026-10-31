package org.rsmod.content.skills.farming

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingLeprechaunScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in LEPRECHAUNS) {
            onOpNpc1(npc) { startDialogue(it.npc) { talk() } }
            onOpNpc3(npc) { openStore() }
            onOpNpc4(npc) { depositAll() }
            onOpNpcU(npc) {
                if (inv[it.invSlot]?.id != it.objType.id) return@onOpNpcU
                val type = it.objType
                val refusal = when {
                    type.isCert -> "That IS a banknote!"
                    type.internalName == "obj.cabbage" &&
                        it.npc.spawnCoords == FarmingServicesData.row.cabbageRefusalCoord ->
                        "A cabbage? Don't ye go showing me yer cabbage; this whole farm stinks of the things! Take it away and get it exchanged somewhere else."
                    !type.canCert -> "Nay, there's no such thing as a banknote for that."
                    !FarmingLeprechaunStorage.canNote(type) -> "Nay, I've got no banknotes to exchange for that item."
                    else -> null
                }
                if (refusal != null) startDialogue(it.npc) { chatNpc(neutral, refusal) }
                else if (!FarmingLeprechaunStorage.note(player, type.id)) mes("You don't have enough inventory space.")
                else objbox(type.internalName, "The leprechaun exchanges your items for banknotes.")
            }
        }
        onIfOpen(SIDE) {
            player.startInvTransmit(player.inv)
            for (tool in FarmingLeprechaunStorage.tools) {
                if (tool.capacity == 1) {
                    player.ifSetEvents(main(tool), -1..-1, IfEvent.Op1, IfEvent.Op10)
                    player.ifSetEvents(side(tool), -1..-1, IfEvent.Op1, IfEvent.Op10)
                } else {
                    player.ifSetEvents(main(tool), -1..-1, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op9, IfEvent.Op10)
                    player.ifSetEvents(side(tool), -1..-1, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op10)
                }
            }
            for (quantity in listOf("quantity_1", "quantity_5", "quantity_x", "quantity_all")) {
                player.ifSetEvents("component.farming_tools:$quantity", -1..-1, IfEvent.Op1)
            }
            player.ifSetEvents("component.farming_tools:deposit_all", -1..-1, IfEvent.Op1)
            VarPlayerIntMapSetter.set(player, QUANTITY, player.vars[QUANTITY])
        }
        for (tool in FarmingLeprechaunStorage.tools) {
            onIfModalButton(main(tool)) {
                if (it.op == IfButtonOp.Op10) {
                    val obj = FarmingLeprechaunStorage.stored(player, tool)
                    mes(if (obj == null) tool.obj.examine else getInvObj(obj).examine)
                    return@onIfModalButton
                }
                val notes = it.op == IfButtonOp.Op9
                val requested = if (notes) Int.MAX_VALUE else requested(tool, it.op)
                if (FarmingLeprechaunStorage.withdraw(player, tool.component, requested, notes) == 0) {
                    mes("No items were moved.")
                }
            }
            onIfModalButton(side(tool)) {
                if (it.op == IfButtonOp.Op10) {
                    mes(tool.obj.examine)
                    return@onIfModalButton
                }
                val requested = requested(tool, it.op)
                if (FarmingLeprechaunStorage.depositTool(player, tool, requested) == 0) {
                    mes("No items were moved.")
                }
            }
        }
        onIfModalButton("component.farming_tools:deposit_all") {
            if (it.op == IfButtonOp.Op1) depositAll()
        }
        onIfModalButton("component.farming_tools:quantity_1") { setQuantity(it.op, 0) }
        onIfModalButton("component.farming_tools:quantity_5") { setQuantity(it.op, 1) }
        onIfModalButton("component.farming_tools:quantity_all") { setQuantity(it.op, 2) }
        onIfModalButton("component.farming_tools:quantity_x") { setQuantity(it.op, 3) }
    }

    private fun ProtectedAccess.openStore() {
        ifOpenMainSidePair(main = MAIN, side = SIDE, transparency = -1)
    }

    private fun ProtectedAccess.depositAll() {
        val moved = FarmingLeprechaunStorage.depositAll(player)
        val message = when {
            moved == 0 -> "None of your items could be deposited."
            inv.indices.any { slot ->
                inv[slot]?.let { FarmingLeprechaunStorage.tool(getInvObj(it)) != null } == true
            } -> "Some of your items couldn't be deposited."
            else -> "You deposit all of your items."
        }
        mes(message)
    }

    private suspend fun Dialogue.talk() {
        chatNpc(neutral, "Ah, 'tis a foine day to be sure! Were yez wantin' me to store yer tools, or maybe ye might be wantin' yer stuff back from me?")
        while (true) {
            when (choice4(
                "Yes please.", 1,
                "What can you store?", 2,
                "What do you do with the tools you're storing?", 3,
                "No thanks, I'll keep hold of my stuff.", 4,
            )) {
                1 -> { access.openStore(); return }
                2 -> {
                    chatNpc(neutral, "We'll hold onto yer rake, yer seed dibber, yer spade, yer secateurs, yer waterin' can and yer trowel - but mind it's not one of them fancy trowels only archaeologists use!")
                    chatNpc(neutral, "We'll take a few buckets off yer hands too, and even yer compost, supercompost an' ultracompost! Also plant cure vials.")
                    chatNpc(neutral, "Aside from that, if ye hands me yer farming produce, I can mebbe change it into banknotes for ye.")
                }
                3 -> chatNpc(neutral, "We leprechauns have a shed where we keep 'em. It's a magic shed, so ye can get yer items back from any of us leprechauns whenever ye want. Saves ye havin' to carry loads of stuff around the country!")
                4 -> {
                    chatNpc(neutral, "Ye must be dafter than ye look if ye likes luggin' yer tools everywhere ye goes!")
                    return
                }
            }
            chatNpc(neutral, "So... do ye want to be using the store?")
        }
    }

    private fun ProtectedAccess.setQuantity(op: IfButtonOp, quantity: Int) {
        if (op == IfButtonOp.Op1) VarPlayerIntMapSetter.set(player, QUANTITY, quantity)
    }

    private suspend fun ProtectedAccess.requested(tool: LeprechaunTool, op: IfButtonOp): Int {
        if (tool.capacity == 1) return if (op == IfButtonOp.Op1) 1 else 0
        val selection = player.vars[QUANTITY]
        val quantity = when (op) {
            IfButtonOp.Op1 -> when (selection) { 1 -> 5; 2 -> Int.MAX_VALUE; 3 -> 0; else -> 1 }
            IfButtonOp.Op2 -> if (selection == 0) 5 else 1
            IfButtonOp.Op3 -> if (selection == 0 || selection == 1) 0 else 5
            IfButtonOp.Op4 -> if (selection == 2) 0 else Int.MAX_VALUE
            else -> return 0
        }
        return if (quantity == 0) countDialog().coerceAtLeast(0) else quantity
    }

    private fun main(tool: LeprechaunTool): String = "component.farming_tools:${tool.component}"
    private fun side(tool: LeprechaunTool): String = "component.farming_tools_side:${tool.component}"

    private companion object {
        const val MAIN = "interface.farming_tools"
        const val SIDE = "interface.farming_tools_side"
        const val QUANTITY = "varbit.farming_tools_selectedquantity"
        val LEPRECHAUNS = listOf(
            "npc.farming_tools_leprechaun", "npc.farming_tools_leprechaun_draynor",
            "npc.farming_tools_leprechaun_varlamore", "npc.fossil_leprechaun_underwater",
            "npc.myarm_leprechaun", "npc.my2arm_leprechaun",
        )
    }
}
