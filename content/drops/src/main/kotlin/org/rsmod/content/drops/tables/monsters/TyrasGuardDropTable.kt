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
public val tyrasGuardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Tyras guard Drops",
    npcs = npcs("npc.regicide_old_camp_guard", "npc.regicide_tyras_camp_guard", "npc.regicide_tyras_camp_tent_guard", "npc.regicide_tyras_guard", "npc.sailing_transport_tyras_guard", "npc.sote_tyras_guard", "npc.sote_tyras_guard_battle", "npc.sote_tyras_guard_battle_heal", "npc.sote_tyras_guard_catapult", "npc.sote_tyras_guard_cutscene"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Tyras guard Drops")
        4 weight "obj.steel_2h_sword" count 1
        3 weight "obj.steel_axe" count 1
        3 weight "obj.steel_battleaxe" count 1
        1 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.adamant_platelegs" count 1
        1 weight "obj.rune_full_helm" count 1
        8 weight "obj.firerune" count 75
        5 weight "obj.chaosrune" count 15
        3 weight "obj.deathrune" count 5
        1 weight "obj.firerune" count 37
        40 weight "obj.coins" count 132
        29 weight "obj.coins" count 44
        10 weight "obj.coins" count 220
        7 weight "obj.coins" count 11
        1 weight "obj.coins" count 460
        3 weight "obj.tuna" count 1
        2 weight "obj.gold_bar" count 1
        1 weight "obj.thread" count 10

        5 weight SharedDropTables.gem
    },
)
