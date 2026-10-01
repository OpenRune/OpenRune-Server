package org.rsmod.content.raids.toa.raid.supplies

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.floor
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

internal enum class ToaSupply(private val base: String, val maxDoses: Int) {
    NECTAR("obj.toa_supply_heal", 4),
    SILK_DRESSING("obj.toa_supply_heal_overtime", 2),
    TEARS("obj.toa_supply_prayer", 4),
    CRYSTAL_SCARAB("obj.toa_supply_prayer_overtime", 2),
    ADRENALINE("obj.toa_supply_energy", 2),
    SALTS("obj.toa_supply_stats", 2),
    AMBROSIA("obj.toa_supply_panicheal", 2);

    fun obj(doses: Int): String = "${base}_$doses"

    fun invObj(doses: Int): InvObj = InvObj(obj(doses))

    companion object {
        private val BY_ID: Map<Int, Dose> by lazy {
            entries
                .flatMap { supply -> (1..supply.maxDoses).map { supply to it } }
                .associate { (supply, doses) ->
                    supply.obj(doses).asRSCM(RSCMType.OBJ) to Dose(supply, doses)
                }
        }

        fun of(obj: InvObj?): Dose? = obj?.let { BY_ID[it.id] }

        val ALL_OBJS: List<String> by lazy {
            entries.flatMap { supply -> (1..supply.maxDoses).map(supply::obj) }
        }
    }
}

internal data class Dose(val supply: ToaSupply, val doses: Int)

internal data class SupplyStack(val supply: ToaSupply, val count: Int)

class ToaSupplies {
    private var offersMade = 0
    private val unclaimed = HashSet<Player>()
    private val announced = HashSet<Player>()

    internal var bundles: List<List<SupplyStack>> = emptyList()
        private set

    fun offerIfDue(raid: ToaRaid): Boolean {
        val offer = OFFER_AFTER_PATHS.indexOf(raid.pathsCompleted.size) + 1
        if (offer == 0) return false
        if (offersMade < offer) {
            offersMade = offer
            bundles = roll(raid)
            unclaimed.clear()
            unclaimed += raid.players
            announced.clear()
        }
        return true
    }

    fun canClaim(player: Player): Boolean = player in unclaimed

    fun claimed(player: Player) {
        unclaimed -= player
    }

    fun forfeit(player: Player) {
        unclaimed -= player
    }

    fun announce(player: Player): Boolean = player in unclaimed && announced.add(player)

    private fun roll(raid: ToaRaid): List<List<SupplyStack>> {
        val random = raid.deps.random
        val factor =
            when {
                raid.isActive(NO_HELP_NEEDED) -> 0.10
                raid.isActive(NEED_LESS_HELP) -> 0.33
                raid.isActive(NEED_SOME_HELP) -> 0.66
                else -> 1.0
            }
        fun amount(base: Int, guaranteed: Boolean): Int {
            val scaled = floor(base * factor).toInt()
            return if (guaranteed) scaled.coerceAtLeast(1) else scaled
        }
        fun rare(): Int {
            val rolled = random.of(maxExclusive = CHAOS_RARE_ONE_IN) == 0
            return if (rolled) amount(1, false) else 0
        }

        val diet = raid.isActive(ON_A_DIET)
        val life =
            listOf(
                SupplyStack(ToaSupply.NECTAR, amount(5, true)),
                SupplyStack(ToaSupply.TEARS, amount(5, true)),
                SupplyStack(ToaSupply.SILK_DRESSING, if (diet) 0 else amount(3, false)),
                SupplyStack(ToaSupply.CRYSTAL_SCARAB, amount(if (diet) 5 else 3, true)),
                SupplyStack(ToaSupply.AMBROSIA, amount(if (diet) 3 else 2, true)),
            )
        val chaos =
            listOf(
                SupplyStack(ToaSupply.NECTAR, amount(random.of(1, 8), true)),
                SupplyStack(ToaSupply.TEARS, amount(random.of(0, 6), false)),
                SupplyStack(ToaSupply.SALTS, amount(random.of(0, 2), false)),
                SupplyStack(ToaSupply.AMBROSIA, rare()),
                SupplyStack(ToaSupply.ADRENALINE, rare()),
            )
        val power =
            listOf(
                SupplyStack(ToaSupply.SALTS, amount(2, true)),
                SupplyStack(ToaSupply.ADRENALINE, amount(1, true)),
            )
        return listOf(life, chaos, power).map { bundle -> bundle.filter { it.count > 0 } }
    }

    private companion object {
        val OFFER_AFTER_PATHS = listOf(2, 4)
        const val CHAOS_RARE_ONE_IN = 8

        const val NEED_SOME_HELP = "Need Some Help?"
        const val NEED_LESS_HELP = "Need Less Help?"
        const val NO_HELP_NEEDED = "No Help Needed"
        const val ON_A_DIET = "On a Diet"
    }
}
