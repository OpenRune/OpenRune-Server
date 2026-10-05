package org.rsmod.content.bosses.doom

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Condition.InPhase
import org.rsmod.api.npc.hit.modifier.StandardNpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DoomNpcs {
    const val BOSS = "npc.dom_boss"
    const val SHIELDED = "npc.dom_boss_shielded"
    const val BURROWED = "npc.dom_boss_burrowed"
}

internal fun doomSpec(delve: DoomDelve): BossSpec =
    boss(DoomNpcs.BOSS, DoomNpcs.SHIELDED, DoomNpcs.BURROWED) {
        stats(attackRate = delve.attackSpeed)

        val larvaRoll = chance(DoomLarvae.SPAWN_ONE_IN, external(DoomLarvae.SPAWN_EXT, params = delve))
        val countAttack = sequence(addVarn(DoomVarns.ATTACKS, 1), addVarn(DoomVarns.STEP_ATTACKS, 1))

        val tongue =
            ability("tongue") {
                attackDelay = DoomAttacks.TONGUE_RATE
                anim(DoomAttacks.TONGUE_SEQ)
                hit {
                    damage(Accuracy(Roll(0..DoomAttacks.TONGUE_MAX_HIT), meleeAttackType = MeleeAttackType.Slash))
                    type(Melee)
                    penetration(DoomAttacks.TONGUE_PRAYER_PENETRATION)
                }
                include(countAttack)
                include(larvaRoll)
            }
        val standard = ability("standard", sequence(DoomAttacks.standard(delve), countAttack, larvaRoll))
        val meleeCharge = ability("melee_charge", DoomCharge.meleeCharge(delve))
        val rockThrow =
            ability(
                "rock_throw",
                sequence(
                    DoomRockThrow.rockThrow(delve),
                    countAttack,
                    larvaRoll,
                    after(
                        MELEE_CHARGE_DELAY,
                        whenever(
                            InPhase(FIGHT_PHASE) and varnIs(DoomVarns.ROTATION_STEP, 0),
                            chance(MELEE_CHARGE_ONE_IN, run(meleeCharge)),
                        ),
                    ),
                ),
                attackDelay = delve.attackSpeed * DoomRockThrow.attackSlots(delve),
            )

        val scriptedRockThrow =
            ability(
                "rock_throw_scripted",
                sequence(DoomRockThrow.rockThrow(delve), countAttack, larvaRoll),
                attackDelay = delve.attackSpeed * DoomRockThrow.attackSlots(delve),
            )
        val volatileEarth =
            ability(
                "volatile_earth",
                DoomShockwave.volatileEarth(delve, scriptedRockThrow),
                attackDelay = DoomShockwave.PRE_SHOCKWAVE_ROCK_THROW_DELAY,
            )

        if (delve.hasBurrow) {
            val emergenceRock =
                ability(
                    "post_emerge_rock",
                    sequence(DoomRockThrow.rockThrow(delve), countAttack, larvaRoll, forceNext(volatileEarth)),
                    attackDelay = 1,
                )
            ability(DoomCar.ENTER_ABILITY, DoomCar.enter(delve))
            if (delve.carSlams > 0) ability(DoomCar.SHOT_ABILITY, DoomAttacks.burrowShot(delve))
            ability(DoomCar.EMERGE_ABILITY, DoomCar.emerge(emergenceRock))
            ability(DoomCar.ROCK_HIT_ABILITY) {
                knockback("seq.agilityarena_player_spikedback", DoomArena.AREA)
                hit {
                    delay = 1
                    damage(10..21).roll()
                    type(Typeless)
                    hazard()
                }
            }
            onIncomingHit(
                ability("restart_burrow_charge", DoomCar.restartCharge),
                requires = varnIs(DoomVarns.CHARGE, DoomVarns.CHARGE_BURROW),
            )
        }

        val punishedHit = varnIs(DoomVarns.CHARGE, DoomVarns.CHARGE_PUNISHED) and hitStyle(Melee)
        val punishingHit = varnIs(DoomVarns.CHARGE, DoomVarns.CHARGE_MELEE) and hitStyle(Melee)
        onIncomingHit(ability("punish_bonus", external(DoomCharge.PUNISH_BONUS_EXT)), requires = punishedHit)
        onIncomingHit(ability("melee_punish", DoomCharge.punish(delve)), requires = punishingHit)

        val startShield = if (delve.hasShield) ability("start_shield", DoomShield.start(delve)) else null
        val shieldDue =
            Condition.HpBelow(DoomShield.TRIGGER_HP, inclusive = true) and
                varnAtLeast(DoomVarns.ATTACKS, DoomShield.TRIGGER_ATTACKS)

        phase(FIGHT_PHASE, transmog = DoomNpcs.BOSS, lockMovement = true) {
            every(1, addVarn(DoomVarns.FIGHT_TICKS, 1))
            startShield?.let {
                forceWhen(shieldDue and varnIs(DoomVarns.ROTATION_STEP, 0), it, once = true)
                if (delve.hasBurrow) {
                    val afterWave = varnIs(DoomVarns.ROTATION_STEP, DoomCar.POST_SHOCKWAVE)
                    forceWhen(afterWave and varnAtLeast(DoomVarns.STEP_ATTACKS, 2), it)
                    forceWhen(afterWave and varnIs(DoomVarns.STEP_ATTACKS, 1), standard)
                }
            }
            if (!delve.hasBurrow) {
                forceWhen(varnAtLeast(DoomVarns.FIGHT_TICKS, DoomShockwave.FIGHT_TICKS_TRIGGER), volatileEarth, once = true)
            }
            weightedSelectorRandom {
                +random(tongue, weight = 1, requires = WithinMeleeRange)
                +random(standard, weight = STANDARD_WEIGHT, requires = !WithinMeleeRange)
                +random(rockThrow, weight = ROCK_THROW_WEIGHT, requires = !WithinMeleeRange)
            }
        }

        if (delve.hasBurrow) {
            phase(DoomCar.ENTRY_PHASE, transmog = DoomNpcs.BOSS, lockMovement = true) {}
            phase(DoomCar.PHASE, transmog = DoomNpcs.BURROWED, lockMovement = true) {
                every(1, DoomCar.chargeTick(delve))
            }
        }

        if (delve.hasShield) {
            ability(DoomShield.END_ABILITY, DoomShield.end(delve))
            phase(DoomShield.PHASE, transmog = DoomNpcs.SHIELDED, lockMovement = true) {
                every(1, DoomShield.chargeTick(delve))
            }
            val shieldCharging = varnIs(DoomVarns.CHARGE, DoomVarns.CHARGE_SHIELD)
            onIncomingHit(ability("restart_shield_charge", DoomShield.restartCharge), requires = shieldCharging and hitDemonbane())
            val resist = ability("shield_resist", message(DoomShield.RESIST_MESSAGE))
            incoming {
                rule(InPhase(DoomShield.PHASE) and !hitDemonbane()) {
                    cap(0)
                    run(resist)
                }
            }
        }
    }

