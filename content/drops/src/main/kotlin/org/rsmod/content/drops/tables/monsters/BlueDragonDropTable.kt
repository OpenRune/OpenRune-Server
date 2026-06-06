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
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val blueDragonDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Blue dragon Drops",
    npcs = npcs("npc.blue_dragon", "npc.blue_dragon2", "npc.blue_dragon3", "npc.blue_dragon4", "npc.blue_dragon5", "npc.blue_dragon_strongholdcave_1", "npc.blue_dragon_strongholdcave_2", "npc.blue_dragon_strongholdcave_3", "npc.blue_dragon_strongholdcave_4", "npc.blue_dragon_strongholdcave_5", "npc.blue_dragon_tapoyauik_1", "npc.blue_dragon_tapoyauik_2"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.dragonhide_blue" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Blue dragon Drops")
        4 weight "obj.steel_platelegs" count 1
        3 weight "obj.mithril_axe" count 1
        3 weight "obj.steel_battleaxe" count 1
        2 weight "obj.mithril_spear" count 1
        1 weight "obj.adamant_full_helm" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.rune_dagger" count 1
        8 weight "obj.waterrune" count 75
        5 weight "obj.naturerune" count 15
        3 weight "obj.lawrune" count 3
        1 weight "obj.firerune" count 37
        29 weight "obj.coins" count 44
        25 weight "obj.coins" count 132
        10 weight "obj.coins" count 200
        5 weight "obj.coins" count 11
        1 weight "obj.coins" count 440
        3 weight "obj.adamantite_ore" count 1
        3 weight "obj.bass" count 1

        15 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 50 weight "obj.arceuus_corpse_dragon" count 1
        1 outOf 50 weight "obj.scaly_bluehide" count 1
        onBuilder { brimstoneKeyRoll() }
        1 outOf 100 weight "obj.frozen_tear" count 8 condition { player ->
            // Drops Need Manual: Only dropped in Ruins of Tapoyauik
             true
        }
        1 outOf 636 weight "obj.varlamore_key_half_1" count 1
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
