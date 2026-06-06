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
public val guardPrifddinasDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Guard (Prifddinas) Drops",
    npcs = npcs("npc.prif_guard0", "npc.prif_guard1", "npc.prif_guard2", "npc.prif_guard3", "npc.prif_guard4", "npc.prif_guard5", "npc.prif_guard6", "npc.prif_guard7"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Guard (Prifddinas) Drops")
        3 weight "obj.dragonhide_chaps" count 1
        4 weight "obj.dragonhide_body" count 1
        2 weight "obj.mithril_spear" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.adamant_full_helm" count 1
        1 weight "obj.rune_dagger" count 1
        8 weight "obj.waterrune" count 70
        5 weight "obj.naturerune" count 12
        3 weight "obj.lawrune" count 2
        2 weight "obj.firerune" count 37
        5 weight "obj.coins" count 20
        29 weight "obj.coins" count 44
        10 weight "obj.coins" count 180
        25 weight "obj.elf_crystal_tiny" count 1
        3 weight "obj.bass" count 1
        3 weight "obj.shark" count 1
        2 weight "obj.adamantite_ore" count 1
        1 weight "obj.mourning_teleport_crystal_1" count 1
        1 outOf 1024 separate "obj.prif_teleport_seed" count 1

        15 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 24 weight "obj.prif_crystal_shard" count 3..5
        1 outOf 50 weight "obj.arceuus_corpse_elf" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
