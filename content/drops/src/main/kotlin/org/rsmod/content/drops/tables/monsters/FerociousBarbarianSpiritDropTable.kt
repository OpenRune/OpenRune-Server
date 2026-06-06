package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val ferociousBarbarianSpiritDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ferocious barbarian spirit Drops",
    npcs = npcs("npc.brut_angry_spirit_bad"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Ferocious barbarian spirit Drops")
        5 weight "obj.mithril_mace" count 1
        4 weight "obj.adamant_mace" count 1
        1 weight "obj.rune_mace" count 1
        2 weight "obj.brut_mithril_spear" count 1
        4 weight "obj.adamant_knife" count 20
        4 weight "obj.adamant_dart_p" count 20
        4 weight "obj.rune_arrow" count 10
        4 weight "obj.xbows_crossbow_bolts_runite" count 10
        8 weight "obj.deathrune" count 5..9
        5 weight "obj.ranarr_seed" count 1
        16 weight "obj.brutal_1doseprayerrestore" count 1
        8 weight "obj.brutal_2doseprayerrestore" count 1
        2 weight "obj.brutal_1dose2strength" count 1
        2 weight "obj.brutal_2dose2strength" count 1
        2 weight "obj.brutal_1dose2attack" count 1
        3 weight "obj.brutal_2dose2attack" count 1
        2 weight "obj.brutal_1dose2defense" count 1
        3 weight "obj.brutal_2dose2defense" count 1
        4 weight "obj.brutal_1dose1antidragon" count 1
        4 weight "obj.brutal_2dose1antidragon" count 1
        1 weight "obj.brutal_2dose2antipoison" count 1
        1 weight "obj.brutal_2dosefisherspotion" count 1
        2 weight "obj.yew_logs" count 3
        2 weight "obj.cert_steel_bar" count 2
        2 weight "obj.cert_mithril_bar" count 2
        1 weight "obj.cert_adamantite_bar" count 1
        1 weight "obj.cert_runite_bar" count 1
        7 weight "obj.cert_big_bones" count 10
        6 weight "obj.shark" count 3

        10 weight SharedDropTables.herb
        8 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: The elite clue scroll and elite casket are only dropped when completing an elite clue scroll asking you to kill a ferocious barbarian spirit.
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
    },
)
