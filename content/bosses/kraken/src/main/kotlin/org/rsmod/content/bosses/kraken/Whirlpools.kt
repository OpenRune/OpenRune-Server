package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import kotlin.math.max
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.startEncounter
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal class Whirlpools(
    private val deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    fun touchCombat(npc: Npc) {
        npc.vars[LAST_COMBAT] = deps.mapClock.cycle
    }

    fun isSubmerged(npc: Npc): Boolean = npc.visType.id == npc.type.id

    fun setImmunity(npc: Npc, melee: Boolean, other: Boolean) {
        val otherFlag = if (other) 1 else 0
        npc.vars[IMMUNE_MELEE] = if (melee) 1 else 0
        npc.vars[IMMUNE_RANGED] = otherFlag
        npc.vars[IMMUNE_MAGIC] = otherFlag
    }

    @OptIn(InternalApi::class)
    fun wake(
        pool: Npc,
        target: Player,
        into: String,
        ariseAnim: String,
        whirlpoolSpec: BossSpec,
        onChange: (Npc) -> Unit,
    ) {
        val surfaced = ServerCacheManager.getNpc(into.asRSCM(RSCMType.NPC)) ?: return
        val damageTaken = pool.type.hitpoints - pool.hitpoints
        pool.transmog(surfaced, Int.MAX_VALUE)
        pool.copyStats(surfaced)
        pool.hitpoints = max(surfaced.hitpoints - damageTaken, 1)
        onChange(pool)
        pool.anim(ariseAnim, 0, 1)
        pool.apRangeOverride = ATTACK_RANGE
        pool.apRequiresLineOfSight = false
        pool.movementLocked = true
        pool.mode = NpcMode.ApPlayer2
        pool.apPlayer2(target, aiPlayerInteractions)
        deps.suppressAttacks(pool, WAKE_ATTACK_DELAY)
        touchCombat(pool)
        watchIdle(pool, whirlpoolSpec, onChange)
    }

    private fun watchIdle(npc: Npc, whirlpoolSpec: BossSpec, onChange: (Npc) -> Unit) {
        deps.worldQueues.add(IDLE_CHECK_INTERVAL) {
            if (!npc.isSlotAssigned || isSubmerged(npc)) return@add
            if (deps.mapClock.cycle - npc.vars[LAST_COMBAT] >= IDLE_RESET_TICKS) {
                submerge(npc, whirlpoolSpec, onChange)
            } else {
                watchIdle(npc, whirlpoolSpec, onChange)
            }
        }
    }

    @OptIn(InternalApi::class)
    private fun submerge(npc: Npc, whirlpoolSpec: BossSpec, onChange: (Npc) -> Unit) {
        deps.startEncounter(npc, whirlpoolSpec)
        npc.movementLocked = false
        npc.apRangeOverride = null
        npc.apRequiresLineOfSight = true
        npc.setRespawnValues()
        onChange(npc)
    }

    companion object {
        const val LAST_COMBAT = "varn.lastcombat"
        const val ATTACK_RANGE = 10
        const val SPLASH = "spotanim.failedspell_impact"
        const val IMPACT_HEIGHT = 124
        const val ATTACK_ANIM = "seq.swan_queen_spellcast"
        const val ARISE_ANIM = "seq.slayer_kraken_arise"
        const val RANGED_DIVISOR = 7
        const val IMMUNE_MESSAGE = "You can't attack it properly like that."

        private const val IMMUNE_MELEE = "varn.immune_melee"
        private const val IMMUNE_RANGED = "varn.immune_ranged"
        private const val IMMUNE_MAGIC = "varn.immune_magic"
        private const val IDLE_RESET_TICKS = 50
        private const val IDLE_CHECK_INTERVAL = 5
        private const val WAKE_ATTACK_DELAY = 3
    }
}
