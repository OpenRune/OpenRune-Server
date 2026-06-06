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
import org.rsmod.api.droptable.wearingRingOfWealth
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val aviansieDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Aviansie Drops",
    npcs = npcs("npc.godwars_armadyl_female_armor01_blue", "npc.godwars_armadyl_female_armor01_green", "npc.godwars_armadyl_female_armor01_red", "npc.godwars_armadyl_female_armor02_blue", "npc.godwars_armadyl_female_armor02_green", "npc.godwars_armadyl_female_armor02_red", "npc.godwars_armadyl_female_armor03_blue", "npc.godwars_armadyl_male_armor01_blue", "npc.godwars_armadyl_male_armor01_green", "npc.godwars_armadyl_male_armor01_red", "npc.godwars_armadyl_male_armor02_blue", "npc.godwars_armadyl_male_armor02_green", "npc.godwars_armadyl_male_armor02_red", "npc.godwars_armadyl_male_armor03_green", "npc.godwars_armadyl_male_armor03_red"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.feather" count 1..6
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Aviansie Drops")
        11 weight "obj.rune_dagger_p+" count 1
        18 weight "obj.airrune" count 15
        13 weight "obj.waterrune" count 30
        4 weight "obj.lawrune" count 2
        4 weight "obj.naturerune" count 9
        3 weight "obj.chaosrune" count 3
        2 weight "obj.bodyrune" count 12
        2 weight "obj.bloodrune" count 11
        1 weight "obj.mindrune" count 5
        1 weight "obj.chaosrune" count 16
        30 weight "obj.adamantite_bar" count 4 condition { player ->
            // Drops Need Manual: Aviansie in the God Wars Dungeon drop unnoted bars until completion of the Fremennik Diary#Hard
             true
        }
        10 weight "obj.silver_ore" count 1
        1 weight "obj.xbows_crossbow_limbs_runite" count 1
        5 weight "obj.3doseantipoison" count 5
        3 weight ringNothing()
        2 weight "obj.swordfish" count 5

        15 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): Aviansie in the God Wars Dungeon drop the ensouled head at a rate of 1/35, while those in the Wilderness God Wars Dungeon drop it at a rate of 1/20.
        1 outOf 35 weight "obj.arceuus_corpse_aviansie" count 1
        // Drops Need Manual (rate): Brimstone key drop rates for levels 69, 71, 73, 79, 83, 84, 89, 92, 94, 97, 131, 137, and 148 are 1/292, 1/268, 1/245, 1/188, 1/157, 1/151, 1/124, 1/112, 1/101, 1/107, 1/93, 1/92, and 1/90, respectively.
        onBuilder { brimstoneKeyRoll() }
        1 outOf 121 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 64 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
