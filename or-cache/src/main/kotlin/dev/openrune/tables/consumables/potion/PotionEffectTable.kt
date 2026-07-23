package dev.openrune.tables.consumables.potion

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object PotionEffectTable {
    private const val KEY = 0
    private const val KIND = 1
    private const val SKILLS = 2
    private const val BASE = 3
    private const val PERCENT = 4
    private const val AMOUNT = 5
    private const val EFFECTS = 6
    private const val EXCLUDED_SKILLS = 7
    private const val RESTORE_PRAYER = 8
    private const val STAMINA = 9
    private const val DURATION = 10
    private const val POISON_IMMUNITY = 11
    private const val VENOM_IMMUNITY = 12
    private const val FULL_PROTECTION = 13
    private const val CURES_DISEASE = 14
    private const val HANDLER = 15
    private const val BASE_EFFECT = 16
    private const val DAMAGE = 17
    private const val VARIANT = 18

    fun table() =
        PotionTomlLoader.effects().let { effects ->

            dbTable("dbtable.potion_effect", serverOnly = true) {
                column("key", KEY, VarType.STRING)
                column("kind", KIND, VarType.STRING)
                column("skills", SKILLS, VarType.STAT)
                column("base", BASE, VarType.INT)
                column("percent", PERCENT, VarType.INT)
                column("amount", AMOUNT, VarType.INT)
                column("effects", EFFECTS, VarType.DBROW)
                column("excluded_skills", EXCLUDED_SKILLS, VarType.STAT)
                column("restore_prayer", RESTORE_PRAYER, VarType.BOOLEAN)
                column("stamina", STAMINA, VarType.BOOLEAN)
                column("duration", DURATION, VarType.INT)
                column("poison_immunity", POISON_IMMUNITY, VarType.INT)
                column("venom_immunity", VENOM_IMMUNITY, VarType.INT)
                column("full_protection", FULL_PROTECTION, VarType.BOOLEAN)
                column("cures_disease", CURES_DISEASE, VarType.BOOLEAN)
                column("handler", HANDLER, VarType.STRING)
                column("base_effect", BASE_EFFECT, VarType.DBROW)
                column("damage", DAMAGE, VarType.INT)
                column("variant", VARIANT, VarType.STRING)

                effects.forEach { effect ->
                    row(effect.row) {
                        column(KEY, effect.key)
                        column(KIND, effect.kind)
                        if (effect.skills.isNotEmpty()) {
                            columnRSCM(SKILLS, *effect.skills.toTypedArray())
                        }
                        column(BASE, effect.base)
                        column(PERCENT, effect.percent)
                        column(AMOUNT, effect.amount)
                        if (effect.effects.isNotEmpty()) {
                            columnRSCM(EFFECTS, *effect.effects.toTypedArray())
                        }
                        if (effect.excludedSkills.isNotEmpty()) {
                            columnRSCM(
                                EXCLUDED_SKILLS,
                                *effect.excludedSkills.toTypedArray(),
                            )
                        }
                        column(RESTORE_PRAYER, effect.restorePrayer)
                        column(STAMINA, effect.stamina)
                        column(DURATION, effect.duration)
                        column(POISON_IMMUNITY, effect.poisonImmunity)
                        column(VENOM_IMMUNITY, effect.venomImmunity)
                        column(FULL_PROTECTION, effect.fullProtection)
                        column(CURES_DISEASE, effect.curesDisease)
                        column(HANDLER, effect.handler)
                        effect.baseEffect?.let { columnRSCM(BASE_EFFECT, it) }
                        column(DAMAGE, effect.damage)
                        column(VARIANT, effect.variant)
                    }
                }
            }
        }
}
