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
import org.rsmod.content.drops.isOnQuest
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val goblinGodWarsDungeonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Goblin (God Wars Dungeon) Drops",
    npcs = npcs("npc.godwars_goblin1", "npc.godwars_goblin2", "npc.godwars_goblin3", "npc.godwars_goblin4", "npc.godwars_goblin5"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Goblin (God Wars Dungeon) Drops")
        3 weight "obj.bronze_axe" count 1
        1 weight "obj.bronze_scimitar" count 1
        9 weight "obj.bronze_spear" count 1
        3 weight "obj.bronze_arrow" count 7
        3 weight "obj.mindrune" count 2
        3 weight "obj.earthrune" count 4
        3 weight "obj.bodyrune" count 2
        2 weight "obj.bronze_javelin" count 5
        1 weight "obj.chaosrune" count 1
        1 weight "obj.naturerune" count 1
        34 weight "obj.coins" count 1
        13 weight "obj.coins" count 3
        8 weight "obj.coins" count 5
        7 weight "obj.coins" count 16
        3 weight "obj.coins" count 24
        9 weight "obj.hammer" count 1
        2 weight "obj.slice_goblin_history_book" count 1
        10 weight "obj.goblin_armour" count 1 condition { player ->
            // Drops Need Manual: Colour received depends on the goblin mail worn.
             true
        }
        1 weight "obj.grapes" count 1
        1 weight "obj.red_cape" count 1
        1 weight "obj.tin_ore" count 1

        2 weight SharedDropTables.herb
        8 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_goblin_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman1")
        }
        1 outOf 11 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 30 weight "obj.arceuus_corpse_goblin" count 1
        1 outOf 5000 weight "obj.champions_challenge_goblin" count 1
        1 outOf 80 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_beginner")
        }
        1 outOf 121 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
        1 outOf 64 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
    },
)
