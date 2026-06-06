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
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val jogreDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Jogre Drops",
    npcs = npcs("npc.godwars_ancient_jogre", "npc.jogre"),
    mainTable = rsPlayerWeightedTable(total = 129) {
        name("Jogre Drops")
        30 weight "obj.bronze_spear" count 1
        4 weight "obj.iron_spear" count 1
        10 weight "obj.naturerune" count 2
        2 weight "obj.naturerune" count 10
        2 weight "obj.naturerune" count 5
        2 weight "obj.steel_javelin" count 5
        5 weight "obj.unidentified_rogues_purse" count 1
        5 weight "obj.unidentified_snake_weed" count 1
        27 weight "obj.village_trade_sticks" count 22
        8 weight "obj.pineapple" count 2
        5 weight "obj.knife" count 1
        3 weight "obj.bones" count 1
        3 weight "obj.big_bones" count 1
        2 weight dropRollable(DropRollItem("obj.big_bones", 3, bonusDrops = listOf(
            DropRollItem("obj.bones", 3),
        )))
        2 weight "obj.bones" count 1
        2 weight ringNothing()

        6 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        10 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_jogre_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_jogre" count 1
        1 outOf 122 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_medium_emote_exp1")
        }
    },
)
