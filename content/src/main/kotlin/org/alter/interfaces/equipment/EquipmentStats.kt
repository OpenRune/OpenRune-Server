package org.alter.interfaces.equipment

import dev.openrune.definition.type.widget.IfEvent
import org.alter.api.BonusSlot
import org.alter.api.CommonClientScripts
import org.alter.api.ext.getBonus
import org.alter.api.ext.getInteractingOption
import org.alter.api.ext.getMagicDamageBonus
import org.alter.api.ext.getPrayerBonus
import org.alter.api.ext.getRangedStrengthBonus
import org.alter.api.ext.getStrengthBonus
import org.alter.api.ext.message
import org.alter.api.ext.runClientScript
import org.alter.game.action.EquipAction
import org.alter.game.model.ExamineEntityType
import org.alter.game.model.entity.Player
import org.alter.game.model.move.stopMovement
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ContainerType
import org.alter.game.pluginnew.event.impl.IfModalDrag
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onIfModalButton
import org.alter.game.pluginnew.event.impl.onIfModalDrag
import org.alter.game.util.enum
import org.alter.game.util.vars.ComponentType
import org.alter.game.util.vars.IntType
import org.alter.interfaceInvInit
import org.alter.interfaces.ifClose
import org.alter.interfaces.ifOpenMainSidePair
import org.alter.interfaces.ifSetEvents
import org.alter.interfaces.ifSetText
import org.alter.rscm.RSCM
import org.alter.statGroupTooltip
import kotlin.inv

class EquipmentStats : PluginEvent() {

    override fun init() {
        onButton("components.wornitems:equipment") {
            selectStats(player)
        }

        enum("enums.equipment_stats_to_slots_map", IntType, ComponentType).forEach {
            onIfModalButton(it.value) { opWornMain(player,it.key, option) }
        }

        onIfModalButton("components.equipment_side:items") { opHeldSide(player,slot, option) }
        onIfModalDrag("components.equipment_side:items") { dragHeldButton(player,selectedSlot,targetSlot) }

    }

    private fun openStats(player : Player) {
        player.stopAction()
        player.stopMovement()
        player.animate(RSCM.NONE)
        player.graphic(RSCM.NONE)
        player.ifOpenMainSidePair(main = "interfaces.equipment", side = "interfaces.equipment_side")
        interfaceInvInit(
            player = player,
            inv = player.inventory,
            target = "components.equipment_side:items",
            objRowCount = 4,
            objColCount = 7,
            dragType = 1,
            op1 = "Equip",
        )
        player.ifSetEvents(
            component = "components.equipment_side:items",
            range = player.inventory.indices,
            IfEvent.Op1,
            IfEvent.Op10,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )
        updateBonuses(player)
    }

    private fun updateBonuses(player: Player) {
        player.ifSetText("components.equipment:stabatt", player.bonusTextMap()[0])
        player.ifSetText("components.equipment:slashatt", player.bonusTextMap()[1])
        player.ifSetText("components.equipment:crushatt", player.bonusTextMap()[2])
        player.ifSetText("components.equipment:magicatt", player.bonusTextMap()[3])
        player.ifSetText("components.equipment:rangeatt", player.bonusTextMap()[4])
        player.ifSetText("components.equipment:attackspeedbase", player.bonusTextMap()[5])
        player.ifSetText("components.equipment:attackspeedactual", player.bonusTextMap()[6])
        player.ifSetText("components.equipment:stabdef", player.bonusTextMap()[7])
        player.ifSetText("components.equipment:slashdef", player.bonusTextMap()[8])
        player.ifSetText("components.equipment:crushdef", player.bonusTextMap()[9])
        player.ifSetText("components.equipment:magicdef", player.bonusTextMap()[10])
        player.ifSetText("components.equipment:rangedef", player.bonusTextMap()[11])
        player.ifSetText("components.equipment:meleestrength", player.bonusTextMap()[12])
        player.ifSetText("components.equipment:rangestrength", player.bonusTextMap()[13])
        player.ifSetText("components.equipment:magicdamage", player.bonusTextMap()[14])
        player.ifSetText("components.equipment:prayer", player.bonusTextMap()[15])
        player.ifSetText("components.equipment:typemultiplier", player.bonusTextMap()[16],)
        statGroupTooltip(
            player,
            "components.equipment:tooltip",
            "components.equipment:typemultiplier",
            "Increases your effective accuracy and damage against undead creatures. " +
                    "For multi-target Ranged and Magic attacks, this applies only to the " +
                    "primary target. It does not stack with the Slayer multiplier.",
        )
        player.ifSetText("components.equipment:slayermultiplier", player.bonusTextMap()[17],)
    }

