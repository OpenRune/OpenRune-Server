package org.rsmod.content.bosses.dagannothkings

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@OptIn(InternalApi::class)
class SpinolypWater
@Inject
constructor(
    private val hunt: Hunt,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (name in ALL_TYPES) {
            onAiTimer(name) { tick(npc) }
        }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.type.id in ALL_IDS) {
                npc.vars[TICKS] = 0
            }
        }
    }

    private fun tick(npc: Npc) {
        if (npc.type.id !in ALL_IDS) {
            return
        }

        val visible = npc.visType.id in VISIBLE_IDS
        val water = npc.visType.id in WATER_IDS
        if (!visible && !water) {
            return
        }

        val nearby = nearestPlayer(npc)
        if (visible && nearby == null) {
            val waterType = counterpart(npc.visType.id) ?: return
            transform(npc, waterType)
            npc.vars[TICKS] = 0
            return
        }

        if (water) {
            if (nearby == null) {
                npc.vars[TICKS] = 0
                return
            }
            rapidMelee(npc)
        }

        val remaining = npc.vars[TICKS]
        if (remaining <= 0) {
            npc.vars[TICKS] = nextDelay(npc, visible, nearby)
            return
        }

        val next = remaining - 1
        npc.vars[TICKS] = next
        if (next > 0) {
            return
        }

        val counterpart = counterpart(npc.visType.id) ?: return
        transform(npc, counterpart)
        npc.vars[TICKS] = 0
    }

    private fun nearestPlayer(npc: Npc): Player? =
        hunt.findPlayers(npc.coords, ACTIVATION_RANGE, HuntVis.Off)
            .firstOrNull { it.coords.level == npc.coords.level }

    private fun rapidMelee(npc: Npc) {
        val target =
            hunt.findPlayers(npc.coords, MELEE_RANGE, HuntVis.Off)
                .firstOrNull { it.coords.level == npc.coords.level }
                ?: return
        if (random.of(0, 5) != 0) {
            return
        }
        target.queueHit(
            source = npc,
            delay = 1,
            type = HitType.Melee,
            damage = random.of(0, 1),
            modifier = NoopPlayerHitModifier,
        )
    }

    private fun nextDelay(npc: Npc, visible: Boolean, nearby: Player?): Int {
        if (visible) {
            return random.of(35, 70)
        }
        val close = nearby != null && npc.coords.chebyshevDistance(nearby.coords) <= MELEE_TRIGGER_RANGE
        return if (close) random.of(5, 20) else random.of(15, 35)
    }

    private fun transform(npc: Npc, target: NpcServerType) {
        val hitpoints = npc.hitpoints
        npc.transmog(target, Int.MAX_VALUE)
        npc.copyCurrentStats(target)
        npc.hitpoints = hitpoints.coerceIn(1, npc.baseHitpointsLvl)
        npc.assignUid()
        npc.clearInteraction()
        npc.resetFaceEntity()
        npc.mode = NpcMode.Wander

        if (target.id in WATER_IDS) {
            npc.setHunt(0)
            return
        }

        val huntMode = target.huntMode ?: return
        npc.setHuntMode(checkNotNull(ServerCacheManager.getHunt(huntMode)))
        npc.setHunt(target.huntRange)
    }

    private fun counterpart(id: Int): NpcServerType? {
        for ((visible, water) in PAIRS) {
            val targetId =
                when (id) {
                    visible -> water
                    water -> visible
                    else -> continue
                }
            val name = NAMES_BY_ID[targetId] ?: continue
            return ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))
        }
        return null
    }

    private companion object {
        const val TICKS = "varn.spinolyp_transform"
        const val ACTIVATION_RANGE = 15
        const val MELEE_RANGE = 1
        const val MELEE_TRIGGER_RANGE = 7

        const val VISIBLE_1X1 = "npc.dagannoth_water_creature_1x1"
        const val VISIBLE_1X12 = "npc.dagannoth_water_creature_1x12"
        const val VISIBLE_1X13 = "npc.dagannoth_water_creature_1x13"
        const val WATER_1X1 = "npc.dagannoth_water_creature_1x1_invisible"
        const val WATER_1X12 = "npc.dagannoth_water_creature_1x1_invisible2"
        const val WATER_1X13 = "npc.dagannoth_water_creature_1x1_invisible3"

        fun String.asNpcId(): Int = asRSCM(RSCMType.NPC)

        val PAIRS =
            listOf(
                VISIBLE_1X1.asNpcId() to WATER_1X1.asNpcId(),
                VISIBLE_1X12.asNpcId() to WATER_1X12.asNpcId(),
                VISIBLE_1X13.asNpcId() to WATER_1X13.asNpcId(),
            )

        val ALL_TYPES =
            listOf(
                VISIBLE_1X1,
                VISIBLE_1X12,
                VISIBLE_1X13,
                WATER_1X1,
                WATER_1X12,
                WATER_1X13,
            )

        val NAMES_BY_ID = ALL_TYPES.associateBy { it.asNpcId() }

        val VISIBLE_IDS =
            setOf(VISIBLE_1X1.asNpcId(), VISIBLE_1X12.asNpcId(), VISIBLE_1X13.asNpcId())

        val WATER_IDS = setOf(WATER_1X1.asNpcId(), WATER_1X12.asNpcId(), WATER_1X13.asNpcId())

        val ALL_IDS: Set<Int> = VISIBLE_IDS + WATER_IDS
    }
}
