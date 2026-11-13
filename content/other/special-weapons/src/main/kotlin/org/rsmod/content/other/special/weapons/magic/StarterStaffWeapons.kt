package org.rsmod.content.other.special.weapons.magic

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.weapons.MagicWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class StarterStaffWeapons @Inject constructor(private val objCharges: ObjChargeManager) :
    WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        register(STARTER_STAFF, StarterStaff(manager, objCharges))
    }

    private class StarterStaff(
        private val manager: WeaponAttackManager,
        private val objCharges: ObjChargeManager,
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
            mes("Starter weapons cannot be used against other players.")
            manager.stopCombat(this)
            return true
        }

        private fun ProtectedAccess.cast(target: Npc, attack: CombatAttack.Staff) {
            val slot = Wearpos.RightHand.slot
            val used = objCharges.getCharges(player.worn[slot], USED_VAROBJ) + 1
            objCharges.setValue(player.worn, slot, USED_VAROBJ, used)

            anim("seq.human_caststrike_staff")
            spotanim("spotanim.firestrike_casting", height = CAST_HEIGHT)
            val proj =
                manager.spawnProjectile(
                    this,
                    target,
                    "spotanim.firestrike_travel",
                    "projanim.magic_spell",
                )
            val (serverDelay, clientDelay) = proj.durations

            if (manager.rollStaffSplash(this, target, attack)) {
                manager.playSplashFx(this, target, clientDelay, CAST_SOUND, SOUND_RADIUS)
                manager.queueSplashHit(this, target, clientDelay, serverDelay)
            } else {
                val damage = manager.rollStaffMaxHit(this, target, MAX_HIT)
                manager.playMagicHitFx(
                    source = this,
                    target = target,
                    clientDelay = clientDelay,
                    castSound = CAST_SOUND,
                    soundRadius = SOUND_RADIUS,
                    hitSpot = "spotanim.firestrike_impact",
                    hitSpotHeight = IMPACT_HEIGHT,
                    hitSound = "synth.firestrike_hit",
                )
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, damage, clientDelay, serverDelay)
            }

            if (used >= MAX_CASTS) {
                player.worn[slot] = null
                manager.stopCombat(this)
                mes("Your starter staff has fallen apart!")
                return
            }
            manager.continueCombat(this, target)
        }
    }

    companion object {
        const val STARTER_STAFF = "obj.deadman_starter_staff"
        const val USED_VAROBJ = PoweredStaffTypes.CHARGES_13_BIT
        const val MAX_CASTS = 500
        private const val MAX_HIT = 8
        private const val CAST_HEIGHT = 92
        private const val IMPACT_HEIGHT = 124
        private const val CAST_SOUND = "synth.firestrike_cast_and_fire"
        private const val SOUND_RADIUS = 10
    }
}
