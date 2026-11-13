package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

private val elfMainTable = rsPlayerWeightedTable(total = 128) {
    name("Elf (Pickpocket)")
    105 weight "obj.pickpocket_coin_pouch_elf" count 1
    8 weight "obj.deathrune" count 2
    6 weight "obj.jug_wine" count 1
    5 weight "obj.naturerune" count 3
    2 weight "obj.fire_orb" count 1
    1 weight "obj.diamond" count 1
    1 weight "obj.gold_ore" count 1
}

@field:RegisterDropTable
@JvmField
public val elfPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Elf (Pickpocket)",
    pickpockets = pickpockets("elf"),
    mainTable = rsPlayerWeightedTable(total = 35) {
        name("Elf, Prifddinas pre-roll (Pickpocket)")
        1 weight "obj.prif_crystal_shard" count 1
        34 weight rsPlayerWeightedTable(total = 1024) {
            name("Elf, Prifddinas seed pre-roll (Pickpocket)")
            1 weight "obj.prif_teleport_seed" count 1
            1023 weight elfMainTable
        }
    },
)

@field:RegisterDropTable
@JvmField
public val lletyaElfPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Elf, Lletya (Pickpocket)",
    pickpockets = pickpockets("elf_lletya"),
    mainTable = elfMainTable,
)
