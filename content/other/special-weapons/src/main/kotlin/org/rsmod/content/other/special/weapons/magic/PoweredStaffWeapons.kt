package org.rsmod.content.other.special.weapons.magic

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.CombatChargeManager
import org.rsmod.api.mechanics.toxins.impl.NpcVenom
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.hat
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.api.weapons.MagicWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

class PoweredStaffWeapons
@Inject
constructor(private val charges: CombatChargeManager, private val objCharges: ObjChargeManager) :
    WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for (type in PoweredStaffTypes.ALL) {
            for (obj in type.charged) {
                register(obj, PoweredStaff(manager, charges, objCharges, type, full = false))
            }
            for ((obj, _) in type.alwaysFull) {
                register(obj, PoweredStaff(manager, charges, objCharges, type, full = true))
            }
            for (obj in type.uncharged) {
                register(obj, UnchargedStaff(manager, type))
            }
        }
    }

    private class PoweredStaff(
        private val manager: WeaponAttackManager,
        private val charges: CombatChargeManager,
        private val objCharges: ObjChargeManager,
        private val type: PoweredStaffType,
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
            val varobj = type.varobj
            var chargesLeft = -1
            if (varobj != null) {
                if (full) {
                    val max = type.maxCharges
                    objCharges.addCharges(player.worn, Wearpos.RightHand.slot, max, varobj, max)
                }
                val chargeResult = charges.attemptDetractWeapon(player, varobj)
                if (chargeResult.isFailure()) {
                    manager.stopCombat(this)
                    return
                }
                chargesLeft = chargeResult.chargesLeft
            }

            type.attackDelay?.let { manager.setNextAttackDelay(this, it) }

            val spell = type.spell
            anim(spell.anim)
            spotanim(spell.castSpot, height = spell.castHeight)

            val proj = manager.spawnProjectile(this, target, spell.projectile, spell.projanim)
            val (serverDelay, clientDelay) = proj.durations

            val splash = !type.alwaysHits && manager.rollStaffSplash(this, target, attack)
            if (splash) {
                manager.playSplashFx(this, target, clientDelay, spell.castSound, SOUND_RADIUS)
                manager.queueSplashHit(this, target, clientDelay, serverDelay)
            } else {
                var damage = manager.rollStaffMaxHit(this, target, type.maxHit(player.magicLvl))
                val leech = type.lifeLeech && random.randomBoolean(LEECH_ODDS)
                if (leech) {
                    damage += LEECH_BONUS_DAMAGE
                }
                manager.playMagicHitFx(
                    source = this,
                    target = target,
                    clientDelay = clientDelay,
                    castSound = spell.castSound,
                    soundRadius = SOUND_RADIUS,
                    hitSpot = if (leech) spell.leechImpact else spell.impact,
                    hitSpotHeight = if (leech) spell.leechHeight else spell.impactHeight,
                    hitSound = spell.hitSound,
                )
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, damage, clientDelay, serverDelay)
                if (leech) {
                    statHeal("stat.hitpoints", constant = damage / 2, percent = 0)
                }
                if (type.venom && damage > 0) {
                    tryVenom(target)
                }
            }

            if (varobj != null) {
                if (chargesLeft == 0) {
                    manager.stopCombat(this)
                    mes("Your ${type.name} has run out of charges.")
                    return
                }
                if (type.warnEvery > 0 && chargesLeft % type.warnEvery == 0) {
                    mes("Your ${type.name} has ${chargesLeft.formatAmount} charges left.")
                }
            }
            manager.continueCombat(this, target)
        }

        private fun ProtectedAccess.tryVenom(target: PathingEntity) {
            val serpentine = SERPENTINE_HELMS.any { player.hat.isType(it) }
            if (!(serpentine && target is Npc) && !random.randomBoolean(VENOM_ODDS)) {
                return
            }
            when (target) {
                is Npc -> NpcVenom.tryVenom(target)
                is Player -> PlayerVenom.tryVenom(target)
            }
        }
    }

    private class UnchargedStaff(
        private val manager: WeaponAttackManager,
        private val type: PoweredStaffType,
    ) : MagicWeapon {
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
            mes(type.noChargesMessage)
            manager.stopCombat(this)
        }
    }

    private companion object {
        const val SOUND_RADIUS = 10
        const val LEECH_ODDS = 5
        const val LEECH_BONUS_DAMAGE = 8
        const val VENOM_ODDS = 4
        val SERPENTINE_HELMS =
            arrayOf(
                "obj.serpentine_helm_charged",
                "obj.serpentine_helm_charged_cyan",
                "obj.serpentine_helm_charged_red",
            )
    }
}
