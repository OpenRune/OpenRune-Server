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
public val blackDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Black dragon Drops",
    npcs = npcs("npc.black_dragon", "npc.black_dragon2", "npc.black_dragon3", "npc.black_dragon4", "npc.black_dragon5", "npc.black_dragon_strongholdcave_1", "npc.black_dragon_strongholdcave_2", "npc.black_dragon_strongholdcave_3", "npc.ds2_black_dragon", "npc.ds2_black_dragon_cutscene", "npc.wild_cave_black_dragon", "npc.wild_cave_black_dragon2", "npc.wild_cave_black_dragon3"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_black" count 1
        "obj.chickenquest_dragon_coin" count 1 condition {
            player -> player.isOnQuest("quest_recipefordisaster")
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Black dragon Drops")
        4 weight "obj.mithril_2h_sword" count 1
        3 weight "obj.mithril_axe" count 1
        3 weight "obj.mithril_battleaxe" count 1
        3 weight "obj.rune_knife" count 2
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.adamant_platebody" count 1
        1 weight "obj.rune_longsword" count 1
        20 weight "obj.adamant_javelin" count 30
        7 weight "obj.adamant_dart_p" count 16
        8 weight "obj.firerune" count 50
        3 weight "obj.bloodrune" count 15
        1 weight "obj.airrune" count 75
        5 weight "obj.lawrune" count 10
        40 weight "obj.coins" count 196
        10 weight "obj.coins" count 330
        1 weight "obj.coins" count 690
        6 weight "obj.dragon_javelin_head" count 10 condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        3 weight "obj.adamantite_bar" count 1
        3 weight "obj.chocolate_cake" count 1

        2 weight SharedDropTables.rareDrop
        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 35 weight "obj.arceuus_corpse_dragon" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 10000 weight "obj.dragonfire_visage" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/121 after unlocking the hard Combat Achievements rewards tier.
        // Drops Need Manual (rate): The hard clue scroll rarity changes to 1/64 if a Ring of wealth (i) is worn.
        1 outOf 128 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/475 after unlocking the elite Combat Achievements rewards tier.
        // Drops Need Manual (rate): The elite clue scroll rarity changes to 1/250 if a Ring of wealth (i) is worn.
        1 outOf 500 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
