package org.alter.game.model.container.key

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.InventoryType
import dev.openrune.types.InventoryServerType
import org.alter.game.model.container.ContainerStackType
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType
import kotlin.properties.Delegates

/**
 * A unique key used for an [org.alter.game.model.container.ItemContainer].
 *
 * @author Tom <rspsmods@gmail.com>
 */
data class ContainerKey(val name: String, val internalName: String, val capacity: Int, val stackType: ContainerStackType) {

    var internalID by Delegates.notNull<Int>()

    var type: InventoryServerType

    init {
        RSCM.requireRSCM(RSCMType.INVTYPES, internalName)
        internalID = internalName.asRSCM()
        type = ServerCacheManager.getInventory(internalID) ?: error("Invalid inventoryId: $internalName")

    }

    override fun equals(other: Any?): Boolean =
        other is ContainerKey && other.internalName == internalName

    override fun hashCode(): Int = internalName.hashCode()
}