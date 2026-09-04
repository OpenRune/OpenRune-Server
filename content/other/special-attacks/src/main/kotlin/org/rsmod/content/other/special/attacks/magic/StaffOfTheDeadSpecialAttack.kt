package org.rsmod.content.other.special.attacks.magic

import org.rsmod.api.config.constants
import org.rsmod.api.player.hit.modifier.PowerOfDeathMeleeProtection
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository

/**
 * Power of Death: consume 100% special attack energy to halve incoming melee damage for one
 * minute while a Staff of the Dead family weapon remains equipped.
 *
 * The cache maps every registered variant below to 1000 special-energy units. The direct sequence
 * IDs are cache-native because this revision does not expose RSCM aliases for them.
 */
class StaffOfTheDeadSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerInstant("obj.sotd", ::staffOfTheDeadPower)
        registerInstant("obj.br_sotd", ::staffOfTheDeadPower)
        registerInstant("obj.staff_of_light", ::staffOfLightPower)
        registerInstant("obj.staff_of_balance", ::staffOfBalancePower)

        registerInstant("obj.toxic_sotd", ::toxicStaffOfTheDeadPower)
        registerInstant("obj.toxic_sotd_charged", ::toxicStaffOfTheDeadPower)
        registerInstant("obj.toxic_sotd_deadman", ::toxicStaffOfTheDeadPower)
        registerInstant("obj.toxic_sotd_charged_deadman", ::toxicStaffOfTheDeadPower)
    }

    private fun staffOfTheDeadPower(access: ProtectedAccess): Boolean =
        access.activatePowerOfDeath(STAFF_OF_THE_DEAD_SEQUENCE, STAFF_OF_THE_DEAD_SPOTANIM)

    private fun staffOfLightPower(access: ProtectedAccess): Boolean =
        access.activatePowerOfDeath(STAFF_OF_LIGHT_SEQUENCE, STAFF_OF_LIGHT_SPOTANIM)

    private fun staffOfBalancePower(access: ProtectedAccess): Boolean =
        access.activatePowerOfDeath(STAFF_OF_BALANCE_SEQUENCE, STAFF_OF_BALANCE_SPOTANIM)

    private fun toxicStaffOfTheDeadPower(access: ProtectedAccess): Boolean =
        access.activatePowerOfDeath(TOXIC_STAFF_OF_THE_DEAD_SEQUENCE, STAFF_OF_THE_DEAD_SPOTANIM)

    private fun ProtectedAccess.activatePowerOfDeath(sequence: String, spotanim: String): Boolean {
        PowerOfDeathMeleeProtection.activate(player)
        player.anim(sequence, priority = POWER_OF_DEATH_SEQUENCE_PRIORITY)
        player.spotanim(
            spotanim,
            height = POWER_OF_DEATH_SPOTANIM_HEIGHT,
            slot = constants.spotanim_slot_combat,
        )
        player.mes(POWER_OF_DEATH_MESSAGE)
        return true
    }

    private companion object {
        const val STAFF_OF_THE_DEAD_SEQUENCE = "seq.sotd_special"
        const val STAFF_OF_THE_DEAD_SPOTANIM = "spotanim.sotd_special_start"
        const val STAFF_OF_LIGHT_SEQUENCE = "seq.staff_of_light_special"
        const val STAFF_OF_LIGHT_SPOTANIM = "spotanim.staff_of_light_special_start"
        const val STAFF_OF_BALANCE_SEQUENCE = "seq.staff_of_balance_special"
        const val STAFF_OF_BALANCE_SPOTANIM = "spotanim.staff_of_balance_special_start"
        const val TOXIC_STAFF_OF_THE_DEAD_SEQUENCE = "seq.sotd_special_toxic_charged"

        const val POWER_OF_DEATH_SEQUENCE_PRIORITY = 6
        const val POWER_OF_DEATH_SPOTANIM_HEIGHT = 300
        const val POWER_OF_DEATH_MESSAGE = "Spirits of deceased evildoers offer you their protection."
    }
}
