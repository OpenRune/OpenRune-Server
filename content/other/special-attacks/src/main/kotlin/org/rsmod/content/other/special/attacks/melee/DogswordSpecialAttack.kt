package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.combat.commons.BindEffectService
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType

/**
 * Power of the Gods combines each godsword special into one doubled-accuracy, 37.5%-stronger
 * Slash hit. The cache does not expose an RSCM alias for this event item's unique animation, so
 * the handler intentionally avoids substituting another godsword's visual effect.
 */
class DogswordSpecialAttack
@Inject
constructor(
    private val sacrifice: AncientGodswordBloodSacrifice,
    private val binds: BindEffectService,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee(
            "obj.echo_godsword",
            PowerOfTheGods(
                manager = manager,
                sacrifice = sacrifice,
                binds = binds,
                bloodSacrificeDamage = LEAGUE_BLOOD_SACRIFICE_DAMAGE,
                bloodSacrificeHealCap = LEAGUE_BLOOD_SACRIFICE_HEAL_CAP,
            ),
        )
        registerMelee(
            "obj.deadman_dogsword",
            PowerOfTheGods(
                manager = manager,
                sacrifice = sacrifice,
                binds = binds,
                bloodSacrificeDamage = DEADMAN_BLOOD_SACRIFICE_DAMAGE,
                bloodSacrificeHealCap = DEADMAN_BLOOD_SACRIFICE_HEAL_CAP,
            ),
        )
    }

    private class PowerOfTheGods(
        private val manager: SpecialAttackManager,
        private val sacrifice: AncientGodswordBloodSacrifice,
        private val binds: BindEffectService,
        private val bloodSacrificeDamage: Int,
        private val bloodSacrificeHealCap: Int,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = powerOfTheGods(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = powerOfTheGods(target, attack)

        private fun ProtectedAccess.powerOfTheGods(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim("seq.leagues_5_godsword_special")
            spotanim(
                spot = "spotanim.league_5_godsword_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = ACCURACY_MULTIPLIER,
                    maxHitMultiplier = MAX_HIT_MULTIPLIER,
                    blockType = MeleeAttackType.Slash,
                )
            val source = player
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value for every effect below - no impact callback needed.
            if (damage > 0) {
                drainBandosStats(target, damage)
                source.statHeal(
                    "stat.hitpoints",
                    constant = max(SARADOMIN_MINIMUM_HITPOINTS_HEAL, (damage + 1) / 2),
                    percent = 0,
                )
                source.statHeal(
                    "stat.prayer",
                    constant = max(SARADOMIN_MINIMUM_PRAYER_HEAL, (damage + 3) / 4),
                    percent = 0,
                )
                // Wiki: the Zamorak godsword's freeze is a real PvM tool (Muttadiles, Barrows'
                // Dharok) - not player-only. Matches ImpactMeleeSpecialAttacks' ZamorakGodsword.
                // Wiki: "Freezes opponent... with a similar animation to Ice Barrage."
                target.spotanim("spotanim.ice_barrage_impact")
                when (target) {
                    is Player -> CombatEffects.freeze(target, ZAMORAK_FREEZE_TICKS)
                    is Npc -> binds.bind(target, ZAMORAK_FREEZE_TICKS)
                }
                sacrifice.mark(
                    source = source,
                    target = target,
                    playerDamage = bloodSacrificeDamage,
                    playerHealCap = bloodSacrificeHealCap,
                    playerHitType = HitType.Magic,
                )
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        const val ACCURACY_MULTIPLIER: Double = 2.0
        const val MAX_HIT_MULTIPLIER: Double = 1.375

        const val SARADOMIN_MINIMUM_HITPOINTS_HEAL: Int = 10
        const val SARADOMIN_MINIMUM_PRAYER_HEAL: Int = 5
        const val ZAMORAK_FREEZE_TICKS: Int = 33

        const val LEAGUE_BLOOD_SACRIFICE_DAMAGE: Int = 25
        const val LEAGUE_BLOOD_SACRIFICE_HEAL_CAP: Int = 25
        const val DEADMAN_BLOOD_SACRIFICE_DAMAGE: Int = 15
        const val DEADMAN_BLOOD_SACRIFICE_HEAL_CAP: Int = 10

        val BANDOS_PLAYER_STAT_ORDER =
            listOf(
                "stat.defence",
                "stat.strength",
                "stat.prayer",
                "stat.attack",
                "stat.magic",
                "stat.ranged",
            )
    }
}

/**
 * Mirrors Bandos's drain order. This stays local because the dogsword combines the finished
 * impacts rather than depending on a particular concrete godsword handler.
 */
private fun drainBandosStats(target: PathingEntity, amount: Int) {
    var remaining = amount
    when (target) {
        is Player -> {
            for (stat in DogswordSpecialAttack.BANDOS_PLAYER_STAT_ORDER) {
                val drain = min(target.stat(stat), remaining)
                if (drain > 0) {
                    target.statSub(stat, constant = drain, percent = 0)
                    remaining -= drain
                }
                if (remaining == 0) {
                    return
                }
            }
        }

        is Npc -> {
            fun drain(current: Int, apply: (Int) -> Unit) {
                val value = min(current, remaining)
                if (value > 0) {
                    apply(value)
                    remaining -= value
                }
            }

            drain(target.defenceLvl) { target.defenceLvl -= it }
            drain(target.strengthLvl) { target.strengthLvl -= it }
            drain(target.attackLvl) { target.attackLvl -= it }
            drain(target.magicLvl) { target.magicLvl -= it }
            drain(target.rangedLvl) { target.rangedLvl -= it }
        }
    }
}
