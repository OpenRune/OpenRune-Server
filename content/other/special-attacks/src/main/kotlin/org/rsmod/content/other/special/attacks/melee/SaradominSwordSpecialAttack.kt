package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Saradomin's Lightning is a Slash-defence melee hit with 10% extra max damage plus an
 * accuracy-dependent 1?16 Magic hit. The extra Magic hit deliberately splashes through Protect
 * from Magic rather than receiving the usual partial PvP prayer reduction.
 */
class SaradominSwordSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.saradomin_sword", SaradominLightning(manager))
    }

    private class SaradominLightning(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
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
            anim("seq.saradomin_sword_special_player")
            // Was only playing `saradomin_lightning` (76) on the target - confirmed via a real
            // packet capture that's genuinely all this ever sent, no caster effect at all. Found
            // a real reference implementation of this exact weapon's special (Zenyte-based
            // Offline_Scape, SARADOMINS_LIGHTNING in SpecialAttack.java) with both graphics:
            // - Caster: `new Graphics(1213)` passed into the special's own declaration, applied
            //   automatically to the player by the generic combat framework before the per-weapon
            //   handler runs (PlayerCombat.java: `player.setGraphics(special.getGraphics())`).
            //   1213's real name is `dh_sword_update_saradomin_god_special_spotanim` - purpose-
            //   built for this exact special, not reused from anywhere else. height/delay default
            //   to 0 (Graphics(id) alone means Graphics(id, delay=0, height=0)).
            // - Target: `new Graphics(1196, 30, 0)` (id, delay, height) - confirms the 1196 id
            //   already found live, but height 0 (not 96) with a 30-client-cycle delay before it
            //   plays, timed to the swing's impact point rather than instantly.
            // `saradomin_lightning` (76) isn't used by this weapon's real special at all - the
            // earlier "confirmed self-applied" evidence was a real but coincidental reuse of that
            // id elsewhere (digging, an unrelated NPC), not this weapon.
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

            val successful =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Slash,
                    multiplier = 1.0,
                )
            val meleeDamage =
                if (successful) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.1,
                    )
                } else {
                    0
                }
            // The extra spell is conditional on accuracy, not the first hitsplat being non-zero.
            val magicDamage =
                if (successful && !target.protectsFromMagic()) {
                    random.of(MAGIC_DAMAGE_RANGE)
                } else {
                    0
                }
            val source = player

            manager.giveCombatXp(this, target, attack, meleeDamage)
            manager.queueMeleeHit(this, target, meleeDamage)
            manager.queueMagicHit(source = this, target = target, damage = magicDamage, clientDelay = 0)
            // Real OSRS doesn't clamp damage after the roll, so the already-known magicDamage
            // here is the authentic value for the bonus xp - no impact callback needed.
            if (magicDamage > 0) {
                source.statAdvance("stat.magic", magicDamage * MAGIC_XP_PER_DAMAGE)
            }
            manager.continueCombat(this, target)
        }

        private companion object {
            val MAGIC_DAMAGE_RANGE = 1..16
            const val MAGIC_XP_PER_DAMAGE = 2.0
        }
    }
}

private fun PathingEntity.protectsFromMagic(): Boolean =
    this is Player && vars["varbit.prayer_protectfrommagic"] == 1
