package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val nexDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Nex Drops",
    npcs = npcs("npc.nex", "npc.nex_deflect", "npc.nex_dying", "npc.nex_soulsplit", "npc.nex_spawning"),
    mainTable = rsPlayerWeightedTable(total = 258) {
        name("Nex Drops")
        1 weight "obj.nihil_horn" count 1
        1 weight "obj.broken_torva_helm" count 1
        1 weight "obj.broken_torva_chest" count 1
        1 weight "obj.broken_torva_legs" count 1
        1 outOf 516 separate "obj.godwars_godsword_hilt_ancient" count 1
        1 outOf 172 separate "obj.zaryte_vambraces" count 1
        1 outOf 82 separate "obj.blood_essence_inactive" count 1..2
        5 outOf 82 separate "obj.nihil_shard" count 80..85
        1 outOf 209 separate "obj.rune_sword" count 1
        8 outOf 209 separate "obj.nihil_shard" count 85..95
        254 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 500 weight "obj.nexpet" count 1
        // Drops Need Manual (rate): The elite clue scroll drop rate increases to 1/45 after unlocking the elite Combat Achievements rewards tier.
        1 outOf 48 weight "obj.trail_elite_emote_exp1" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Air rune [main/Uncommon]
//   - Fire rune [main/Uncommon]
//   - Blood rune [main/Common]
//   - Death rune [main/Common]
//   - Water rune [main/Uncommon]
//   - Soul rune [main/Common]
//   - Dragon bolts (unf) [main/Common]
//   - Onyx bolts (e) [main/Uncommon]
//   - Steel cannonball [main/Common]
//   - Air orb [main/Common]
//   - Coal [main/Uncommon]
//   - Runite ore [main/Uncommon]
//   - Uncut ruby [main/Common]
//   - Uncut diamond [main/Common]
//   - Wine of zamorak [main/Common]
//   - Shark [main/Common]
//   - Prayer potion(4) [main/Common]
//   - Saradomin brew(4) [main/Common]
//   - Super restore(4) [main/Common]
//   - Coins [main/Uncommon]
//   - Ecumenical key shard [main/Common]
