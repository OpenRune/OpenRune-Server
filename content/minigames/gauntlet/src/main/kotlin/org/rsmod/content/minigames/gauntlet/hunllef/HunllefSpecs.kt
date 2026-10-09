package org.rsmod.content.minigames.gauntlet.hunllef

import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.DamageExpr
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal object HunllefVarns {
    const val STYLE = "varn.hunllef_style"
    const val CYCLE = "varn.hunllef_cycle"
    const val PROTECT = "varn.hunllef_protect"
    const val OFF_PRAYER = "varn.hunllef_off_prayer"

    const val RANGED = 0
    const val MAGIC = 1

    const val PROTECT_MELEE = 0
    const val PROTECT_RANGED = 1
    const val PROTECT_MAGIC = 2
}

@Singleton
class HunllefSpecs @Inject constructor() {
    val crystalline: BossSpec = build(corrupted = false)
    val corrupted: BossSpec = build(corrupted = true)

    fun of(corrupted: Boolean): BossSpec = if (corrupted) this.corrupted else crystalline

    private fun build(corrupted: Boolean): BossSpec {
        val sfx = if (corrupted) "_hm" else ""
        val tornadoBonus = if (corrupted) 1 else 0

        fun tiers(player: Player) = HunllefDamage.armourTiers(player, corrupted)

        val attackDamage =
            DamageExpr.Accuracy(
                on =
                    DamageExpr.Custom { _, target ->
                        HunllefDamage.rollAttack(corrupted, tiers(target), prayed = false)
                    }
            )

        val flight =
            ProjectileConfig(
                startHeight = PROJECTILE_START_HEIGHT,
                endHeight = PROJECTILE_END_HEIGHT,
                startDelay = PROJECTILE_START_DELAY,
                travelTime = PROJECTILE_TRAVEL,
                angle = PROJECTILE_ANGLE,
                progress = 0,
                stepMultiplier = PROJECTILE_STEP,
            )

        val advanceCycle =
            sequence(
                addVarn(HunllefVarns.CYCLE, 1),
                whenever(
                    varnAtLeast(HunllefVarns.CYCLE, ATTACKS_PER_STYLE),
                    sequence(
                        setVarn(HunllefVarns.CYCLE, 0),
                        switch(
                            HunllefVarns.STYLE,
                            HunllefVarns.RANGED to setVarn(HunllefVarns.STYLE, HunllefVarns.MAGIC),
                            HunllefVarns.MAGIC to setVarn(HunllefVarns.STYLE, HunllefVarns.RANGED),
                        ),
                        after(
                            TRANSITION_DELAY,
                            switch(
                                HunllefVarns.STYLE,
                                HunllefVarns.MAGIC to anim(TRANSITION_MAGIC_SEQ),
                                HunllefVarns.RANGED to anim(TRANSITION_RANGED_SEQ),
                            ),
                        ),
                    ),
                ),
            )

        return boss(*TYPES.toTypedArray()) {
            stats(attackRate = ATTACK_RATE)

            val ranged =
                ability(
                    "ranged",
                    sequence(
                        anim(ATTACK_SEQ),
                        projectile(
                            spotanim = "spotanim.crystal_hunllef_range_travel$sfx",
                            config = flight,
                            resolveOnImpact = true,
                            hit =
                                hit {
                                    damage(attackDamage)
                                    type(HitType.Ranged)
                                    penetration(PRAYED_DAMAGE_PERCENT, whenever = TargetPraying(HitType.Ranged))
                                },
                        ),
                        advanceCycle,
                    ),
                )

            val magic =
                ability(
                    "magic",
                    sequence(
                        anim(ATTACK_SEQ),
                        projectile(
                            spotanim = "spotanim.crystal_hunllef_magic_travel$sfx",
                            config = flight,
                            resolveOnImpact = true,
                            hit =
                                hit {
                                    damage(attackDamage)
                                    type(HitType.Magic)
                                    penetration(PRAYED_DAMAGE_PERCENT, whenever = TargetPraying(HitType.Magic))
                                    spotanim(
                                        "spotanim.crystal_hunllef_magic_impact$sfx",
                                        height = IMPACT_HEIGHT,
                                    )
                                },
                        ),
                        advanceCycle,
                    ),
                )

            val prayerDisable =
                ability(
                    "prayer_disable",
                    sequence(
                        anim(ATTACK_SEQ),
                        projectile(
                            spotanim = "spotanim.crystal_hunllef_prayer_travel$sfx",
                            config = flight,
                            impact = "spotanim.crystal_hunllef_prayer_impact$sfx",
                            resolveOnImpact = true,
                            hit =
                                hit {
                                    damage(attackDamage)
                                    type(HitType.Magic)
                                    penetration(PRAYED_DAMAGE_PERCENT, whenever = TargetPraying(HitType.Magic))
                                    onHit(
                                        sequence(disablePrayers(), message(PRAYERS_DISABLED)),
                                        evenOnMiss = true,
                                    )
                                },
                        ),
                        advanceCycle,
                    ),
                )

            val stomp =
                ability(
                    "stomp",
                    sequence(
                        anim(STOMP_SEQ),
                        message(STOMP_MESSAGE),
                        hit {
                            damage(
                                DamageExpr.Custom { _, target ->
                                    HunllefDamage.rollAttack(corrupted, tiers(target), prayed = false)
                                }
                            )
                            type(HitType.Typeless)
                            delay = STOMP_DELAY
                        },
                    ),
                )

            val protectMelee =
                ability("protect_melee", protect("npc.crystal_hunllef_melee$sfx", HunllefVarns.PROTECT_MELEE))
            val protectRanged =
                ability("protect_ranged", protect("npc.crystal_hunllef_ranged$sfx", HunllefVarns.PROTECT_RANGED))
            val protectMagic =
                ability("protect_magic", protect("npc.crystal_hunllef_magic$sfx", HunllefVarns.PROTECT_MAGIC))

            fun tornado(stage: Int): AbilityRef =
                ability(
                    "tornado_$stage",
                    sequence(
                        anim(SPECIAL_SEQ),
                        after(
                            TORNADO_DELAY,
                            summon(
                                npc = "npc.crystal_hunllef_crystals$sfx",
                                count = stage + tornadoBonus,
                                radius = TORNADO_RADIUS,
                                centeredOn = spawnTile(ARENA_CENTRE_OFFSET, ARENA_CENTRE_OFFSET),
                                mode = NpcMode.None,
                                duration = TORNADO_LIFETIME,
                                onSummon = HunllefTornadoes.CHASE_EXT,
                                onSummonParams = corrupted,
                                owned = true,
                            ),
                        ),
                        advanceCycle,
                    ),
                )

            incoming {
                rule(varnIs(HunllefVarns.PROTECT, HunllefVarns.PROTECT_MELEE)) {
                    scalePercent(0, HitType.Melee)
                }
                rule(varnIs(HunllefVarns.PROTECT, HunllefVarns.PROTECT_RANGED)) {
                    scalePercent(0, HitType.Ranged)
                }
                rule(varnIs(HunllefVarns.PROTECT, HunllefVarns.PROTECT_MAGIC)) {
                    scalePercent(0, HitType.Magic)
                }
            }

            val underBoss =
                Condition.Custom { npc, target -> target != null && standsUnder(npc, target) }

            fun stage(name: String, entryHp: Double?, tornado: AbilityRef) {
                phase(name, entryHp = entryHp, lockMovement = true) {
                    weightedSelectorRandom {
                        +random(ranged, weight = 1, requires = varnIs(HunllefVarns.STYLE, HunllefVarns.RANGED))
                        +random(magic, weight = 3, requires = varnIs(HunllefVarns.STYLE, HunllefVarns.MAGIC))
                        +random(
                            prayerDisable,
                            weight = 1,
                            requires = varnIs(HunllefVarns.STYLE, HunllefVarns.MAGIC),
                            cooldown = PRAYER_DISABLE_COOLDOWN,
                        )
                    }
                    forceWhen(underBoss, stomp)
                    forceEveryAttacks(TORNADO_MIN_ATTACKS, TORNADO_MAX_ATTACKS, tornado)
                }
            }

            stage(STAGE_ONE, entryHp = null, tornado = tornado(1))
            stage(STAGE_TWO, entryHp = HunllefStage.TWO_THIRDS, tornado = tornado(2))
            stage(STAGE_THREE, entryHp = HunllefStage.ONE_THIRD, tornado = tornado(3))
        }
    }

