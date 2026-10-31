package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.api.combat.formulas.accuracy.magic.NvPMagicAccuracy
import org.rsmod.api.combat.formulas.accuracy.ranged.NvPRangedAccuracy
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType as EngineHitType
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val SPAWN_ANIM = "seq.npc_araxxor_01_spawn_01"
private const val MELEE_ANIM = "seq.npc_araxxor_01_attack_melee_01"
private const val RANGED_ANIM = "seq.npc_araxxor_01_acid_cannon_01"
private const val MAGIC_ANIM = "seq.npc_araxxor_01_attack_magic_01"
private const val BALL_THROW_ANIM = "seq.npc_araxxor_01_attack_ranged_01"
private const val SPRAY_ANIM = "seq.npc_araxxor_01_attack_acid_spray_01"
private const val LEAK_ANIM = "seq.npc_araxxor_01_attack_acid_leak_01"
private const val CLEAVE_ANIM = "seq.npc_araxxor_01_attack_melee_enraged_01"
private const val ENRAGE_ANIM = "seq.npc_araxxor_01_enrage_transition_01"
private const val ENRAGED_ANIM = "seq.npc_araxxor_01_enrage_transition_02"

private const val RANGED_PROJ = "spotanim.fx_venenatis_web_projectile"
private const val MAGIC_PROJ = "spotanim.araxxor_magic_projectile"
private const val MAGIC_IMPACT = "spotanim.araxxor_magic_impact"
private const val MAGIC_IMPACT_HEIGHT = 50
private const val ACIDIC_PROJ ="spotanim.tob_nylocas_rangedprojectile_sizemid"
private const val ACIDIC_ANIM = "seq.npc_araxyte_acidic_01_attack"
private const val MIRRORBACK_ANIM = "seq.npc_araxyte_mirror_back_01_attack"
private const val RUPTURA_BURST_SPOT = "spotanim.araxyte_explosive_spider_explosion"

private const val POOL_EXT_BALL = "araxxor.ball"
private const val POOL_EXT_DRIP = "araxxor.drip"
private const val HATCH_EXT = "araxxor.hatch"
private const val ENRAGE_EXT = "araxxor.enrage"
private const val DESPAWN_EXT = "araxxor.despawn"
private const val RUPTURA_BLAST_EXT = "araxxor.ruptura_blast"

private const val ARANEA_BOOTS = "obj.aranea_boots"
private const val ARANEA_RANGED_COOLDOWN = 8
private const val ARANEA_CLEAVE_COOLDOWN = 20

private const val ATTACK_RATE = 6
private const val ENRAGED_ATTACK_RATE = 4
private const val ENRAGE_HP_FRACTION = 0.25
private const val SPECIAL_EVERY = 6
private const val ATTACK_AP_RANGE = 10
private const val SPAWN_ATTACK_SUPPRESS_TICKS = 6
private const val ENGAGE_DELAY = 2
private const val SPAWN_REVEAL_DELAY = 1
private const val ENRAGE_DEFENCE_BONUS = 35
private const val ENRAGE_FIRST_CLEAVE_TICKS = 3

private const val MELEE_MAX = 38
private const val RANGED_MAX = 34
private const val MAGIC_MAX = 21
private const val PROTECT_MELEE_CAP = 5
private const val RANGED_PRAYER_PENETRATION = 55
private const val MAGIC_PRAYER_PENETRATION = 50
private const val CLEAVE_PRAYER_PENETRATION = 34
private const val DRAIN_PERCENT = 9
private const val VENOM_CHANCE = 1
private const val VENOM_OUT_OF = 3

private const val CLEAVE_REACH = 5
private const val CLEAVE_NPC_HIT_TICKS = 1
private const val CLEAVE_LAND_TICKS = 2
private const val CLEAVE_LOCK_TICKS = 3
private const val CLEAVE_AIM = "cleave_aim"
private const val CLEAVE_SELF_MIN = 8
private const val CLEAVE_SELF_MAX = 12
private const val BALL_LAND_TICKS = 2

private const val SPRAY_BLOBS = 9
private const val SPRAY_RADIUS = 3

private const val POOL_DAMAGE_MIN = 4
private const val POOL_DAMAGE_MAX = 5

private const val MINION_MAX = 15
private const val MINION_ATTACK_RATE = 6
private const val RUPTURA_RADIUS = 3
private const val RUPTURA_BLAST_TICKS = 2
private const val RUPTURA_DESPAWN_TICKS = 4
private const val RUPTURA_LOCKOUT = 20
private val RUPTURA_PLAYER_DAMAGE = intArrayOf(70, 49, 28, 7)

