package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val runeDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Rune dragon Drops",
    npcs = npcs("npc.ds2_rune_dragon", "npc.rune_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.runite_bar" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 132) {
        name("Rune dragon Drops")
        9 weight "obj.rune_platebody" count 1
        8 weight "obj.rune_longsword" count 1
        7 weight "obj.rune_mace" count 1
        7 weight "obj.rune_scimitar" count 1
        7 weight "obj.rune_warhammer" count 1
        6 weight "obj.rune_platelegs" count 1
        1 weight "obj.dragon_platelegs" count 1
        1 weight "obj.dragon_plateskirt" count 1
        1 weight "obj.dragon_med_helm" count 1
        8 weight "obj.rune_arrow" count 30..40
        8 weight "obj.wrathrune" count 30..50
        7 weight "obj.chaosrune" count 75..150
        7 weight "obj.deathrune" count 50..100
        11 weight "obj.xbows_crossbow_bolts_runite_unfeathered" count 20..30
        15 weight "obj.rune_javelin_head" count (20..30) condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        7 weight "obj.cert_dragonstone" count 1
        6 weight "obj.cert_runite_ore" count 2..5
        5 weight "obj.dragon_javelin_head" count (30..40) condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        1 weight "obj.dragon_bolts_unfeathered" count 20..40
        1 weight "obj.wrath_talisman" count 1

        1 weight SharedDropTables.rareDrop
        8 weight SharedDropTables.usefulHerb
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 800 weight "obj.xbows_crossbow_limbs_dragon" count 1
        1 outOf 5000 weight "obj.dragon_lump" count 1
        1 outOf 8000 weight "obj.dragonfire_visage" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/285 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 300 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
