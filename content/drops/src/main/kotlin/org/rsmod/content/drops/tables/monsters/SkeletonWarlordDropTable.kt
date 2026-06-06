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
public val skeletonWarlordDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeleton warlord Drops",
    npcs = npcs("npc.brut_skeleton3"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Skeleton warlord Drops")
        8 weight "obj.iron_kiteshield" count 1
        5 weight "obj.iron_plateskirt" count 1
        5 weight "obj.brut_steel_spear" count 1
        5 weight "obj.steel_longsword" count 1
        3 weight "obj.yew_shortbow" count 1
        3 weight "obj.steel_knife" count 10
        2 weight "obj.steel_mace" count 1
        2 weight "obj.steel_scimitar" count 1
        1 weight "obj.steel_dart" count 10
        1 weight "obj.steel_full_helm" count 1
        1 weight "obj.steel_kiteshield" count 1
        1 weight "obj.mithril_med_helm" count 1
        1 weight "obj.mithril_sq_shield" count 1
        1 weight "obj.adamant_battleaxe" count 1
        1 weight "obj.rune_scimitar" count 1
        1 weight "obj.battlestaff" count 1
        5 weight "obj.deathrune" count 5
        3 weight "obj.bloodrune" count 3
        3 weight "obj.steel_arrow" count 20
        3 weight "obj.xbows_crossbow_bolts_silver" count 5
        2 weight "obj.adamant_arrow" count 5
        12 weight "obj.yew_logs" count 1
        5 weight "obj.cert_steel_bar" count 3
        5 weight "obj.cert_silver_bar" count 3
        5 weight "obj.gold_bar" count 1
        2 weight "obj.adamantite_bar" count 1
        2 weight "obj.gold_ore" count 1
        10 weight "obj.coins" count 142
        5 weight "obj.coins" count 106
        2 weight "obj.coins" count 106
        1 weight "obj.coins" count 323
        5 weight "obj.brut_document_0" count 1
        5 weight "obj.brutal_2dose1strength" count 1
        3 weight "obj.xbows_grapple_tip_bolt_mithril_rope" count 2
        2 weight "obj.sapphire_ring" count 1

        5 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5000 weight "obj.champions_challenge_skeleton" count 1
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
