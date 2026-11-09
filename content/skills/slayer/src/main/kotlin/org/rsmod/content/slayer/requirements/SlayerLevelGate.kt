package org.rsmod.content.slayer.requirements

import dev.openrune.types.NpcServerType
import dev.openrune.types.SlayerTaskKey
import jakarta.inject.Inject
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.player.stat.slayerLvl
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.api.table.slayer.SlayerTaskSublistRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal object SlayerLevelGate {
    const val MESSAGE = "You need a higher Slayer level to know how to wound this monster."

    private const val NO_REQUIREMENT = 1
    private const val SLAYER_STAT = "stat.slayer"

    private val levelByKey: Map<Int, Int> = buildMap {
        for (task in SlayerTaskRow.all()) {
            if (task.id > 0) put(task.id, slayerLevel(task))
        }
        for (boss in SlayerTaskSublistRow.all()) {
            put(SlayerTaskKey.of(SlayerTaskKey.BOSS_TASK_ID, boss.taskSubtableId), slayerLevel(boss.task))
        }
    }

    fun required(npc: Npc): Int = required(npc.visType) ?: required(npc.type) ?: NO_REQUIREMENT

    fun required(type: NpcServerType): Int? {
        type.paramOrNull(BaseParams.slayer_level)?.let { return it }
        return SlayerTaskCredit.keys(type).mapNotNull(levelByKey::get).maxOrNull()
    }

    fun verdict(slayerLevel: Int, required: Int): NpcAttackValidateResult =
        if (slayerLevel >= required) {
            NpcAttackValidateResult.Pass
        } else {
            NpcAttackValidateResult.Deny(MESSAGE)
        }

    private fun slayerLevel(task: SlayerTaskRow): Int =
        task.minStatRequirementAll
            .filter { (_, stat) -> stat.internalName == SLAYER_STAT }
            .maxOfOrNull { (level, _) -> level } ?: NO_REQUIREMENT
}

internal class SlayerLevelAttackHook @Inject constructor() : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult =
        SlayerLevelGate.verdict(player.slayerLvl, SlayerLevelGate.required(npc))
}
