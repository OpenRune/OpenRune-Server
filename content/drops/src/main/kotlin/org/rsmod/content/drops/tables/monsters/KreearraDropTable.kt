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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val kreearraDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Kree'arra Drops",
    npcs = npcs("npc.godwars_armadyl_avatar"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.feather" count 1..16
        "obj.nex_frozen_key_armadyl" count 1 condition { player ->
            // Drops Need Manual: Frozen key pieces are only dropped during The Frozen Door miniquest.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 127) {
        name("Kree'arra Drops")
        8 weight "obj.black_dragonhide_body" count 1
        8 weight "obj.xbows_crossbow_runite" count 1
        8 weight "obj.mindrune" count 586..601
        8 weight "obj.rune_arrow" count 100..105
        8 weight "obj.xbows_crossbow_bolts_runite" count 20..25
        8 weight "obj.xbows_crossbow_bolts_runite_tipped_dragonstone_enchanted" count 5..10
        44 weight "obj.coins" count (19500..20000) condition { player ->
            // Drops Need Manual: Coins come from rolls on multiple loot tables, including GDT and RDT.
             true
        }
        8 weight dropRollable(DropRollItem("obj.3doserangerspotion", 3, bonusDrops = listOf(
            DropRollItem("obj.3dose2defense", 3),
        )))
        8 weight "obj.cert_unidentified_dwarf_weed" count 8..13
        8 weight "obj.dwarf_weed_seed" count 2
        1 weight "obj.coins" count (20500..21000) condition { player ->
            // Drops Need Manual: Coins come from rolls on the unique tables, including the hilt table and the godsword shard table.
             true
        }
        1 weight "obj.crystal_key" count 1
        1 weight "obj.yew_seed" count 1
        1 outOf 381 separate rsPlayerWeightedTable {
            1 weight "obj.armadyl_helmet" count 1
            1 weight "obj.armadyl_chestplate" count 1
            1 weight "obj.armadyl_skirt" count 1
        }
        1 outOf 508 separate "obj.godwars_godsword_hilt_armadyl" count 1
        1 outOf 762 separate rsPlayerWeightedTable {
            1 weight "obj.godwars_godsword_blade1" count 1
            1 weight "obj.godwars_godsword_blade2" count 1
            1 weight "obj.godwars_godsword_blade3" count 1
        }
        8 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5000 weight "obj.armadylpet" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 237 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
