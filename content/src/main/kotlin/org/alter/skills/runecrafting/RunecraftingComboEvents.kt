package org.alter.skills.runecrafting

import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.generated.tables.runecrafting.RunecraftingAltarsRow
import org.generated.tables.runecrafting.RunecraftingCombonationRunesRow

class RunecraftingComboEvents : PluginEvent() {

    override fun init() {
        RunecraftingAltarsRow.all().forEach { altar ->

            val combos = altar.combo
                .filterNotNull()
                .map { RunecraftingCombonationRunesRow.getRow(it) }

            if (combos.isEmpty()) return@forEach

            combos.forEach { combo ->

                on<ItemOnObject> {
                    where { gameObject.internalID == altar.altarObject && item.id == altar.talisman }
                    then { RunecraftAction.craftCombination(player, combo) }
                }

                on<ItemOnObject> {
                    where { gameObject.internalID == altar.altarObject && item.id == combo.talisman }
                    then { RunecraftAction.craftCombination(player, combo) }
                }
            }
        }
    }
}