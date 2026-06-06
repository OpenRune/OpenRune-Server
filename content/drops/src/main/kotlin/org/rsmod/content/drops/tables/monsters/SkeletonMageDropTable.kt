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
public val skeletonMageDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skeleton (mage) Drops",
    npcs = npcs("npc.lotr_mage_skeleton"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Skeleton (mage) Drops")
        8 weight "obj.iron_kiteshield" count 1
        5 weight "obj.bronze_full_helm" count 1
        5 weight "obj.bronze_plateskirt" count 1
        5 weight "obj.iron_warhammer" count 1
        5 weight "obj.steel_dagger" count 1
        5 weight "obj.steel_spear" count 1
        3 weight "obj.shortbow" count 1
        3 weight "obj.iron_battleaxe" count 1
        2 weight "obj.iron_scimitar" count 1
        2 weight "obj.steel_axe" count 1
        2 weight "obj.steel_chainbody" count 1
        2 weight "obj.mithril_mace" count 1
        1 weight "obj.magic_staff" count 1
        1 weight "obj.black_longsword" count 1
        1 weight "obj.steel_full_helm" count 1
        1 weight "obj.steel_kiteshield" count 1
        1 weight "obj.mithril_med_helm" count 1
        1 weight "obj.mithril_sq_shield" count 1
        1 weight "obj.adamant_battleaxe" count 1
        1 weight "obj.rune_scimitar" count 1
        3 weight "obj.bloodrune" count 3
        2 weight "obj.steel_arrow" count 11
        3 weight "obj.xbows_crossbow_bolts_steel_poisoned+" count 2
        3 weight "obj.steel_javelin_p+" count 2
        3 weight "obj.xbows_crossbow_bolts_silver" count 5
        2 weight "obj.adamant_arrow" count 4
        5 weight "obj.coins" count 17
        5 weight "obj.coins" count 41
        5 weight "obj.coins" count 142
        3 weight "obj.coins" count 206
        2 weight "obj.coins" count 106
        1 weight "obj.coins" count 323
        5 weight "obj.studs" count 6
        5 weight "obj.nails" count 12
        5 weight "obj.unlit_candle" count 1
        5 weight "obj.3dose1strength" count 1
        3 weight "obj.knife" count 1
        2 weight "obj.gold_ore" count 1
        2 weight "obj.adamantite_bar" count 1
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
