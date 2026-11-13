package org.rsmod.content.minigames.sepulchre

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

internal enum class LootTier {
    Low,
    Mid,
    High,
}

internal fun lootTier(weights: List<Int>, roll: Int): LootTier {
    var left = roll
    for ((index, weight) in weights.withIndex()) {
        if (left < weight) {
            return LootTier.entries[index]
        }
        left -= weight
    }
    return LootTier.High
}

/**
 * The coffins' loot (https://oldschool.runescape.wiki/w/Coffin_(Hallowed_Sepulchre)): one roll on
 * a tier table, each line 1/10, with the tier picked by the floor's weights.
 */
internal object SepulchreLoot {
    // UNVERIFIED: which stacks are noted; the monkfish and grimy ranarr are taken as noted.
    private val LOW =
        rsPlayerWeightedTable(total = 10) {
            name("Hallowed Sepulchre Coffin (low)")
            1 weight "obj.monkrobetop" count 1
            1 weight "obj.monkrobebottom" count 1
            1 weight "obj.blessedstar" count 1
            1 weight "obj.airrune" count 500..750
            1 weight "obj.firerune" count 500..750
            1 weight "obj.chaosrune" count 25..50
            1 weight "obj.xbows_crossbow_bolts_mithril" count 50..150
            1 weight "obj.2doseprayerrestore" count 1
            1 weight "obj.whitelilly" count 1
            1 weight "obj.coins" count 1500..3000
        }

    private val MID =
        rsPlayerWeightedTable(total = 10) {
            name("Hallowed Sepulchre Coffin (mid)")
            1 weight "obj.adamant_2h_sword" count 1
            1 weight "obj.adamant_platebody" count 1
            1 weight "obj.cosmicrune" count 60..100
            1 weight "obj.deathrune" count 60..100
            1 weight "obj.naturerune" count 60..100
            1 weight "obj.xbows_crossbow_bolts_adamantite" count 50..200
            1 weight "obj.cert_monkfish" count 1..3
            1 weight "obj.4doseprayerrestore" count 1
            1 weight "obj.cert_unidentified_ranarr" count 1..2
            1 weight "obj.coins" count 7500..12500
        }

    private val HIGH =
        rsPlayerWeightedTable(total = 10) {
            name("Hallowed Sepulchre Coffin (high)")
            1 weight "obj.rune_2h_sword" count 1
            1 weight "obj.rune_platebody" count 1
            1 weight "obj.lawrune" count 150..250
            1 weight "obj.bloodrune" count 150..250
            1 weight "obj.soulrune" count 150..250
            1 weight "obj.xbows_crossbow_bolts_runite" count 100..300
            1 weight "obj.cert_monkfish" count 2..6
            1 weight "obj.sanfew_salve_4_dose" count 1..2
            1 weight "obj.ranarr_seed" count 1..2
            1 weight "obj.coins" count 17500..25000
        }

    fun roll(player: Player, tier: LootTier): List<DropRollItem> {
        val table =
            when (tier) {
                LootTier.Low -> LOW
                LootTier.Mid -> MID
                LootTier.High -> HIGH
            }
        return when (val result = table.roll(player, ArgMap()).flatten()) {
            is RollResult.Nothing -> emptyList()
            is RollResult.Single -> listOf(result.result)
            is RollResult.ListOf -> result.results
        }.filterNot { it.isNothing }
    }
}
