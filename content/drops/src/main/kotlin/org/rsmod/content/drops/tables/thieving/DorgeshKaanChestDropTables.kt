package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

private val CAVE_GOBLIN_WIRE =
    DropRollItem(
        obj = "obj.dorgesh_wire",
        count = 1,
        bonusDrops =
            listOf(
                DropRollItem(
                    obj = "obj.dorgesh_wire",
                    count = 1,
                    condition = { it.vars["varbit.lumbridge_diary_elite_complete"] == 1 },
                ),
            ),
    )

// Zanik's and Oldak's chests share the average chest's loc, so they roll this table too.
@field:RegisterDropTable
@JvmField
public val dorgeshKaanAverageChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, Dorgesh-Kaan average (Thieving)",
    locs = locs("loc.dorgesh_average_chest_closed"),
    mainTable = rsPlayerWeightedTable(total = 7) {
        name("Chest, Dorgesh-Kaan average (Thieving)")
        3 weight "obj.coins" count 1..250
        1 weight "obj.oil_lantern_unlit" count 1
        1 weight "obj.bullseye_lantern_unlit" count 1
        1 weight "obj.cave_goblin_mining_helmet_unlit" count 1
        1 weight CAVE_GOBLIN_WIRE
    },
)

@field:RegisterDropTable
@JvmField
public val dorgeshKaanRichChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, Dorgesh-Kaan rich (Thieving)",
    locs = locs("loc.dorgesh_rich_chest_closed"),
    mainTable = rsPlayerWeightedTable(total = 15) {
        name("Chest, Dorgesh-Kaan rich (Thieving)")
        1 weight "obj.uncut_sapphire" count 1
        1 weight "obj.uncut_emerald" count 1
        1 weight "obj.uncut_ruby" count 1
        1 weight "obj.uncut_diamond" count 1
        1 weight "obj.uncut_opal" count 1
        1 weight "obj.uncut_jade" count 1
        1 weight "obj.uncut_red_topaz" count 1
        1 weight "obj.bullseye_lantern_unlit" count 1
        1 weight "obj.cave_goblin_mining_helmet_unlit" count 1
        1 weight "obj.dorgesh_frog_armour_bottoms" count 1
        1 weight "obj.dorgesh_frog_armour_top" count 1
        1 weight "obj.iron_bar" count 1
        1 weight CAVE_GOBLIN_WIRE
        1 weight "obj.dorgesh_light_bulb" count 1
        1 weight "obj.dorgesh_lightbulb_nofilament" count 1
    },
)