private val RANGED_PROJECTILE = ProjectileConfig.fixed(296, 50, 30, 90, 35, 0)
private val MAGIC_PROJECTILE = ProjectileConfig.fixed(69, 50, 30, 90, 35, 62)
private val LEAK_PROJECTILE = ProjectileConfig.fixed(170, 30, 30, 90, 35, 200)
private val SPRAY_PROJECTILE = ProjectileConfig(170, 0, 30, 70, 35, 200, 10)
private val ACIDIC_PROJECTILE = ProjectileConfig(32, 64, 39, 0, 16, 40, 10)

private val LAIR = area(spawnTile(-16, -17), spawnTile(23, 21))

private var Player.araneaRangedAt by intVarp("varp.araxxor_boots_ranged")
private var Player.araneaCleaveAt by intVarp("varp.araxxor_boots_cleave")

class Araxxor
@Inject
constructor(
    private val deps: BossDeps,
    private val eggs: AraxxorEggs,
    private val acid: AraxxorAcid,
    private val corpse: AraxxorCorpse,
    private val interactions: AiPlayerInteractions,
    private val death: NpcDeath,
    private val npcList: NpcList,
    private val nvpMagic: NvPMagicAccuracy,
    private val nvpRanged: NvPRangedAccuracy,
) : PluginScript() {

    private val venomOnHit: Effect = venom(VENOM_CHANCE, VENOM_OUT_OF)

    private fun accuracy(max: Int): DamageExpr = Accuracy(on = Roll(0..max))

    private val meleeAccuracy: DamageExpr =
        Accuracy(on = Roll(0..MELEE_MAX), meleeAttackType = MeleeAttackType.Crush)

    private val rangedWeaker =
        Condition.Custom { _, target ->
            target != null &&
                nvpRanged.computeDefenceRoll(target) <= nvpMagic.computeDefenceRoll(target)
        }

    private val standingInAcid =
        Condition.Custom { npc, target ->
            target != null && deps.encounterRegistry.of(npc).ownsLocAt(target.coords)
        }

    private val araneaBlocksRanged =
        Condition.Custom { _, target ->
            target != null &&
                araneaBlocks(target, target::araneaRangedAt, ARANEA_RANGED_COOLDOWN)
        }

    private val araneaBlocksCleave =
        Condition.Custom { _, target ->
            target != null &&
                araneaBlocks(target, target::araneaCleaveAt, ARANEA_CLEAVE_COOLDOWN)
        }

    private val standardTick: Effect =
        sequence(
            addVarn(HATCH_IN_VARN, -1),
            whenever(
                varnIs(HATCH_IN_VARN, 0),
                sequence(
                    setVarn(HATCH_IN_VARN, HATCH_INTERVAL_STANDARDS),
                    after(HATCH_DELAY_TICKS, external(HATCH_EXT)),
                ),
            ),
        )

    private val meleeEffect: Effect =
        sequence(
            anim(MELEE_ANIM),
            whenever(
                TargetPraying(Melee),
                hit {
                    damage(DamageExpr.Min(meleeAccuracy, Fixed(PROTECT_MELEE_CAP)))
                    type(Melee)
                    penetration(100)
                    onHit(venomOnHit)
                },
                hit {
                    damage(meleeAccuracy)
                    type(Melee)
                    onHit(venomOnHit)
                },
            ),
            standardTick,
        )

    private val rangedEffect: Effect =
        sequence(
            anim(RANGED_ANIM),
            whenever(
                araneaBlocksRanged,
                projectile(spotanim = RANGED_PROJ, config = RANGED_PROJECTILE),
                projectile(
                    spotanim = RANGED_PROJ,
                    config = RANGED_PROJECTILE,
                    resolveOnImpact = true,
                    hit =
                        hit {
                            damage(accuracy(RANGED_MAX))
                            type(Ranged)
                            penetration(RANGED_PRAYER_PENETRATION)
                            onHit(
                                sequence(
                                    statDrainPercent("stat.defence", percent = DRAIN_PERCENT),
                                    venomOnHit,
                                )
                            )
                        },
                ),
            ),
            standardTick,
        )

    private val magicEffect: Effect =
        sequence(
            anim(MAGIC_ANIM),
            projectile(
                spotanim = MAGIC_PROJ,
                config = MAGIC_PROJECTILE,
                resolveOnImpact = true,
                hit =
                    hit {
                        damage(accuracy(MAGIC_MAX))
                        type(Magic)
                        spotanim(MAGIC_IMPACT, height = MAGIC_IMPACT_HEIGHT)
                        missSpotanim(MAGIC_IMPACT)
                        penetration(MAGIC_PRAYER_PENETRATION)
                        onHit(
                            sequence(
                                statDrainPercent("stat.prayer", percent = DRAIN_PERCENT),
                                venomOnHit,
                            )
                        )
                    },
            ),
            standardTick,
        )

    private val cleaveIsNorthSouth =
        Condition.Custom { npc, target ->
            target != null && cleaveRunsNorthSouth(npc.coords, target.coords)
        }

    private fun cleaveLine(northSouth: Boolean): List<TargetExpr.Single> =
        (-1..1).map { step ->
            if (northSouth) offset(tile(CLEAVE_AIM), 0, step) else offset(tile(CLEAVE_AIM), step, 0)
        }

    private fun cleaveStrike(northSouth: Boolean, hitsPlayers: Boolean): Effect {
        val line = cleaveLine(northSouth)
        val tiles =
            line.map { tile ->
                val pool = spawnLoc(ACID_POOL_LOC, tile)
                if (!hitsPlayers) {
                    pool
                } else {
                    sequence(
                        hit {
                            target = playersOn(tile)
                            damage(Roll(0..MELEE_MAX))
                            type(Melee)
                            penetration(CLEAVE_PRAYER_PENETRATION)
                        },
                        pool,
                    )
                }
            }
        return parallel(*tiles.toTypedArray())
    }

    private fun cleaveNpcHits(northSouth: Boolean): Effect =
        hitNpcs(
            on = cleaveLine(northSouth),
            damage = Roll(0..MELEE_MAX),
            type = Melee,
            selfDamage = Roll(CLEAVE_SELF_MIN..CLEAVE_SELF_MAX),
            npcTypes = listOf(MINION_ACIDIC, MINION_MIRRORBACK, MINION_RUPTURA),
        )

    private fun cleaveLanding(northSouth: Boolean): Effect =
        parallel(
            after(CLEAVE_NPC_HIT_TICKS, cleaveNpcHits(northSouth)),
            after(
                CLEAVE_LAND_TICKS,
                whenever(
                    araneaBlocksCleave,
                    cleaveStrike(northSouth, hitsPlayers = false),
                    cleaveStrike(northSouth, hitsPlayers = true),
                ),
            ),
        )

    private val cleaveEffect: Effect =
        sequence(
            anim(CLEAVE_ANIM),
            say("Skree!"),
            lockMovement(),
            withTile(
                CLEAVE_AIM,
                CurrentTargetTile,
                sequence(
                    faceTile(tile(CLEAVE_AIM)),
                    whenever(cleaveIsNorthSouth, cleaveLanding(true), cleaveLanding(false)),
                ),
            ),
            after(CLEAVE_LOCK_TICKS, unlockMovement()),
            standardTick,
        )

    private fun blob(at: TargetExpr.Single): Effect =
        projectile(
            spotanim = POOLS_PROJ,
            config = SPRAY_PROJECTILE,
            target = at,
            impact = POOLS_SPLASH,
            onImpact = spawnLoc(ACID_POOL_LOC, ImpactTile),
        )

    private val ballEffect: Effect =
        sequence(
            anim(BALL_THROW_ANIM, delay = 30),
            withTile("aim", CurrentTargetTile, after(BALL_LAND_TICKS, external(POOL_EXT_BALL, at = tile("aim")))),
        )

    private val sprayEffect: Effect =
        sequence(
            anim(SPRAY_ANIM),
            parallel(
                *Array(SPRAY_BLOBS) {
                    blob(TargetExpr.RandomWalkableTile(SPRAY_RADIUS, of = CurrentTargetTile))
                }
            ),
        )

    private val leakEffect: Effect =
        sequence(
            anim(LEAK_ANIM),
            projectile(
                spotanim = POOLS_PROJ,
                config = LEAK_PROJECTILE,
                target = CurrentTarget,
                onImpact = external(POOL_EXT_DRIP),
            ),
        )

    private val specialEffect: Effect =
        switch(PATTERN_VARN, 0 to ballEffect, 1 to sprayEffect, 2 to leakEffect)

    private val enrageEffect: Effect =
        sequence(
            anim(ENRAGE_ANIM),
            wait(2),
            anim(ENRAGED_ANIM),
            external(ENRAGE_EXT),
            nextAttackIn(ENRAGE_FIRST_CLEAVE_TICKS),
        )

    private val poolDamage: Effect =
        sequence(
            hit {
                damage((POOL_DAMAGE_MIN..POOL_DAMAGE_MAX).roll())
                type(Typeless)
                hazard()
            },
            venom(),
        )

    private val spec =
        boss(ARAXXOR) {
            stats(attackRate = ATTACK_RATE)

            every(1, onEach(playersIn(LAIR), whenever(standingInAcid, poolDamage)))

            val melee = ability("melee") { include(meleeEffect) }
            val ranged = ability("ranged") { include(rangedEffect) }
            val magic = ability("magic") { include(magicEffect) }
            val cleave = ability("cleave") { include(cleaveEffect) }
            val special = ability("special") { include(specialEffect) }
            val enrage = ability("enrage") { include(enrageEffect) }

            phase("combat") {
                attackRate = ATTACK_RATE
                weightedSelectorRandom {
                    +random(melee, weight = 1, requires = WithinMeleeRange)
                    +random(ranged, weight = 1, requires = !WithinMeleeRange and rangedWeaker)
                    +random(magic, weight = 1, requires = !WithinMeleeRange and !rangedWeaker)
                }
                forceEveryAttacks(SPECIAL_EVERY, SPECIAL_EVERY, special)
            }

            phase("enraged", entryHp = ENRAGE_HP_FRACTION) {
                entry = enrage.name
                attackRate = ENRAGED_ATTACK_RATE
                weightedSelectorRandom {
                    +random(cleave, weight = 1, requires = targetWithin(CLEAVE_REACH))
                    +random(
                        ranged,
                        weight = 1,
                        requires = !targetWithin(CLEAVE_REACH) and rangedWeaker,
                    )
                    +random(
                        magic,
                        weight = 1,
                        requires = !targetWithin(CLEAVE_REACH) and !rangedWeaker,
                    )
                }
                forceEveryAttacks(SPECIAL_EVERY, SPECIAL_EVERY, special)
            }
        }

    private val mirrorbackSpec =
        boss(MINION_MIRRORBACK) {
            stats(attackRate = MINION_ATTACK_RATE)
            val attack =
                ability("attack") {
                    anim(MIRRORBACK_ANIM)
                    hit {
                        damage(accuracy(MINION_MAX))
                        type(Melee)
                    }
                }
            phase("combat") {
                attackRate = MINION_ATTACK_RATE
                weightedSelectorRandom { +random(attack, weight = 1, requires = WithinMeleeRange) }
            }
        }

    private val acidicSpec =
        boss(MINION_ACIDIC) {
            stats(attackRate = MINION_ATTACK_RATE)
            val attack =
                ability("attack") {
                    anim(ACIDIC_ANIM)
                    projectile {
                        spotanim = ACIDIC_PROJ
                        config = ACIDIC_PROJECTILE
                        hit {
                            damage(accuracy(MINION_MAX))
                            type(Ranged)
                        }
                    }
                }
            phase("combat") {
                attackRate = MINION_ATTACK_RATE
                weightedSelectorRandom { +random(attack, weight = 1) }
            }
        }

    private val rupturaEffect: Effect =
        sequence(
            anim(EXPLODE_ANIM),
            spotanim(RUPTURA_BURST_SPOT),
            after(
                RUPTURA_BLAST_TICKS,
                hit {
                    target = AllInRadius(RUPTURA_RADIUS)
                    damage(
                        DamageExpr.Custom { npc, player ->
                            RUPTURA_PLAYER_DAMAGE[
                                npc.edgeDistanceTo(player.coords).coerceIn(0, RUPTURA_RADIUS)]
                        }
                    )
                    type(Typeless)
                    hazard()
                },
            ),
            after(RUPTURA_BLAST_TICKS, external(RUPTURA_BLAST_EXT)),
            after(RUPTURA_DESPAWN_TICKS, external(DESPAWN_EXT)),
        )

    private val rupturaSpec =
        boss(MINION_RUPTURA) {
            stats(attackRate = 1)
            val explode = ability("explode", rupturaEffect, attackDelay = RUPTURA_LOCKOUT)
            phase("combat") {
                attackRate = 1
                weightedSelectorRandom { +random(explode, weight = 1, requires = WithinMeleeRange) }
            }
        }

    override fun ScriptContext.startup() {
        val araxxorId = ARAXXOR.asRSCM(RSCMType.NPC)

        deps.extensionRegistry.register(HATCH_EXT) { _, npc, _, _ -> eggs.hatchNext(npc) }
        deps.extensionRegistry.register(POOL_EXT_BALL) { ctx ->
            ctx.tile?.let { acid.startBall(ctx.npc, it) }
        }
        deps.extensionRegistry.register(POOL_EXT_DRIP) { _, npc, target, _ ->
            acid.startDrip(npc, target)
        }
        deps.extensionRegistry.register(ENRAGE_EXT) { _, npc, _, _ ->
            npc.defenceLvl += ENRAGE_DEFENCE_BONUS
            npc.baseDefenceLvl += ENRAGE_DEFENCE_BONUS
        }
        deps.extensionRegistry.register(DESPAWN_EXT) { _, npc, _, _ ->
            deps.npcRepo.del(npc, Int.MAX_VALUE)
        }
        deps.extensionRegistry.register(RUPTURA_BLAST_EXT) { _, npc, _, _ -> eggs.rupturaBlast(npc) }
        onEvent<NpcStateEvents.Create> { if (npc.type.id == araxxorId) onSpawn(npc) }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.id == araxxorId) onSpawn(npc) }

        onOpNpc1(CORPSE_NPC) { corpse.harvest(this, it.npc) }
        onOpNpc3(CORPSE_NPC) { corpse.destroy(this, it.npc) }
        val corpseId = CORPSE_NPC.asRSCM(RSCMType.NPC)
        onEvent<NpcStateEvents.Delete> { if (npc.type.id == corpseId) corpse.onCorpseDeleted(npc) }

        registerBroodDeaths()

        BossCombat.register(
            this,
            spec,
            deps,
            onModifyHit = {
                val attacker = attackerOf(hit)
                if (attacker != null) hit.damage = eggs.redirect(npc, hit.damage, attacker)
            },
            onHit = { if (npc.hitpoints <= 0) eggs.crumbleEggs(npc) },
        )
        BossCombat.register(
            this,
            mirrorbackSpec,
            deps,
            onModifyHit = {
                val attacker = attackerOf(hit)
                if (
                    attacker != null &&
                        hit.type == EngineHitType.Melee &&
                        npc.edgeDistanceTo(attacker.coords) <= 1
                ) {
                    val recoil = hit.damage / 2
                    if (recoil > 0) {
                        attacker.queueHit(1, EngineHitType.Typeless, recoil, NoopPlayerHitModifier)
                    }
                }
            },
        )
        BossCombat.register(this, acidicSpec, deps)
        BossCombat.register(this, rupturaSpec, deps)
    }

    private fun attackerOf(hit: HitBuilder): Player? =
        hit.sourceUid?.takeIf { hit.isFromPlayer }?.let { PlayerUid(it).resolve(deps.playerList) }

    private fun ScriptContext.registerBroodDeaths() {
        for (name in listOf(EGG_ACIDIC, EGG_MIRRORBACK, EGG_RUPTURA, MINION_MIRRORBACK, MINION_RUPTURA)) {
            val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)) ?: continue
            onNpcQueue(type, "queue.death") { death.deathNoDrops(this) }
        }
        val acidic = ServerCacheManager.getNpc(MINION_ACIDIC.asRSCM(RSCMType.NPC)) ?: return
        onNpcQueue(acidic, "queue.death") {
            findBoss(npc)?.let { acid.acidicDeath(it, npc) }
            death.deathNoDrops(this)
        }
    }

    private fun findBoss(near: Npc): Npc? {
        val araxxorId = ARAXXOR.asRSCM(RSCMType.NPC)
        return npcList.firstOrNull {
            it.visType.id == araxxorId && it.isSlotAssigned && it.hitpoints > 0 &&
                it.edgeDistanceTo(near) <= BOSS_SEARCH_RADIUS
        }
    }

    private fun onSpawn(boss: Npc) {
        boss.vars[SPAWN_TICK_VARN] = deps.mapClock.cycle
        boss.apRangeOverride = ATTACK_AP_RANGE
        boss.interactsWhenUnderTarget = true
        boss.anim(SPAWN_ANIM, 0, 0)
        deps.worldQueues.add(SPAWN_REVEAL_DELAY) {
            if (!boss.isSlotAssigned) return@add
            eggs.spawn(boss)
            deps.suppressAttacks(boss, SPAWN_ATTACK_SUPPRESS_TICKS)
        }
        boss.vars[ENGAGE_AT_VARN] = deps.mapClock.cycle + ENGAGE_DELAY
        eggs.keepEngaged(boss)
    }

    private fun araneaBlocks(target: Player, lastBlock: KMutableProperty0<Int>, cooldown: Int): Boolean {
        if (!target.worn[Wearpos.Feet.slot].isType(ARANEA_BOOTS)) return false
        val now = deps.mapClock.cycle
        if (lastBlock.get() != 0 && now - lastBlock.get() < cooldown) return false
        lastBlock.set(now)
        target.mes("Your Aranea boots let you avoid the spider-based attack.")
        return true
    }

    private companion object {
        private const val HATCH_DELAY_TICKS = 3
        private const val BOSS_SEARCH_RADIUS = 40
    }
}
