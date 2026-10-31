package org.rsmod.content.bosses.madangel

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import kotlin.math.abs
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
import org.rsmod.api.bosses.dsl.spotanim
import org.rsmod.api.bosses.dsl.wait
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.vars.typePlayerUidVarn
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.vars.typeNpcUidVarp
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

private var Player.madAngelAggressiveNpc: NpcUid? by typeNpcUidVarp("varp.aggressive_npc")
private var Player.madAngelLastCombat: Int by intVarp("varp.lastcombat")
private var Npc.madAngelAggressivePlayer: PlayerUid? by typePlayerUidVarn("varn.aggressive_player")

/**
 * The Mad Angel inside the Wyrmscraig cathedral, dormant until a player wakes it off its pew.
 *
 * The angel is spawned as [NPC_MAD_ANGEL] (so its kill count and hp bar params apply)
 * and kept dormant through a [NPC_INITIAL] transmog, whose stand animation is the kneel pose and
 * whose `Wake` option is gated on Fallen From Grace being complete. Waking plays the spawn animation
 * on the anim form, then switches to the fight type and gives it the cathedral to hunt in. A kill
 * leaves the dead form behind for the respawn wait, then revives the angel in place with `Wake`
 * available again.
 */
class MadAngel
@Inject
constructor(
    deps: BossDeps,
    private val fight: MadAngelFight,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val npcDeath: NpcDeath,
    private val instances: InstanceManager,
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
            val blastEffect =
                sequence(
                    anim(ANIM_BOMB_THROW),
                    spotanim(SPOT_BOMB_THROW, slot = MadAngelFight.SPECIAL_SLOT),
                    external(HANDLER_BLAST, params = false),
                )
            val blast = ability("blast") { include(blastEffect) }
            val blastEnragedEffect =
                sequence(
                    anim(ANIM_BOMB_THROW),
                    spotanim(SPOT_BOMB_THROW, slot = MadAngelFight.SPECIAL_SLOT),
                    external(HANDLER_BLAST, params = true),
                )
            val blastEnraged = ability("blast_enraged") { include(blastEnragedEffect) }
            val smiteEffect =
                sequence(
                    anim(ANIM_SMITE),
                    spotanim(SPOT_SMITE, slot = MadAngelFight.SPECIAL_SLOT),
                    external(HANDLER_SMITE, params = false),
                )
            val smite = ability("smite") { include(smiteEffect) }
            val smiteEnragedEffect =
                sequence(
                    anim(ANIM_SMITE_ENRAGED),
                    spotanim(SPOT_SMITE_ENRAGED, slot = MadAngelFight.SPECIAL_SLOT),
                    external(HANDLER_SMITE, params = true),
                )
            val smiteEnraged = ability("smite_enraged") { include(smiteEnragedEffect) }

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

            val enragedSpecialBranches =
                mapOf(
                    sweepEnraged.name to run(sweepEnraged),
                    blastEnraged.name to run(blastEnraged),
                    smiteEnraged.name to run(smiteEnraged),
                )

            val specialEnraged =
                ability("special_enraged") {
                    include(
                        choose(
                            selector =
                                rotation {
                                    randomStart = true
                                    +then(sweepEnraged)
                                    +then(blastEnraged)
                                    +then(smiteEnraged)
                                },
                            branches = enragedSpecialBranches,
                        )
                    )
                }

            val enrageOpeningEffect =
                sequence(
                    run(smiteEnraged),
                    wait(fight.smiteAttackDelay(enraged = true)),
                    run(sweepEnraged),
                    wait(fight.sweepAttackDelay(enraged = true)),
                    run(blastEnraged),
                )
            val enrageOpening = ability("enrage_opening") { include(enrageOpeningEffect) }

            val spendBuff = ability("spend_buff") { include(external(HANDLER_SPEND_BUFF)) }

            val enrage =
                ability("enrage") {
                    say(ENRAGE_TEXT)
                    nextAttackIn(ENRAGE_SAY_TICKS)
                    transitionTo(ENRAGED_PHASE)
                    wait(ENRAGE_SAY_TICKS)
                    run(enrageOpening)
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
                weightedSelectorRandom { +random(melee, weight = 1) }
                forceEveryAttacks(SPECIAL_AFTER_ATTACKS, SPECIAL_AFTER_ATTACKS, specialEnraged)
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
        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                if (hit.isFromPlayer) {
                    val source = hit.sourceUid?.let { PlayerUid(it).resolve(deps.playerList) }
                    if (source != null && source.nextHitBuff != MadAngelFight.NEXT_HIT_NONE) {
                        source.nextHitBuff = MadAngelFight.NEXT_HIT_NONE
                        npc.vars[MadAngelFight.GUARANTEED_HIT] = 0
                    }
                }
            },
        )

        onOpNpc1(NPC_INITIAL) { wake(it.npc, player) }

        onEvent<NpcStateEvents.Create> { if (npc.type.id == MAD_ANGEL_ID) dormant(npc) }

        for (id in listOf(MAD_ANGEL_ID, ANIM_ID)) {
            ServerCacheManager.getNpc(id)?.let { type ->
                onNpcQueue(type, "queue.death") { madAngelDeath() }
            }
        }

        deps.extensionRegistry.register(HANDLER_SWEEP) { _, npc, target, params ->
            fight.sweep(npc, target, params as SweepParams)
        }
        deps.extensionRegistry.register(HANDLER_BLAST) { _, npc, target, params ->
            fight.blast(npc, target, params as Boolean)
        }
        deps.extensionRegistry.register(HANDLER_SMITE) { _, npc, target, params ->
            fight.smite(npc, target, params as Boolean)
        }
        deps.extensionRegistry.register(HANDLER_SPEND_BUFF) { _, npc, target, _ ->
            npc.vars[MadAngelFight.GUARANTEED_HIT] = 0
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

    private suspend fun StandardNpcAccess.madAngelDeath() {
        val respawnRate = ServerCacheManager.getNpc(MAD_ANGEL_ID)?.respawnRate ?: npc.type.respawnRate
        val deathCoords = npc.coords
        val deathAngle = npc.deathFacingAngle()
        walk(coords)
        noneMode()
        hideAllOps()
        arriveDelay()

        val deathAnim = ServerCacheManager.getAnim(DEATH_SEQ_ID) ?: return
        anim(DEATH_ANIM)
        delay(deathAnim)

        npcDeath.spawnDrops(this)
        for (player in deps.playerList) {
            if (player.madAngelAggressiveNpc?.packed == npc.uid.packed) {
                player.madAngelAggressiveNpc = null
                player.madAngelLastCombat = 0
            }
        }

        val deadType = ServerCacheManager.getNpc(DEAD_ID) ?: return
        changeType(deadType, Int.MAX_VALUE)
        delay(respawnRate.coerceAtLeast(1))
        deps.worldQueues.add(1) { respawnDormant(npc, deathCoords, deathAngle) }
    }

    private fun Npc.deathFacingAngle(): Int {
        if (isFacingLocked) {
            calculateAngle(faceLockSquare, faceLockWidth, faceLockLength)?.let { return it }
        }
        val target = facingTarget(deps.playerList) ?: madAngelAggressivePlayer?.resolve(deps.playerList)
        if (target != null) {
            calculateAngle(target.coords, target.size, target.size)?.let { return it }
        }
        return respawnDir.angle
    }

    private fun respawnDormant(npc: Npc, coords: CoordGrid, angle: Int) {
        if (!npc.isSlotAssigned || npc.visType.id != DEAD_ID) return
        val instanceId = instances.instanceForNpc(npc) ?: return
        if (instances.sessionForId(instanceId) == null) return
        val type = ServerCacheManager.getNpc(MAD_ANGEL_ID) ?: return
        val revived = Npc(type, coords)
        revived.respawnDir = directionOf(angle)
        deps.npcRepo.del(npc, Int.MAX_VALUE)
        deps.npcRepo.add(revived, Int.MAX_VALUE)
        instances.attachNpc(instanceId, revived)
        revived.infoProtocol.setFaceAngle(angle, instant = true)
    }

    private fun directionOf(angle: Int): Direction =
        Direction.entries.minBy { direction ->
            val diff = ((direction.angle - angle + 1024) and 2047) - 1024
            abs(diff)
        }

    @OptIn(InternalApi::class)
    private fun dormant(npc: Npc) {
        val type = ServerCacheManager.getNpc(INITIAL_ID) ?: return
        val fightType = ServerCacheManager.getNpc(MAD_ANGEL_ID) ?: return
        deps.encounterRegistry.remove(npc)
        npc.mode = NpcMode.None
        npc.setHunt(0)
        npc.apRangeOverride = null
        npc.apRequiresLineOfSight = true
        npc.moveRestrict = type.moveRestrict
        npc.ignoreCombatInteractions = false
        npc.clearIdleAnim()
        npc.clearInteraction()
        npc.clearHeroPoints()
        npc.queueList.clear()
        npc.vars["varn.aggressive_player"] = null
        npc.vars[MadAngelFight.GUARANTEED_HIT] = 0
        npc.copyStats(fightType)
        npc.transmog(type, Int.MAX_VALUE)
        npc.assignUid()
        npc.hitpoints = npc.baseHitpointsLvl
        npc.movementLocked = false
        npc.clearFacingLock()
        npc.resetAnim()
        npc.respawnDir = Direction.East
        npc.showAllOps()
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
        private val NPC_DEAD = "npc.mad_angel_dead"

        private const val DEATH_ANIM = "seq.npc_mad_angel_attack_death"

        private val MAD_ANGEL_ID = NPC_MAD_ANGEL.asRSCM(RSCMType.NPC)
        private val INITIAL_ID = NPC_INITIAL.asRSCM(RSCMType.NPC)
        private val ANIM_ID = NPC_ANIM.asRSCM(RSCMType.NPC)
        private val DEAD_ID = NPC_DEAD.asRSCM(RSCMType.NPC)
        private val DEATH_SEQ_ID = DEATH_ANIM.asRSCM(RSCMType.SEQ)

        private const val HANDLER_SWEEP = "madangel.sweep"
        private const val HANDLER_BLAST = "madangel.blast"
        private const val HANDLER_SMITE = "madangel.smite"
        private const val HANDLER_SPEND_BUFF = "madangel.spend_buff"

        private const val ANIM_MELEE = "seq.npc_mad_angel_attack_melee"
        private const val ANIM_BOMB_THROW = "seq.npc_mad_angel_attack_bomb_throw"
        private const val SPOT_BOMB_THROW = "spotanim.vfx_mad_angel_attack_bomb_throw"
        private const val ANIM_SMITE = "seq.npc_mad_angel_attack_smite"
        private const val ANIM_SMITE_ENRAGED = "seq.npc_mad_angel_attack_smite_enraged"
        private const val SPOT_SMITE = "spotanim.vfx_mad_angel_attack_smite"
        private const val SPOT_SMITE_ENRAGED = "spotanim.vfx_mad_angel_attack_smite_enraged"
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
