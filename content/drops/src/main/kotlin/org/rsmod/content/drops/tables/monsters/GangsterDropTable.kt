package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val gangsterDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Gangster Drops",
    npcs = npcs("npc.shayzien_criminal_1", "npc.shayzien_criminal_2", "npc.shayzien_criminal_3", "npc.shayzien_criminal_4"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 80 weight "obj.rune_scimitar" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 256) {
        name("Gangster Drops")
        8 weight "obj.black_chainbody" count 1
        8 weight "obj.black_med_helm" count 1
        8 weight "obj.black_plateskirt" count 1
        11 weight "obj.black_scimitar" count 1
        20 weight "obj.cosmicrune" count 30..60
        15 weight "obj.cert_gold_bar" count 8..16
        20 weight "obj.cert_plank_mahogany" count 4..8
        40 weight "obj.cert_plank_oak" count 10..20
        30 weight "obj.cert_plank_teak" count 5..10
        30 weight "obj.coins" count 600..800
        25 weight "obj.cert_bucket_compost" count 40..80
        25 outOf 1024 separate rsPlayerWeightedTable {
            25 weight "obj.unidentified_cadantine" count 1
            25 weight "obj.unidentified_dwarf_weed" count 1
        }
        31 outOf 1024 separate "obj.unidentified_kwuarm" count 1
        19 outOf 1024 separate "obj.unidentified_lantadyme" count 1
        41 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 5 weight "obj.shayzien_gang_intelligence" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/61 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 65 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
