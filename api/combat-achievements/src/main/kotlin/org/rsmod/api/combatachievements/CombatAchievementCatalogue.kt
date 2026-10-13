package org.rsmod.api.combatachievements

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.varp.VarpLifetime
import jakarta.inject.Inject
import jakarta.inject.Singleton

public data class CombatAchievementTask(
    public val id: Int,
    public val structId: Int,
    public val name: String,
    public val description: String,
    public val tier: Int,
    public val bossId: Int,
    public val type: Int,
    public val excludedGroupSize: Int,
)

@Singleton
public class CombatAchievementCatalogue @Inject public constructor() {
    public val tasks: List<CombatAchievementTask> by lazy {
        val idParam = "param.ca_task_id".asRSCM()
        val nameParam = "param.ca_task_name".asRSCM()
        val descriptionParam = "param.ca_task_desc".asRSCM()
        val tierParam = "param.ca_task_tier".asRSCM()
        val bossParam = "param.ca_task_boss_id".asRSCM()
        val typeParam = "param.ca_task_type".asRSCM()
        val excludedGroupParam = "param.ca_task_gim_excluded_size".asRSCM()
        val loaded = TIERS.flatMapIndexed { index, tier ->
            val table = checkNotNull(ServerCacheManager.getEnum("enum.ca_tasks_$tier".asRSCM()))
            table.values.values.map { structId ->
                val struct = checkNotNull(ServerCacheManager.getStruct(structId as Int))
                CombatAchievementTask(
                    id = struct.getInt(idParam),
                    structId = struct.id,
                    name = struct.getString(nameParam),
                    description = struct.getString(descriptionParam),
                    tier = struct.getInt(tierParam),
                    bossId = struct.getInt(bossParam),
                    type = struct.getInt(typeParam),
                    excludedGroupSize = struct.params?.get(excludedGroupParam) as? Int ?: 0,
                ).also { check(it.tier == index + 1) }
            }
        }.sortedBy { it.id }
        check(loaded.map { it.id } == (0..654).toList()) {
            "Combat Achievements requires the pinned revision-240 catalogue"
        }
        check((1..6).map { tier -> loaded.count { it.tier == tier } } == TIER_COUNTS)
        repeat(21) { word ->
            val varp = checkNotNull(ServerCacheManager.getVarp("varp.ca_task_completed_$word".asRSCM()))
            check(varp.scope == VarpLifetime.Perm) { "CA completion words must be permanent" }
        }
        loaded
    }

    public val thresholds: List<Int> by lazy {
        thresholds(0)
    }

    public fun thresholds(groupSize: Int): List<Int> {
        var total = 0
        return (1..6).map { tier ->
            total += tasks.count { it.tier == tier && isAvailable(it, groupSize) } * tier
            total
        }
    }

    public fun isAvailable(task: CombatAchievementTask, groupSize: Int): Boolean =
        groupSize !in 2..5 || groupSize > task.excludedGroupSize

    public fun task(id: Int): CombatAchievementTask = tasks[id].also { check(it.id == id) }

    public companion object {
        public val TIERS: List<String> = listOf("easy", "medium", "hard", "elite", "master", "grandmaster")
        private val TIER_COUNTS = listOf(41, 64, 89, 166, 173, 122)
    }
}