    private fun dragHeldButton(player: Player,selectedSlot: Int?,targetSlot : Int?) {
        val fromSlot = selectedSlot ?: return
        val intoSlot = targetSlot ?: return
        player.inventory.swap(fromSlot,intoSlot)
    }

    private fun opWornMain(player: Player,wornSlot: Int, op: MenuOption) {
        when (op.id) {
            1 -> {
                EquipAction.unequip(player, wornSlot, ContainerType.WORN_EQUIPMENT)
                player.calculateBonuses()
                updateBonuses(player)
            }
            10 -> {
                val item = player.equipment[wornSlot] ?: return
                world.sendExamine(player, item.id, ExamineEntityType.ITEM)
            }
            else -> {
                val item = player.equipment[wornSlot] ?: return
                !world.plugins.executeItem(player, item.id, op.id)
            }
        }
    }

    private fun opHeldSide(player: Player,invSlot: Int, op: MenuOption) {
        val item = player.inventory[invSlot] ?: return

        if (op.id == 1) {
            val result = EquipAction.equip(player, item, inventorySlot = invSlot, ContainerType.WORN_EQUIPMENT)
            if (result == EquipAction.Result.SUCCESS) {
                player.calculateBonuses()
                updateBonuses(player)
            } else if (result == EquipAction.Result.UNHANDLED) {
                player.message("You can't equip that.")
            }
        } else if (op.id == 10) {
            world.sendExamine(player, item.id, ExamineEntityType.ITEM)
        }
    }

    private fun selectStats(player: Player) {
        player.ifClose()
        openStats(player)
    }

    fun Player.bonusTextMap(): List<String> {
        var magicDamageBonus = getMagicDamageBonus().toDouble()
        return listOf(
            "Stab: ${formatBonus(this, BonusSlot.ATTACK_STAB)}",
            "Slash: ${formatBonus(this, BonusSlot.ATTACK_SLASH)}",
            "Crush: ${formatBonus(this, BonusSlot.ATTACK_CRUSH)}",
            "Magic: ${formatBonus(this, BonusSlot.ATTACK_MAGIC)}",
            "Range: ${formatBonus(this, BonusSlot.ATTACK_RANGED)}",
            "Base: TODO",
            "Actual: TODO",
            "Stab: ${formatBonus(this, BonusSlot.DEFENCE_STAB)}",
            "Slash: ${formatBonus(this, BonusSlot.DEFENCE_SLASH)}",
            "Crush: ${formatBonus(this, BonusSlot.DEFENCE_CRUSH)}",
            "Range: ${formatBonus(this, BonusSlot.DEFENCE_RANGED)}",
            "Magic: ${formatBonus(this, BonusSlot.DEFENCE_MAGIC)}",
            "Melee STR: ${formatBonus(this.getStrengthBonus())}",
            "Ranged STR: ${formatBonus(this.getRangedStrengthBonus())}",
            "Magic DMG: ${formatBonus(this.getMagicDamageBonus())}",
            "Prayer: ${formatBonus(this.getPrayerBonus())}",
            "Undead: TODO",
            "Slayer: TODO"
        )
    }


    fun formatBonus(p: Player, slot: BonusSlot): String = formatBonus(p.getBonus(slot))

    fun formatBonus(bonus: Int): String = if (bonus < 0) bonus.toString() else "+$bonus"

}