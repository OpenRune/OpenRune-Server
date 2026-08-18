package org.rsmod.content.interfaces.collectionlog

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

public object CollectionLog {
    private const val LATEST_ITEM_SLOT_COUNT = 12
    private const val CHAT_MESSAGE_ENABLED_MASK = 1 shl 0
    private const val POPUP_ENABLED_MASK = 1 shl 1

    private const val EMPTY_SLOT = -1

    private var Player.runeday: Int by intVarBit("varbit.current_runeday")

    public fun initializeOverviewSlots(player: Player) {
        for (slot in 0 until LATEST_ITEM_SLOT_COUNT) {
            if (player.vars[latestItemVarp(slot)] == 0) {
                VarPlayerIntMapSetter.set(player, latestItemVarp(slot), EMPTY_SLOT)
                VarPlayerIntMapSetter.set(player, latestItemDateVarp(slot), EMPTY_SLOT)
            }
        }
    }

    /** Central entry point for marking an item as obtained for the collection log */
    public fun grant(player: Player, obj: String, count: Int = 1) {
        grant(player, obj.asRSCM(RSCMType.OBJ), count)
    }

    public fun grant(player: Player, objId: Int, count: Int = 1) {
        if (count <= 0) {
            return
        }
        if (!CollectionLogItems.contains(objId)) {
            return
        }
        val isNewItem = player.collectionTransmit.countOf(objId) == 0
        player.invAdd(player.collectionTransmit, objId, count)
        checkCategoryCompletion(player, objId)
        if (isNewItem) {
            onNewItemObtained(player, objId)
        }
    }

    /** Runs the first time [objId] is added to [player]'s collection log. */
    private fun onNewItemObtained(player: Player, objId: Int) {
        for (slot in LATEST_ITEM_SLOT_COUNT - 1 downTo 1) {
            val previousObj = player.vars[latestItemVarp(slot - 1)]
            val previousDate = player.vars[latestItemDateVarp(slot - 1)]
            VarPlayerIntMapSetter.set(player, latestItemVarp(slot), previousObj)
            VarPlayerIntMapSetter.set(player, latestItemDateVarp(slot), previousDate)
        }
        VarPlayerIntMapSetter.set(player, latestItemVarp(0), objId)
        VarPlayerIntMapSetter.set(player, latestItemDateVarp(0), player.runeday)

        val settings = player.vars["varbit.option_collection_new_item"]
        val chatMessageEnabled = settings and CHAT_MESSAGE_ENABLED_MASK != 0
        val popupEnabled = settings and POPUP_ENABLED_MASK != 0
        if (!chatMessageEnabled && !popupEnabled) {
            return
        }
        val itemName = ServerCacheManager.getItem(objId)?.name ?: return
        if (chatMessageEnabled) {
            player.mes("New item added to your collection log: $itemName")
        }
        if (popupEnabled) {
            ClientScripts.notificationDisplay(
                player,
                "Collection Log",
                "New item added to your collection log: $itemName",
            )
        }
    }

    private fun latestItemVarp(slot: Int) = "varp.collection_overview_last_item$slot"

    private fun latestItemDateVarp(slot: Int) = "varp.collection_overview_last_item${slot}_date"

    private fun checkCategoryCompletion(player: Player, itemId: Int) {
        val inv = player.collectionTransmit
        for (category in CollectionLogCategories.categoriesContaining(itemId)) {
            if (player.vars[category.completedVarbit] != 0) {
                continue
            }
            val allObtained = category.itemIds.all { inv.countOf(it) > 0 }
            if (allObtained) {
                VarPlayerIntMapSetter.set(player, category.completedVarbit, 1)
            }
        }
    }
}
