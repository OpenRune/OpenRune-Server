package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val ancientZygomiteDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ancient Zygomite Drops",
    npcs = npcs("npc.fossil_zygomite"),
    mainTable = rsPlayerWeightedTable(total = 118) {
        name("Ancient Zygomite Drops")
        3 weight "obj.rune_axe" count 1
        18 weight "obj.earthrune" count 100
        6 weight "obj.lawrune" count 15
        6 weight "obj.cosmicrune" count 15
        6 weight "obj.naturerune" count 15
        6 weight "obj.unidentified_torstol" count 1
        10 weight "obj.mushroom_seed" count 1
        18 weight dropRollable(DropRollItem("obj.fossil_pyrophosphite", 1, condition = { player ->
            // Drops Need Manual: Only dropped by ancient zygomites on Fossil Island.
             true
        }, bonusDrops = listOf(
            DropRollItem("obj.fossil_calcite", 1),
        )))
        10 weight "obj.cert_mortmyremushroom" count 5
        15 outOf 472 separate "obj.unidentified_kwuarm" count 1 condition { player ->
            // Drops Need Manual: Only dropped by ancient zygomites in the Stalker Den.
             true
        }
        12 outOf 472 separate rsPlayerWeightedTable {
            12 weight "obj.unidentified_dwarf_weed" count 1
            12 weight "obj.unidentified_cadantine" count 1
        }
        9 outOf 472 separate "obj.unidentified_lantadyme" count 1
        64 outOf 590 separate "obj.cert_bucket_supercompost" count 2
        16 outOf 590 separate "obj.fossil_volcanic_ash" count 2

        6 weight SharedDropTables.gem
        29 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
