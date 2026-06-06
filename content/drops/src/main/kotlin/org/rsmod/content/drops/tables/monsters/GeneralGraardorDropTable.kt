package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val generalGraardorDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "General Graardor Drops",
    npcs = npcs("npc.godwars_bandos_avatar"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.nex_frozen_key_bandos" count 1 condition { player ->
            // Drops Need Manual: Frozen key pieces are only dropped during The Frozen Door miniquest.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 127) {
        name("General Graardor Drops")
        8 weight "obj.rune_longsword" count 1
        8 weight "obj.rune_2h_sword" count 1
        8 weight "obj.rune_platebody" count 1
        6 weight "obj.rune_pickaxe" count 1
        32 weight "obj.coins" count (19500..20000) condition { player ->
            // Drops Need Manual: Coins come from rolls on multiple loot tables, including GDT and RDT.
             true
        }
        8 weight "obj.cert_unidentified_snapdragon" count 3
        8 weight "obj.snapdragon_seed" count 1
        8 weight "obj.4dose2restore" count 3
        8 weight "obj.cert_adamantite_ore" count 15..20
        8 weight "obj.cert_coal" count 115..120
        8 weight "obj.cert_magic_logs" count 15..20
        8 weight "obj.naturerune" count 65..70
        1 weight "obj.coins" count (20100..20600) condition { player ->
            // Drops Need Manual: Coins come from rolls on the unique tables, including the hilt table and the godsword shard table.
             true
        }
        1 outOf 381 separate rsPlayerWeightedTable {
            1 weight "obj.bandos_chestplate" count 1
            1 weight "obj.bandos_skirt" count 1
            1 weight "obj.bandos_boots" count 1
        }
        1 outOf 508 separate "obj.godwars_godsword_hilt_bandos" count 1
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
        1 outOf 5000 weight "obj.bandospet" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        1 outOf 237 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
        }
    },
)
