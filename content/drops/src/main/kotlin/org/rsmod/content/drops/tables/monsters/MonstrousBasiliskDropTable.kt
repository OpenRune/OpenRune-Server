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
import org.rsmod.content.drops.hasCompletedQuest
import org.rsmod.content.drops.isOnQuest
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val monstrousBasiliskDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Monstrous basilisk Drops",
    npcs = npcs("npc.superior_basilisk"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.konar_key" count 1 killCondition {
            player, npc, areaChecker -> player.shouldDropBrimstoneKey(npc, areaChecker)
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Monstrous basilisk Drops")
        3 weight "obj.mithril_axe" count 1
        2 weight "obj.mithril_spear" count 1
        3 weight "obj.steel_battleaxe" count 1
        1 weight "obj.adamant_full_helm" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.rune_dagger" count 1
        8 weight "obj.waterrune" count 75
        5 weight "obj.naturerune" count 15
        3 weight "obj.lawrune" count 3
        1 weight "obj.naturerune" count 37
        3 weight "obj.adamantite_ore" count 1
        29 weight "obj.coins" count 44
        17 weight "obj.coins" count 200
        5 weight "obj.coins" count 132
        5 weight "obj.coins" count 11
        1 weight "obj.coins" count 440
        1 outOf 512 separate "obj.mystic_hat_light" count 1

        35 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 4 weight "obj.rag_baby_basilisk_bone" count 1 condition {
            player -> player.isOnQuest("quest_ragandboneman2")
        }
        1 outOf 2000 weight "obj.poh_trophydrop_basilisk" count 1
    },
)
