package org.rsmod.api.stats.plugin.levelup

import org.rsmod.api.stats.plugin.levelup.LevelUpJingle.EveryTenth
import org.rsmod.api.stats.plugin.levelup.LevelUpJingle.FromLevel
import org.rsmod.api.stats.plugin.levelup.LevelUpJingle.GuideList
import org.rsmod.api.stats.plugin.levelup.LevelUpJingle.MaxLevel
import org.rsmod.api.stats.plugin.levelup.LevelUpJingle.Parity
import org.rsmod.api.stats.plugin.levelup.LevelUpJingle.Single
import org.rsmod.api.stats.plugin.levelup.LevelUpJingle.Unlocks

internal enum class LevelUpStat(
    val stat: String,
    val label: String,
    layerName: String,
    val jingle: LevelUpJingle,
    val levelPrefix: String = "Your $label level is now",
) {
    Attack("stat.attack", "Attack", "attack", unlocks("advance_attack")),
    Defence("stat.defence", "Defence", "defence", unlocks("advance_defense")),
    Strength(
        "stat.strength",
        "Strength",
        "strength",
        FromLevel("jingle.advance_strength", "jingle.advance_strength2", threshold = 50),
    ),
    Hitpoints(
        "stat.hitpoints",
        "Hitpoints",
        "hitpoints",
        FromLevel("jingle.advance_hitpoints", "jingle.advance_hitpoints2", threshold = 50),
        levelPrefix = "Your Hitpoints are now",
    ),
    Ranged("stat.ranged", "Ranged", "ranged", unlocks("advance_ranged")),
    Prayer("stat.prayer", "Prayer", "prayer", unlocks("advance_prayer")),
    Magic("stat.magic", "Magic", "magic", unlocks("advance_magic")),
    Cooking("stat.cooking", "Cooking", "cooking", unlocks("advance_cooking")),
    Woodcutting("stat.woodcutting", "Woodcutting", "woodcutting", unlocks("advance_woodcutting")),
    Fletching("stat.fletching", "Fletching", "fletching", unlocks("advance_fletching")),
    Fishing("stat.fishing", "Fishing", "fishing", unlocks("advance_fishing")),
    Firemaking("stat.firemaking", "Firemaking", "firemaking", unlocks("advance_firemarking")),
    Crafting("stat.crafting", "Crafting", "crafting", unlocks("advance_crafting")),
    Smithing(
        "stat.smithing",
        "Smithing",
        "smithing",
        GuideList("jingle.advance_smithing", "jingle.advance_smithing2"),
    ),
    Mining("stat.mining", "Mining", "mining", unlocks("advance_mining")),
    Herblore("stat.herblore", "Herblore", "herblore", unlocks("advance_herblaw")),
    Agility("stat.agility", "Agility", "agility", Single("jingle.advance_agility")),
    Thieving("stat.thieving", "Thieving", "thieving", unlocks("advance_thieving")),
    Slayer("stat.slayer", "Slayer", "slayer", unlocks("advance_slayer")),
    Farming(
        "stat.farming",
        "Farming",
        "farming",
        Unlocks("jingle.farming_levelup", "jingle.farming_levelup_2"),
    ),
    Runecraft("stat.runecrafting", "Runecraft", "runecraft", unlocks("advance_runecraft")),
    Hunter(
        "stat.hunter",
        "Hunter",
        "hunter",
        Parity(even = "jingle.advance_hunting", odd = "jingle.advance_hunting2"),
    ),
    Construction(
        "stat.construction",
        "Construction",
        "construction",
        EveryTenth("jingle.advance_carpentry", "jingle.advance_carpentry2"),
    ),
    Sailing(
        "stat.sailing",
        "Sailing",
        "sailing",
        MaxLevel("jingle.advance_sailing", "jingle.advance_sailing2"),
    );

    val layer: String = "component.levelup_display:$layerName"

    companion object {
        const val COMBAT_LABEL: String = "Combat"
        const val COMBAT_LAYER: String = "component.levelup_display:combat"
        const val COMBAT_JINGLE: String = "jingle.combat_level_up"

        private val byStat = entries.associateBy(LevelUpStat::stat)

        fun of(stat: String): LevelUpStat? = byStat[stat]
    }
}

private fun unlocks(jingle: String): Unlocks = Unlocks("jingle.$jingle", "jingle.${jingle}2")
