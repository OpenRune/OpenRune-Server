package org.rsmod.content.other.special.weapons.magic

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.refs.params
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.api.weapons.MagicWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class BoneStaffWeapons @Inject constructor(private val objCharges: ObjChargeManager) :
    WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        register(BONE_STAFF, BoneStaff(manager, objCharges))
    }

    private class BoneStaff(
        private val manager: WeaponAttackManager,
        private val objCharges: ObjChargeManager,
    ) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            if (target.visType.param(params.rat) == 0) {
                refuse()
                return true
            }
            cast(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            refuse()
            return true
        }

        private fun ProtectedAccess.refuse() {
            mes("You can only use this weapon against rats.")
            manager.stopCombat(this)
        }

        private fun ProtectedAccess.cast(target: Npc, attack: CombatAttack.Staff) {
            val slot = Wearpos.RightHand.slot
            val left = objCharges.getCharges(player.worn[slot], CHARGES_VAROBJ)
            if (left <= 0) {
                mes("Your Bone staff has no charges left.")
                manager.stopCombat(this)
                return
            }
            objCharges.setValue(player.worn, slot, CHARGES_VAROBJ, left - 1)
            if ((left - 1) % WARN_EVERY == 0) {
                mes("Your weapon has ${(left - 1).formatAmount} charges remaining.")
            }

            anim("seq.human_axe_chop")
            spotanim("spotanim.vfx_weapon_staff_rat01_spotanim")
            val proj =
                manager.spawnProjectile(
                    this,
                    target,
                    "spotanim.spells_ratbone01_travel01",
                    "projanim.bone_staff",
                )
            val (serverDelay, clientDelay) = proj.durations

            if (manager.rollStaffSplash(this, target, attack)) {
                manager.playSplashFx(this, target, clientDelay, CAST_SOUND, SOUND_RADIUS)
                manager.queueSplashHit(this, target, clientDelay, serverDelay)
            } else {
                val baseMaxHit = player.magicLvl / 3 + MAX_HIT_BONUS + RAT_BONUS
                val damage = manager.rollStaffMaxHit(this, target, baseMaxHit)
                manager.playMagicHitFx(
                    source = this,
                    target = target,
                    clientDelay = clientDelay,
                    castSound = CAST_SOUND,
                    soundRadius = SOUND_RADIUS,
                    hitSpot = "spotanim.vfx_weapon_staff_rat01_impact_01",
                    hitSpotHeight = IMPACT_HEIGHT,
                    hitSound = null,
                )
                statAdvance("stat.magic", BASE_XP)
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, damage, clientDelay, serverDelay)
            }
            manager.continueCombat(this, target)
        }
    }

    companion object {
        const val BONE_STAFF = "obj.rat_bone_staff"
        const val CHARGES_VAROBJ = PoweredStaffTypes.CHARGES_15_BIT
        const val MAX_CHARGES = 20_000
        private const val WARN_EVERY = 100
        private const val CAST_SOUND = "synth.rat_boss_bone_staff_defend_1"
        private const val SOUND_RADIUS = 10
        private const val IMPACT_HEIGHT = 124
        private const val MAX_HIT_BONUS = 5
        private const val RAT_BONUS = 10
        private const val BASE_XP = 35.0
    }
}
