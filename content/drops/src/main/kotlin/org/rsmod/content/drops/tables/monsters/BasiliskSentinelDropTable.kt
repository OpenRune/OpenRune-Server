package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val basiliskSentinelDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Basilisk Sentinel Drops",
    npcs = npcs("npc.superior_basilisk_knight"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 52) {
        name("Basilisk Sentinel Drops")
        1 weight "obj.adamant_platelegs" count 1
        1 weight "obj.adamant_kiteshield" count 1
        2 weight "obj.rune_axe" count 1
        1 weight "obj.rune_battleaxe" count 1
        1 weight "obj.rune_dagger" count 1
        1 weight "obj.rune_scimitar" count 1
        1 weight "obj.rune_spear" count 1
        1 weight "obj.rune_med_helm" count 1
        6 weight "obj.astralrune" count 15..35
        6 weight "obj.naturerune" count 15..30
        6 weight "obj.lawrune" count 20..30
        3 weight "obj.deathrune" count 10..25
        3 weight "obj.bloodrune" count 8..20
        7 weight "obj.coins" count 500..2498
        1 weight "obj.cert_adamantite_ore" count 1..2

        8 weight SharedDropTables.gem
        3 weight SharedDropTables.usefulHerb
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_baby_basilisk_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 256 weight "obj.mystic_hat_light" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 1000 weight "obj.poh_trophydrop_basilisk" count 1
        1 outOf 1000 weight "obj.basilisk_jaw" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/18 after unlocking the hard Combat Achievements rewards tier.
        10 outOf 192 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
