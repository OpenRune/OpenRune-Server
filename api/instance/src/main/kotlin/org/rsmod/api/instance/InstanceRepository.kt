package org.rsmod.api.instance

import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeMap
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public class InstanceRepository
@Inject
constructor(
    private val manager: InstanceManager,
    private val regionRepo: RegionRepository,
    private val regionRegistry: RegionRegistry,
) {
    public fun instanceOf(player: Player): Instance? = manager.instanceOf(player)

    public fun create(template: RegionTemplate, fallbackPosition: CoordGrid? = null): Instance? {
        val region = regionRepo.add(template) ?: return null
        return Instance(region, fallbackPosition, AttributeMap())
    }

    public fun enter(instance: Instance, player: Player) {
        instance.players += player
        manager.register(player, instance)
    }

    public fun leave(instance: Instance, player: Player) {
        if (!instance.players.remove(player)) return
        manager.unregister(player)
        if (instance.players.isEmpty()) {
            destroy(instance)
        }
    }

    public fun destroy(instance: Instance) {
        for (player in instance.players.toList()) {
            manager.unregister(player)
        }
        instance.players.clear()
        regionRegistry.unregister(instance.region)
        instance.invokeTeardown()
    }
}
