package org.rsmod.content.other.special.attacks.magic

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MagicSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList

class NightmareStaffSpecialAttacks @Inject constructor(private val queues: WorldQueueList) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (weapon in VOLATILE) registerMagic(weapon, StaffAttack(manager, false))
        registerMagic("obj.nightmare_staff_eldritch", StaffAttack(manager, true))
    }

    private inner class StaffAttack(private val manager: SpecialAttackManager, private val eldritch: Boolean) : MagicSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Staff): Boolean = cast(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Staff): Boolean = cast(target, attack)

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Staff): Boolean {
            val name = if (eldritch) "eldritch" else "volatile"
            anim("seq.nightmare_staff_special")
            spotanim("spotanim.nightmare_staff_${name}_cast_spotanim")
            manager.setNextAttackDelay(this, 5)
            val accurate = manager.rollStaffAccuracy(this, target, attack.style, if (eldritch) 1.0 else 1.5)
            val maximum = NightmareStaffRules.baseMaxHit(player.magicLvl, eldritch)
            val damage = if (accurate) manager.rollStaffMaxHit(this, target, maximum, 1.0) else 0
            target.spotanim(if (accurate) "spotanim.nightmare_staff_${name}_hit_spotanim" else "spotanim.failedspell_impact", delay = 60)
            val hit = manager.queueMagicHit(this, target, damage, 60, 2)
            manager.giveCombatXp(this, target, attack, hit.damage)
            if (eldritch && hit.damage > 0) {
                val source = player
                val uid = source.uid
                val targetUid = when (target) { is Npc -> target.uid.packed; is Player -> target.uid.packed }
                val restored = hit.damage / 2
                queues.add(2) {
                    val targetValid = when (target) {
                        is Npc -> target.isSlotAssigned && target.uid.packed == targetUid
                        is Player -> target.isSlotAssigned && target.uid.packed == targetUid
                    }
                    if (source.isSlotAssigned && source.uid == uid && targetValid) {
                        val amount = NightmareStaffRules.prayerRestoration(source.stat("stat.prayer"), restored)
                        if (amount > 0) source.statAdd("stat.prayer", amount, 0)
                    }
                }
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        val VOLATILE = listOf("obj.nightmare_staff_volatile", "obj.deadman_blighted_volatile_staff", "obj.deadman_nightmare_staff_volatile", "obj.br_nightmare_staff_volatile")
    }
}

internal object NightmareStaffRules {
    fun baseMaxHit(magic: Int, eldritch: Boolean): Int {
        val cap = if (eldritch) 44 else 58
        return ((99 + cap * magic) / 99).coerceIn(1, cap)
    }
    fun prayerRestoration(current: Int, restoration: Int): Int = restoration.coerceAtMost((120 - current).coerceAtLeast(0))
}
