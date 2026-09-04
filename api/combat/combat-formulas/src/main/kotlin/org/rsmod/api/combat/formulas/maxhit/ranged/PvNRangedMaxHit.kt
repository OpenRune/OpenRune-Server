package org.rsmod.api.combat.formulas.maxhit.ranged

import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import java.util.EnumSet
import kotlin.math.max
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.formulas.attributes.CombatNpcAttributes
import org.rsmod.api.combat.formulas.attributes.CombatRangedAttributes
import org.rsmod.api.combat.formulas.attributes.collector.CombatNpcAttributeCollector
import org.rsmod.api.combat.formulas.attributes.collector.CombatRangedAttributeCollector
import org.rsmod.api.combat.formulas.isSlayerTask
import org.rsmod.api.combat.maxhit.player.PlayerRangedMaxHit
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.ranged.BlowpipeAmmo
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public class PvNRangedMaxHit
@Inject
constructor(
    private val bonuses: WornBonuses,
    private val weaponSpeeds: WeaponSpeeds,
    private val npcAttributes: CombatNpcAttributeCollector,
    private val rangedAttributes: CombatRangedAttributeCollector,
) {
    private var Player.maxHit by intVarp("varp.com_maxhit")

    /**
     * Computes the maximum ranged hit for [player] against [target].
     *
     * **Notes:**
     * - This function should be used instead of [computeMaxHit] in most cases to ensure consistency
     *   in max hit calculations. Future optimizations may depend on this function as the main entry
     *   point.
     * - The `com_maxhit` varp for [player] is updated with the computed max hit.
     *
     * @param boltSpecDamage The additive bonus damage from bolt proc special attacks. For example,
     *   Opal bolts (e) special should set this value to `visible ranged level * 10%, rounded down`.
     */
    public fun getMaxHit(
        player: Player,
        target: Npc,
        attackType: RangedAttackType?,
        attackStyle: RangedAttackStyle?,
        specialMultiplier: Double,
        boltSpecDamage: Int,
    ): Int {
        val targetType = target.visType
        val targetMagic = max(target.magicLvl, targetType.param(params.attack_magic))
        val maxHit =
            computeMaxHit(
                source = player,
                target = targetType,
                targetCurrHp = target.hitpoints,
                targetMaxHp = target.baseHitpointsLvl,
                targetMagic = targetMagic,
                attackType = attackType,
                attackStyle = attackStyle,
                specialMultiplier = specialMultiplier,
                boltSpecDamage = boltSpecDamage,
            )
        player.maxHit = maxHit
        return maxHit
    }

    public fun computeMaxHit(
        source: Player,
        target: NpcServerType,
        targetCurrHp: Int,
        targetMaxHp: Int,
        targetMagic: Int,
        attackType: RangedAttackType?,
        attackStyle: RangedAttackStyle?,
        specialMultiplier: Double,
        boltSpecDamage: Int,
    ): Int {
        val rangeAttributes = rangedAttributes.collect(source, attackType, attackStyle)

        val slayerTask = target.isSlayerTask(source)
        val npcAttributes = npcAttributes.collect(target, targetCurrHp, targetMaxHp, slayerTask)

        val modifiedDamage =
            computeModifiedDamage(source, targetMagic, attackStyle, rangeAttributes, npcAttributes)
        val specMaxHit = (modifiedDamage * specialMultiplier).toInt()
        return modifyPostSpec(source, specMaxHit, boltSpecDamage, rangeAttributes, npcAttributes)
    }

    public fun computeModifiedDamage(
        source: Player,
        targetMagic: Int,
        attackStyle: RangedAttackStyle?,
        rangeAttributes: EnumSet<CombatRangedAttributes>,
        npcAttributes: EnumSet<CombatNpcAttributes>,
    ): Int {
        // Eclipse atlatl: wiki-confirmed unique mechanic - damage is computed off melee strength
        // instead of ranged strength, even though accuracy (rolled elsewhere) stays ranged-based.
        val baseDamage =
            if (EquipmentChecks.isEclipseAtlatl(source.righthand)) {
                EclipseAtlatlMaxHit.computeBaseDamage(source, bonuses)
            } else {
                val effectiveRanged =
                    RangedMaxHitOperations.calculateEffectiveRanged(source, attackStyle)
                // Toxic/rosewood blowpipe darts are packed inside the weapon's own vars, not
                // worn in the quiver, so the standard equipment-bonus scan never sees the loaded
                // dart's own ranged strength - add it back in explicitly. A no-op (0) for every
                // other weapon, since `loadedDart` returns null unless `source.righthand` is a
                // real blowpipe with a dart loaded.
                val rangedBonus =
                    bonuses.rangedStrengthBonus(source) +
                        BlowpipeAmmo.rangedStrengthBonus(source.righthand)
                PlayerRangedMaxHit.calculateBaseDamage(effectiveRanged, rangedBonus)
            }
        return RangedMaxHitOperations.modifyBaseDamage(
            baseDamage = baseDamage,
            targetMagic = targetMagic,
            rangeAttributes = rangeAttributes,
            npcAttributes = npcAttributes,
        )
    }

    public fun modifyPostSpec(
        source: Player,
        modifiedDamage: Int,
        boltSpecDamage: Int,
        rangeAttributes: EnumSet<CombatRangedAttributes>,
        npcAttributes: EnumSet<CombatNpcAttributes>,
    ): Int {
        val attackRate = weaponSpeeds.actual(source)
        return RangedMaxHitOperations.modifyPostSpec(
            modifiedDamage = modifiedDamage,
            boltSpecDamage = boltSpecDamage,
            attackRate = attackRate,
            rangeAttributes = rangeAttributes,
            npcAttributes = npcAttributes,
        )
    }
}
