package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val feverSpiderDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Fever spider Drops",
    npcs = npcs("npc.deal_fever_spiders1"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.deal_spider_body" count 1 condition {
            player -> player.isOnQuest("quest_rumdeal")
        }
    },
    mainTable = rsPlayerWeightedTable(total = 36) {
        name("Fever spider Drops")
        2 weight "obj.adamant_axe" count 1
        3 weight "obj.red_dragonhide_chaps" count 1
        1 weight "obj.adamant_battleaxe" count 1
        2 weight "obj.adamant_med_helm" count 1
        6 weight "obj.unidentified_kwuarm" count 1
        7 weight "obj.bass" count 1..2
        8 weight "obj.coins" count 200..600
        2 weight "obj.cert_blankrune_high" count 100..200
        4 weight "obj.limpwurt_root" count 1
        1 weight nothing()
    },
)
