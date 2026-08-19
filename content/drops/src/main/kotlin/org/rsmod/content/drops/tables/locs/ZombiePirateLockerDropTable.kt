package org.rsmod.content.drops.tables.locs

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val zombiePirateLockerDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Zombie Pirate's Locker",
        locs = locs("loc.wildy_pirate_boat_locker"),
        mainTable =
            rsPlayerWeightedTable(total = 173) {
                name("Zombie Pirate's Locker")
                4 weight "obj.bloodrune" count 60..120
                4 weight "obj.deathrune" count 60..180
                4 weight "obj.chaosrune" count 60..180
                4 weight "obj.mindrune" count 60..180
                8 weight "obj.battlestaff" count 2..6
                6 weight "obj.adamant_platebody" count 2
                6 weight "obj.rune_med_helm" count 2
                6 weight "obj.rune_warhammer" count 2
                6 weight "obj.rune_battleaxe" count 2
                6 weight "obj.rune_longsword" count 2
                6 weight "obj.rune_sword" count 2
                6 weight "obj.rune_mace" count 2
                1 weight "obj.dragon_dagger" count 2
                1 weight "obj.dragon_longsword" count 2
                1 weight "obj.dragon_scimitar" count 2
                12 weight "obj.blighted_sack_icebarrage" count 20..60
                12 weight "obj.cert_blighted_anglerfish" count 10..30
                12 weight "obj.cert_blighted_mantaray" count 10..30
                12 weight "obj.cert_blighted_karambwan" count 10..30
                12 weight "obj.cert_blighted_4dose2restore" count 2..6
                12 weight "obj.coins" count 2000..16000
                12 weight "obj.mcannonball" count 40..200
                12 weight "obj.cert_gold_ore" count 10..30
                8 weight "obj.adamant_seed" count 10..20
            },
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf 275 weight "obj.wilderness_blip_blocking_scroll" count 1
            },
    )
