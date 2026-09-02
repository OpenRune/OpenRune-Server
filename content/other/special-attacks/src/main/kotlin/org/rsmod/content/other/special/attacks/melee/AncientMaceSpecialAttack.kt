package org.rsmod.content.other.special.attacks.melee

import kotlin.math.max
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.player.disablePrayers
import org.rsmod.api.player.hit.modifier.BypassProtectionPrayerPlayerHitModifier
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Favour of the War God uses a normal melee roll, but ignores a player's Protect from Melee.
 * Its prayer effects use the successful raw damage roll: this deliberately permits restoration
 * from attacks on otherwise damage-immune NPCs, matching the live game's behaviour.
 */
class AncientMaceSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.ancient_goblin_mace", FavourOfTheWarGod(manager))
    }

    private class FavourOfTheWarGod(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            favourOfTheWarGod(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            favourOfTheWarGod(target, attack)
            return true
        }

        private fun ProtectedAccess.favourOfTheWarGod(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.slice_player_mace_special_attack")
            spotanim(
                spot = "spotanim.slice_player_mace_special_attack_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val rawDamage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.0,
                    maxHitMultiplier = 1.0,
                )
            val source = player
            manager.giveCombatXp(this, target, attack, rawDamage)
            manager.queueMeleeHit(
                source = this,
                target = target,
                damage = rawDamage,
                modifier = BypassProtectionPrayerPlayerHitModifier,
            )
            // Real OSRS doesn't clamp damage after the roll, so the already-known pre-clamp
            // rawDamage is the authentic value for both prayer effects - no impact callback
            // needed to observe it after the fact.
            if (rawDamage > 0) {
                restorePrayer(source, rawDamage)
                (target as? Player)?.let { targetPlayer -> drainPrayer(targetPlayer, rawDamage) }
            }
            manager.continueCombat(this, target)
        }

        private fun restorePrayer(source: Player, damage: Int) {
            val current = source.stat("stat.prayer")
            val base = source.statBase("stat.prayer")
            val restored =
                if (current < base) {
                    current + damage
                } else {
                    max(current, base + damage)
                }
            val added = restored - current
            if (added > 0) {
                source.statAdd("stat.prayer", constant = added, percent = 0)
            }
        }

        private fun drainPrayer(target: Player, damage: Int) {
            target.statSub("stat.prayer", constant = damage, percent = 0)
            if (target.prayerLvl == 0) {
                target.rebuildAppearance()
                target.disablePrayers()
            }
        }
    }
}
