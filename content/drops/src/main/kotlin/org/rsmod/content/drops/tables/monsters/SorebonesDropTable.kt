package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val sorebonesDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Sorebones Drops",
    npcs = npcs("npc.brain_sawbones_1", "npc.brain_sawbones_2"),
    mainTable = rsPlayerWeightedTable(total = 129) {
        name("Sorebones Drops")
        3 weight "obj.xbows_crossbow_bolts_steel" count 2..12
        2 weight "obj.mithril_dagger" count 1
        3 weight "obj.pirate_bandana_eyepatch_blue" count 1
        5 weight "obj.pirate_bandana_eyepatch_red" count 1
        1 weight "obj.eye_patch" count 1
        3 weight "obj.mithril_scimitar" count 1
        1 weight "obj.waterrune" count 23
        2 weight "obj.bodyrune" count 6
        1 weight "obj.chaosrune" count 7
        3 weight "obj.cosmicrune" count 5
        3 weight "obj.lawrune" count 2
        10 weight "obj.coins" count 30
        21 weight "obj.coins" count 38
        8 weight "obj.coins" count 46
        6 weight "obj.coins" count 55
        2 weight "obj.coins" count 60
        22 weight "obj.fishing_bait" count 10
        1 weight "obj.deal_rusty_scimitar" count 1
        1 weight "obj.fever_rum" count 1

        30 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.brain_inv_tongs" count 1 condition {
            player -> player.isOnQuest("quest_greatbrainrobbery")
        }
        1 outOf 1 weight "obj.brain_inv_cranial_clamp" count 1
        1 outOf 1 weight "obj.brain_inv_bell_jar" count 3
        1 outOf 1 weight "obj.brain_inv_skull_staple" count 30
        1 outOf 4 weight "obj.rag_zombie_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 5000 weight "obj.champions_challenge_zombie" count 1
    },
)
