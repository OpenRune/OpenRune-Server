package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.styles.MagicAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.accuracy.melee.MeleeAgainstMagicAccuracy
import org.rsmod.api.config.constants
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class SaradominSwordSpecialAttacks @Inject constructor(
    private val magicAccuracy: MeleeAgainstMagicAccuracy,
    private val specialDamage: MeleeSpecialDamage,
    private val rng: GameRandom,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.saradomin_sword", Lightning(manager, false))
        val blessed = Lightning(manager, true)
        registerMelee("obj.blessed_saradomin_sword", blessed)
        registerMelee("obj.blessed_saradomin_sword_degraded", blessed)
    }

    private inner class Lightning(private val manager: SpecialAttackManager, private val blessed: Boolean) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = perform(target, attack)

        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim(if (blessed) BLESSED_ANIMATION else ANIMATION)
            spotanim(CASTER_EFFECT, height = 0, slot = constants.spotanim_slot_combat)
            val accurate = if (blessed) magicAccuracy.roll(player, target, attack) else
                manager.rollMeleeAccuracy(this, target, attack.type, attack.style, MeleeAttackType.Slash, 1.0)
            if (blessed) {
                val maximum = specialDamage.maximum(player, target, attack, 125, deferReductions = true).coerceAtLeast(1)
                var amount = if (!accurate) 0 else if (player.adminMaxHit) maximum else rng.of(1, maximum)
                // This is magical melee: only accuracy uses magic defence. Prayers and immunities
                // must still see a melee hit (including Warriors' Guild cyclopes).
                amount = specialDamage.modifyRolledHit(player, target, attack, amount)
                val hit = manager.queueMeleeHit(this, target, amount, 1)
                manager.giveCombatXp(this, target, attack, hit.damage)
            } else {
                val amount = if (accurate) manager.rollMeleeMaxHit(this, target, attack.type, attack.style, 1.1).coerceAtLeast(1) else 0
                val melee = manager.queueMeleeHit(this, target, amount, 1)
                val protectedFromMagic = target is Player && target.vars["varbit.prayer_protectfrommagic"] != 0
                var magicDamage = if (accurate && !protectedFromMagic) { if (player.adminMaxHit) 16 else rng.of(1, 16) } else 0
                if (target is Player) magicDamage = specialDamage.reduce(target, magicDamage)
                val magic = manager.queueMagicHit(this, target, magicDamage, clientDelay = 30, hitDelay = 1)
                manager.giveCombatXp(this, target, attack, melee.damage)
                manager.giveCombatXp(this, target, CombatAttack.Staff(requireNotNull(attack.weapon), MagicAttackStyle.Accurate), magic.damage)
            }
            if (accurate) target.spotanim(TARGET_EFFECT, delay = 30, height = 0)
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        const val ANIMATION = "seq.saradomin_sword_special_player"
        const val BLESSED_ANIMATION = "seq.blessed_saradomin_sword_special_player"
        const val CASTER_EFFECT = "spotanim.dh_sword_update_saradomin_special_spotanim"
        const val TARGET_EFFECT = "spotanim.godwars_saradomin_sword_attack_spot"
    }
}
