package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val zalcanoDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Zalcano Drops",
    npcs = npcs("npc.zalcano", "npc.zalcano_weak"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.prif_crystal_shard" count 1..3
    },
    mainTable = rsPlayerWeightedTable(total = 36) {
        name("Zalcano Drops")
        1 weight "obj.bloodrune" count 64..480
        1 weight "obj.cosmicrune" count 295..926
        1 weight "obj.deathrune" count 201..832
        1 weight "obj.lawrune" count 166..790
        1 weight "obj.soulrune" count 47..392
        1 weight "obj.naturerune" count 1..937
        3 weight "obj.cert_silver_ore" count 86..800
        1 weight "obj.cert_coal" count 169..938
        2 weight "obj.cert_mithril_ore" count 56..469
        2 weight "obj.cert_adamantite_ore" count 51..343
        2 weight "obj.cert_runite_ore" count 3..31
        3 weight "obj.cert_gold_ore" count 86..756
        3 weight "obj.cert_steel_bar" count 16..699
        3 weight "obj.cert_mithril_bar" count 53..501
        2 weight "obj.cert_adamantite_bar" count 13..111
        2 weight "obj.cert_runite_bar" count 3..27
        2 weight "obj.cert_uncut_diamond" count 2..21
        2 weight "obj.cert_uncut_dragonstone" count 1..12
        2 weight "obj.xbows_bolt_tips_onyx" count 1..45
        1 weight "obj.cert_blankrune_high" count 784..5500
    },
    tertiaries = rsPlayerTertiaryTable {
        39 outOf 8000 weight "obj.prif_tool_seed" count 1
        1 outOf 750 weight "obj.zalcano_pickaxe_kit" count 1
        1 outOf 2250 weight "obj.zalcanopet" count 1
        1 outOf 8000 weight "obj.uncut_onyx" count 1
    },
)
