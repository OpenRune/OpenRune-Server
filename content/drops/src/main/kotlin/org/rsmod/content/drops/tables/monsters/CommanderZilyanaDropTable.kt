package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.dropRollable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val commanderZilyanaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Commander Zilyana Drops",
    npcs = npcs("npc.godwars_saradomin_avatar"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.nex_frozen_key_saradomin" count 1 condition { player ->
            // Drops Need Manual: Frozen key pieces are only dropped during The Frozen Door miniquest.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 133) {
        name("Commander Zilyana Drops")
        1 weight "obj.saradomin_sword" count 1
        8 weight "obj.adamant_platebody" count 1
        8 weight "obj.rune_dart" count 35..40
        8 weight "obj.rune_kiteshield" count 1
        8 weight "obj.rune_plateskirt" count 1
        8 weight "obj.4doseprayerrestore" count 3
        6 weight dropRollable(DropRollItem("obj.3dosepotionofsaradomin", 3, bonusDrops = listOf(
            DropRollItem("obj.3dose2restore", 3),
        )))
        6 weight "obj.4dose2restore" count 3
        8 weight dropRollable(DropRollItem("obj.3dose2defense", 3, bonusDrops = listOf(
            DropRollItem("obj.1dose1magic", 3),
        )))
        8 weight "obj.3dose1magic" count 3
        31 weight "obj.coins" count (19500..20000) condition { player ->
            // Drops Need Manual: Coins come from rolls on all loot tables, including the unique table, GDT and RDT.
             true
        }
        8 weight "obj.cert_diamond" count 6
        8 weight "obj.lawrune" count 95..100
        8 weight "obj.cert_unidentified_ranarr" count 5
        8 weight "obj.ranarr_seed" count 2
        1 weight "obj.magic_tree_seed" count 1
        1 outOf 254 separate "obj.saradomin_light" count 1
        1 outOf 508 separate rsPlayerWeightedTable {
            1 weight "obj.acb" count 1
            1 weight "obj.godwars_godsword_hilt_saradomin" count 1
        }
        1 outOf 762 separate rsPlayerWeightedTable {
            1 weight "obj.godwars_godsword_blade1" count 1
            1 weight "obj.godwars_godsword_blade2" count 1
            1 weight "obj.godwars_godsword_blade3" count 1
        }
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 5000 weight "obj.saradominpet" count 1
        1 outOf 237 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
