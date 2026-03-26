package org.alter.combat

import org.alter.api.CombatAttributes
import org.alter.api.Skills
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.isMagic
import org.alter.game.model.combat.isRanged
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent

/**
 * Grants OSRS-accurate combat XP to the attacker on every successful hit.
 *
 * XP is only awarded when:
 *   - the attacker is a [Player]
 *   - damage > 0 (landed hit)
 *
 * XP rates per damage point:
 *   ACCURATE    → 4 Attack XP
 *   AGGRESSIVE  → 4 Strength XP
 *   DEFENSIVE   → 4 Defence XP
 *   CONTROLLED  → 1.33 Attack + 1.33 Strength + 1.33 Defence XP
 *   RAPID       → 4 Ranged XP
 *   LONG_RANGE  → 2 Ranged + 2 Defence XP
 *   Magic       → spell baseXp + (damage * 0.133) Magic XP
 *
 * All styles additionally grant 1.33 Hitpoints XP per damage point.
 *
 * For magic attacks the spell's baseXp is read from the [CombatAttributes.CASTING_SPELL]
 * attribute via reflection, since [CombatSpell] lives in game-plugins and cannot be
 * imported directly from the content module.
 */
class CombatXpPlugin : PluginEvent() {

    override fun init() {
        onEvent<PostAttackEvent> {
            val player = attacker as? Player ?: return@onEvent
            if (damage <= 0) return@onEvent

            val dmg = damage.toDouble()

            when {
                combatStyle.isMagic() -> {
                    val baseXp = getSpellBaseXp(player)
                    player.addXp(Skills.MAGIC, baseXp + dmg * 0.133)
                    player.addXp(Skills.HITPOINTS, dmg * 1.33)
                }

                combatStyle.isRanged() -> {
                    val attackStyle = AttackStyleResolver.getAttackStyle(player)
                    when (attackStyle) {
                        AttackStyle.LONG_RANGE -> {
                            player.addXp(Skills.RANGED, dmg * 2.0)
                            player.addXp(Skills.DEFENCE, dmg * 2.0)
                        }
                        else -> {
                            // ACCURATE, RAPID, or any other ranged style
                            player.addXp(Skills.RANGED, dmg * 4.0)
                        }
                    }
                    player.addXp(Skills.HITPOINTS, dmg * 1.33)
                }

                else -> {
                    // Melee styles
                    val attackStyle = AttackStyleResolver.getAttackStyle(player)
                    when (attackStyle) {
                        AttackStyle.ACCURATE -> player.addXp(Skills.ATTACK, dmg * 4.0)
                        AttackStyle.AGGRESSIVE -> player.addXp(Skills.STRENGTH, dmg * 4.0)
                        AttackStyle.DEFENSIVE -> player.addXp(Skills.DEFENCE, dmg * 4.0)
                        AttackStyle.CONTROLLED -> {
                            player.addXp(Skills.ATTACK, dmg * 1.33)
                            player.addXp(Skills.STRENGTH, dmg * 1.33)
                            player.addXp(Skills.DEFENCE, dmg * 1.33)
                        }
                        else -> {
                            // Fallback: treat as ACCURATE for any unrecognised melee style
                            player.addXp(Skills.ATTACK, dmg * 4.0)
                        }
                    }
                    player.addXp(Skills.HITPOINTS, dmg * 1.33)
                }
            }
        }
    }

    /**
     * Reads the spell's baseXp from the [CombatAttributes.CASTING_SPELL] attribute.
     *
     * [CombatSpell] is an enum in game-plugins and cannot be imported directly from
     * the content module. Reflection is used to read the `baseXp` property so that
     * no cross-module import is required.
     *
     * Returns 0.0 if the attribute is absent or if reflection fails (e.g., a trident
     * attack that has no discrete spell object).
     */
    private fun getSpellBaseXp(player: Player): Double {
        val spell = player.attr[CombatAttributes.CASTING_SPELL] ?: return 0.0
        return try {
            val method = spell.javaClass.getMethod("getBaseXp")
            method.invoke(spell) as? Double ?: 0.0
        } catch (_: NoSuchMethodException) {
            // Kotlin enum properties are exposed as getters; try the property accessor directly
            try {
                val field = spell.javaClass.getDeclaredField("baseXp")
                field.isAccessible = true
                field.get(spell) as? Double ?: 0.0
            } catch (_: Exception) {
                0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }
}
