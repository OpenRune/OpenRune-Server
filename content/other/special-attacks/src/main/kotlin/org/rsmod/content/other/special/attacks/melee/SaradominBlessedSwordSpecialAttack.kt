package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Saradomin's Blessed Lightning is Magic-based melee damage: its accuracy is the wielder's Slash
 * attack bonus against Magic defence, while its maximum hit is the normal melee maximum ? 1.25.
 */
class SaradominBlessedSwordSpecialAttack @Inject constructor(private val worldRepo: WorldRepository) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val lightning = BlessedLightning(manager, worldRepo)
        registerMelee("obj.blessed_saradomin_sword", lightning)
        registerMelee("obj.blessed_saradomin_sword_degraded", lightning)
    }

    private class BlessedLightning(
        private val manager: SpecialAttackManager,
        private val worldRepo: WorldRepository,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            lightning(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            lightning(target, attack)
            return true
        }

        private fun ProtectedAccess.lightning(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.blessed_saradomin_sword_special_player")
            // Same reference implementation as the base Saradomin sword (Zenyte-based
            // Offline_Scape, BLESSED_SARADOMINS_LIGHTNING in SpecialAttack.java) - shares the
            // caster glow (1213) and the target's entity-attached lightning (1196, matching the
            // base sword exactly) but adds a third effect the base sword doesn't have: a
            // ground-location graphic at the target's own tile (`godwars_saradomin_light_attk_spot`,
            // 1221, height 0, delay 30) sent via World.sendGraphics to a coord rather than attached
            // to the entity - a genuinely richer effect for the blessed upgrade.
            spotanim(
                spot = "spotanim.dh_sword_update_saradomin_god_special_spotanim",
                height = 0,
                slot = constants.spotanim_slot_combat,
            )
            target.spotanim(
                spot = "spotanim.godwars_saradomin_magic_attack_spotanim",
                height = 0,
                delay = 30,
                slot = constants.spotanim_slot_combat,
            )
            spotanimMap(
                repo = worldRepo,
                internal = "spotanim.godwars_saradomin_light_attk_spot",
                coord = target.coords,
                height = 0,
                delay = 30,
            )

            val successful =
                manager.rollMagicalMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = MeleeAttackType.Slash,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            val damage =
                if (successful) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.25,
                    )
                } else {
                    0
                }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, damage, clientDelay = 0)
            manager.continueCombat(this, target)
        }
    }
}
