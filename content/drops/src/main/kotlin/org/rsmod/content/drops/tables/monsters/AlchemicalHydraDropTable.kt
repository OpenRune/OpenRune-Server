package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.config.constants
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val alchemicalHydraDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Alchemical Hydra Drops",
    npcs = npcs("npc.hydraboss", "npc.hydraboss_2", "npc.hydraboss_3", "npc.hydraboss_4", "npc.hydraboss_finaldeath", "npc.hydraboss_p1_transition", "npc.hydraboss_p2_transition", "npc.hydraboss_p3_transition"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 2000 weight "obj.dragon_thrownaxe" count 500..1000
    },
    mainTable = rsPlayerWeightedTable(total = 110) {
        name("Alchemical Hydra Drops")
        8 weight dropRollable(DropRollItem("obj.mystic_fire_staff", 1, bonusDrops = listOf(
            DropRollItem("obj.mystic_water_staff", 1),
        )))
        8 weight "obj.mystic_water_staff" count 1
        5 weight "obj.cert_battlestaff" count 8..12
        5 weight "obj.black_dragonhide_body" count 1
        3 weight "obj.dragon_longsword" count 1
        3 weight "obj.dragon_med_helm" count 1
        3 weight dropRollable(DropRollItem("obj.rune_platebody", 1, bonusDrops = listOf(
            DropRollItem("obj.rune_platelegs", 1, condition = { player -> player.appearance.bodyType == constants.bodytype_a }),
            DropRollItem("obj.rune_plateskirt", 1, condition = { player -> player.appearance.bodyType == constants.bodytype_b }),
        )))
        2 weight "obj.dragon_battleaxe" count 1
        2 weight "obj.rune_platelegs" count 1
        2 weight "obj.rune_plateskirt" count 1
        1 weight dropRollable(DropRollItem("obj.mystic_robe_top_light", 1, bonusDrops = listOf(
            DropRollItem("obj.mystic_robe_bottom_light", 1),
        )))
        1 weight "obj.mystic_robe_bottom_light" count 1
        6 weight "obj.chaosrune" count 150..300
        6 weight "obj.deathrune" count 150..300
        6 weight "obj.bloodrune" count 150..300
        6 weight "obj.astralrune" count 150..300
        2 weight "obj.xbows_crossbow_bolts_runite_tipped_dragonstone_enchanted" count 100..120
        1 weight "obj.xbows_crossbow_bolts_runite_tipped_onyx_enchanted" count 35..50
        1 weight "obj.coins" count 5550..25550
        10 weight "obj.coins" count 40000..60000
        7 weight "obj.shark" count 2..4
        7 weight dropRollable(DropRollItem("obj.3doserangerspotion", 1, bonusDrops = listOf(
            DropRollItem("obj.3dose2restore", 2),
        )))
        7 weight "obj.3dose2restore" count 2
        6 weight "obj.cert_dragon_bones" count 30
        1 weight "obj.crystal_key" count 1

        1 weight SharedDropTables.rareDrop
        10 outOf 1811 separate "obj.hydra_eye" count 1
        10 outOf 1811 separate "obj.hydra_fang" count 1
        10 outOf 1811 separate "obj.hydra_heart" count 1
        1 outOf 513 separate "obj.hydra_tail" count 1
        1 outOf 514 separate "obj.hydra_leather" count 1
        1 outOf 1001 separate "obj.hydra_claw" count 1
        1 outOf 2001 separate "obj.dragon_knife" count 500..1000
        5 outOf 323 separate "obj.cert_unidentified_avantoe" count 10..15
        5 outOf 323 separate "obj.cert_unidentified_kwuarm" count 25..30
        5 outOf 404 separate "obj.cert_unidentified_ranarr" count 10..15
        5 outOf 404 separate "obj.cert_unidentified_snapdragon" count 10..15
        5 outOf 404 separate "obj.cert_unidentified_cadantine" count 25..30
        5 outOf 404 separate "obj.cert_unidentified_dwarf_weed" count 25..30
        10 outOf 1077 separate "obj.cert_unidentified_lantadyme" count 25..30
        10 outOf 1077 separate "obj.cert_unidentified_torstol" count 10..15
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 256 weight "obj.poh_alchemical_hydra_head" count 1
        1 outOf 2000 weight "obj.jar_of_chemicals" count 1
        1 outOf 3000 weight "obj.hydrapet" count 1
        1 outOf 95 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 243 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