    private fun protect(type: String, protectValue: Int) =
        sequence(
            transmog(type, Int.MAX_VALUE),
            setVarn(HunllefVarns.PROTECT, protectValue),
            setVarn(HunllefVarns.OFF_PRAYER, 0),
            headIcon(HEAD_ICON_SLOT, HEAD_ICON_GRAPHIC, protectValue),
        )


    private fun standsUnder(npc: Npc, target: Player): Boolean {
        val dx = target.coords.x - npc.coords.x
        val dz = target.coords.z - npc.coords.z
        return dx in 0 until npc.size && dz in 0 until npc.size
    }

    companion object {
        val TYPES =
            listOf("melee", "ranged", "magic").flatMap { style ->
                listOf("npc.crystal_hunllef_$style", "npc.crystal_hunllef_${style}_hm")
            }

        const val STAGE_ONE = "stage_one"
        const val STAGE_TWO = "stage_two"
        const val STAGE_THREE = "stage_three"

        const val HEAD_ICON_SLOT = 0
        const val HEAD_ICON_GRAPHIC = 440
        const val ATTACKS_PER_STYLE = 4
        const val OFF_PRAYER_SWITCH = 6
        private const val ATTACK_RATE = 5
        private const val TRANSITION_DELAY = 2
        private const val TORNADO_DELAY = 3
        private const val TORNADO_LIFETIME = 21
        private const val TORNADO_RADIUS = 5
        private const val ARENA_CENTRE_OFFSET = 2
        private const val TORNADO_MIN_ATTACKS = 8
        private const val TORNADO_MAX_ATTACKS = 11
        private const val PRAYER_DISABLE_COOLDOWN = 20
        private const val STOMP_DELAY = 2
        private const val PRAYED_DAMAGE_PERCENT = 24
        private const val IMPACT_HEIGHT = 124

        private const val PROJECTILE_START_HEIGHT = 214
        private const val PROJECTILE_END_HEIGHT = 124
        private const val PROJECTILE_START_DELAY = 19
        private const val PROJECTILE_TRAVEL = 35
        private const val PROJECTILE_ANGLE = 18
        private const val PROJECTILE_STEP = 5

        private const val ATTACK_SEQ = "seq.hunllef_attack_ranged"
        private const val SPECIAL_SEQ = "seq.hunllef_attack_special"
        private const val STOMP_SEQ = "seq.hunllef_attack_melee"
        private const val TRANSITION_MAGIC_SEQ = "seq.hunllef_attack_transition_magic"
        private const val TRANSITION_RANGED_SEQ = "seq.hunllef_attack_transition_ranged"

        private const val PRAYERS_DISABLED = "<col=ef1020>Your prayers have been disabled!</col>"
        private const val STOMP_MESSAGE = "You're trampled beneath the Hunllef."
    }
}
