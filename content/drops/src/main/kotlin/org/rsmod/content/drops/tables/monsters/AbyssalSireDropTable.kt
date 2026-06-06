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
public val abyssalSireDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Abyssal Sire Drops",
    npcs = npcs("npc.abyssalsire_sire_apocalypse", "npc.abyssalsire_sire_panicking", "npc.abyssalsire_sire_puppet", "npc.abyssalsire_sire_stasis_awake", "npc.abyssalsire_sire_stasis_sleeping", "npc.abyssalsire_sire_stasis_stunned", "npc.abyssalsire_sire_wandering"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 100 weight "obj.abyssalsire_unsired" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 139) {
        name("Abyssal Sire Drops")
        6 weight "obj.cert_battlestaff" count 10
        4 weight "obj.cert_rune_full_helm" count 3
        4 weight "obj.cert_mystic_lava_staff" count 2
        4 weight "obj.cert_rune_sword" count 3
        4 weight "obj.cert_rune_platebody" count 2
        3 weight "obj.cert_rune_kiteshield" count 2
        2 weight "obj.cert_mystic_air_staff" count 2
        2 weight "obj.cert_air_battlestaff" count 6
        5 weight "obj.bloodrune" count 190..210
        5 weight "obj.deathrune" count 330..370
        5 weight "obj.lawrune" count 250
        5 weight "obj.soulrune" count 225..275
        4 weight "obj.cosmicrune" count 350
        4 weight "obj.mcannonball" count 300
        5 weight "obj.cert_earth_orb" count 47..53
        5 weight "obj.cert_blankrune_high" count 600
        5 weight "obj.cert_magic_logs" count 50..70
        5 weight "obj.cert_uncut_diamond" count 15
        4 weight "obj.cert_coal" count 380..420
        4 weight "obj.cert_runite_ore" count 6
        3 weight "obj.xbows_bolt_tips_onyx" count 10
        2 weight "obj.cert_runite_bar" count 5
        2 weight "obj.cosmic_soul_catalyst" count 220..270
        11 weight "obj.coins" count 48000..52000
        7 weight "obj.potato_chilli+carne" count 10
        5 weight "obj.4dose2restore" count 4
        4 weight "obj.cert_magic_emerald_necklace" count 25
        2 weight "obj.3dosepotionofsaradomin" count 6
        2 weight "obj.cert_jug_water" count 250..350
        25 outOf 2224 separate "obj.cert_unidentified_kwuarm" count 25
        20 outOf 2224 separate rsPlayerWeightedTable {
            20 weight "obj.cert_unidentified_dwarf_weed" count 25
            20 weight "obj.cert_unidentified_cadantine" count 25
        }
        15 outOf 2224 separate "obj.cert_unidentified_lantadyme" count 25

        3 weight SharedDropTables.rareDrop
        13 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/171 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 180 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
