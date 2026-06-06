package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropBrimstoneKey
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val magmaStrykewyrmDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Magma strykewyrm Drops",
    npcs = npcs("npc.superior_lava_strykewyrm"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Magma strykewyrm Drops")
        5 weight "obj.lava_battlestaff" count 1
        4 weight "obj.adamant_platebody" count 1
        2 weight "obj.rune_longsword" count 1
        2 weight "obj.rune_mace" count 1
        2 weight "obj.rune_med_helm" count 1
        2 weight "obj.rune_pickaxe" count 1
        6 weight "obj.lavarune" count 50..100
        6 weight "obj.deathrune" count 5..10
        6 weight "obj.chaosrune" count 10..20
        6 weight "obj.firerune" count 50..100
        5 weight "obj.lavarune" count 200
        5 weight "obj.rune_dart" count 20..30
        5 weight "obj.rune_knife" count 20..30
        5 weight "obj.rune_arrow" count 20..30
        3 weight "obj.adamant_cannonball" count 12..22
        1 weight "obj.rune_cannonball" count 9..14
        6 weight "obj.coins" count 100..200
        5 weight "obj.potato_chilli+carne" count 1
        5 weight "obj.fire_orb" count 1
        5 weight "obj.uncut_diamond" count 1
        4 weight "obj.cert_ashes" count 2..3
        4 weight "obj.3dose2attack" count 1
        4 weight "obj.3dose2defense" count 1
        4 weight "obj.dragon_arrowheads" count 2..6
        4 weight "obj.cert_coal" count 4..6
        4 weight "obj.cert_adamantite_ore" count 2..4
        4 weight "obj.chaos_talisman" count 1
        4 weight "obj.fire_talisman" count 1
        3 weight "obj.cert_runite_ore" count 1..2
        1 outOf 100 separate "obj.dragon_sheet" count 1

        2 weight SharedDropTables.herb
        5 weight nothing()
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Diamond bolt tips [main/1/{{#expr:1/(5/128 * 25/92) round 1}}]
//   - Emerald bolt tips [main/1/{{#expr:1/(5/128 * 20/92) round 1}}]
//   - Ruby bolt tips [main/1/{{#expr:1/(5/128 * 20/92) round 1}}]
//   - Dragonstone bolt tips [main/1/{{#expr:1/(5/128 * 15/92) round 1}}]
//   - Onyx bolt tips [main/1/{{#expr:1/(5/128 * 7/92) round 1}}]
//   - Sapphire bolt tips [main/1/{{#expr:1/(5/128 * 5/92) round 1}}]
//   - Clue scroll (hard) [tertiary/Unknown]
