package org.rsmod.content.other.special.weapons.magic

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.weapons.MagicWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.content.other.special.weapons.scripts.charge.TridentCharges
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

class TridentWeapons : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for (kind in TridentCharges.kinds) for (symbol in kind.symbols) {
            register(symbol, Trident(manager, kind))
        }
    }

    private class Trident(private val manager: WeaponAttackManager, private val kind: TridentCharges.Kind) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Staff): Boolean {
            // Ordinary powered tridents cannot attack players.
            mes("You cannot use this trident against other players.")
            manager.stopCombat(this)
            return true
        }

        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Staff): Boolean {
            val weapon = player.worn[Wearpos.RightHand.slot]
            if (weapon == null || weapon != attack.weapon || TridentCharges.kind(weapon) != kind) {
                manager.stopCombat(this)
                return true
            }
            val count = TridentCharges.count(weapon)
            if (count == 0) {
                player.worn[Wearpos.RightHand.slot] = TridentCharges.withCharges(weapon, 0)
                mes("Your trident has no charges. Use the required runes on it to recharge it.")
                manager.stopCombat(this)
                return true
            }
            val ornament = kind.charged.endsWith("_orn")
            val prefix = if (kind.toxic) "toxic_tots" else "slayer_tots"
            val suffix = if (ornament) "_orn_leagues6" else ""
            val seq = getInvObj(weapon).param(params.attack_anim_stance1)
            // A splash still consumes a cast; preserve the worn item's other packed state.
            player.worn[Wearpos.RightHand.slot] = TridentCharges.withCharges(weapon, count - 1)
            manager.setNextAttackDelay(this, 4)
            anim(RSCM.getReverseMapping(RSCMType.SEQ, seq.id))
            spotanim("spotanim.${prefix}_casting$suffix", height = 100)
            val projectile = manager.spawnProjectile(this, target, "spotanim.${prefix}_projectile$suffix", "projanim.magic_spell")
            val clientDelay = projectile.clientCycles
            val serverDelay = projectile.serverCycles
            val accurate = manager.rollStaffAccuracy(this, target, attack)
            val damage = if (accurate) manager.rollStaffMaxHit(this, target, baseMaxHit(player.magicLvl, kind.toxic)) else 0
            target.spotanim(if (accurate) "spotanim.${prefix}_impact$suffix" else "spotanim.failedspell_impact", height = 100, delay = clientDelay)
            if (accurate) {
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, damage, clientDelay, serverDelay)
            } else manager.queueSplashHit(this, target, clientDelay, serverDelay)
            if (count == 1) {
                mes("Your trident has run out of charges.")
                manager.stopCombat(this)
            } else manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        fun baseMaxHit(magic: Int, toxic: Boolean): Int = (magic / 3 - if (toxic) 2 else 5).coerceAtLeast(1)
    }
}
