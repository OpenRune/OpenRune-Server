package org.rsmod.content.bosses.gemstonecrab

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.isValidTarget
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class GemstoneCrabBoss
@Inject
constructor(deps: BossDeps, private val interactions: AiPlayerInteractions) :
    BossPluginScript(deps) {

    private var heldTarget: Player? = null
    private var heldSinceCycle = 0
    private var holdCycles = 0

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps, onCombatTick = { rotateTarget(it) })
    }

    override val spec =
        boss("npc.gemstone_crab") {
            stats(attackRate = ATTACK_RATE)

            val attack =
                ability("attack") {
                    anim("seq.crab_boss_attack")
                    hit {
                        target = FacingQuadrant(reach = REACH)
                        damage(Accuracy(npcMaxHit()))
                        type(Melee)
                    }
                }

            phase("active", lockMovement = true) {
                weightedSelectorRandom {
                    +random(attack, weight = 1, requires = WithinMeleeRange)
                }
            }
        }

    private fun StandardNpcAccess.rotateTarget(target: Player) {
        val now = deps.mapClock.cycle
        if (target !== heldTarget) {
            heldTarget = target
            heldSinceCycle = now
            holdCycles = deps.random.of(HOLD_ATTACKS) * ATTACK_RATE
        }

        val expired = now - heldSinceCycle >= holdCycles
        if (!expired && npc.isWithinDistance(target, REACH)) {
            return
        }

        val candidates =
            deps.playerList.filter {
                it !== target &&
                    it.isValidTarget() &&
                    it.coords.level == npc.coords.level &&
                    npc.isWithinDistance(it, REACH) &&
                    !npc.isCentreTile(it)
            }
        if (candidates.isEmpty()) {
            return
        }
        opPlayer2(candidates[deps.random.of(candidates.size)], interactions)
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
