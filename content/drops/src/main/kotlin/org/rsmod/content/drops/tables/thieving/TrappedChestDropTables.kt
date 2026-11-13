package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val tenCoinChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, 10 coins (Thieving)",
    locs = locs("loc.trapchest1"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.coins" count 10
    },
)

@field:RegisterDropTable
@JvmField
public val natureRuneChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, nature rune (Thieving)",
    locs = locs("loc.trapchest2"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.naturerune" count 1
        "obj.coins" count 3
    },
)

@field:RegisterDropTable
@JvmField
public val fiftyCoinChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, 50 coins (Thieving)",
    locs = locs("loc.trapchest3"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.coins" count 50
    },
)

@field:RegisterDropTable
@JvmField
public val steelArrowtipsChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, steel arrowtips (Thieving)",
    locs = locs("loc.pickchest3"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.steel_arrowheads" count 5
        "obj.coins" count 20
    },
)

@field:RegisterDropTable
@JvmField
public val bloodRuneChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, blood runes (Thieving)",
    locs = locs("loc.trapchest4"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.bloodrune" count 2
        "obj.coins" count 500
    },
)

@field:RegisterDropTable
@JvmField
public val ardougneCastleChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, Ardougne Castle (Thieving)",
    locs = locs("loc.trapchest5"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.coins" count 1000
        "obj.raw_shark" count 1
        "obj.adamantite_ore" count 1
        "obj.uncut_sapphire" count 1
    },
)
