package org.rsmod.content.bosses.gemstonecrab

import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class GemstoneCrabBoss
@Inject
constructor(
    deps: BossDeps,
    private val interactions: AiPlayerInteractions,
    private val crab: GemstoneCrabManager,
) : BossPluginScript(deps) {

    private var heldTarget: Player? = null
    private var heldSinceCycle = 0
    private var holdCycles = 0

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onCombatTick = { rotateTarget(it) })
        onEvent<GameLifecycle.LateCycle> { acquireTarget() }
    }

    override val spec =
        boss("npc.gemstone_crab") {
            stats(attackRate = ATTACK_RATE)

            val attack =
                ability("attack") {
                    anim("seq.crab_boss_attack")
                    hit {
                        target = FacingQuadrant(reach = REACH)
                        damage(
                            Accuracy(
                                npcMaxHit(MeleeAttackType.Crush),
                                meleeAttackType = MeleeAttackType.Crush,
                            )
                        )
                        type(Melee)
                    }
                }

            phase("active", lockMovement = true) {
                weightedSelectorRandom {
                    +random(attack, weight = 1, requires = WithinMeleeRange)
                }
            }
        }

    @OptIn(InternalApi::class)
    private fun acquireTarget() {
        if (!crab.isCrabActive) {
            return
        }
        val npc = crab.liveNpc ?: return
        if (npc.hasInteraction()) {
            return
        }
        val candidates = npc.attackableNearby()
        if (candidates.isEmpty()) {
            return
        }
        npc.opPlayer2(candidates[deps.random.of(candidates.size)], interactions)
    }

    private fun StandardNpcAccess.rotateTarget(target: Player) {
        val now = deps.mapClock.cycle
        if (target !== heldTarget) {
            heldTarget = target
            heldSinceCycle = now
            holdCycles = deps.random.of(HOLD_ATTACKS) * ATTACK_RATE
        }

        val expired = now - heldSinceCycle >= holdCycles
        val safespotted = npc.isCentreTile(target)
        if (!expired && !safespotted && npc.isWithinDistance(target, REACH)) {
            return
        }

        val candidates = npc.attackableNearby().filter { it !== target }
        if (candidates.isEmpty()) {
            return
        }
        opPlayer2(candidates[deps.random.of(candidates.size)], interactions)
    }

    private fun Npc.attackableNearby(): List<Player> =
        deps.playerList.filter {
            it.isValidTarget() &&
                it.coords.level == coords.level &&
                isWithinDistance(it, REACH) &&
                !isCentreTile(it)
        }

    private fun Npc.isCentreTile(player: Player): Boolean {
        val half = size / 2
        return player.coords.x == coords.x + half && player.coords.z == coords.z + half
    }

    private companion object {
        private const val ATTACK_RATE = 7
        private const val REACH = 1
        private val HOLD_ATTACKS = 3..6
    }
}
