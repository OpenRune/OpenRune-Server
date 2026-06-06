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
public val dagannothRexDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dagannoth Rex Drops",
    npcs = npcs("npc.dagcave_melee_boss"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dagganoth_hide" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Dagannoth Rex Drops")
        17 weight "obj.steel_kiteshield" count 1
        11 weight "obj.mithril_warhammer" count 1
        7 weight "obj.adamant_axe" count 1
        4 weight "obj.steel_platebody" count 1
        3 weight "obj.mithril_pickaxe" count 1
        2 weight "obj.adamant_platebody" count 1
        2 weight "obj.viking_sword" count 1
        1 weight "obj.dragon_axe" count 1
        1 weight "obj.rune_axe" count 1
        1 weight "obj.viking_shield" count 1
        1 weight "obj.viking_helmet" count 1
        1 weight "obj.mithril_2h_sword" count 1
        1 weight "obj.ring_of_life" count 1
        1 weight "obj.dagganoth_melee_body" count 1
        1 weight "obj.dagganoth_melee_legs" count 1
        1 weight "obj.berzerker_ring" count 1
        1 weight "obj.warrior_ring" count 1
        1 weight "obj.2dose1antidragon" count 1
        1 weight "obj.2doseprayerrestore" count 1
        1 weight "obj.2dosestatrestore" count 1
        1 weight "obj.2dose2attack" count 1
        1 weight "obj.2dose2strength" count 1
        1 weight "obj.2dose2defense" count 1
        1 weight "obj.2dosepotionofzamorak" count 1
        10 weight "obj.cert_mithril_ore" count 25
        3 weight "obj.adamantite_bar" count 1
        2 weight "obj.cert_coal" count 100
        1 weight "obj.cert_iron_ore" count 150
        1 weight "obj.cert_steel_bar" count 15..30
        10 weight "obj.coins" count 100..1209
        7 weight "obj.unidentified_ranarr" count 1
        7 weight "obj.bass" count 5
        4 weight "obj.swordfish" count 5
        1 weight "obj.shark" count 5

        8 weight SharedDropTables.rareDrop
        10 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_dagganoth_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 20 weight "obj.arceuus_corpse_dagannoth" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 5000 weight "obj.rexpet" count 1
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
