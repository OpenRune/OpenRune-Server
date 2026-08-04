package org.rsmod.content.other.toolbelt

import dev.openrune.types.ItemServerType
import org.rsmod.api.table.ToolbeltSlotsRow

val ToolbeltSlotsRow.defaultItem: ItemServerType
    get() = allowedItems.first()

fun ToolbeltSlotsRow.accepts(internalName: String): Boolean =
    allowedItems.any { it.internalName == internalName }

fun ToolbeltSlotsRow.accepts(type: ItemServerType): Boolean =
    allowedItems.any { it.id == type.id }
