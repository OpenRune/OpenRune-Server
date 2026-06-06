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
public val dagannothPrimeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dagannoth Prime Drops",
    npcs = npcs("npc.dagcave_magic_boss"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dagganoth_hide" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Dagannoth Prime Drops")
        10 weight "obj.earth_battlestaff" count 1
        5 weight "obj.water_battlestaff" count 1
        4 weight "obj.air_battlestaff" count 1
        1 weight "obj.cert_battlestaff" count 1..10
        1 weight "obj.viking_sword" count 1
        1 weight "obj.viking_shield" count 1
        1 weight "obj.viking_helmet" count 1
        1 weight "obj.mud_battlestaff" count 1
        1 weight "obj.dragon_axe" count 1
        1 weight "obj.viking_helmet_magic" count 1
        1 weight "obj.dagganoth_mage_body" count 1
        1 weight "obj.dagganoth_mage_legs" count 1
        1 weight "obj.seer_ring" count 1
        6 weight "obj.airrune" count 100..200
        5 weight "obj.earthrune" count 50..100
        2 weight "obj.bloodrune" count 25..75
        2 weight "obj.lawrune" count 10..75
        2 weight "obj.naturerune" count 25..75
        2 weight "obj.mudrune" count 25..75
        2 weight "obj.deathrune" count 25..85
        10 weight "obj.cert_earth_talisman" count 25..75
        7 weight "obj.cert_air_talisman" count 25..75
        7 weight "obj.cert_water_talisman" count 1..76
        10 weight "obj.shark" count 5
        3 weight "obj.coins" count 500..1109
        5 weight "obj.bigoysterpearls" count 1
        5 weight "obj.cert_blankrune_high" count 150
        5 weight "obj.unidentified_ranarr" count 1

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
        1 outOf 5000 weight "obj.primepet" count 1
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
