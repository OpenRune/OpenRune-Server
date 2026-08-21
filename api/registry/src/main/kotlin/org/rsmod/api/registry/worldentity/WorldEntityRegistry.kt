package org.rsmod.api.registry.worldentity

import jakarta.inject.Inject
import org.rsmod.events.EventBus
import org.rsmod.game.entity.WorldEntity
import org.rsmod.game.entity.WorldEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.WorldEntityList
import org.rsmod.game.entity.worldentity.NoopWorldEntityInfo
import org.rsmod.game.entity.worldentity.WorldEntityStateEvents

public class WorldEntityRegistry
@Inject
constructor(
    private val worldEntityList: WorldEntityList,
    private val eventBus: EventBus,
) {
    public fun count(): Int = worldEntityList.count()

    public fun add(entity: WorldEntity): WorldEntityRegistryResult.Add {
        val slot =
            worldEntityList.nextFreeSlot() ?: return WorldEntityRegistryResult.Add.NoAvailableSlot
        worldEntityList[slot] = entity
        entity.slotId = slot
        eventBus.publish(WorldEntityStateEvents.Create(entity))
        return WorldEntityRegistryResult.Add.Success
    }

    public fun del(entity: WorldEntity): WorldEntityRegistryResult.Delete {
        val slot = entity.slotId
        if (slot == INVALID_SLOT) {
            return WorldEntityRegistryResult.Delete.UnexpectedSlot
        } else if (worldEntityList[slot] != entity) {
            return WorldEntityRegistryResult.Delete.ListSlotMismatch(worldEntityList[slot])
        }
        worldEntityList.remove(slot)
        eventBus.publish(WorldEntityStateEvents.Delete(entity))
        entity.slotId = INVALID_SLOT
        entity.infoProtocol = NoopWorldEntityInfo
        return WorldEntityRegistryResult.Delete.Success
    }

    public fun findAll(): Sequence<WorldEntity> = worldEntityList.asSequence()
}
