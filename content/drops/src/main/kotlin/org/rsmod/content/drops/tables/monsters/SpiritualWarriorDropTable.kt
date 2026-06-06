package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val spiritualWarriorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Spiritual warrior Drops",
    npcs = npcs("npc.godwars_spiritual_armadyl_warrior", "npc.godwars_spiritual_bandos_warrior", "npc.godwars_spiritual_saradomin_warrior", "npc.godwars_spiritual_zamorak_warrior", "npc.nex_prison_warrior"),
    mainTable = rsPlayerWeightedTable(total = 127) {
        name("Spiritual warrior Drops")
        3 weight "obj.iron_sword" count 1
        8 weight "obj.iron_scimitar" count 1
        15 weight "obj.steel_longsword" count 1
        7 weight "obj.steel_sword" count 1
        5 weight "obj.black_warhammer" count 1
        7 weight "obj.mithril_mace" count 1
        2 weight "obj.black_dagger" count 1
        9 weight "obj.mithril_spear_p" count 1
        2 weight "obj.adamant_battleaxe" count 1
        8 weight "obj.mithril_axe" count 1
        1 weight "obj.adamant_2h_sword" count 1
        1 weight "obj.rune_longsword" count 1
        1 weight "obj.rune_halberd" count 1
        9 weight "obj.leather_gloves" count 1
        11 weight "obj.mithril_platelegs" count 1
        18 weight "obj.steel_chainbody" count 1
        8 weight "obj.adamant_full_helm" count 1
        3 weight "obj.black_kiteshield" count 1
        4 weight "obj.iron_plateskirt" count 1
        1 weight "obj.rune_kiteshield" count 1

        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/64 if a ring of wealth (i) is worn and fought in the Wilderness.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
