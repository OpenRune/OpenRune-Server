package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val brineRatDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Brine rat Drops",
    npcs = npcs("npc.olaf2_brine_rats"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_rat_meat" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Brine rat Drops")
        18 weight "obj.deathrune" count 7
        2 weight "obj.airrune" count 18
        6 weight "obj.earthrune" count 10
        2 weight "obj.earthrune" count 18
        4 weight "obj.earthrune" count 36
        3 weight "obj.waterrune" count 10
        2 weight "obj.waterrune" count 18
        2 weight "obj.bloodrune" count 4
        6 weight "obj.cert_raw_lobster" count 10
        6 weight "obj.cert_raw_shark" count 3
        2 weight "obj.cert_raw_rat_meat" count 18
        2 weight "obj.cert_raw_pike" count 18
        2 weight "obj.cert_raw_shark" count 8
        2 weight "obj.raw_swordfish" count 9
        1 weight "obj.raw_shark" count 1
        21 weight "obj.coins" count 1
        16 weight "obj.coins" count 2
        9 weight "obj.coins" count 4
        3 weight "obj.coins" count 29
        16 weight ringNothing()
        3 weight "obj.water_talisman" count 1
        1 outOf 512 separate "obj.olaf2_brine_sabre" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_rat_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The medium clue scroll drop rate increases to 1/121 after unlocking the medium Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_medium_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
