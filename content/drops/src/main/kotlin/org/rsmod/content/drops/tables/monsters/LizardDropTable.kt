package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val lizardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Lizard Drops",
    npcs = npcs("npc.slayer_lizard_massive"),
    mainTable = rsPlayerWeightedTable(total = 147) {
        name("Lizard Drops")
        30 weight "obj.firerune" count 5
        14 weight "obj.firerune" count 42
        4 weight "obj.naturerune" count 5
        13 weight "obj.coal" count 1
        3 weight "obj.copper_ore" count 1
        22 weight "obj.iron_ore" count 1
        2 weight "obj.silver_bar" count 1
        3 weight "obj.silver_ore" count 1
        4 weight "obj.tin_ore" count 1
        1 weight "obj.mithril_ore" count 1
        13 weight "obj.kebab" count 1
        13 weight "obj.water_skin0" count 2
        1 outOf 512 separate "obj.mystic_gloves_light" count 1

        10 weight SharedDropTables.herb
        6 weight SharedDropTables.gem
        9 weight SharedDropTables.seed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_desert_lizard_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
