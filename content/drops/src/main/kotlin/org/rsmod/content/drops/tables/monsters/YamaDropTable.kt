package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val yamaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Yama Drops",
    npcs = npcs("npc.yama_throne_occupied", "npc.yama_throne_unoccupied"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.cosmic_soul_catalyst" count 2000
        "obj.diabolic_worms" count 250
        "obj.oathplate_shards" count 45
        "obj.oathplate_helm" count 1 condition { player ->
            // Drops Need Manual: Choice of [[Oathplate helm
             true
        }
        "obj.oathplate_chest" count 1
        "obj.oathplate_legs" count 1
        "obj.soulflame_horn" count 1
        "obj.yama_binding_contract" count 1
        "obj.purifying_sigil_top" count 1
        "obj.yama_special_contract" count 1
        "obj.purifying_sigil_bottom" count 1
        "obj.yama_spell_contract" count 1
        "obj.purifying_sigil_left" count 1
        "obj.yama_heavyranged_contract" count 1
        "obj.purifying_sigil_right" count 1
        "obj.yama_2h_contract" count 1
        "obj.purifying_sigil_middle" count 1
        "obj.yama_pet_contract" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 111) {
        name("Yama Drops")
        8 weight "obj.pineapple_pizza" count 3..4
        8 weight "obj.wild_pie" count 3..4
        8 weight "obj.3doseprayerrestore" count 2
        8 weight "obj.brutal_2dose2restore" count 2
        8 weight "obj.1dose2combat" count 1
        8 weight "obj.brutal_2dosepotionofzamorak" count 1
        5 weight "obj.cert_rune_chainbody" count 8
        4 weight "obj.cert_battlestaff" count 40
        3 weight "obj.cert_rune_platebody" count 8
        2 weight "obj.dragon_plateskirt" count 1
        2 weight "obj.dragon_platelegs" count 1
        3 weight "obj.bloodrune" count 400
        3 weight "obj.lawrune" count 150
        2 weight "obj.smokerune" count 350
        2 weight "obj.soulrune" count 500
        2 weight "obj.soulrune" count 1000
        1 weight "obj.firerune" count 40000
        1 weight "obj.wrathrune" count 800
        7 weight "obj.cosmic_soul_catalyst" count 850
        7 weight "obj.diabolic_worms" count 90
        5 weight "obj.demonic_tallow_barrel_full" count 1
        4 weight "obj.teleportscroll_chasmoffire" count 6
        3 weight "obj.cert_emerald" count 40
        3 weight "obj.cert_ruby" count 40
        3 weight "obj.cert_diamond" count 40
        1 weight "obj.xbows_bolt_tips_onyx" count 150
        2 outOf 600 separate "obj.soulflame_horn" count 1 condition { player ->
            // Drops Need Manual: Scales with contribution; rates and quantities are shown for 100% contribution.
             true
        }
        1 outOf 600 separate rsPlayerWeightedTable {
            1 weight "obj.oathplate_helm" count 1
            1 weight "obj.oathplate_chest" count 1
            1 weight "obj.oathplate_legs" count 1
        }
        1 outOf 12 separate "obj.yama_dossier" count 1
        1 outOf 33 separate "obj.forgotten_lockbox" count 1
        1 outOf 17 separate "obj.oathplate_shards" count 12
        1 outOf 100 separate "obj.yamapet" count 1
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The drop rate is fixed at 1/2500, as long as contribution is higher than 15%.
        1 outOf 2500 weight "obj.yamapet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/28 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 30 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
