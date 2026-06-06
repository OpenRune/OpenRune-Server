package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val scarabMageDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Scarab Mage Drops",
    npcs = npcs("npc.bcs_scarab_mage", "npc.contact_insectoid_mage", "npc.contact_insectoid_mage_b"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Scarab Mage Drops")
        3 weight "obj.adamant_kiteshield" count 1
        2 weight "obj.cert_battlestaff" count 3
        2 weight "obj.rune_dagger" count 1
        1 weight "obj.rune_mace" count 1
        1 weight "obj.rune_sq_shield" count 1
        4 weight "obj.adamant_arrow" count 36
        4 weight "obj.bloodrune" count 18
        2 weight "obj.firerune" count 250
        2 weight "obj.cosmicrune" count 35
        2 weight "obj.lavarune" count 150
        2 weight "obj.rune_arrow" count 18
        7 weight "obj.cert_raw_lobster" count 15
        2 weight "obj.cert_adamantite_ore" count 14
        6 weight "obj.cert_coal" count 32
        2 weight "obj.cert_desert_goat_horn" count 6
        2 weight "obj.cert_mithril_bar" count 22
        4 weight "obj.cert_raw_bass" count 24
        3 weight "obj.cert_uncut_sapphire" count 4
        32 weight "obj.coins" count 2000..3000
        1 weight "obj.water_skin4" count 1
        4 weight "obj.bass" count 6
        1 weight "obj.lobster" count 5
        1 weight "obj.salamander_tar_orange" count 80

        20 weight SharedDropTables.herb
        1 weight SharedDropTables.rareDrop
        15 weight SharedDropTables.gem
        2 weight SharedDropTables.rareSeed
    },
)
