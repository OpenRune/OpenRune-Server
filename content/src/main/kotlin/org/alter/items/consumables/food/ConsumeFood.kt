package org.alter.items.consumables.food

import org.alter.api.ext.message
import org.alter.game.pluginnew.MenuOption
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemClickEvent
import org.alter.game.pluginnew.event.impl.onItemOption
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.column
import org.alter.game.util.columnOptional
import org.alter.game.util.multiColumn
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType

class ConsumeFood : PluginEvent() {

    override fun init() {
        table("tables.consumable_food").forEach { food ->
            val itemIds = food.multiColumn("columns.consumable_food:items", ObjType)
            //val location = food.columnOptional("columns.teleport_tablets:location", IntType)

            on<ItemClickEvent> {
                where { itemIds.contains(item) && hasOption("eat") }
                then { player.message("SCRANN") }
            }

        }
    }
}