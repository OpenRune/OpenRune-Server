package org.rsmod.content.skills.farming

import dev.openrune.types.ItemServerType
import org.rsmod.api.table.farming.FarmingProduceContainersRow

internal class ContainerForm(val obj: ItemServerType, val produce: ItemServerType, val count: Int)

object FarmingProduceContainers {
    internal val forms: List<ContainerForm> by lazy {
        FarmingProduceContainersRow.getRow("dbrow.farming_produce_containers").forms.map {
            ContainerForm(it.t0, it.t1, it.t2)
        }
    }

    private val filled by lazy { forms.filter { it.count > 0 }.associateBy { it.obj.id } }
    internal val sackFillChoices by lazy {
        forms.filter { it.count == 0 && it.obj.internalName == "obj.sack_empty" }
    }
    private val byContents by lazy { forms.associateBy { it.produce.id to it.count } }
    private val capacities by lazy {
        forms.groupBy { it.produce.id }.mapValues { (_, forms) -> forms.maxOf { it.count } }
    }

    internal fun filled(obj: Int): ContainerForm? = filled[obj]

    internal fun form(produce: Int, count: Int): ContainerForm? = byContents[produce to count]

    internal fun capacity(produce: Int): Int = capacities.getValue(produce)

    fun fullContainer(produce: String): String? =
        forms.lastOrNull { it.count > 0 && it.produce.internalName == produce }?.obj?.internalName
}
