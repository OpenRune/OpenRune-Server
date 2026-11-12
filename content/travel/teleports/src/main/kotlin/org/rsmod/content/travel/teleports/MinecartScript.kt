package org.rsmod.content.travel.teleports

import jakarta.inject.Inject
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.travel.teleports.RouteTravel.travelAnywhere
import org.rsmod.content.travel.teleports.RouteTravel.travelFromHub
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Minecarts: the Lovakengj network across Kourend (`lovakengj_minecart` rows, any station to any
 * other) and the dwarven carts that meet at Keldagrim (`keldagrim_cart` rows, Keldagrim first).
 * Keldagrim's own platform carts are not in the map, so they are spawned at startup.
 */
class MinecartScript @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    private val kourend: List<TeleportDestination> by lazy { TeleportNetworks.of(KOUREND) }
    private val keldagrim: List<List<TeleportDestination>> by lazy {
        listOf(TeleportNetworks.of(KELDAGRIM))
    }

    override fun ScriptContext.startup() {
        onGameStartup {
            for (tile in KELDAGRIM_PLATFORM) {
                val shape = LocShape.CentrepieceStraight
                locRepo.add(tile, KELDAGRIM_CART, Int.MAX_VALUE, LocAngle.West, shape)
            }
        }
        onOpLoc1(KOUREND_CART) {
            travelAnywhere(kourend, KOUREND_TITLE) { "You ride the minecart to $it." }
        }
        for (cart in KELDAGRIM_CARTS) {
            onOpLoc1(cart) { travelFromHub(keldagrim, TRAIN_TITLE) { "You ride the cart to $it." } }
        }
        onOpLoc2(GE_TRAPDOOR) {
            travelFromHub(keldagrim, TRAIN_TITLE) { "You climb down and ride the cart to $it." }
        }
    }

    private companion object {
        private const val KOUREND = "lovakengj_minecart"
        private const val KELDAGRIM = "keldagrim_cart"
        private const val KOUREND_CART = "loc.lovakengj_minecart"
        private const val GE_TRAPDOOR = "loc.ge_keldagrim_trapdoor"
        private const val KOUREND_TITLE = "Where would you like to travel to?"
        private const val TRAIN_TITLE = "Where would you like to go?"

        private const val KELDAGRIM_CART = "loc.keldagrim_train_cart"

        private val KELDAGRIM_PLATFORM =
            listOf(CoordGrid(2923, 10171, 0), CoordGrid(2923, 10175, 0), CoordGrid(2919, 10169, 0))

        private val KELDAGRIM_CARTS =
            listOf(
                KELDAGRIM_CART,
                "loc.icemountain_train_cart",
                "loc.whitewolfmountain_train_cart",
            )
    }
}
