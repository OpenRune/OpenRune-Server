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
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val werewolfDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Werewolf Drops",
    npcs = npcs("npc.canafis_werewolf_man1", "npc.canafis_werewolf_man2", "npc.canafis_werewolf_man3", "npc.canafis_werewolf_man4", "npc.canafis_werewolf_man5", "npc.canafis_werewolf_man6", "npc.canafis_werewolf_man7", "npc.canafis_werewolf_man8", "npc.canafis_werewolf_woman1", "npc.canafis_werewolf_woman10", "npc.canafis_werewolf_woman11", "npc.canafis_werewolf_woman12", "npc.canafis_werewolf_woman2", "npc.canafis_werewolf_woman3", "npc.canafis_werewolf_woman4", "npc.canafis_werewolf_woman5", "npc.canafis_werewolf_woman6", "npc.canafis_werewolf_woman7", "npc.canafis_werewolf_woman8", "npc.canafis_werewolf_woman9", "npc.godwars_ancient_werewolf1", "npc.godwars_ancient_werewolf2"),
    mainTable = rsPlayerWeightedTable(total = 512) {
        name("Werewolf Drops")
        25 weight "obj.steel_axe" count 1
        32 weight "obj.steel_scimitar" count 1
        15 weight "obj.steel_full_helm" count 1
        10 weight "obj.mithril_chainbody" count 1
        10 weight "obj.mithril_sq_shield" count 1
        3 weight "obj.rune_med_helm" count 1
        10 weight "obj.raw_chicken" count 5
        10 weight "obj.raw_beef" count 5
        10 weight "obj.raw_bear_meat" count 5
        20 weight "obj.jug_wine" count 1
        80 weight "obj.coins" count 10
        20 weight "obj.coins" count 90
        20 weight "obj.coins" count 120
        20 weight "obj.coins" count 222
        20 weight "obj.coins" count 364
        100 weight "obj.grey_wolf_fur" count 1
        100 weight "obj.werewolve_fur" count 1
        2 weight ringNothing()

        3 weight SharedDropTables.herb
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        2 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_werewolf_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/64 if a ring of wealth (i) is worn and fought in the Wilderness.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/486 after unlocking the medium Combat Achievements rewards tier.
        // Drops Need Manual (rate): The medium clue scroll drop rate is increased to 1/256 if a ring of wealth (i) is worn and fought in the Wilderness.
        1 outOf 512 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
