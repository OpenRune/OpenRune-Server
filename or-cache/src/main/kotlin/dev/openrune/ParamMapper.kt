package dev.openrune

object ParamMapper {
    object item {
        val STAB_ATTACK_BONUS = 0
        val SLASH_ATTACK_BONUS = 1
        val CRUSH_ATTACK_BONUS = 2
        val MAGIC_ATTACK_BONUS = 3
        val RANGED_ATTACK_BONUS = 4
        val STAB_DEFENCE_BONUS = 5
        val SLASH_DEFENCE_BONUS = 6
        val CRUSH_DEFENCE_BONUS = 7
        val MAGIC_DEFENCE_BONUS = 8
        val RANGED_DEFENCE_BONUS = 9
        val MELEE_STRENGTH = 10
        val PRAYER_BONUS = 11
        val ATTACK_RATE = 14
        val MAGIC_DAMAGE_BONUS_SALAMANDER = 65
        val MAGIC_DAMAGE_STRENGTH = 299 // Should be divided by 10
        val RANGED_STRENGTH_BONUS = 189
        val PRIMARY_SKILL = 434
        val PRIMARY_LEVEL = 436
        val SECONDARY_SKILL = 435
        val SECONDARY_LEVEL = 437
        val TERTIARY_SKILL = 191
        val TERTIARY_LEVEL = 613
        val QUATERNARY_SKILL = 579
        val QUATERNARY_LEVEL = 614
    }

    object npc {
        const val STAB_ATTACK_BONUS = 0
        const val SLASH_ATTACK_BONUS = 1
        const val CRUSH_ATTACK_BONUS = 2
        const val MAGIC_ATTACK_BONUS = 3
        const val RANGED_ATTACK_BONUS = 4
        const val STAB_DEFENCE_BONUS = 5
        const val SLASH_DEFENCE_BONUS = 6
        const val CRUSH_DEFENCE_BONUS = 7
        const val MAGIC_DEFENCE_BONUS = 8
        const val RANGED_DEFENCE_BONUS = 9
        const val MELEE_STRENGTH_BONUS = 10
        const val RANGED_STRENGTH_BONUS = 12
        const val ATTACK_RATE = 14 // Attack rate in ticks
        const val MAGIC_DAMAGE_BONUS = 65
        const val DRACONIC = 190 // 1 = If is draconic
        const val GOLEM = 1178 // 1 = If is Golem
        const val KALPHITE = 1353 // 1 = If is Kalphite
        const val DEATH_DROP = 46 // What to drop on death Bones/Ashes etc.

    }
}
