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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val cerberusDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cerberus Drops",
    npcs = npcs("npc.cerberus_attacking", "npc.cerberus_resetting", "npc.cerberus_sitting"),
    mainTable = rsPlayerWeightedTable(total = 130) {
        name("Cerberus Drops")
        5 weight "obj.rune_platebody" count 1
        4 weight "obj.rune_chainbody" count 1
        4 weight "obj.rune_2h_sword" count 1
        3 weight "obj.black_dragonhide_body" count 1
        3 weight "obj.rune_axe" count 1
        3 weight "obj.rune_pickaxe" count 1
        3 weight "obj.cert_battlestaff" count 6
        3 weight "obj.rune_full_helm" count 1
        2 weight "obj.lava_battlestaff" count 1
        2 weight "obj.rune_halberd" count 1
        6 weight "obj.firerune" count 300
        6 weight "obj.soulrune" count 100
        5 weight "obj.cert_blankrune_high" count 300
        4 weight "obj.bloodrune" count 60
        4 weight "obj.mcannonball" count 50
        4 weight "obj.xbows_crossbow_bolts_runite_unfeathered" count 40
        3 weight "obj.deathrune" count 100
        6 weight "obj.cert_coal" count 120
        6 weight "obj.4dose2restore" count 2
        6 weight "obj.summer_pie" count 3
        5 weight "obj.coins" count 10000..20000
        5 weight "obj.cert_dragon_bones" count 20
        5 weight "obj.blessedsnake" count 1
        5 weight "obj.cert_wine_of_zamorak" count 15
        4 weight "obj.cert_ashes" count 50
        4 weight "obj.cert_fire_orb" count 20
        4 weight "obj.cert_unidentified_torstol" count 6
        3 weight "obj.cert_runite_ore" count 5
        3 weight "obj.cert_uncut_diamond" count 5
        2 weight "obj.torstol_seed" count 3
        2 weight "obj.ranarr_seed" count 2
        2 weight "obj.teleportscroll_cerberus" count 7
        1 outOf 520 separate rsPlayerWeightedTable {
            1 weight "obj.primordial_crystal" count 1
            1 weight "obj.pegasian_crystal" count 1
            1 weight "obj.eternal_crystal" count 1
            1 weight "obj.smouldering_stone" count 1
        }

        3 weight SharedDropTables.rareDrop
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 15 weight "obj.arceuus_corpse_hellhound" count 1
        1 outOf 1 weight "obj.trail_elite_emote_exp1" count 1 condition { player ->
            // Drops Need Manual: Elite clue scrolls and reward caskets are only dropped when completing an elite clue scroll asking you to kill a hellhound.
             true
        }
        1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 2000 weight "obj.jar_of_souls" count 1
        1 outOf 3000 weight "obj.hell_pet" count 1
        1 outOf 95 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
