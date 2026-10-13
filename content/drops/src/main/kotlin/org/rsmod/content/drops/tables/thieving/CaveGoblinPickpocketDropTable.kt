package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val caveGoblinPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cave Goblin (Pickpocket)",
    pickpockets = pickpockets("cave_goblin"),
    mainTable = rsPlayerWeightedTable(total = 20) {
        name("Cave Goblin (Pickpocket)")
        7 weight "obj.pickpocket_coin_pouch_cavegoblin" count 1
        1 weight "obj.dorgesh_bat_shish" count 1
        1 weight "obj.dorgesh_crispy_froglegs" count 1
        1 weight "obj.dorgesh_wall_beast_fingers" count 1
        1 weight "obj.dorgesh_frog_burger" count 1
        1 weight "obj.dorgesh_frog_spawn_gumbo" count 1
        1 weight "obj.dorgesh_green_gloop_soup" count 1
        1 weight "obj.bullseye_lantern_unlit" count 1
        1 weight "obj.dorgesh_wire" count 1..2
        1 weight "obj.iron_ore" count 1..4
        1 weight "obj.oil_lantern_unlit" count 1
        1 weight "obj.swamp_tar" count 1
        1 weight "obj.tinderbox" count 1
        1 weight "obj.torch_unlit" count 1
    },
)
