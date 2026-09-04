package org.rsmod.content.other.special.weapons.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.mechanics.toxins.NoxiousHalberdVirulence
import org.rsmod.api.mechanics.toxins.NpcPoisonEffectService
import org.rsmod.api.mechanics.toxins.ToxinImmunity
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.weapons.MeleeWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Wiki: "33% chance (50% with a Serpentine helm equipped) to envenom the target (if not venom
 * immune)". Also consumes [NoxiousHalberdVirulence]'s pending minimum-hit boost on the next
 * accurate hit - the special attack (`NoxiousHalberdSpecialAttack.kt`) already set that buff
 * correctly, but nothing ever consumed it, since this weapon had no normal-attack registration at
 * all before this fix (live-testing turned this up).
 *
 * Known gap: real Virulence loses its buff on weapon-switch/logout; this doesn't implement that,
 * since there's no generic "on weapon change" hook anywhere in this codebase to attach it to.
 * `NoxiousHalberdVirulence.consume` only ever gets called from this weapon's own attack code
 * though, so the buff can't leak onto a different weapon - it can only linger unconsumed if the
 * player stops using this halberd, which is a much smaller gap than a real exploit.
 */
class NoxiousHalberdWeapons
@Inject
constructor(
    private val random: GameRandom,
    private val poisons: NpcPoisonEffectService,
) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val halberd = NoxiousHalberd(manager, random, poisons)
        register("obj.noxious_halberd", halberd)
        register("obj.br_noxious_halberd", halberd)
    }

    private class NoxiousHalberd(
        private val manager: WeaponAttackManager,
        private val random: GameRandom,
        private val poisons: NpcPoisonEffectService,
    ) : MeleeWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = swing(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = swing(target, attack)

        private fun ProtectedAccess.swing(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            manager.playWeaponFx(this, attack)

            val accurate =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            val damage = if (accurate) rolledDamage(target, attack) else 0

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)

            if (accurate && random.of(100) < envenomChancePercent()) {
                envenomTarget(target)
            }

            manager.continueCombat(this, target)
            return true
        }

        /**
         * Wiki: on Virulence's pending boost, "the next accurate attack will do a damage roll
         * between [the cured damage] and their max hit" - a true uniform roll across that whole
         * range, not the normal `rollMeleeMaxHit` roll floored up to the minimum (which would
         * bias the distribution towards the minimum instead).
         */
        private fun ProtectedAccess.rolledDamage(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Int {
            val minimum = NoxiousHalberdVirulence.consume(player) ?: return normalMaxHit(target, attack)
            val maxHit = manager.calculateMeleeMaxHit(this, target, attack.type, attack.style, 1.0)
            return if (minimum >= maxHit) maxHit else random.of(minimum..maxHit)
        }

        private fun ProtectedAccess.normalMaxHit(target: PathingEntity, attack: CombatAttack.Melee): Int =
            manager.rollMeleeMaxHit(this, target, attack.type, attack.style, 1.0)

        private fun ProtectedAccess.envenomChancePercent(): Int = if (hasSerpentineHelm()) 50 else 33

        private fun ProtectedAccess.hasSerpentineHelm(): Boolean =
            "obj.serpentine_helm_charged" in player.worn ||
                "obj.serpentine_helm_charged_cyan" in player.worn ||
                "obj.serpentine_helm_charged_red" in player.worn

        private fun ProtectedAccess.envenomTarget(target: PathingEntity) {
            when (target) {
                is Npc -> poisons.applyVenom(player, target)
                is Player -> {
                    if (!ToxinImmunity.hasVenomImmunity(target)) {
                        PlayerVenom.tryVenom(target)
                    }
                }
            }
        }
    }
}
