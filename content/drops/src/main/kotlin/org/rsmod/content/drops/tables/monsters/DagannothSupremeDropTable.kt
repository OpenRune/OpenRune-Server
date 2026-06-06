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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val dagannothSupremeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dagannoth Supreme Drops",
    npcs = npcs("npc.dagcave_ranged_boss"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dagganoth_hide" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Dagannoth Supreme Drops")
        10 weight "obj.mithril_knife" count 25..50
        7 weight "obj.red_dragon_vambraces" count 1
        5 weight "obj.rune_thrownaxe" count 5..10
        5 weight "obj.adamant_dart" count 10..25
        5 weight "obj.iron_knife" count 200..500
        5 weight "obj.steel_knife" count 50..150
        1 weight "obj.viking_sword" count 1
        1 weight "obj.viking_shield" count 1
        1 weight "obj.viking_helmet" count 1
        1 weight "obj.daganoth_cave_magic_shortbow" count 1
        1 weight "obj.dragon_axe" count 1
        1 weight "obj.viking_helmet_range" count 1
        1 weight "obj.dagganoth_ranged_body" count 1
        1 weight "obj.dagganoth_ranged_legs" count 1
        1 weight "obj.ranger_ring" count 1
        5 weight "obj.steel_arrow" count 50..250
        5 weight "obj.xbows_crossbow_bolts_runite" count 2..12
        4 weight "obj.iron_arrow" count 200..700
        10 weight "obj.coins" count 500..1110
        6 weight "obj.bigoysterpearls" count 1
        5 weight "obj.opal_bolttips" count 10..30
        5 weight "obj.shark" count 5
        5 weight "obj.cert_yew_logs" count 50..150
        5 weight "obj.unidentified_ranarr" count 1
        3 weight "obj.cert_maple_logs" count 15..65
        2 weight "obj.xbows_crossbow_limbs_runite" count 1
        1 weight "obj.feather" count 250..500

        8 weight SharedDropTables.rareDrop
        10 weight SharedDropTables.gem
        7 weight SharedDropTables.rareSeed
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_dagganoth_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 20 weight "obj.arceuus_corpse_dagannoth" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 5000 weight "obj.supremepet" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/39 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 42 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/712 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 750 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
