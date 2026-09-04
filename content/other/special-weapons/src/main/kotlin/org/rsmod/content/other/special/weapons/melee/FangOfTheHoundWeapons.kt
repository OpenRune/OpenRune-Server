package org.rsmod.content.other.special.weapons.melee

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.soundSynth
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
 * Ordinary attacks (Stab/Lunge/Slash/Block) - the item's own cache-defined bonuses, no bespoke
 * accuracy or damage. The weapon's only unusual behavior outside its special attack (see
 * `FangOfTheHoundSpecialAttack.kt`) is its passive. Wiki: "Every hit with the Fang of the Hound has
 * a 5% chance to cast Flames of Cerberus at the target, which is a fire spell with a base max hit
 * of 10... When the effect is triggered, the magic damage is dealt without needing to pass a second
 * accuracy check."
 *
 * The cache entry for `obj.fang_of_the_hound` originally had no `weaponCategory` at all, which is
 * why the combat-style tab showed Punch/Kick/Block instead of this weapon's real styles - that
 * part can only be fixed in the cache (added a `weaponCategory="StabSword"` override), since the
 * client reads it directly for that tab. The animation itself is still played directly here rather
 * than relying on `WeaponAttackManager.playWeaponFx`'s cache-param lookup.
 */
class FangOfTheHoundWeapons
@Inject
constructor(
    private val random: GameRandom,
) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val fang = FangOfTheHound(manager, random)
        register("obj.fang_of_the_hound", fang)
    }

    private class FangOfTheHound(
        private val manager: WeaponAttackManager,
        private val random: GameRandom,
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
            playSwingFx()

            val accurate =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            val damage =
                if (accurate) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.0,
                    )
                } else {
                    0
                }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            if (accurate && random.of(100) < PASSIVE_PROC_CHANCE_PERCENT) {
                flamesOfCerberus(target)
            }
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.flamesOfCerberus(target: PathingEntity) {
            val damage =
                manager.rollSpellMaxHit(
                    source = this,
                    target = target,
                    spell = FLAMES_OF_CERBERUS,
                    spellbook = Spellbook.Standard,
                    baseMaxHit = FLAMES_BASE_MAX_HIT,
                    attackRate = FANG_ATTACK_RATE,
                )

            // Reported too high at 96 (same blind-copy-from-Dragon-claws pattern as the special
            // attacks) - lowered to ground level per live feedback.
            target.spotanim(
                spot = "spotanim.vfx_flames_of_cerberus",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )
            manager.queueMagicHit(
                source = this,
                target = target,
                damage = damage,
                clientDelay = 0,
                spell = FLAMES_OF_CERBERUS,
            )
        }

        /**
         * The cache entry for `obj.fang_of_the_hound` (a Demonic Pacts League exclusive, same
         * situation as Thunder khopesh) is missing `attack_anim_stance*`/`attack_sound_stance*`
         * params entirely, so `WeaponAttackManager.playWeaponFx` has nothing to read and falls back
         * to the generic unarmed punch/kick. Played directly here instead, with no cache dependency
         * at all - the item's own real animation (`seq.human_karambit_attack`), the non-special
         * counterpart to its special attack's `seq.human_karambit_spec` (same weapon swing, without
         * the special's swipe-up finish). Used for all four stances; gameval has no separate
         * per-style variant, matching how the special attack itself only has the one animation too.
         */
        private fun ProtectedAccess.playSwingFx() {
            player.anim(SWING_ANIM, priority = 6)
            player.soundSynth(SWING_SOUND)
        }
    }

    private companion object {
        const val SWING_ANIM: String = "seq.human_karambit_attack"
        const val SWING_SOUND: Int = 2549
        const val PASSIVE_PROC_CHANCE_PERCENT: Int = 5
        const val FANG_ATTACK_RATE: Int = 3
        const val FLAMES_BASE_MAX_HIT: Int = 10

        // Cache item: obj.fang_of_the_hound_fire (id 33377).
        val FLAMES_OF_CERBERUS: ItemServerType = ItemServerType(33377)
    }
}
