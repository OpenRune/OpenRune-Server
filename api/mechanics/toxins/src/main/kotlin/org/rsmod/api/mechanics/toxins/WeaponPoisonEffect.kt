package org.rsmod.api.mechanics.toxins

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Weapon poison - (p)/(p+)/(p++) melee weapons (daggers, spears, hastae) and ranged ammo (arrows,
 * bolts, darts, javelins, knives) get a chance to poison the target on a successful hit.
 *
 * Wiki-verified (Weapon poison / Weapon poison(+) / Weapon poison(++)): melee attacks apply
 * poison at a 25% chance (1/4), ranged at 12.5% (1/8) - both only on a successful (non-zero
 * damage) hit. Initial poison damage is 4/5/6 (melee) or 2/3/4 (ranged) depending on tier - the
 * existing [PlayerPoison]/[NpcPoisonEffectService] severity math (unchanged, already fixed to not
 * hit instantly) handles everything past that point identically to any other poison source.
 *
 * Tier is read directly off an RSCM alias suffix ("_p"/"_p+"/"_p++") rather than a new
 * `items.toml` param - the real cache already carries this consistently across every poisoned
 * melee weapon and ranged ammo variant, so no per-item toml edits are needed to cover them. For
 * melee this is the wielded weapon itself; for ranged it's whichever item was actually consumed
 * as ammo (quiver ammo for bows/crossbows, or the wielded item itself for thrown weapons like
 * darts/knives/javelins) - the tier lives on the ammo, not the bow/crossbow firing it.
 */
public class WeaponPoisonEffect
@Inject
constructor(
    private val npcPoison: NpcPoisonEffectService,
    private val random: GameRandom,
) {
    public fun rollOnMeleeHit(
        source: Player,
        target: PathingEntity,
        weapon: ItemServerType,
        damage: Int,
    ) {
        if (damage <= 0) return
        val tier = WeaponPoisonTier.of(weapon.internalName) ?: return
        if (!random.randomBoolean(WeaponPoisonTier.MELEE_CHANCE_DENOMINATOR)) return
        apply(source, target, tier.meleeDamage)
    }

    public fun rollOnRangedHit(
        source: Player,
        target: PathingEntity,
        ammo: ItemServerType?,
        damage: Int,
    ) {
        if (damage <= 0 || ammo == null) return
        val tier = WeaponPoisonTier.of(ammo.internalName) ?: return
        if (!random.randomBoolean(WeaponPoisonTier.RANGED_CHANCE_DENOMINATOR)) return
        apply(source, target, tier.rangedDamage)
    }

    private fun apply(source: Player, target: PathingEntity, initialDamage: Int) {
        when (target) {
            is Npc -> npcPoison.apply(source, target, initialDamage)
            is Player -> PlayerPoison.tryPoison(target, initialDamage = initialDamage)
        }
    }
}

/** Pure alias-to-tier lookup, kept separate from [WeaponPoisonEffect] so it's testable without DI. */
internal enum class WeaponPoisonTier(val meleeDamage: Int, val rangedDamage: Int) {
    Regular(meleeDamage = 4, rangedDamage = 2),
    Plus(meleeDamage = 5, rangedDamage = 3),
    PlusPlus(meleeDamage = 6, rangedDamage = 4);

    companion object {
        const val MELEE_CHANCE_DENOMINATOR: Int = 4
        const val RANGED_CHANCE_DENOMINATOR: Int = 8

        fun of(weaponAlias: String?): WeaponPoisonTier? {
            if (weaponAlias == null) return null
            return when {
                weaponAlias.endsWith("_p++") -> PlusPlus
                weaponAlias.endsWith("_p+") -> Plus
                weaponAlias.endsWith("_p") -> Regular
                else -> null
            }
        }
    }
}
