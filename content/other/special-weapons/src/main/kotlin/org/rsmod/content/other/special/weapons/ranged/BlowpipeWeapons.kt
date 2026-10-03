package org.rsmod.content.other.special.weapons.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.WeaponVenom
import org.rsmod.api.combat.commons.ranged.RangedAmmunition
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.BlowpipeCharges
import org.rsmod.api.weapons.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

class BlowpipeWeapons : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for ((loaded, _) in BlowpipeCharges.variants) register(loaded, Blowpipe(manager))
    }
    private class Blowpipe(private val manager: WeaponAttackManager) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged): Boolean = fire(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Ranged): Boolean = fire(target, attack)
        private fun ProtectedAccess.fire(target: PathingEntity, attack: CombatAttack.Ranged): Boolean {
            val item = player.worn[Wearpos.RightHand.slot]
            val contents = BlowpipeCharges.read(item)
            if (item == null || item != attack.weapon || !contents.ready) {
                mes("Your blowpipe needs both darts and Zulrah's scales.")
                manager.stopCombat(this)
                return true
            }
            val dart = getInvObj(checkNotNull(contents.ammunition))
            val travel = RSCM.getReverseMapping(RSCMType.SPOTANIM, dart.param(params.proj_travel).id)
            manager.playWeaponFx(this, attack)
            manager.setNextAttackDelay(this, (if (target is Player) 4 else 3) - if (attack.style == RangedAttackStyle.Rapid) 1 else 0)
            val projectile = manager.spawnProjectile(this, target, travel, "projanim.thrown")
            val damage = manager.rollRangedDamage(this, target, attack, blockType = RangedAttackType.Light)
            // Formula reads the loaded dart before the final dart is removed.
            manager.giveCombatXp(this, target, attack, damage)
            val hit = manager.queueRangedHit(this, target, dart, damage, projectile.clientCycles, projectile.serverCycles)
            WeaponVenom.attach(hit, player, target, random)
            player.worn[Wearpos.RightHand.slot] = BlowpipeCharges.consume(item, RangedAmmunition.conserveAmmo(player, random), random.of(3) != 0)
            if (!BlowpipeCharges.read(player.worn[Wearpos.RightHand.slot]).ready) {
                mes("Your blowpipe has run out of darts or scales.")
                manager.stopCombat(this)
            } else manager.continueCombat(this, target)
            return true
        }
    }
}
