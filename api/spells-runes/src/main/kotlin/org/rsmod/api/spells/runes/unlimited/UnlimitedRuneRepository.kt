package org.rsmod.api.spells.runes.unlimited

import dev.openrune.types.ItemServerType
import org.rsmod.api.enums.RuneEnums.rune_staves
import org.rsmod.api.enums.UnlimitedRunesEnums.unlimited_runes_hiprio
import org.rsmod.api.enums.UnlimitedRunesEnums.unlimited_runes_loprio
import org.rsmod.game.inv.InvObj

public class UnlimitedRuneRepository {
    // Magic rune validation has a subtle distinction involving certain "unlimited source" objs
    // (e.g., elemental staves): some are checked before the `fake_runes_enabled` condition, and
    // others after. While niche, deviating from the original order would cause behavioral
    // differences. To preserve this, we categorize the pre-check sources as "high-priority" and
    // post-check ones as "low-priority."
    private lateinit var highPriority: Map<Int, Set<Int>>
    private lateinit var lowPriority: Map<Int, Set<Int>>

    public fun isHighPrioritySource(
        rune: ItemServerType,
        righthand: InvObj?,
        lefthand: InvObj?,
    ): Boolean {
        val sources = highPriority[rune.id] ?: return false
        return righthand?.id in sources || lefthand?.id in sources
    }

    public fun isLowPrioritySource(
        rune: ItemServerType,
        righthand: InvObj?,
        lefthand: InvObj?,
    ): Boolean {
        val sources = lowPriority[rune.id] ?: return false
        return righthand?.id in sources || lefthand?.id in sources
    }

    public fun isSource(rune: ItemServerType, righthand: InvObj?, lefthand: InvObj?): Boolean =
        isHighPrioritySource(rune, righthand, lefthand) ||
            isLowPrioritySource(rune, righthand, lefthand)

    internal fun init(highPriority: Map<Int, Set<Int>>, lowPriority: Map<Int, Set<Int>>) {
        this.highPriority = highPriority
        this.lowPriority = lowPriority
    }

    internal fun init() {
        val highPriority = loadHighPriority()
        val lowPriority = loadLowPriority()
        init(highPriority, lowPriority)
    }

    private fun loadHighPriority(): Map<Int, Set<Int>> {
        val mapped = hashMapOf<Int, MutableSet<Int>>()

        val affinityStaffEnum = rune_staves.filterValuesNotNull()
        for ((rune, staffEnum) in affinityStaffEnum) {
            val staffList = staffEnum.filterValuesNotNull().filter { it.value }
            val targetSet = mapped.getOrPut(rune.id) { mutableSetOf() }
            targetSet += staffList.map { it.key.id }
        }

        val unlimitedSourceEnum = unlimited_runes_hiprio.filterValuesNotNull()
        for ((rune, sourceListEnum) in unlimitedSourceEnum) {
            val sources = sourceListEnum.filterValuesNotNull().values
            val targetSet = mapped.getOrPut(rune.id) { mutableSetOf() }
            targetSet += sources.map { it.id }
        }

        return mapped
    }

    private fun loadLowPriority(): Map<Int, Set<Int>> {
        val mappedUnlimited = hashMapOf<Int, Set<Int>>()

        val unlimitedSourceEnum = unlimited_runes_loprio.filterValuesNotNull()
        for ((rune, sourceListEnum) in unlimitedSourceEnum) {
            val sources = sourceListEnum.filterValuesNotNull().values
            mappedUnlimited[rune.id] = sources.map { it.id }.toHashSet()
        }

        return mappedUnlimited
    }
}
