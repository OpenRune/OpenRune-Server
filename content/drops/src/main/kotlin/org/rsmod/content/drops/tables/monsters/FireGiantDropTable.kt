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
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val fireGiantDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Fire giant Drops",
    npcs = npcs("npc.firegiant", "npc.firegiant2", "npc.firegiant3", "npc.firegiant_big", "npc.firegiant_big2", "npc.firegiant_big3", "npc.firegiant_strongholdcave_1", "npc.firegiant_strongholdcave_2", "npc.firegiant_strongholdcave_3", "npc.firegiant_strongholdcave_4", "npc.kourend_firegiant1", "npc.kourend_firegiant2"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Fire giant Drops")
        3 weight "obj.steel_axe" count 1
        2 weight "obj.mithril_sq_shield" count 1
        1 weight "obj.fire_battlestaff" count 1
        1 weight "obj.rune_scimitar" count 1
        10 weight "obj.firerune" count 150
        7 weight "obj.chaosrune" count 5
        5 weight "obj.rune_arrow" count 12
        4 weight "obj.bloodrune" count 5
        1 weight "obj.firerune" count 37
        1 weight "obj.lawrune" count 2
        40 weight "obj.coins" count 60
        7 weight "obj.coins" count 15
        6 weight "obj.coins" count 25
        2 weight "obj.coins" count 300
        1 weight "obj.coins" count 50
        3 weight "obj.lobster" count 1
        2 weight "obj.steel_bar" count 1
        1 weight "obj.2dose1strength" count 1

        19 weight SharedDropTables.herb
        1 weight SharedDropTables.rareDrop
        11 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 4 weight "obj.rag_fire_giant_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 20 weight "obj.arceuus_corpse_giant" count 1
        // Drops Need Manual (rate): Brimstone key drop rates for level 86, 104 and 109 fire giants are , and respectively.
        onBuilder { brimstoneKeyRoll() }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5000 weight "obj.champions_challenge_giant" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
