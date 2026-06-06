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
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val iceGiantDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Ice giant Drops",
    npcs = npcs("npc.ice_giant_tapoyauik", "npc.ice_giant_tapoyauik02", "npc.icegiant", "npc.icegiant2", "npc.icegiant3", "npc.icegiant_low_wanderrange", "npc.icegiant_low_wanderrange2", "npc.wild_cave_icegiant", "npc.wild_cave_icegiant2", "npc.wild_cave_icegiant3"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Ice giant Drops")
        5 weight "obj.iron_2h_sword" count 1
        5 weight "obj.black_kiteshield" count 1
        4 weight "obj.steel_axe" count 1
        4 weight "obj.steel_sword" count 1
        1 weight "obj.iron_platelegs" count 1
        1 weight "obj.mithril_mace" count 1
        1 weight "obj.mithril_sq_shield" count 1
        6 weight "obj.adamant_arrow" count 5
        4 weight "obj.naturerune" count 6
        3 weight "obj.mindrune" count 24
        3 weight "obj.bodyrune" count 37
        2 weight "obj.lawrune" count 3
        1 weight "obj.waterrune" count 12
        1 weight "obj.cosmicrune" count 4
        1 weight "obj.deathrune" count 3
        1 weight "obj.bloodrune" count 2
        32 weight "obj.coins" count 117
        12 weight "obj.coins" count 53
        10 weight "obj.coins" count 196
        7 weight "obj.coins" count 8
        6 weight "obj.coins" count 22
        2 weight "obj.coins" count 400
        3 weight "obj.jug_wine" count 1
        1 weight "obj.mithril_ore" count 1
        1 weight "obj.banana" count 1

        4 weight SharedDropTables.gem
        7 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_ice_giant_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 21 weight "obj.arceuus_corpse_giant" count 1
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5000 weight "obj.champions_challenge_giant" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 1026 weight "obj.varlamore_key_half_1" count 1
        1 outOf 40 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Frozen tear [tertiary/<ref group=d name=tapo>Only dropped in the [[Ruins of Tapoyauik]].</ref>]