internal const val FIGHT_PHASE = "fight"
private const val STANDARD_WEIGHT = 60
private const val ROCK_THROW_WEIGHT = 40
private const val MELEE_CHARGE_DELAY = 2
private const val MELEE_CHARGE_ONE_IN = 2

@Singleton
internal class DoomSpecs @Inject constructor() {
    private val byLevel: Map<Int, BossSpec> = DoomDelve.LEVELS.associate { it.level to doomSpec(it) }

    val all: Collection<BossSpec>
        get() = byLevel.values

    fun of(delve: DoomDelve): BossSpec = byLevel.getValue(delve.level)
}

class Doom
@Inject
internal constructor(
    private val deps: BossDeps,
    private val specs: DoomSpecs,
    private val delves: DoomDelves,
    private val loot: DoomLoot,
    private val holyWater: DoomHolyWater,
    private val shockwaves: DoomShockwaves,
    private val larvae: DoomLarvae,
    private val shields: DoomShields,
    private val acid: DoomAcid,
    private val cars: DoomCars,
    private val wornBonuses: WornBonuses,
    private val npcHitModifier: StandardNpcHitModifier,
) : PluginScript() {
    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            specs.all,
            deps,
            onModifyHit = { shields.capHit(npc, hit) },
            onHit = {
                acid.onBossHit(npc, hit)
                shields.onHit(npc)
                if (!hit.isFromPlayer) cars.onHit(npc)
            },
        )
        deps.extensionRegistry.register(DoomRockThrow.PLACE_ROCK_EXT) { ctx ->
            ctx.tile?.let { deps.locRepo.add(it, ROCK_LOC, Int.MAX_VALUE, LocAngle[deps.random.of(4)], LocShape.CentrepieceStraight) }
        }
        deps.extensionRegistry.register(DoomCharge.PUNISH_BONUS_EXT) { _, npc, target, _ -> punishBonus(npc, target) }
        shockwaves.register(this)
        larvae.register(this)
        shields.register()
        cars.register()

        for (name in listOf(DoomNpcs.BOSS, DoomNpcs.SHIELDED, DoomNpcs.BURROWED)) {
            val type = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!
            onNpcQueue(type, "queue.death") {
                if (shields.absorbsDeath(npc)) return@onNpcQueue
                val unique = loot.roll(this)
                holyWater.onDeath(this)
                delves.onDeath(this, unique)
            }
        }
    }

    private fun punishBonus(npc: Npc, attacker: Player) {
        val bonus = wornBonuses.strengthBonus(attacker) * PUNISH_BONUS_PERCENT / 100
        if (bonus <= 0) return
        npc.queueHit(PUNISH_BONUS_DELAY, HitType.Typeless, bonus, npcHitModifier)
    }

    private companion object {
        private const val ROCK_LOC = "loc.dom_rock"
        private const val PUNISH_BONUS_PERCENT = 20
        private const val PUNISH_BONUS_DELAY = 1
    }
}
