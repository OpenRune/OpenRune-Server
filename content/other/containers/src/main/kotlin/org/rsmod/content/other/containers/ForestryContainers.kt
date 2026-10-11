package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.game.entity.Player

/** The basket has two shared stores; opening it affects only log collection. */
internal object ForestryContainers {
    const val KIT = "obj.forestry_kit"
    const val CLOSED = "obj.forestry_basket_closed"
    const val OPEN = "obj.forestry_basket_open"
    const val HARNESS = "obj.forestry_sturdy_harness"
    val baskets = listOf(CLOSED, OPEN)
    val basketIds get() = baskets.map { it.asRSCM() }.toSet()
    val kit get() = ContainerCatalog.byId[KIT.asRSCM()]!!
    val logs get() = ContainerCatalog.byId["obj.log_basket_closed".asRSCM()]!!
    val attachedItems get() = setOf("obj.forestry_clothes_pouch".asRSCM(), "obj.forestry_cape_pouch".asRSCM())
    val outfits = listOf("hat", "top", "legs", "boots").map { part -> listOf("obj.ramble_lumberjack_$part", "obj.forestry_lumberjack_$part") }
    val outfitIds get() = outfits.flatten().map { it.asRSCM() }.toSet()
    val capes get() = setOf("obj.skillcape_woodcutting".asRSCM(), "obj.skillcape_woodcutting_trimmed".asRSCM())
    val singleItems get() = attachedItems + outfitIds + capes
    val leaves get() = listOf("", "_oak", "_willow", "_maple", "_yew", "_magic").map { "obj.leaves$it".asRSCM() }.toSet()

    fun ownsFamily(id: Int, spec: ContainerSpec): Boolean = when (spec.family) {
        "forestry_kit" -> id in basketIds
        "log_basket" -> if (spec.opened) id == OPEN.asRSCM() else id in basketIds
        else -> false
    }
    fun accepts(player: Player, store: ContainerStore, item: Int): Boolean = when {
        item == "obj.forestry_cape_pouch".asRSCM() -> player.woodcuttingLvl >= 99
        item in outfitIds -> store.count(player, kit, "obj.forestry_clothes_pouch".asRSCM()) > 0
        item in capes -> store.count(player, kit, "obj.forestry_cape_pouch".asRSCM()) > 0
        else -> true
    }
    fun destroyLastKit(player: Player, store: ContainerStore) {
        val contents = store.contents(player, kit)
        // Irreversible upgrades, the attached clothing and bark survive replacement of the kit.
        val protected = attachedItems + (if (contents["obj.forestry_clothes_pouch".asRSCM().toString()] != null) outfitIds else emptySet()) + capes + "obj.forestry_currency".asRSCM()
        val kept = contents.filterKeys { it.toInt() in protected }.toMutableMap()
        store.clear(player, kit)
        if (kept.isNotEmpty()) player.attr[kit.stateKey] = kept
    }
}
