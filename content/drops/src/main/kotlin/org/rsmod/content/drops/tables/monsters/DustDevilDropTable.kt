package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.shouldDropLootingBag
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val dustDevilDropTableRegular: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dust devil Regular",
    npcs = npcs("npc.kourend_dustdevil", "npc.slayer_dustdevil"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Dust devil Regular")
        3 weight "obj.adamant_axe" count 1
        2 weight "obj.rune_dagger" count 1
        2 weight "obj.red_dragon_vambraces" count 1
        1 weight "obj.black_dragon_vambraces" count 1
        2 weight "obj.air_battlestaff" count 1
        2 weight "obj.earth_battlestaff" count 1
        1 weight "obj.mystic_air_staff" count 1
        1 weight "obj.mystic_earth_staff" count 1
        1 weight "obj.dragon_dagger" count 1
        10 weight "obj.dustrune" count 200
        10 weight "obj.earthrune" count 300
        10 weight "obj.firerune" count 300
        1 weight "obj.firerune" count 50
        7 weight "obj.chaosrune" count 65
        5 weight "obj.rune_arrow" count 12
        4 weight "obj.soulrune" count 15
        1 weight "obj.soulrune" count 50
        28 weight "obj.coins" count 2000..4000
        2 weight "obj.ugthanki_kebab" count 4
        3 weight "obj.cert_mithril_bar" count 10
        1 weight "obj.cert_adamantite_bar" count 4
        1 outOf 4000 separate "obj.dust_battlestaff" count 1

        19 weight SharedDropTables.herb
        11 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
    },
)

@field:RegisterDropTable
@JvmField
public val dustDevilDropTableWilderness: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Dust devil Wilderness Slayer Cave",
    npcs = npcs("npc.wild_cave_dustdevil"),
    mainTable = rsPlayerWeightedTable(total = 126) {
        name("Dust devil Wilderness Slayer Cave")
        3 weight "obj.adamant_axe" count 1
        2 weight "obj.rune_dagger" count 1
        2 weight "obj.red_dragon_vambraces" count 1
        1 weight "obj.black_dragon_vambraces" count 1
        2 weight "obj.air_battlestaff" count 1
        2 weight "obj.earth_battlestaff" count 1
        1 weight "obj.mystic_air_staff" count 1
        1 weight "obj.mystic_earth_staff" count 1
        1 weight "obj.dragon_dagger" count 1
        10 weight "obj.dustrune" count 200
        10 weight "obj.earthrune" count 300
        10 weight "obj.firerune" count 300
        1 weight "obj.firerune" count 50
        7 weight "obj.chaosrune" count 80
        5 weight "obj.rune_arrow" count 12
        4 weight "obj.soulrune" count 20
        1 weight "obj.soulrune" count 50
        28 weight "obj.coins" count 2000..4000
        3 weight "obj.cert_mithril_bar" count 10
        1 weight "obj.cert_adamantite_bar" count 4
        1 outOf 4000 separate "obj.dust_battlestaff" count 1

        19 weight SharedDropTables.herb
        11 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 3 weight "obj.looting_bag" count 1 condition {
            player -> player.shouldDropLootingBag()
        }
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Dragon chainbody [main/1/{{#expr:128*256}}]
//   - Nothing [main/255/{{#expr:128*256}}]

// Unknown wiki drop rates (text rarity — need data collection):
//   - Dragon chainbody [main/1/{{#expr:126*256}}]
//   - Nothing [main/255/{{#expr:126*256}}]
