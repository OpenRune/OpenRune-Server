package org.alter.interfaces.equipment

import dev.openrune.definition.type.widget.IfEvent
import org.alter.combat.WeaponSpeeds
import org.alter.combat.WornBonuses
import org.alter.api.ext.message
import org.alter.game.action.EquipAction
import org.alter.game.model.ExamineEntityType
import org.alter.game.model.entity.Player
import org.alter.game.model.entity.UpdateInventory.resendSlot
import org.alter.game.model.move.stopMovement
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ContainerType
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onIfModalButton
import org.alter.game.pluginnew.event.impl.onIfModalDrag
import org.alter.game.util.enum
import org.alter.game.util.vars.ComponentVarType
import org.alter.game.util.vars.IntType
import org.alter.interfaceInvInit
import org.alter.interfaces.ifClose
import org.alter.interfaces.ifOpenMainSidePair
import org.alter.interfaces.ifSetEvents
import org.alter.interfaces.ifSetText
import org.alter.invMoveToSlot
import org.alter.rscm.RSCM
import org.alter.statGroupTooltip

class EquipmentStats : PluginEvent() {

    override fun init() {

        onButton("components.wornitems:equipment") {
            selectStats(player)
        }

        enum("enums.equipment_stats_to_slots_map", IntType, ComponentVarType).forEach {
            onIfModalButton(it.value) { opWornMain(player,it.key, op) }
        }

        enum("enums.equipment_tab_to_slots_map", IntType, ComponentVarType).forEach {
            onIfModalButton(it.value) { opWornMain(player,it.key, op) }
        }

        onIfModalButton("components.equipment_side:items") { opHeldSide(player,slot, op) }
        onIfModalDrag("components.equipment_side:items") { dragHeldButton(player,selectedSlot,targetSlot) }

    }

    private fun openStats(player : Player) {
        player.stopAction()
        player.stopMovement()
        player.animate(RSCM.NONE)
        player.graphic(RSCM.NONE)
        player.ifOpenMainSidePair(main = "interfaces.equipment", side = "interfaces.equipment_side")
        player.invTransmit(player.inventory)

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
        player.updateBonuses()
    }

    private fun Player.updateBonuses() {
        val stats = WornBonuses.calculate(this)
        val speedBase = WeaponSpeeds.base(this)
        val speedActual = WeaponSpeeds.actual(this)
        val magicDmg = stats.finalMagicDmg
        val magicDmgSuffix = stats.magicDmgSuffix
        val undeadSuffix = stats.undeadSuffix
        val slayerSuffix = stats.slayerSuffix
        ifSetText("components.equipment:stabatt", "Stab: ${stats.offStab.signed}")
        ifSetText("components.equipment:slashatt", "Slash: ${stats.offSlash.signed}")
        ifSetText("components.equipment:crushatt", "Crush: ${stats.offCrush.signed}")
        ifSetText("components.equipment:magicatt", "Magic: ${stats.offMagic.signed}")
        ifSetText("components.equipment:rangeatt", "Range: ${stats.offRange.signed}")
        ifSetText("components.equipment:attackspeedbase", "Base: ${speedBase.tickToSecs}")
        ifSetText("components.equipment:attackspeedactual", "Actual: ${speedActual.tickToSecs}")
        ifSetText("components.equipment:stabdef", "Stab: ${stats.defStab.signed}")
        ifSetText("components.equipment:slashdef", "Slash: ${stats.defSlash.signed}")
        ifSetText("components.equipment:crushdef", "Crush: ${stats.defCrush.signed}")
        ifSetText("components.equipment:magicdef", "Range: ${stats.defRange.signed}")
        ifSetText("components.equipment:rangedef", "Magic: ${stats.defMagic.signed}")
        ifSetText("components.equipment:meleestrength", "Melee STR: ${stats.meleeStr.signed}")
        ifSetText("components.equipment:rangestrength", "Ranged STR: ${stats.rangedStr.signed}")
        ifSetText("components.equipment:magicdamage", "Magic DMG: $magicDmg$magicDmgSuffix")
        ifSetText("components.equipment:prayer", "Prayer: ${stats.prayer.signed}")
        ifSetText(
            "components.equipment:typemultiplier",
            "Undead: ${stats.undead.formatWholePercent}$undeadSuffix",
        )
        statGroupTooltip(
            this,
            "components.equipment:tooltip",
            "components.equipment:typemultiplier",
            "Increases your effective accuracy and damage against undead creatures. " +
                    "For multi-target Ranged and Magic attacks, this applies only to the " +
                    "primary target. It does not stack with the Slayer multiplier.",
        )
        ifSetText(
            "components.equipment:slayermultiplier",
            "Slayer: ${stats.slayer.formatWholePercent}$slayerSuffix",
        )
    }

    private fun dragHeldButton(player: Player,selectedSlot: Int?,targetSlot : Int?) {
        val fromSlot = selectedSlot ?: return
        val intoSlot = targetSlot ?: return

        player.invMoveToSlot(player.inventory, player.inventory, fromSlot, intoSlot)
    }

    private fun opWornMain(player: Player,wornSlot: Int, op: MenuOption) {
        resendSlot(player.equipment, 0)
        when (op.id) {
            1 -> {
                EquipAction.unequip(player, wornSlot, ContainerType.WORN_EQUIPMENT)
                player.updateBonuses()
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

        if (op == MenuOption.OP10) {
            world.sendExamine(player, item.id, ExamineEntityType.ITEM)
            return
        }

        if (op == MenuOption.OP1) {
            val result = EquipAction.equip(player, item, inventorySlot = invSlot, ContainerType.WORN_EQUIPMENT)
            if (result == EquipAction.Result.SUCCESS) {
                player.updateBonuses()
            } else if (result == EquipAction.Result.UNHANDLED) {
                player.message("You can't equip that.")
            }
        }
    }

    private fun selectStats(player: Player) {
        player.ifClose()
        openStats(player)
    }




}

private val Int.signed: String
    get() = if (this < 0) "$this" else "+$this"

private val Int.formatPercent: String
    get() = "+${this / 10.0}%"

private val Int.formatWholePercent: String
    get() = "+${this / 10}%"

private val Int.tickToSecs: String
    get() = "${(this * 600) / 1000.0}s"

private val WornBonuses.Bonuses.finalMagicDmg: String
    get() = multipliedMagicDmg.formatPercent

private val WornBonuses.Bonuses.magicDmgSuffix: String
    get() = if (magicDmgAdditive == 0) "" else "<col=be66f4> ($magicDmgAdditive%)</col>"

// Undead bonus has a trailing whitespace when bonus is at 0.
private val WornBonuses.Bonuses.undeadSuffix: String
    get() = if (undead == 0) " " else if (undeadMeleeOnly) " (melee)" else " (all styles)"

private val WornBonuses.Bonuses.slayerSuffix: String
    get() = if (slayer == 0) "" else if (slayerMeleeOnly) " (melee)" else " (all styles)"
