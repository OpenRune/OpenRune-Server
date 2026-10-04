package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

internal enum class KrakenKind(val pool: String, val active: String, val maximum: Int, val rate: Int) {
    CAVE("npc.slayer_kraken_sub", "npc.slayer_kraken", 13, 6),
    BOSS("npc.slayer_kraken_boss_whirlpool", "npc.slayer_kraken_boss", 28, 4),
    TENTACLE("npc.slayer_kraken_boss_tentacle_whirlpool", "npc.slayer_kraken_boss_tentacle", 2, 4);

    val poolId get() = pool.asRSCM()
    val activeId get() = active.asRSCM()
    val spawn get() = if (this == TENTACLE) "seq.tentacle_monster_spawn" else "seq.slayer_kraken_arise"
    val attack get() = if (this == TENTACLE) "seq.tentacle_monster_attack" else "seq.swan_queen_attack"
    val death get() = if (this == TENTACLE) "seq.tentacle_monster_death" else "seq.swan_queen_death"
    val spawnTicks get() = checkNotNull(ServerCacheManager.getAnim(spawn.asRSCM())).tickDuration.coerceAtLeast(1)
}

internal object KrakenRules {
    private val tasks by lazy {
        SlayerTaskRow.all().filter { it.nameLowercase in setOf("cave kraken", "cave krakens", "kraken") }
            .map { it.id }.toSet()
    }
    fun allowed(player: Player): Boolean = player.statBase("stat.slayer") >= 87 && player.stat("stat.slayer") >= 87 &&
        player.vars["varp.slayer_count"] > 0 && player.vars["varp.slayer_target"] in tasks

    fun damage(kind: KrakenKind, type: HitType, damage: Int): Int = when {
        type == HitType.Melee -> 0
        type == HitType.Ranged -> damage / 7
        else -> damage
    }
}
