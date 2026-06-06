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
public val adamantDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Adamant dragon Drops",
    npcs = npcs("npc.adamant_dragon", "npc.ds2_adamant_dragon"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.adamantite_bar" count 2
    },
    mainTable = rsPlayerWeightedTable(total = 110) {
        name("Adamant dragon Drops")
        9 weight "obj.adamant_platebody" count 1
        7 weight "obj.rune_mace" count 1
        7 weight "obj.rune_scimitar" count 1
        1 weight "obj.dragon_med_helm" count 1
        1 weight "obj.dragon_platelegs" count 1
        1 weight "obj.dragon_plateskirt" count 1
        8 weight "obj.adamant_arrow" count 30..40
        8 weight "obj.wrathrune" count 10..30
        7 weight "obj.chaosrune" count 60..120
        7 weight "obj.deathrune" count 30..60
        8 weight "obj.adamant_javelin_head" count (40..50) condition {
            player -> !player.hasCompletedQuest("quest_monkeymadness2")
        }
        11 weight "obj.xbows_crossbow_bolts_adamantite_unfeathered" count 20..40
        7 weight "obj.cert_diamond" count 1..3
        7 weight "obj.dragon_javelin_head" count (20..30) condition {
            player -> player.hasCompletedQuest("quest_monkeymadness2")
        }
        6 weight "obj.cert_adamantite_ore" count 8..20
        4 weight "obj.cert_adamantite_bar" count 5..35
        1 weight "obj.dragon_bolts_unfeathered" count 15..20
        1 weight "obj.wrath_talisman" count 1

        1 weight SharedDropTables.rareDrop
        8 weight SharedDropTables.usefulHerb
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 1000 weight "obj.xbows_crossbow_limbs_dragon" count 1
        1 outOf 5000 weight "obj.dragon_slice" count 1
        1 outOf 9000 weight "obj.dragonfire_visage" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/304 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 320 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
