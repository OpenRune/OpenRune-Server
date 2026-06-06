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
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val thermonuclearSmokeDevilDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Thermonuclear smoke devil Drops",
    npcs = npcs("npc.smoke_devil_boss"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 350 weight "obj.occult_necklace" count 1
        1 outOf 512 weight "obj.smoke_battlestaff" count 1
        1 outOf 2000 weight "obj.dragon_chainbody" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 107) {
        name("Thermonuclear smoke devil Drops")
        5 weight "obj.rune_dagger" count 1
        4 weight "obj.rune_chainbody" count 1
        3 weight "obj.red_dragonhide_body" count 1
        3 weight "obj.rune_battleaxe" count 1
        3 weight "obj.mystic_air_staff" count 1
        3 weight "obj.mystic_fire_staff" count 1
        2 weight "obj.rune_scimitar" count 1
        1 weight "obj.rune_knife_p++" count 50
        1 weight "obj.dragon_scimitar" count 1
        1 weight "obj.staff_of_zaros" count 1
        10 weight "obj.smokerune" count 100
        8 weight "obj.airrune" count 300
        8 weight "obj.soulrune" count 100
        2 weight "obj.rune_arrow" count 100
        1 weight "obj.ugthanki_kebab" count 3
        3 weight "obj.potato_tuna+sweetcorn" count 3
        3 weight "obj.sanfew_salve_4_dose" count 2
        1 weight "obj.4doseprayerrestore" count 2
        2 weight "obj.cert_blankrune_high" count 300
        2 weight "obj.cert_molten_glass" count 100
        2 weight "obj.cert_mithril_bar" count 20
        2 weight "obj.cert_magic_logs" count 20
        2 weight "obj.cert_gold_ore" count 200
        1 weight "obj.cert_diamond" count 10
        15 weight "obj.coins" count 10000..19999
        1 weight "obj.tinderbox" count 1
        1 weight "obj.fire_talisman" count 1
        2 weight "obj.cert_desert_goat_horn" count 50
        2 weight "obj.cert_unidentified_toadflax" count 15
        2 weight "obj.xbows_bolt_tips_onyx" count 12
        2 weight "obj.snapdragon_seed" count 2
        2 weight "obj.ranarr_seed" count 2
        1 weight "obj.cert_grapes" count 100
        1 weight "obj.magic_tree_seed" count 1
        1 weight "obj.dragonstone_ring" count 1
        1 weight "obj.crystal_key" count 1
        5 outOf 856 separate "obj.cert_raw_shark" count 30
        3 outOf 856 separate "obj.shark_lure" count 60

        2 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 2000 weight "obj.jar_of_smoke" count 1
        1 outOf 3000 weight "obj.smokepet" count 1
        // Drops Need Manual (rate): The hard clue scroll drop rate increases to 1/91 after unlocking the hard Combat Achievements rewards tier.
        1 outOf 96 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/475 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 500 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
