package org.rsmod.content.skills.farming

import dev.openrune.types.ItemServerType
import org.rsmod.api.enums.FarmingProduceEnums.farming_produce_containers
import org.rsmod.api.enums.FarmingProduceEnums.farming_produce_fill_priority

internal class ContainerForm(val obj: ItemServerType, val produce: ItemServerType, val count: Int)

object FarmingProduceContainers {
    internal val forms: List<ContainerForm> =
        farming_produce_containers.backing.entries
            .sortedBy { farming_produce_fill_priority.getValue(it.key) }
            .flatMap { (produce, variants) ->
                checkNotNull(variants).map { (count, obj) ->
                    ContainerForm(checkNotNull(obj), produce, count)
                }
            }

    internal val fillChoices = forms.filter { it.count == 0 }.groupBy { it.obj.id }
    private val byContents =
        forms.groupBy { it.produce.id }.mapValues { (_, forms) -> forms.associateBy { it.count } }
    private val capacities = byContents.mapValues { (_, forms) -> forms.keys.max() }

    internal fun form(produce: Int, count: Int): ContainerForm? = byContents[produce]?.get(count)

    internal fun capacity(produce: Int): Int = capacities.getValue(produce)
}
