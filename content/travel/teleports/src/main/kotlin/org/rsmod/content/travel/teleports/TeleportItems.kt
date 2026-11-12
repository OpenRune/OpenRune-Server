package org.rsmod.content.travel.teleports

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.table.TeleportDestinationRow
import org.rsmod.api.table.TeleportItemRow
import org.rsmod.map.CoordGrid

data class TeleportDestination(val name: String, val coords: CoordGrid, val wilderness: Boolean)

/**
 * One `teleport_item` row: a family of objs that take the player to the same [destinations] and
 * spend charges the same way.
 *
 * @param objs For a charge chain, obj names from the most charges to the fewest. Otherwise every
 *   obj in the family, each behaving the same.
 */
class TeleportItem(
    val label: String,
    val objs: List<String>,
    val destinations: List<TeleportDestination>,
    val teleportType: TeleportType = TeleportType.Standard,
    val uncharged: String? = null,
    val unlimited: Boolean = false,
    val chargeVarbit: String? = null,
    val maxCharges: Int? = null,
    val departMessage: String? = null,
    val arriveMessage: String? = null,
) {
    val isChain: Boolean
        get() = !unlimited && chargeVarbit == null

    init {
        require(objs.isNotEmpty()) { "$label needs at least one obj." }
        require(destinations.isNotEmpty()) { "$label needs at least one destination." }
        require(chargeVarbit == null || maxCharges != null) { "$label needs a charge cap." }
    }

    /** The obj [obj] turns into once a chain charge is spent, or `null` when it crumbles. */
    fun afterCharge(obj: String): String? {
        val index = objs.indexOf(obj)
        if (!isChain || index == -1) {
            return obj
        }
        return objs.getOrNull(index + 1) ?: uncharged
    }

    /** Chain charges left on [obj] once it has been used, or `null` for non-chain items. */
    fun chargesAfter(obj: String): Int? {
        val index = objs.indexOf(obj)
        return if (!isChain || index == -1) null else objs.size - index - 1
    }
}

object TeleportItems {
    val all: List<TeleportItem> by lazy { load() }

    val byObj: Map<String, TeleportItem> by lazy {
        buildMap { all.forEach { item -> item.objs.forEach { put(it, item) } } }
    }

    private fun load(): List<TeleportItem> =
        TeleportItemRow.all().map { row ->
            TeleportItem(
                label = row.label,
                objs = row.objs.map { it.rscm() },
                destinations = TeleportNetworks.of(row.network),
                teleportType = TeleportType.valueOf(row.teleportType),
                uncharged = row.uncharged?.rscm(),
                unlimited = row.unlimited,
                chargeVarbit =
                    row.chargeVarbit?.let { RSCM.getReverseMapping(RSCMType.VARBIT, it) },
                maxCharges = row.maxCharges,
                departMessage = row.departMessage,
                arriveMessage = row.arriveMessage,
            )
        }

    private fun ItemServerType.rscm(): String = RSCM.getReverseMapping(RSCMType.OBJ, id)
}

/** Every network in `teleport_destination`, each in the order its rows are numbered. */
object TeleportNetworks {
    private val all: Map<String, List<TeleportDestination>> by lazy {
        TeleportDestinationRow.all()
            .groupBy { it.network }
            .mapValues { (_, rows) ->
                rows.sortedBy { it.ordinal }.map {
                    TeleportDestination(it.name, it.coords, it.wilderness)
                }
            }
    }

    fun of(network: String): List<TeleportDestination> =
        all[network] ?: error("No teleport destinations for network `$network`.")
}
