package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.wearingRingOfWealth
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val scorpiaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Scorpia Drops",
    npcs = npcs("npc.scorpia"),
    preRoll = rsPlayerPrerollTable {
        1 outOf 256 weight "obj.odium_shard3" count 1
        1 outOf 256 weight "obj.malediction_shard3" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Scorpia Drops")
        6 weight "obj.cert_battlestaff" count 5..8
        5 weight "obj.rune_2h_sword" count 1
        5 weight "obj.rune_pickaxe" count 1
        5 weight "obj.rune_kiteshield" count 1
        4 weight "obj.rune_chainbody" count 1
        4 weight "obj.rune_platelegs" count 1
        4 weight "obj.rune_scimitar" count 1
        4 weight "obj.rune_warhammer" count 1
        4 weight "obj.mystic_earth_staff" count 1
        1 weight "obj.mystic_robe_top" count 1
        1 weight "obj.mystic_robe_bottom" count 1
        1 weight "obj.dragon_scimitar" count 1
        1 weight "obj.dragon_2h_sword" count 1
        8 weight "obj.deathrune" count 100..150
        8 weight "obj.bloodrune" count 100..150
        8 weight "obj.chaosrune" count 150..200
        5 weight "obj.cert_unidentified_kwuarm" count 10..15
        5 weight "obj.cert_unidentified_dwarf_weed" count 10..15
        5 weight "obj.cert_unidentified_torstol" count 10..15
        5 weight "obj.cert_unidentified_snapdragon" count 4..7
        6 weight "obj.cert_uncut_ruby" count 15..20
        4 weight "obj.cert_uncut_diamond" count 10..15
        4 weight "obj.cert_runite_ore" count 3
        4 weight "obj.dragon_javelin_head" count 30..50
        2 weight "obj.xbows_bolt_tips_onyx" count 6..10
        7 weight "obj.coins" count 25002..34962
        5 weight "obj.cert_blighted_anglerfish" count 15..25
        5 weight "obj.cert_blighted_4dose2restore" count 5
        2 weight "obj.tablet_wildycrabs" count 2
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
        1 outOf 18 weight "obj.arceuus_corpse_scorpion" count 1
        onBuilder { brimstoneKeyRoll() }
        // Drops Need Manual (rate): Due to the pre-rolled malediction and odium shard table, Scorpia's offspring is 1/128th rarer than its intended 1/2,000 rarity.
        1 outOf 2016 weight "obj.scorpia_pet" count 1
        1 outOf 95 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
        1 outOf 50 weight "obj.trail_clue_hard_map001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_hard_map001")
        }
    },
)
