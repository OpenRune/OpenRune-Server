package org.rsmod.content.drops.tables.thieving

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.singleRollable
import dtx.rs.RSDropTable
import dtx.rs.RSWeightedTable
import dtx.rs.locs
import dtx.rs.rsGuaranteedTable
import kotlin.random.Random
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.entity.Player

private const val CLUE = "obj.trail_clue_hard_map001"
private const val CLUE_DENOMINATOR = 99

private val MEDIUM_DIARY_TABLE: RSWeightedTable<Player, DropRollItem> =
    rsPlayerWeightedTable(total = 66) {
        name("Chest, Rogues' Castle, medium diary (Thieving)")
        11 weight "obj.naturerune" count 40
        10 weight "obj.cert_red_spiders_eggs" count 6
        10 weight "obj.lawrune" count 40
        8 weight "obj.cert_coal" count 20
        6 weight "obj.coins" count 4500
        3 weight "obj.cert_vile_ashes" count 15
        3 weight "obj.cert_uncut_diamond" count 3
        2 weight "obj.cert_uncut_diamond" count 5
        2 weight "obj.cert_uncut_emerald" count 10
        2 weight "obj.blighted_sack_icebarrage" count 13
        1 weight "obj.cert_iron_ore" count 40
        1 weight "obj.chaosrune" count 60
        1 weight "obj.deathrune" count 50
        1 weight "obj.cert_blighted_mantaray" count 20
        1 weight "obj.cert_blighted_anglerfish" count 15
        1 weight "obj.cert_uncut_sapphire" count 15
        2 weight "obj.2doseprayerrestore" count 1
        1 weight "obj.cert_dragonstone" count 2
    }

private val HARD_DIARY_TABLE: RSWeightedTable<Player, DropRollItem> =
    rsPlayerWeightedTable(total = 66) {
        name("Chest, Rogues' Castle, hard diary (Thieving)")
        11 weight "obj.naturerune" count 50
        10 weight "obj.cert_red_spiders_eggs" count 7
        10 weight "obj.lawrune" count 50
        8 weight "obj.cert_coal" count 25
        6 weight "obj.coins" count 5625
        3 weight "obj.cert_vile_ashes" count 18
        3 weight "obj.cert_uncut_diamond" count 3
        2 weight "obj.cert_uncut_diamond" count 6
        2 weight "obj.cert_uncut_emerald" count 12
        2 weight "obj.blighted_sack_icebarrage" count 16
        1 weight "obj.cert_iron_ore" count 50
        1 weight "obj.chaosrune" count 75
        1 weight "obj.deathrune" count 62
        1 weight "obj.cert_blighted_mantaray" count 25
        1 weight "obj.cert_blighted_anglerfish" count 18
        1 weight "obj.cert_uncut_sapphire" count 18
        2 weight "obj.2doseprayerrestore" count 1
        1 weight "obj.cert_dragonstone" count 2
    }

@field:RegisterDropTable
@JvmField
public val roguesCastleChestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chest, Rogues' Castle (Thieving)",
    locs = locs("loc.wilderness_rogue_chest"),
    guaranteed = rsGuaranteedTable {
        add(singleRollable { selectResult { player, args -> player.rollRoguesChest(args) } })
    },
)

/** The chest holds nothing without the medium Wilderness Diary; the hard diary swaps the table. */
private fun Player.rollRoguesChest(args: ArgMap): RollResult<DropRollItem> {
    if (vars["varbit.wilderness_diary_medium_complete"] != 1) return RollResult.Nothing()
    val clueChances = if ("obj.ring_of_wealth_i" in worn) 2 else 1
    if (Random.nextInt(CLUE_DENOMINATOR) < clueChances) {
        return RollResult.Single(
            DropRollItem(CLUE, 1, transformObj = { it.clueScrollTransformObj(CLUE) })
        )
    }
    val hard = vars["varbit.wilderness_diary_hard_complete"] == 1
    return (if (hard) HARD_DIARY_TABLE else MEDIUM_DIARY_TABLE).roll(this, args)
}
