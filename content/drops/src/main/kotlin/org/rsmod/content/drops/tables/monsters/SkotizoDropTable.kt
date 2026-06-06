package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val skotizoDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Skotizo Drops",
    npcs = npcs("npc.cata_boss"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
    mainTable = rsPlayerWeightedTable(total = 199) {
        name("Skotizo Drops")
        75 weight "obj.cata_shard" count 1
        15 weight "obj.cata_shard" count 2
        5 weight "obj.cata_shard" count 3
        4 weight "obj.cata_shard" count 4
        1 weight "obj.cata_shard" count 5
        7 weight "obj.cert_rune_platebody" count 3
        7 weight "obj.cert_rune_platelegs" count 3
        7 weight "obj.cert_rune_plateskirt" count 3
        7 weight "obj.deathrune" count 500
        7 weight "obj.soulrune" count 450
        7 weight "obj.bloodrune" count 450
        7 weight "obj.cert_adamantite_ore" count 75
        7 weight "obj.cert_unidentified_snapdragon" count 20
        7 weight "obj.cert_unidentified_torstol" count 20
        7 weight "obj.cert_raw_anglerfish" count 60
        7 weight "obj.cert_runite_bar" count 20
        7 weight "obj.cert_plank_mahogany" count 150
        7 weight "obj.cert_battlestaff" count 25
        7 weight "obj.xbows_bolt_tips_onyx" count 40
        1 weight "obj.dragonshield_a" count 1
        9 outOf 1000 separate "obj.cert_uncut_dragonstone" count 10
        1 outOf 1000 separate "obj.uncut_onyx" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 10 weight "obj.arceuus_corpse_demon" count 1
        1 outOf 25 weight "obj.cata_boss_claw" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 65 weight "obj.skotizopet" count 1
        3 outOf 128 weight "obj.cata_totem1" count 1
        3 outOf 128 weight "obj.cata_totem2" count 1
        3 outOf 128 weight "obj.cata_totem3" count 1
        1 outOf 128 weight "obj.cata_totem" count 1
        1 outOf 200 weight "obj.jar_of_darkness" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/4 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 5 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
