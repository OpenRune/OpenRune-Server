package org.rsmod.content.bosses.madangel

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.Melee
import org.rsmod.api.bosses.dsl.Ranged
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.choose
import org.rsmod.api.bosses.dsl.external
import org.rsmod.api.bosses.dsl.oneOf
import org.rsmod.api.bosses.dsl.rotation
import org.rsmod.api.bosses.dsl.run
import org.rsmod.api.bosses.dsl.say
import org.rsmod.api.bosses.dsl.sequence
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.midiSong
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.map.Direction
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Mad Angel inside the Wyrmscraig cathedral, dormant until a player wakes it off its pew.
 *
 * The angel is spawned as [NPC_MAD_ANGEL] (so its kill count and hp bar params apply)
 * and kept dormant through a [NPC_INITIAL] transmog, whose stand animation is the kneel pose and
 * whose `Wake` option is gated on Fallen From Grace being complete. Waking plays the spawn animation
 * on the anim form, then switches to the fight type and gives it the cathedral to hunt in. The
 * permanent spawn keeps its respawn flag, so the repeatable fight respawns after a kill.
 */
class MadAngel
@Inject
constructor(
    deps: BossDeps,
    private val fight: MadAngelFight,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : BossPluginScript(deps) {

    override val spec =
        boss(NPC_MAD_ANGEL, NPC_INITIAL) {
            stats(attackRate = MadAngelFight.MELEE_SPEED)

            val melee =
                ability("melee") {
                    anim(ANIM_MELEE)
                    hit {
                        damage(DamageExpr.Custom { _, target -> rollMelee(target) })
                        type(Melee)
                        delay = MELEE_HIT_DELAY
                        penetration(PRAYER_PENETRATION)
                    }
                    attackDelay = MadAngelFight.MELEE_SPEED
                }

            val sweepLeft =
                sequence(
                    anim(fight.windup(SweepSide.Left, enraged = false)),
                    external(HANDLER_SWEEP, params = SweepParams(SweepSide.Left, enraged = false)),
                )
            val sweepRight =
                sequence(
                    anim(fight.windup(SweepSide.Right, enraged = false)),
                    external(HANDLER_SWEEP, params = SweepParams(SweepSide.Right, enraged = false)),
                )
            val sweepLeftEnraged =
                sequence(
                    anim(fight.windup(SweepSide.Left, enraged = true)),
                    external(HANDLER_SWEEP, params = SweepParams(SweepSide.Left, enraged = true)),
                )
            val sweepRightEnraged =
                sequence(
                    anim(fight.windup(SweepSide.Right, enraged = true)),
                    external(HANDLER_SWEEP, params = SweepParams(SweepSide.Right, enraged = true)),
                )

            val sweep = ability("sweep") { include(oneOf(sweepRight, sweepLeft)) }
            val sweepEnraged =
                ability("sweep_enraged") { include(oneOf(sweepRightEnraged, sweepLeftEnraged)) }
            val blast = ability("blast") { include(external(HANDLER_BLAST, params = false)) }
            val blastEnraged =
                ability("blast_enraged") { include(external(HANDLER_BLAST, params = true)) }
            val smite = ability("smite") { include(external(HANDLER_SMITE, params = false)) }
            val smiteEnraged =
                ability("smite_enraged") { include(external(HANDLER_SMITE, params = true)) }

            val specialBranches =
                mapOf(
                    sweep.name to run(sweep),
                    blast.name to run(blast),
                    smite.name to run(smite),
                )

            val special =
                ability("special") {
                    include(
                        choose(
                            selector =
                                rotation {
                                    randomStart = true
                                    +then(sweep)
                                    +then(blast)
                                    +then(smite)
                                },
                            branches = specialBranches,
                        )
                    )
                }

            val spendBuff = ability("spend_buff") { include(external(HANDLER_SPEND_BUFF)) }

            val enrage =
                ability("enrage") {
                    say(ENRAGE_TEXT)
                    nextAttackIn(ENRAGE_SAY_TICKS)
                    transitionTo(ENRAGED_PHASE)
                }

            phase(COMBAT_PHASE) {
                weightedSelectorRandom { +random(melee, weight = 1) }
                forceEveryAttacks(SPECIAL_AFTER_ATTACKS, SPECIAL_AFTER_ATTACKS, special)
                forceWhen(
                    Condition.HpBelow(ENRAGE_HP / MAD_ANGEL_HP, inclusive = true),
                    enrage,
                    once = true,
                )
            }

            phase(ENRAGED_PHASE) {
                rotationSelector {
                    +then(smiteEnraged)
                    +then(sweepEnraged)
                    +then(blastEnraged)
                }
            }

            incoming {
                rule(buffRule(MadAngelFight.NEXT_HIT_MAX)) {
                    floorPercentOfMaxHit(100, Melee)
                    floorPercentOfMaxHit(100, Ranged)
                    run(spendBuff)
                }
                rule(buffRule(MadAngelFight.NEXT_HIT_ACCURATE)) {
                    floorPercentOfMaxHit(50, Melee)
                    floorPercentOfMaxHit(50, Ranged)
                    run(spendBuff)
                }
            }
        }

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)

        onOpNpc1(NPC_INITIAL) { wake(it.npc, player) }

        onEvent<NpcStateEvents.Create> { if (npc.type.id == MAD_ANGEL_ID) dormant(npc) }

        deps.extensionRegistry.register(HANDLER_SWEEP) { _, npc, target, params ->
            fight.sweep(npc, target, params as SweepParams)
        }
        deps.extensionRegistry.register(HANDLER_BLAST) { _, npc, target, params ->
            fight.blast(npc, target, params as Boolean)
        }
        deps.extensionRegistry.register(HANDLER_SMITE) { _, npc, target, params ->
            fight.smite(npc, target, params as Boolean)
        }
        deps.extensionRegistry.register(HANDLER_SPEND_BUFF) { _, _, target, _ ->
            target.nextHitBuff = MadAngelFight.NEXT_HIT_NONE
        }
    }

    @OptIn(InternalApi::class)
    private fun wake(npc: Npc, player: Player) {
        if (npc.visType.id != INITIAL_ID) return
        val type = ServerCacheManager.getNpc(MAD_ANGEL_ID) ?: return
        val animType = ServerCacheManager.getNpc(ANIM_ID) ?: type
        npc.transmog(animType, Int.MAX_VALUE)
        npc.assignUid()
        npc.hitpoints = npc.baseHitpointsLvl
        npc.anim(ANIM_SPAWN)
        npc.spotanim(SPOT_SPAWN)
        player.midiSong(FIGHT_MUSIC)
        npc.setHunt(type.huntRange)
        type.huntMode?.let { mode -> ServerCacheManager.getHunt(mode)?.let(npc::setHuntMode) }
        deps.worldQueues.add(WAKE_TICKS) {
            if (!npc.isValidTarget() || !player.isValidTarget()) return@add
            npc.transmog(type, Int.MAX_VALUE)
            npc.assignUid()
            npc.hitpoints = npc.baseHitpointsLvl
            npc.resetAnim()
            npc.apPlayer2(player, aiPlayerInteractions)
        }
    }

    @OptIn(InternalApi::class)
    private fun dormant(npc: Npc) {
        val type = ServerCacheManager.getNpc(INITIAL_ID) ?: return
        npc.mode = NpcMode.None
        npc.setHunt(0)
        npc.transmog(type, Int.MAX_VALUE)
        npc.assignUid()
        npc.hitpoints = npc.baseHitpointsLvl
        npc.movementLocked = false
        npc.clearFacingLock()
        npc.resetAnim()
        npc.respawnDir = Direction.East
        npc.infoProtocol.setFaceAngle(Direction.East.angle, instant = true)
    }

    private fun rollMelee(target: Player): Int {
        val roll = deps.random.of(MELEE_MAX_HIT + 1)
        if (target.vars[PRAYER_MELEE] != 1) return roll
        return roll * MELEE_PROTECTED_MAX_HIT / MELEE_MAX_HIT
    }

    private fun buffRule(buff: Int): Condition =
        Condition.And(
            Condition.Custom { _, target -> target?.nextHitBuff == buff },
            Condition.Or(Condition.HitStyle(Melee), Condition.HitStyle(Ranged)),
        )

    private companion object {
        private val NPC_MAD_ANGEL = "npc.mad_angel"
        private val NPC_INITIAL = "npc.mad_angel_initial"
        private val NPC_ANIM = "npc.mad_angel_anim"

        private val MAD_ANGEL_ID = NPC_MAD_ANGEL.asRSCM(RSCMType.NPC)
        private val INITIAL_ID = NPC_INITIAL.asRSCM(RSCMType.NPC)
        private val ANIM_ID = NPC_ANIM.asRSCM(RSCMType.NPC)

        private const val HANDLER_SWEEP = "madangel.sweep"
        private const val HANDLER_BLAST = "madangel.blast"
        private const val HANDLER_SMITE = "madangel.smite"
        private const val HANDLER_SPEND_BUFF = "madangel.spend_buff"

        private const val ANIM_MELEE = "seq.npc_mad_angel_attack_melee"
        private const val ANIM_SPAWN = "seq.npc_mad_angel_spawn"
        private const val SPOT_SPAWN = "spotanim.vfx_mad_angel_spawn"
        private const val FIGHT_MUSIC = 885

        private const val ENRAGE_HP = 350.0
        private val MAD_ANGEL_HP: Double =
            ServerCacheManager.getNpc(MAD_ANGEL_ID)?.hitpoints?.toDouble() ?: ENRAGE_HP
        private const val ENRAGE_TEXT = "...!"
        private const val ENRAGE_SAY_TICKS = 4
        private const val SPECIAL_AFTER_ATTACKS = 3

        private const val COMBAT_PHASE = "combat"
        private const val ENRAGED_PHASE = "enraged"

        private const val MELEE_MAX_HIT = 31
        private const val MELEE_PROTECTED_MAX_HIT = 5

        // The engine decrements a player-targeted hit queue on the tick it is created, so a hit
        // lands `delay - 1` ticks after the animation: 3 gives the two ticks the capture shows.
        private const val MELEE_HIT_DELAY = 3
        private const val PRAYER_PENETRATION = 100
        private const val PRAYER_MELEE = "varbit.prayer_protectfrommelee"

        private const val WAKE_TICKS = 3
    }
}
