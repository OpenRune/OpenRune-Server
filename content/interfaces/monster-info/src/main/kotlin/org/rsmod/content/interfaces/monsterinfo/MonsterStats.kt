package org.rsmod.content.interfaces.monsterinfo

import dev.openrune.types.NpcServerType
import org.rsmod.api.combat.maxhit.npc.NpcMeleeMaxHit
import org.rsmod.api.config.refs.params

internal object MonsterStats {
    fun payload(type: NpcServerType): String {
        val stats = listOf(
            heading("Base stats"), "Combat level: ${type.combatLevel}",
            "Hitpoints: ${type.hitpoints}", "Attack: ${type.attack}",
            "Defence: ${type.defence}", "Strength: ${type.strength}",
            "Magic: ${type.magic}", "Ranged: ${type.ranged}",
            "Base melee max: ${NpcMeleeMaxHit.calculateBaseDamage(NpcMeleeMaxHit.calculateEffectiveStrength(type.strength), type.param(params.melee_strength))}",
        )
        val aggressive = listOf(
            heading("Aggressive stats"), "Attack speed: ${type.param(params.attackrate)} ticks",
            "Attack bonus: ${type.param(params.attack_melee)}",
            "Magic bonus: ${type.param(params.attack_magic)}",
            "Ranged bonus: ${type.param(params.attack_ranged)}",
            "Strength bonus: ${type.param(params.melee_strength)}",
            "Ranged strength: ${type.param(params.ranged_strength)}",
            "Magic damage: ${type.param(params.npc_magic_damage_bonus)}",
        )
        val defensive = listOf(
            heading("Defensive stats"), "Stab: ${type.param(params.defence_stab)}",
            "Slash: ${type.param(params.defence_slash)}", "Crush: ${type.param(params.defence_crush)}",
            "Magic: ${type.param(params.defence_magic)}",
            "Light ranged: ${type.param(params.defence_light)}",
            "Standard ranged: ${type.param(params.defence_standard)}",
            "Heavy ranged: ${type.param(params.defence_heavy)}",
        )
        val other = buildList {
            add(heading("Other attributes"))
            if (type.param(params.demon) != 0) add("- Is a demon.")
            if (type.param(params.undead) != 0) add("- Is undead.")
            if (type.param(params.draconic) != 0) add("- Is draconic.")
            if (type.param(params.kalphite) != 0) add("- Is a kalphite.")
            if (type.param(params.rat) != 0) add("- Is a rat.")
            if (type.param(params.poison_immunity) != 0) add("- Immune to poison.")
            if (type.param(params.venom_immunity) != 0) add("- Immune to venom.")
            val element = listOf("air", "water", "earth", "fire").getOrNull(type.paramOrNull(params.elemental_weakness_type) ?: -1)
            val weakness = type.param(params.elemental_weakness_percent)
            if (element != null && weakness > 0) add("- $weakness% weakness to $element spells.")
            add("Size: ${type.size} x ${type.size}")
            add("Base values; boss phases and combat effects can differ.")
        }
        return (listOf(stats, aggressive, defensive, other).map { it.joinToString("<br>") } + safeText(type.name)).joinToString("|")
    }

    private fun heading(text: String) = "<col=ff981f>$text</col>"
    fun safeText(text: String): String = text.replace(Regex("[<>|]"), "")
}
