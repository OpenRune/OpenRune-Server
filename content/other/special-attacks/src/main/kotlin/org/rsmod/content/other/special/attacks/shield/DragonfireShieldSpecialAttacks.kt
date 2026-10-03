package org.rsmod.content.other.special.attacks.shield

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.DragonfireProtection
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.worn.DragonfireShields
import org.rsmod.api.player.worn.DragonfireShields.Kind
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.ShieldSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class DragonfireShieldSpecialAttacks @Inject constructor(private val bonuses: WornBonuses) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val special = ShieldSpecialAttack { access, target -> access.discharge(target, manager) }
        for (kind in Kind.entries) {
            registerShield(kind.charged, special)
            registerShield(kind.uncharged, special)
        }
    }

    private fun ProtectedAccess.discharge(target: PathingEntity, manager: SpecialAttackManager) {
        val slot = Wearpos.LeftHand.slot
        val shield = player.worn[slot]
        val kind = DragonfireShields.kind(shield)
        val charges = DragonfireShields.charges(shield)
        if (shield == null || kind == null || charges == 0) {
            mes("Your shield has no charges.")
            manager.continueCombat(this, target)
            return
        }
        val now = ShieldCooldown.now()
        val remaining = ShieldCooldown.remaining(player.shieldReadyEpoch, now)
        if (remaining > 0) {
            mes("Your shield is still cooling down ($remaining seconds).")
            manager.continueCombat(this, target)
            return
        }
        player.worn[slot] = DragonfireShields.withCharges(shield, charges - 1)
        player.shieldReadyEpoch = now + ShieldCooldown.SECONDS
        manager.setNextAttackDelay(this, 5)

        val icy = kind == Kind.WYVERN
        val animation = when (kind) {
            Kind.SHIELD -> "seq.qip_dragon_slayer_player_unleashing_fire"
            Kind.WARD -> "seq.dragonfire_ward_releasing_charge"
            Kind.WYVERN -> "seq.fossil_wyvern_shield_release_charge"
        }
        anim(animation)
        spotanim(if (icy) "spotanim.fossil_wyvern_shield_attack_spotanim" else "spotanim.qip_dragon_slayer_shield_attack_spotanim")
        val projectile = manager.spawnProjectile(
            this, target,
            if (icy) "spotanim.wyvern_skeleton_travel_breath_ancient" else "spotanim.qip_dragon_slayer_player_projanim",
            "projanim.dragonfire",
        )
        val maxHit = if (icy) 15 else 25
        val blocked = when (target) {
            is Npc -> target.visType.param(params.draconic) != 0
            is Player -> DragonfireProtection.blocksShieldBlast(target)
        }
        val defenceLevel = when (target) {
            is Npc -> target.magicLvl
            is Player -> (target.stat("stat.magic") * 7 + target.stat("stat.defence") * 3) / 10
        }
        val defenceBonus = when (target) {
            is Npc -> target.visType.param(params.defence_magic)
            is Player -> bonuses.defensiveMagicBonus(target)
        }
        val attackRoll = (player.stat("stat.defence") + 8) * 64
        val defenceRoll = (defenceLevel + 9) * (defenceBonus + 64).coerceAtLeast(0)
        val accurate = !blocked && random.of(attackRoll + 1) > random.of(defenceRoll + 1)
        val damage = if (accurate) random.of(maxHit + 1) else 0
        target.spotanim(
            if (icy) "spotanim.wyvern_skeleton_impact_breath" else "spotanim.qip_dragon_slayer_player_impact",
            delay = projectile.clientCycles,
        )
        manager.queueMagicHit(this, target, damage, projectile.clientCycles, projectile.serverCycles)
        // Shield blasts award no weapon XP and do not spend or restore weapon special energy.
        manager.continueCombat(this, target)
    }
}
