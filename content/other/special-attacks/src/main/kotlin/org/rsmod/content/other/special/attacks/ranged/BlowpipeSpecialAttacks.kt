package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.ranged.RangedAmmunition
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.worn.BlowpipeCharges
import org.rsmod.api.specials.*
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj

class BlowpipeSpecialAttacks @Inject constructor(private val queues: WorldQueueList) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for ((loaded, _) in BlowpipeCharges.variants) registerRanged(loaded, Siphon(manager))
    }
    private inner class Siphon(private val manager: SpecialAttackManager) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged): Boolean = fire(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Ranged): Boolean = fire(target, attack)
        private fun ProtectedAccess.fire(target: PathingEntity, attack: CombatAttack.Ranged): Boolean {
            val weapon = player.worn[Wearpos.RightHand.slot]
            val contents = BlowpipeCharges.read(weapon)
            if (weapon == null || weapon != attack.weapon || !contents.ready) {
                mes("Your blowpipe needs both darts and Zulrah's scales.")
                manager.stopCombat(this)
                return false
            }
            val dart = getInvObj(checkNotNull(contents.ammunition))
            val travel = RSCM.getReverseMapping(RSCMType.SPOTANIM, dart.param(params.proj_travel).id)
            anim("seq.toxic_blowpipe_special_updated")
            val ornament = BlowpipeCharges.variant(weapon) == BlowpipeCharges.variants[1]
            spotanim(if (ornament) "spotanim.toxic_blowpipe_specialattack_league04" else "spotanim.toxic_blowpipe_specialattack", height = 96)
            manager.setNextAttackDelay(this, (if (target is Player) 4 else 3) - if (attack.style == RangedAttackStyle.Rapid) 1 else 0)
            val projectile = manager.spawnProjectile(this, target, travel, "projanim.thrown")
            val damage = manager.rollRangedDamage(this, target, attack, 2.0, 1.5, blockType = RangedAttackType.Light)
            val hit = manager.queueRangedHit(this, target, dart, damage, projectile.clientCycles, projectile.serverCycles)
            manager.giveCombatXp(this, target, attack, hit.damage)
            player.worn[Wearpos.RightHand.slot] = BlowpipeCharges.consume(weapon, RangedAmmunition.conserveAmmo(player, random), random.of(3) != 0)
            val source = player
            val uid = source.uid
            val heal = hit.damage / 2
            if (heal > 0) queues.add(projectile.serverCycles) {
                if (source.isSlotAssigned && source.uid == uid) source.statHeal("stat.hitpoints", heal, 0)
            }
            if (BlowpipeCharges.read(player.worn[Wearpos.RightHand.slot]).ready) manager.continueCombat(this, target)
            else manager.stopCombat(this)
            return true
        }
    }
}
