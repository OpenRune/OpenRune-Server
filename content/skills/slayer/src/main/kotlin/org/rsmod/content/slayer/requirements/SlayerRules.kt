package org.rsmod.content.slayer.requirements

import dev.openrune.types.NpcServerType
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.table.slayer.SlayerFinishersRow
import org.rsmod.api.table.slayer.SlayerGearRow
import org.rsmod.api.table.slayer.SlayerWeaponRulesRow
import org.rsmod.game.entity.Npc

object SlayerRules {
    private val gear = index(SlayerGearRow.all(), { it.tasks }, { it.variant })
    private val finishers =
        index(SlayerFinishersRow.all(), { listOf(it.task) }, { it.variant })
    private val weaponRules = index(SlayerWeaponRulesRow.all(), { it.tasks }, { it.variant })

    fun gear(npc: Npc): SlayerGearRow? = find(gear, npc)

    fun finisher(npc: Npc): SlayerFinishersRow? = find(finishers, npc)

    fun weaponRule(npc: Npc): SlayerWeaponRulesRow? = find(weaponRules, npc)

    fun gear(type: NpcServerType): SlayerGearRow? = find(gear, type)

    fun finisher(type: NpcServerType): SlayerFinishersRow? = find(finishers, type)

    fun weaponRule(type: NpcServerType): SlayerWeaponRulesRow? = find(weaponRules, type)

    private fun <R> find(rows: Map<Long, R>, npc: Npc): R? =
        find(rows, npc.visType) ?: find(rows, npc.type)

    private fun <R> find(rows: Map<Long, R>, type: NpcServerType): R? {
        val variant = type.paramOrNull(BaseParams.slayer_variant) ?: return null
        for (key in SlayerTaskCredit.keys(type)) {
            rows[pack(key, variant)]?.let { return it }
        }
        return null
    }

    private fun <R> index(
        rows: List<R>,
        tasks: (R) -> List<Int>,
        variant: (R) -> Int,
    ): Map<Long, R> = buildMap {
        for (row in rows) {
            for (task in tasks(row)) {
                put(pack(task, variant(row)), row)
            }
        }
    }

    private fun pack(task: Int, variant: Int): Long = (task.toLong() shl Int.SIZE_BITS) or variant.toLong()
}
