package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * Liquify is an underwater-only attack with double accuracy. On a positive final hit, it raises
 * the user's Attack, Strength, and Defence by 25% of the damage dealt, capped at
 * base level + 3 + 10% of the base level for each stat.
 */
class BrineSabreSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.olaf2_brine_sabre", Liquify(manager))
    }

    private class Liquify(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = liquify(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = liquify(target, attack)

        private fun ProtectedAccess.liquify(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            if (!BrineSabreUnderwaterAreas.contains(coords)) {
                mes("You can only use this special attack underwater.")
                return false
            }

            anim("seq.olaf2_brine_sabre_special")
            // Sourced from this item's own wiki "Sound effects" table
            // ("brain_special_brine_saber"). Unaliased in this cache's gamevals.
            soundSynth(LIQUIFY_SOUND)
            spotanim(
                spot = "spotanim.olaf2_brine_sabre_special_spot",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 2.0,
                    maxHitMultiplier = 1.0,
                    blockType = MeleeAttackType.Stab,
                )
            val source = player
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value for the stat boost - no impact callback needed.
            if (damage > 0) {
                boostLiquifyStats(source, damage)
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val LIQUIFY_SOUND = 3473
    }
}

private fun boostLiquifyStats(
    source: Player,
    damage: Int,
) {
    for (stat in BrineSabreLiquify.BOOSTED_STATS) {
        val added =
            BrineSabreLiquify.levelsToAdd(
                base = source.statBase(stat),
                current = source.stat(stat),
                damage = damage,
            )
        if (added > 0) {
            source.statAdd(stat, constant = added, percent = 0)
        }
    }
}

internal object BrineSabreLiquify {
    internal val BOOSTED_STATS =
        listOf(
            "stat.attack",
            "stat.strength",
            "stat.defence",
        )

    fun levelsToAdd(
        base: Int,
        current: Int,
        damage: Int,
    ): Int {
        val damageBoost = damage / 4
        if (damageBoost <= 0) {
            return 0
        }
        val cap = base + 3 + (base / 10)
        return (minOf(cap, current + damageBoost) - current).coerceAtLeast(0)
    }
}

/**
 * The cache does not expose a general-purpose underwater area flag, so this remains deliberately
 * isolated instead of making Liquify usable everywhere. It covers the revision's mapped static
 * underwater regions (Mogre Camp, Harmony Island's underwater tunnel, and Fossil Island's
 * underwater area). The Rum Deal 'Rum'-geon instance is not present in OpenRune's map
 * configuration; it is intentionally not guessed here and should be added when that instance is
 * loaded.
 */
internal object BrineSabreUnderwaterAreas {
    fun contains(coords: CoordGrid): Boolean {
        val regionX = coords.x ushr 6
        val regionZ = coords.z ushr 6
        return when (coords.level) {
            0 -> regionZ == FOSSIL_ISLAND_REGION_Z && regionX in FOSSIL_ISLAND_REGION_X
            1 ->
                (regionX == MOGRE_CAMP_REGION_X && regionZ == MOGRE_CAMP_REGION_Z) ||
                    (regionX == HARMONY_TUNNEL_REGION_X && regionZ == HARMONY_TUNNEL_REGION_Z) ||
                    (regionZ == FOSSIL_ISLAND_REGION_Z && regionX in FOSSIL_ISLAND_REGION_X)
            else -> false
        }
    }

    private const val MOGRE_CAMP_REGION_X: Int = 46
    private const val MOGRE_CAMP_REGION_Z: Int = 148
    private const val HARMONY_TUNNEL_REGION_X: Int = 59
    private const val HARMONY_TUNNEL_REGION_Z: Int = 144
    private val FOSSIL_ISLAND_REGION_X: IntRange = 58..59
    private const val FOSSIL_ISLAND_REGION_Z: Int = 160
}
