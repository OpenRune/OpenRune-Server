package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.player.PvPAreaAttackManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Powerstab: a normal-accuracy, normal-damage hit on every eligible target in the attacker's
 * surrounding 3x3 area while in multi-combat.
 */
class Dragon2hSwordSpecialAttack
@Inject
constructor(
    private val targets: AreaMeleeTargetSelector,
    private val pvp: PvPAreaAttackManager,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val powerstab = Powerstab(manager, targets, pvp)
        registerMelee("obj.dragon_2h_sword", powerstab)
        registerMelee("obj.br_dragon_2h", powerstab)
        registerMelee("obj.bh_dragon_2h_sword_corrupted", powerstab)
    }

    private class Powerstab(
        private val manager: SpecialAttackManager,
        private val targets: AreaMeleeTargetSelector,
        private val pvp: PvPAreaAttackManager,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = powerstab(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = powerstab(target, attack)

        private fun ProtectedAccess.powerstab(
            primary: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim("seq.dragon_two_handed_sword")
            // Confirmed against a reference implementation of this exact special (Zenyte-based
            // Offline_Scape/Near Reality, POWERSTAB in SpecialAttack.java). Unaliased in this
            // cache's gamevals.
            soundSynth(POWERSTAB_SOUND)
            spotanim("spotanim.dragon_two_handed_sword_blast")

            val affected =
                if (mapMultiway()) {
                    targets.select(
                        source = this,
                        primary = primary,
                        tiles = targets.square(player.coords, radius = 1),
                        npcLimit = MAX_NPC_TARGETS,
                        playerLimit = MAX_PLAYER_TARGETS,
                    )
                } else {
                    listOf(primary)
                }

            for (target in affected) {
                val damage =
                    manager.rollMeleeDamage(
                        source = this,
                        target = target,
                        attack = attack,
                        accuracyMultiplier = 1.0,
                        maxHitMultiplier = 1.0,
                    )
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMeleeHit(this, target, damage)

                if (target is Player && target !== primary) {
                    pvp.applySecondarySpecialAttack(this, target)
                }
            }
            manager.continueCombat(this, primary)
            return true
        }

        private companion object {
            private const val MAX_NPC_TARGETS: Int = 14
            private const val MAX_PLAYER_TARGETS: Int = 3

            /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
            private const val POWERSTAB_SOUND = 2530
        }
    }
}
