package org.rsmod.content.drops.tables.locs

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val muddyChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Muddy Chest",
        locs = locs("loc.muddy_chestclosed"),
        guaranteed =
            rsPlayerGuaranteedTable {
                "obj.uncut_ruby" count 1
                "obj.cert_mithril_bar" count 2
                "obj.lawrune" count 5
                "obj.deathrune" count 5
                "obj.chaosrune" count 15
            },
        tertiaries =
            rsPlayerTertiaryTable {
                3 outOf
                    40 chance
                    rsPlayerWeightedTable(total = 7) {
                        1 weight "obj.cert_blighted_mantaray" count 25
                        1 weight "obj.cert_blighted_karambwan" count 25
                        1 weight "obj.blighted_sack_icebarrage" count 25
                        1 weight "obj.cert_blighted_anglerfish" count 15
                        1 weight "obj.cert_blighted_4dose2restore" count 3
                        2 weight "obj.slayer_wilderness_key" count 1
                    }
            },
    )
