package org.rsmod.content.bosses.moons

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flattenToList
import dtx.core.singleRollable
import dtx.rs.RSDropTable
import dtx.rs.locs
import dtx.rs.rsGuaranteedTable
import kotlin.math.floor
import kotlin.random.Random
import org.rsmod.api.droptable.DropRateModifiers
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

private val STANDARD_ROLLS = intArrayOf(0, 1, 3, 6)

private val lunarChestStandard =
    rsPlayerWeightedTable(total = 30) {
        name("Lunar Chest Standard")
        5 weight "obj.atlatl_dart" count 72..120
        2 weight "obj.blessed_bone_shard" count 160..179
        1 weight "obj.cert_babywyrm_bones" count 42..54
        3 weight "obj.cert_sun_kissed_bone" count 6..12
        4 weight "obj.swamp_tar" count 79..119
        2 weight "obj.cert_water_orb" count 30..45
        3 weight "obj.cert_bucket_supercompost" count 6..12
        3 weight "obj.cert_softclay" count 15..25
        3 weight "obj.cert_unidentified_harralander" count 12..18
        1 weight "obj.cert_unidentified_irit" count 12..18
        2 weight "obj.maple_seed" count 1..2
        1 weight "obj.yew_seed" count 1
    }

@field:RegisterDropTable
@JvmField
public val lunarChestDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Lunar Chest",
        locs = locs("loc.chest_sun01_default01", "loc.chest_sun01_open01"),
        guaranteed =
            rsGuaranteedTable {
                add(
                    singleRollable {
                        selectResult { player, args -> player.rollLunarChest(args) }
                    }
                )
            },
    )

private fun Player.rollLunarChest(args: ArgMap): RollResult<DropRollItem> {
    val defeated = Moon.entries.filter { vars[it.deadVarbit] != 0 }
    if (defeated.isEmpty()) return RollResult.Nothing()
    val uniques = rollUniques(defeated)
    if (uniques.isNotEmpty()) return RollResult.ListOf(uniques)
    val rolls = List(STANDARD_ROLLS[defeated.size]) { lunarChestStandard.roll(this, args) }
    return rolls.flattenToList()
}

private fun Player.rollUniques(defeated: List<Moon>): List<DropRollItem> {
    val denominator =
        floor(MoonsUniques.CHANCE / DropRateModifiers.multiplierFor(this)).toInt().coerceAtLeast(1)
    var mask = vars[MoonsUniques.MASK_VARP]
    val uniques = mutableListOf<DropRollItem>()
    for (moon in defeated) {
        val pick = MoonsUniques.pick(moon, mask, denominator) { Random.nextInt(it) } ?: continue
        mask = pick.mask
        uniques += DropRollItem(pick.obj, 1)
    }
    VarPlayerIntMapSetter.set(this, MoonsUniques.MASK_VARP, mask)
    return uniques
}
