package org.rsmod.content.skills.magic.spell.attacks.ancient

import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.toxins.impl.NpcPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class AncientSpellEffects(private val random: GameRandom) {
    fun apply(
        spell: AncientSpell,
        caster: Player,
        target: PathingEntity,
        damage: Int,
        sceptre: Boolean,
        guaranteedFreeze: Boolean = false,
    ) {
        when (spell.element) {
            Element.Smoke -> applySmoke(spell, target, sceptre)
            Element.Shadow -> applyShadow(spell, target, sceptre)
            Element.Blood -> applyBlood(spell, caster, damage, sceptre)
            Element.Ice -> applyIce(spell, target, sceptre, guaranteedFreeze)
        }
    }

    fun claimGuaranteedFreeze(spell: AncientSpell, target: PathingEntity): Boolean {
        if (spell.element != Element.Ice || target !is Npc) {
            return false
        }
        if (target.vars["varn.freeze_guaranteed"] != 1) {
            return false
        }
        target.vars["varn.freeze_guaranteed"] = 0
        return true
    }

    private fun applySmoke(spell: AncientSpell, target: PathingEntity, sceptre: Boolean) {
        if (random.of(SMOKE_POISON_CHANCE) != 0) {
            return
        }
        val severity = if (sceptre) spell.effectStrength + 1 else spell.effectStrength
        when (target) {
            is Npc -> NpcPoison.tryPoison(target, severity)
            is Player -> PlayerPoison.tryPoison(target, severity = severity)
        }
    }

    private fun applyShadow(spell: AncientSpell, target: PathingEntity, sceptre: Boolean) {
        val permille = spell.effectStrength * if (sceptre) 11 else 10
        when (target) {
            is Npc -> {
                if (target.attackLvl < target.baseAttackLvl) {
                    return
                }
                target.attackLvl -= target.baseAttackLvl * permille / 1000
            }
            is Player -> {
                val base = target.statBase("stat.attack")
                if (target.stat("stat.attack") < base) {
                    return
                }
                target.statSub("stat.attack", constant = base * permille / 1000, percent = 0)
            }
        }
    }

    private fun applyBlood(spell: AncientSpell, caster: Player, damage: Int, sceptre: Boolean) {
        val permille = spell.effectStrength * if (sceptre) 11 else 10
        val heal = damage * permille / 1000
        if (heal > 0) {
            caster.statHeal("stat.hitpoints", constant = heal, percent = 0)
        }
    }

    private fun applyIce(
        spell: AncientSpell,
        target: PathingEntity,
        sceptre: Boolean,
        guaranteed: Boolean,
    ) {
        var ticks = spell.effectStrength
        if (sceptre) {
            ticks = ticks * 11 / 10
        }
        when (target) {
            is Npc -> {
                val resistance = target.visType.paramOrNull(params.freeze_resistance) ?: 0
                if (!guaranteed && resistance > 0 && random.of(100) < resistance) {
                    return
                }
                CombatEffects.freeze(target, ticks, ignoreImmunity = guaranteed)
            }
            is Player -> {
                if (target.vars["varbit.prayer_protectfrommagic"] > 0) {
                    ticks /= 2
                }
                CombatEffects.freeze(target, ticks)
            }
        }
    }

    internal companion object {
        const val SMOKE_POISON_CHANCE = 8
        const val AOE_RADIUS = 1

        fun overlapsAoe(centre: CoordGrid, coords: CoordGrid, size: Int): Boolean =
            coords.x <= centre.x + AOE_RADIUS &&
                coords.x + size - 1 >= centre.x - AOE_RADIUS &&
                coords.z <= centre.z + AOE_RADIUS &&
                coords.z + size - 1 >= centre.z - AOE_RADIUS
    }
}
