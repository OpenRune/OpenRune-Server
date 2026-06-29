package org.rsmod.content.bosses.scurrius

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class Scurrius
@Inject
constructor(
    deps: BossDeps,
    private val routeFactory: RouteFactory,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : BossPluginScript(deps) {

    private fun resolveTarget(npc: Npc, preferred: Player): Player? =
        preferred.takeIf(Player::isValidTarget)
            ?: deps.playerList
                .filter {
                    it.isValidTarget() && it.coords.chebyshevDistance(npc.coords) <= ARENA_RADIUS
                }
                .minByOrNull { it.coords.chebyshevDistance(npc.coords) }

    private fun engageRanged(npc: Npc, preferred: Player) {
        val target = resolveTarget(npc, preferred) ?: return
        npc.apRangeOverride = ENGAGE_AP_RANGE
        npc.apRequiresLineOfSight = false
        npc.apPlayer2(target, aiPlayerInteractions)
    }

    private fun engageMelee(npc: Npc, preferred: Player) {
        val target = resolveTarget(npc, preferred) ?: return
        npc.apRangeOverride = null
        npc.apRequiresLineOfSight = true
        npc.opPlayer2(target, aiPlayerInteractions)
    }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)

        deps.extensionRegistry.register("scurrius.eat_cheese") { _, npc, target, _ ->
            val cheeseTile = CoordGrid(npc.coords.level, npc.coords.mx, npc.coords.mz, 34, 20)
            npc.resetFaceEntity()
            // Ignore combat interaction so walkTo can't be cancelled
            npc.ignoreCombatInteractions = true
            npc.walkTo(routeFactory, cheeseTile) {
                npc.ignoreCombatInteractions = false
                deps.encounter(npc).transitionTo("feeding", deps.mapClock.cycle)
                engageRanged(npc, target)
                // Lock facing on cheese pile
                npc.lockFacing(CoordGrid(npc.coords.level, npc.coords.mx, npc.coords.mz, 34, 20))
            }
        }

        deps.extensionRegistry.register("scurrius.enrage_walk") { _, npc, target, _ ->
            // Force walk to center of arena
            val centreTile = CoordGrid(npc.coords.level, npc.coords.mx, npc.coords.mz, 33, 10)
            npc.resetFaceEntity()
            npc.movementLocked = false
            npc.ignoreCombatInteractions = true
            npc.walkTo(routeFactory, centreTile) {
                npc.ignoreCombatInteractions = false
                deps.encounter(npc).transitionTo("enraged", deps.mapClock.cycle)
                engageRanged(npc, target)
            }
        }

        deps.extensionRegistry.register("scurrius.resume_combat") { _, npc, target, _ ->
            engageMelee(npc, target)
        }
    }

    override val spec =
        boss("npc.rat_boss_instance") {
            stats(attackRate = 4, aggressionRadius = 8)

            val eatCheese = ability("eat_cheese") { include(external("scurrius.eat_cheese")) }

            val enrageWalk = ability("enrage_walk") { include(external("scurrius.enrage_walk")) }

            val resumeCombat =
                ability("resume_combat") { include(external("scurrius.resume_combat")) }

            val melee =
                ability("melee") {
                    anim("seq.npc_rat_boss_attack_melee_01")
                    hit {
                        damage(0..13).roll()
                        type(Melee)
                    }
                }

            val rockfall =
                ability("rockfall") {
                    anim("seq.npc_rat_boss_attack_stomp_01")
                    debris(
                        telegraph = DEBRIS_SPOTANIM,
                        damage = Roll(15..22),
                        type = Typeless,
                        windup = WINDUP_TICKS,
                        targetRadius = ARENA_RADIUS,
                        scatterRadius = SCATTER_RADIUS,
                        count = ROCK_MIN..ROCK_MAX,
                    )
                }

            val feedingMagic =
                ability("feeding_magic") {
                    anim("seq.npc_rat_boss_feeding_attack_magic_01")
                    projectile(
                        spotanim = "spotanim.vfx_rat_boss_proj_magic_01",
                        hit = Effect.Hit(damage = Roll(0..8), type = Magic),
                    )
                }

            val feedingRanged =
                ability("feeding_ranged") {
                    anim("seq.npc_rat_boss_feeding_attack_ranged_01")
                    projectile(
                        spotanim = "spotanim.vfx_rat_boss_proj_ranged_01",
                        hit = Effect.Hit(damage = Roll(0..7), type = Ranged),
                    )
                }

            val feedingRockfall =
                ability("feeding_rockfall") {
                    anim("seq.npc_rat_boss_feeding_attack_stomp_01")
                    debris(
                        telegraph = DEBRIS_SPOTANIM,
                        damage = Roll(15..22),
                        type = Typeless,
                        windup = WINDUP_TICKS,
                        targetRadius = ARENA_RADIUS,
                        scatterRadius = SCATTER_RADIUS,
                        count = ROCK_MIN..ROCK_MAX,
                    )
                }

            val feedingSummonRats =
                ability("feeding_summon_rats") {
                    anim("seq.npc_rat_boss_feeding_attack_summon_01")
                    summon(GIANT_RAT, count = 6, radius = 5)
                }

            val magic =
                ability("magic") {
                    anim("seq.npc_rat_boss_attack_magic_01")
                    projectile(
                        spotanim = "spotanim.vfx_rat_boss_proj_magic_01",
                        hit = Effect.Hit(damage = Roll(0..8), type = Magic),
                    )
                }

            val ranged =
                ability("ranged") {
                    anim("seq.npc_rat_boss_attack_ranged_01")
                    projectile(
                        spotanim = "spotanim.vfx_rat_boss_proj_ranged_01",
                        hit = Effect.Hit(damage = Roll(0..7), type = Ranged),
                    )
                }

            val summonRats =
                ability("summon_rats") {
                    anim("seq.npc_rat_boss_attack_summon_01")
                    summon(GIANT_RAT, count = 6, radius = 5)
                }

            phase("combat") {
                entry = "resume_combat"
                weightedSelectorRandom {
                    +random(melee, weight = 7, requires = WithinMeleeRange)
                    +random(rockfall, weight = 3)
                }
            }

            phase("eating_transition", entryHp = 0.80) {
                entry = "eat_cheese"
            }

            phase(
                "feeding",
                lockMovement = true,
                idleAnim = EATING_IDLE_SEQ,
                exitAfter = FEEDING_DURATION,
                nextPhase = "combat",
            ) {
                weightedSelectorRandom {
                    +random(feedingMagic, weight = 4)
                    +random(feedingRanged, weight = 4)
                    +random(feedingRockfall, weight = 2)
                    +random(feedingSummonRats, weight = 1, cooldown = RAT_SUMMON_COOLDOWN)
                }
            }

            phase("enrage_transition", entryHp = 0.30) {
                entry = "enrage_walk"
            }

            phase("enraged", lockMovement = true) {
                weightedSelectorRandom {
                    +random(magic, weight = 5)
                    +random(ranged, weight = 5)
                    +random(rockfall, weight = 5)
                    +random(summonRats, weight = 1, cooldown = RAT_SUMMON_COOLDOWN)
                }
            }
        }

    private companion object {
        private const val GIANT_RAT = "npc.rat_boss_giant_rat"
        private const val DEBRIS_SPOTANIM = "spotanim.vfx_rat_boss_falling_debris_01"
        private const val RAT_SUMMON_COOLDOWN = 50
        private const val FEEDING_DURATION = 30

        private const val ENGAGE_AP_RANGE = 15
        private const val ARENA_RADIUS = 25
        private const val SCATTER_RADIUS = 7
        private const val WINDUP_TICKS = 3
        private const val ROCK_MIN = 15
        private const val ROCK_MAX = 20
        private const val EATING_IDLE_SEQ = "seq.npc_rat_boss_feeding_idle_01"
    }
}
