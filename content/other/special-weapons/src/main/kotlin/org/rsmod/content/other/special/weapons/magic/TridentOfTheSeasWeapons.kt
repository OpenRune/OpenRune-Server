package org.rsmod.content.other.special.weapons.magic

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.CombatChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.weapons.MagicWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class TridentOfTheSeasWeapons
@Inject
constructor(
    private val charges: CombatChargeManager,
    private val objCharges: ObjChargeManager,
) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        register("obj.tots_uncharged", UnchargedTrident(manager))
        register("obj.tots_charged", Trident(manager, charges, objCharges, full = false))
        register("obj.tots", Trident(manager, charges, objCharges, full = true))
    }

    private class Trident(
        private val manager: WeaponAttackManager,
        private val charges: CombatChargeManager,
        private val objCharges: ObjChargeManager,
        private val full: Boolean,
    ) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            cast(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            cast(target, attack)
            return true
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Staff) {
            if (full) {
                objCharges.addCharges(
                    player.worn,
                    Wearpos.RightHand.slot,
                    MAX_CHARGES,
                    CHARGE_VAROBJ,
                    MAX_CHARGES,
                )
            }

            val chargeResult = charges.attemptDetractWeapon(player, CHARGE_VAROBJ)
            if (chargeResult.isFailure()) {
                manager.stopCombat(this)
                return
            }

            anim("seq.human_castwave_staff")
            spotanim("spotanim.slayer_tots_casting")

            val proj =
                manager.spawnProjectile(
                    source = this,
                    target = target,
                    spotanim = "spotanim.slayer_tots_projectile",
                    projanim = "projanim.magic_spell",
                )
            val (serverDelay, clientDelay) = proj.durations

            val splash = manager.rollStaffSplash(this, target, attack)
            if (splash) {
                manager.playSplashFx(this, target, clientDelay, null, soundRadius = 10)
                manager.queueSplashHit(this, target, clientDelay, serverDelay)
            } else {
                val baseMaxHit = getMaxHit(player.magicLvl)
                val damage = manager.rollStaffMaxHit(this, target, baseMaxHit)
                manager.playMagicHitFx(
                    source = this,
                    target = target,
                    clientDelay = clientDelay,
                    castSound = null,
                    soundRadius = 10,
                    hitSpot = "spotanim.slayer_tots_impact",
                    hitSpotHeight = 92,
                    hitSound = HIT_SOUND,
                )
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, damage, clientDelay, serverDelay)
            }

            if (chargeResult.fullyUncharged) {
                manager.stopCombat(this)
                mes("Your Trident of the Seas has run out of charges.")
                return
            }
            manager.continueCombat(this, target)
        }

        private fun getMaxHit(magicLvl: Int): Int = max(1, magicLvl / 3 - 5)
    }

    private class UnchargedTrident(private val manager: WeaponAttackManager) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            terminateAttack()
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            terminateAttack()
            return true
        }

        private fun ProtectedAccess.terminateAttack() {
            mes(
                "Your trident has no charges! You need to charge it with death runes, " +
                    "chaos runes, fire runes and coins."
            )
            manager.stopCombat(this)
        }
    }

    private companion object {
        const val CHARGE_VAROBJ = "varobj.charges_16383"
        const val MAX_CHARGES = 2_500
        const val HIT_SOUND = "synth.contact_darkness_impact"
    }
}
