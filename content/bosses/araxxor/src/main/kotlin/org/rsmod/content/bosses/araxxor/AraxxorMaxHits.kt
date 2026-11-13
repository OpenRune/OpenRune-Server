package org.rsmod.content.bosses.araxxor

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType

private const val NOXIOUS_HALBERD = "obj.noxious_halberd"
private val HEAVY_RANGED_CATEGORIES = listOf("category.crossbow", "category.ballista")

@Singleton
class AraxxorMaxHits
@Inject
constructor(
    private val deps: BossDeps,
    private val bonuses: WornBonuses,
    private val styles: AttackStyles,
) {
    fun guarantee(minion: Npc, hit: HitBuilder, attacker: Player) {
        val maxHit =
            when (hit.type) {
                HitType.Melee -> meleeMaxHit(minion, hit, attacker)
                HitType.Ranged -> rangedMaxHit(minion, hit, attacker)
                else -> null
            } ?: return
        hit.damage = maxHit
    }

    private fun meleeMaxHit(minion: Npc, hit: HitBuilder, attacker: Player): Int? {
        val stab = bonuses.offensiveStabBonus(attacker)
        val slash = bonuses.offensiveSlashBonus(attacker)
        val crush = bonuses.offensiveCrushBonus(attacker)
        val type =
            when {
                crush > stab && crush > slash -> MeleeAttackType.Crush
                slash >= stab -> MeleeAttackType.Slash
                else -> MeleeAttackType.Stab
            }
        val halberd = hit.righthandType()?.isType(NOXIOUS_HALBERD) == true
        if (!halberd && type != MeleeAttackType.Crush) return null
        val style = MeleeAttackStyle.from(styles.get(attacker))
        return deps.maxHit.getMeleeMaxHit(attacker, minion, type, style, 1.0)
    }

    private fun rangedMaxHit(minion: Npc, hit: HitBuilder, attacker: Player): Int? {
        val weapon = hit.righthandType() ?: return null
        if (HEAVY_RANGED_CATEGORIES.none { weapon.isCategoryType(it) }) return null
        val style = RangedAttackStyle.from(styles.get(attacker))
        return deps.maxHit.getRangedMaxHit(attacker, minion, RangedAttackType.Heavy, style, 1.0, 0)
    }
}
