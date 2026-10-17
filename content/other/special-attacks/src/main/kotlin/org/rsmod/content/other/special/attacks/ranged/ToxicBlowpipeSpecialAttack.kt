package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.ranged.RangedAmmunition
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.toxins.NpcPoisonEffectService
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.hat
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ranged.BlowpipeAmmo
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

/**
 * Toxic Siphon fires one stored dart with doubled accuracy and 150% maximum damage, then restores
 * half of the prospective hit. The heal uses the raw roll so overkill still heals correctly.
 */
class ToxicBlowpipeSpecialAttack
@Inject
constructor(private val random: GameRandom, private val poisons: NpcPoisonEffectService) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val toxicSiphon = ToxicSiphon(manager, random, poisons)
        registerRanged("obj.toxic_blowpipe_loaded", toxicSiphon)
        registerRanged("obj.toxic_blowpipe_loaded_ornament", toxicSiphon)
    }

    private class ToxicSiphon(
        private val manager: SpecialAttackManager,
        private val random: GameRandom,
        private val poisons: NpcPoisonEffectService,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = toxicSiphon(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = toxicSiphon(target, attack)

        private fun ProtectedAccess.toxicSiphon(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val equipped = player.righthand
            val dart = BlowpipeAmmo.loadedDart(equipped)
            if (dart == null || !BlowpipeAmmo.canUseLoadedDart(equipped)) {
                manager.stopCombat(this)
                mes("Your toxic blowpipe has no usable darts loaded.")
                return false
            }
            if (!BlowpipeAmmo.hasScales(equipped, 1)) {
                manager.stopCombat(this)
                mes("Your toxic blowpipe has run out of scales.")
                return false
            }

            val travelSpotanim = dart.type.paramOrNull(params.proj_travel)
            if (travelSpotanim == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return false
            }

            val consumeDart = !RangedAmmunition.conserveAmmo(player, random)
            val consumeScale = !random.randomBoolean(SCALE_CONSERVE_ROLL)

            // `seq.toxic_blowpipe_special_updated` (used further down for the spotanim) is
            // actually the flying dart's own model animation, not a player pose - it has none of
            // the replaceheldright/walkmerge markers a real player seq carries. Toxic Siphon
            // reuses the weapon's ordinary throwing animation for the player instead, same as
            // Rosewood blowpipe's Rapid Burst does with its own dedicated special seq.
            val attackAnim = weaponType.paramOrNull(params.attack_anim_stance1)
            if (attackAnim != null) {
                anim(RSCM.getReverseMapping(RSCMType.SEQ, attackAnim.id))
            }
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }

            // `spotanim.toxic_blowpipe_specialattack` is the special's own flying-dart effect -
            // it replaces the normal dart's travel spotanim for the projectile itself, rather
            // than playing as a separate effect on the player. (Previously played on the player
            // at height=96, which looked like a stray effect floating overhead instead of a
            // projectile heading to the target.)
            //
            // Uses its own dedicated `projanim.toxic_blowpipe_special` type (registered as a real
            // alias in .data/gamevals/projanim.rscm, plus its own projectiles.toml entry) instead
            // of the shared `projanim.thrown` (163/146) every other thrown weapon uses - that
            // looked too high for this effect's own model; a first attempt reusing
            // `projanim.dragonfire`'s values (43/31) was reported too low. Height values here are
            // an unverified visual-tuning guess pending live confirmation.
            val projectile =
                manager.spawnProjectile(
                    source = this,
                    target = target,
                    spotanim = TOXIC_SIPHON_SPOTANIM,
                    projanim = TOXIC_SIPHON_PROJANIM,
                )
            val damage =
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = TOXIC_SIPHON_ACCURACY_MULTIPLIER,
                    maxHitMultiplier = TOXIC_SIPHON_MAX_HIT_MULTIPLIER,
                )
            val heal = ToxicBlowpipeSpecialDamage.heal(damage)
            val guaranteedNpcVenom = target is Npc && EquipmentChecks.isSerpentineHelm(player.hat)
            val venom =
                damage > 0 && (guaranteedNpcVenom || random.randomBoolean(VENOM_ROLL_DENOMINATOR))

            val consumption =
                BlowpipeAmmo.consume(
                    player = player,
                    darts = if (consumeDart) 1 else 0,
                    scales = if (consumeScale) 1 else 0,
                )
            if (consumption == null) {
                manager.stopCombat(this)
                return false
            }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = dart.type,
                damage = damage,
                clientDelay = projectile.clientCycles,
                hitDelay = projectile.serverCycles,
            )
            // Real OSRS doesn't clamp damage after the roll, so the already-known heal/venom
            // outcomes above are authentic - no impact callback needed.
            if (heal > 0 && player.hitpoints > 0) {
                player.statHeal("stat.hitpoints", constant = heal, percent = 0)
            }
            if (venom) {
                when (target) {
                    is Npc -> poisons.applyVenom(player, target)
                    is Player -> PlayerVenom.tryVenom(target)
                }
            }

            if (!consumption.canFire) {
                if (consumption.dartsLeft == 0) {
                    mes("Your toxic blowpipe has run out of darts.")
                } else {
                    mes("Your toxic blowpipe has run out of scales.")
                }
                manager.stopCombat(this)
            } else {
                manager.continueCombat(this, target)
            }
            return true
        }
    }

    private companion object {
        const val SCALE_CONSERVE_ROLL: Int = 3
        const val VENOM_ROLL_DENOMINATOR: Int = 4
        const val TOXIC_SIPHON_ACCURACY_MULTIPLIER: Double = 2.0
        const val TOXIC_SIPHON_MAX_HIT_MULTIPLIER: Double = 1.5

        // Confirmed via the real cache: this spotanim's own embedded "anim" is the flying dart's
        // model animation, not a player pose - no separate player anim() call needed for it.
        const val TOXIC_SIPHON_SPOTANIM: String = "spotanim.toxic_blowpipe_specialattack"
        const val TOXIC_SIPHON_PROJANIM: String = "projanim.toxic_blowpipe_special"
    }
}

internal object ToxicBlowpipeSpecialDamage {
    fun heal(prospectiveDamage: Int): Int = prospectiveDamage.coerceAtLeast(0) / 2
}
