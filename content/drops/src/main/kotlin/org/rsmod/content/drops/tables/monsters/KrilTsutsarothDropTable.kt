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
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val krilTsutsarothDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "K'ril Tsutsaroth Drops",
    npcs = npcs("npc.godwars_zamorak_avatar"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.nex_frozen_key_zamorak" count 1 condition { player ->
            // Drops Need Manual: [[Frozen key (The Frozen Door)
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 127) {
        name("K'ril Tsutsaroth Drops")
        1 weight "obj.steam_battlestaff" count 1
        1 weight "obj.zamorak_spear" count 1
        8 weight "obj.adamant_arrow_p++" count 295..300
        8 weight "obj.rune_scimitar" count 1
        8 weight "obj.adamant_platebody" count 1
        7 weight "obj.rune_platelegs" count 1
        2 weight "obj.dragon_dagger_p++" count 1
        8 weight dropRollable(DropRollItem("obj.3dose2attack", 3, bonusDrops = listOf(
            DropRollItem("obj.3dose2strength", 3),
        )))
        8 weight dropRollable(DropRollItem("obj.3dose2restore", 3, bonusDrops = listOf(
            DropRollItem("obj.3dosepotionofzamorak", 3),
        )))
        37 weight "obj.coins" count (19500..20000) condition { player ->
            // Drops Need Manual: Coins come from rolls on all loot tables, including the unique table, GDT and RDT.
             true
        }
        8 weight "obj.cert_unidentified_lantadyme" count 10
        8 weight "obj.lantadyme_seed" count 3
        8 weight "obj.deathrune" count 120..125
        8 weight "obj.bloodrune" count 80..85
        1 outOf 508 separate rsPlayerWeightedTable {
            1 weight "obj.sotd" count 1
            1 weight "obj.godwars_godsword_hilt_zamorak" count 1
        }
        1 outOf 762 separate rsPlayerWeightedTable {
            1 weight "obj.godwars_godsword_blade1" count 1
            1 weight "obj.godwars_godsword_blade2" count 1
            1 weight "obj.godwars_godsword_blade3" count 1
        }
        7 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll() }
        1 outOf 5000 weight "obj.zamorakpet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/237 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 250 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
