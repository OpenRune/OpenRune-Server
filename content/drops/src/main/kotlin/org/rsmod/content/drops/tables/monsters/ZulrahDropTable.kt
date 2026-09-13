package org.rsmod.content.drops.tables.monsters

import dtx.core.RollResult
import dtx.core.singleRollable
import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.*
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.game.entity.Player

/** Wiki revision 15342915; see content/bosses/zulrah/DROP_EVIDENCE.md. */
internal object ZulrahDropTables {
    val wealthRings = listOf("obj.ring_of_wealth", "obj.ring_of_wealth_i") +
        (1..5).flatMap { listOf("obj.ring_of_wealth_$it", "obj.ring_of_wealth_i$it") }
    private val emptyRareRoll = dropRollable(nothingDrop { player -> wealthRings.none { it in player.worn } })
    val megaRare = rsPlayerWeightedTable(total = 128) {
        name("Zulrah mega-rare table")
        113 weight emptyRareRoll
        8 weight "obj.rune_spear" count 1
        4 weight "obj.dragonshield_a" count 1
        3 weight "obj.dragon_spear" count 1
    }

    val uniques = rsPlayerWeightedTable(total = 4) {
        name("Zulrah uniques")
        1 weight "obj.blowpipe_fang" count 1
        1 weight "obj.magic_fang" count 1
        1 weight "obj.serpentine_visage" count 1
        1 weight "obj.uncut_onyx" count 1
    }

    val flax = rsPlayerWeightedTable(total = 5264) {
        name("Zulrah flax and mutagens")
        5244 weight "obj.cert_flax" count 1000
        10 weight "obj.cyan_mutagen" count 1
        10 weight "obj.red_mutagen" count 1
    }

    val sharks = rsPlayerWeightedTable(total = 8) {
        name("Zulrah shark table")
        3 weight "obj.cert_raw_shark" count 35
        3 weight "obj.shark_lure" count 70
        2 weight "obj.cert_mantaray" count 35
    }

    // Local variants avoid changing other monsters' generated/shared tables.
    // Zulrah's source map is above ground, irrespective of its allocated instance coordinates.
    val gems = rsPlayerWeightedTable(total = 128) {
        name("Zulrah gem table")
        63 weight emptyRareRoll
        32 weight "obj.uncut_sapphire" count 1
        16 weight "obj.uncut_emerald" count 1
        8 weight "obj.uncut_ruby" count 1
        3 weight "obj.nature_talisman" count 1
        2 weight "obj.uncut_diamond" count 1
        1 weight "obj.rune_javelin" count 5
        1 weight "obj.keyhalf2" count 1
        1 weight "obj.keyhalf1" count 1
        1 weight singleRollable<Player, DropRollItem> {
            selectResult { player, args ->
                if (player.hasCompletedQuest("quest_legendsquest")) megaRare.roll(player, args)
                else RollResult.Single(DropRollItem("obj.nature_talisman", 1))
            }
        }
    }

    val rare = rsPlayerWeightedTable(total = 128) {
        name("Zulrah rare drop table")
        21 weight "obj.coins" count 3000
        20 weight "obj.keyhalf2" count 1
        20 weight "obj.keyhalf1" count 1
        20 weight gems
        15 weight megaRare
        5 weight "obj.runite_bar" count 1
        3 weight "obj.naturerune" count 67
        3 weight "obj.rune_2h_sword" count 1
        3 weight "obj.rune_battleaxe" count 1
        2 weight "obj.adamant_javelin" count 20
        2 weight "obj.deathrune" count 45
        2 weight "obj.lawrune" count 45
        2 weight "obj.rune_arrow" count 42
        2 weight "obj.steel_arrow" count 150
        2 weight "obj.rune_sq_shield" count 1
        2 weight "obj.dragonstone" count 1
        2 weight "obj.cert_silver_ore" count 100
        1 weight "obj.dragon_med_helm" count 1
        1 weight "obj.rune_kiteshield" count 1
    }

    val common = rsPlayerWeightedTable(total = 249) {
        name("Zulrah common table")
        10 weight flax
        10 weight "obj.cert_battlestaff" count 10
        2 weight "obj.dragon_med_helm" count 1
        2 weight "obj.dragon_halberd" count 1
        12 weight "obj.deathrune" count 250
        12 weight "obj.lawrune" count 200
        12 weight "obj.chaosrune" count 400
        2 weight "obj.cert_snapdragon" count 10
        2 weight "obj.cert_dwarf_weed" count 30
        2 weight "obj.cert_toadflax" count 25
        2 weight "obj.cert_torstol" count 10
        6 weight "obj.palm_tree_seed" count 1
        6 weight "obj.papaya_tree_seed" count 3
        6 weight "obj.calquat_tree_seed" count 2
        4 weight "obj.magic_tree_seed" count 1
        2 weight "obj.toadflax_seed" count 2
        2 weight "obj.snapdragon_seed" count 1
        2 weight "obj.dwarf_weed_seed" count 2
        2 weight "obj.torstol_seed" count 1
        1 weight "obj.spirit_tree_seed" count 1
        11 weight "obj.cert_village_snake_skin" count 35
        11 weight "obj.cert_runite_ore" count 2
        10 weight "obj.cert_blankrune_high" count 1500
        10 weight "obj.cert_yew_logs" count 35
        8 weight "obj.cert_adamantite_bar" count 20
        8 weight "obj.cert_coal" count 200
        8 weight "obj.cert_dragon_bones" count 12
        8 weight "obj.cert_mahogany_logs" count 50
        12 weight sharks
        15 weight "obj.teleportscroll_zulandra" count 4
        9 weight "obj.cert_antidote++4" count 10
        8 weight "obj.xbows_bolt_tips_dragonstone" count 12
        6 weight "obj.cert_grapes" count 250
        6 weight "obj.cert_coconut" count 20
        5 weight "obj.swamp_tar" count 1000
        5 weight "obj.snakeboss_scale" count 500
        10 weight rare
    }

    val lootRoll = rsPlayerWeightedTable(total = 256) {
        name("Zulrah single loot roll")
        1 weight uniques
        255 weight common
    }

    val main = rsPlayerGuaranteedTable {
        add(lootRoll)
        add(lootRoll)
    }

    val tertiaries = rsPlayerTertiaryTable {
        1 outOf 3000 weight "obj.jar_of_swamp" count 1
        onBuilder { brimstoneKeyRoll() }
        // Elite clue (1/75, 1/71 with rewards) and pet (1/4000) await native award systems.
    }
}

@field:RegisterDropTable
@JvmField
public val zulrahDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zulrah Drops",
    npcs = npcs("npc.snakeboss_boss_ranged", "npc.snakeboss_boss_melee", "npc.snakeboss_boss_magic"),
    guaranteed = rsPlayerGuaranteedTable { "obj.snakeboss_scale" count 100..299 },
    mainTable = ZulrahDropTables.main,
    tertiaries = ZulrahDropTables.tertiaries,
)
