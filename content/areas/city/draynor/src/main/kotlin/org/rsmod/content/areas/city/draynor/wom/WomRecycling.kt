package org.rsmod.content.areas.city.draynor.wom

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.recyclingTab by intVarBit("varbit.wom_interface_tab")

/**
 * The Wise Old Man's Recycling Centre. The client draws the junk list itself from the
 * `wom_recycling_tabs` enum tree and the bank, inventory and worn transmits; the server keeps the
 * list's click range in sync and destroys whatever the player removes.
 */
class WomRecycling : PluginScript() {
    override fun ScriptContext.startup() {
        onIfModalButton("component.wom_recycling:items_layer") { button ->
            val obj = button.obj ?: return@onIfModalButton
            when (button.op) {
                IfButtonOp.Op1 -> removeJunk(listOf(obj), announce = true)
                IfButtonOp.Op2 -> mes(obj.examine)
                else -> Unit
            }
        }
        onIfModalButton("component.wom_recycling:layer_1") { selectTab(ALL_TAB) }
        onIfModalButton("component.wom_recycling:layer_2") { selectTab(1) }
        onIfModalButton("component.wom_recycling:layer_3") { selectTab(2) }
        onIfModalButton("component.wom_recycling:layer_button") {
            ifSetHide("component.wom_recycling:confirm_layer", false)
        }
        onIfModalButton("component.wom_recycling:decline_button") {
            ifSetHide("component.wom_recycling:confirm_layer", true)
        }
        onIfModalButton("component.wom_recycling:confirm_button") {
            ifSetHide("component.wom_recycling:confirm_layer", true)
            removeJunk(junkFor(player.recyclingTab), announce = false)
        }
        onIfClose("interface.wom_recycling") { player.stopInvTransmit(player.bankInv()) }
    }

    private fun ProtectedAccess.selectTab(tab: Int) {
        player.recyclingTab = tab
        setListEvents()
    }

    private fun ProtectedAccess.setListEvents() {
        ifSetEvents(
            "component.wom_recycling:items_layer",
            0..LIST_COMPONENTS,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Depth2,
        )
    }

    private fun ProtectedAccess.removeJunk(objs: List<ItemServerType>, announce: Boolean) {
        val junk = junkFor(ALL_TAB).map { it.id }.toSet()
        var removedAny = false
        for (obj in objs) {
            if (obj.id !in junk) continue
            val name = obj.internalName
            val banked = bank.count(name)
            val carried = inv.count(name)
            if (banked == 0 && carried == 0) continue
            if (banked > 0) invDel(bank, name, banked)
            if (carried > 0) invDel(inv, name, carried)
            removedAny = true
            if (announce) mes("You have removed ${obj.name}.")
        }
        if (removedAny) soundSynth(REMOVE_SYNTH)
    }

    private fun Player.bankInv() = invMap.getOrPut("inv.bank")

    companion object {
        private const val ALL_TAB = 0
        private const val LIST_COMPONENTS = 2047
        private const val REMOVE_SYNTH = "synth.wom_recycle_remove"
        private const val OPEN_SYNTH = "synth.pillory_success"
        private val QUEST_ENUMS = listOf("enum.wom_recycling_free_quests", "enum.wom_recycling_members_quests")

        fun ProtectedAccess.openRecyclingCentre() {
            player.recyclingTab = ALL_TAB
            player.startInvTransmit(bank)
            soundSynth(OPEN_SYNTH)
            ifOpenMainModal("interface.wom_recycling")
            ifSetEvents(
                "component.wom_recycling:items_layer",
                0..LIST_COMPONENTS,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Depth2,
            )
        }

        private fun tabs(): Map<Int, Int> {
            val root = ServerCacheManager.getEnum("enum.wom_recycling_tabs".asRSCM(RSCMType.ENUM))
            return root?.values?.mapValues { (it.value as Number).toInt() } ?: emptyMap()
        }

        private fun selectedTabs(tab: Int): List<Int> {
            val tabs = tabs()
            return if (tab == ALL_TAB) tabs.toSortedMap().values.toList() else listOfNotNull(tabs[tab])
        }

        private fun groups(tab: Int): List<Pair<Int, List<Int>>> {
            val tabEnum = ServerCacheManager.getEnum(tab) ?: return emptyList()
            return tabEnum.values.entries.sortedBy { it.key }.map { (_, group) ->
                val groupId = (group as Number).toInt()
                val items = ServerCacheManager.getEnum(groupId)?.values?.values
                groupId to (items?.map { (it as Number).toInt() } ?: emptyList())
            }
        }

        private fun questRowFor(group: Int): Int? =
            QUEST_ENUMS.firstNotNullOfOrNull { questEnum ->
                ServerCacheManager.getEnum(questEnum.asRSCM(RSCMType.ENUM))
                    ?.values
                    ?.get(group)
                    ?.let { (it as Number).toInt() }
            }

        private fun Player.completedQuestRow(row: Int): Boolean =
            Quest.all().firstOrNull { it.rowID == row }?.isQuestCompleted(this) == true

        private fun ProtectedAccess.junkFor(tab: Int): List<ItemServerType> =
            selectedTabs(tab)
                .flatMap { groups(it) }
                .filter { (group, _) -> questRowFor(group)?.let { player.completedQuestRow(it) } ?: false }
                .flatMap { (_, items) -> items }
                .mapNotNull { ServerCacheManager.getItem(it) }
    }
}
