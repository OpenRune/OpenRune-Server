package org.rsmod.content.other.maxcape

import org.rsmod.map.CoordGrid

internal enum class CapeDestination(val label: String, val coords: CoordGrid, val hunterCharge: Boolean = false) {
    Warriors("Warrior's Guild", CoordGrid(2879, 3546)),
    Fishing("Fishing Guild", CoordGrid(2611, 3390)),
    Crafting("Crafting Guild", CoordGrid(2931, 3286)),
    Farming("Farming Guild", CoordGrid(1249, 3719)),
    Otto("Otto's Grotto", CoordGrid(2502, 3487)),
    Feldip("Feldip Hills", CoordGrid(2555, 2916), true),
    BlackChins("Black chinchompas", CoordGrid(3154, 3635), true),
    Hunter("Hunter Guild", CoordGrid(1546, 3045)),
    Home("Home", CoordGrid(3221, 3218)),
    Rimmington("Rimmington", CoordGrid(2954, 3224)),
    Taverley("Taverley", CoordGrid(2893, 3465)),
    Pollnivneach("Pollnivneach", CoordGrid(3340, 3004)),
    Hosidius("Hosidius", CoordGrid(1744, 3517)),
    Aldarin("Aldarin", CoordGrid(1421, 2963)),
    Rellekka("Rellekka", CoordGrid(2670, 3632)),
    Brimhaven("Brimhaven", CoordGrid(2758, 3178)),
    Yanille("Yanille", CoordGrid(2544, 3095)),
    Prifddinas("Prifddinas", CoordGrid(3239, 6076)),
}

internal sealed interface CapeAction {
    data class Teleport(val destination: CapeDestination) : CapeAction
    data class Spellbook(val index: Int) : CapeAction
    data object Check : CapeAction
    data object Search : CapeAction
    data object RingOfLife : CapeAction
    data object Commune : CapeAction
    data object Stamina : CapeAction
    data object Sailing : CapeAction
}

internal object MaxCapeOptions {
    const val HELD = "obj.skillcape_max"
    const val WORN = "obj.skillcape_max_worn"
    val capes = listOf(HELD, WORN)
    private fun tele(destination: CapeDestination) = CapeAction.Teleport(destination)
    val guilds = listOf(CapeDestination.Crafting, CapeDestination.Farming, CapeDestination.Fishing,
        CapeDestination.Hunter, CapeDestination.Warriors).map(::tele)
    val skilling = listOf(tele(CapeDestination.Otto), tele(CapeDestination.Feldip),
        tele(CapeDestination.BlackChins), CapeAction.Sailing, CapeAction.Sailing, CapeAction.Sailing)
    val portals = CapeDestination.entries.dropWhile { it != CapeDestination.Home }.map(::tele)
    val spells = (0..3).map(CapeAction::Spellbook) + CapeAction.Check
    val features = listOf(CapeAction.Search, CapeAction.RingOfLife, CapeAction.Commune,
        CapeAction.Stamina, CapeAction.Sailing, CapeAction.Sailing)
    val heldTeleports = CapeDestination.entries.take(8).map(::tele) + CapeAction.Sailing + portals + CapeAction.Sailing

    fun held(op: Int, subop: Int, wornVariant: Boolean = false): CapeAction? = when (op) {
        2 -> heldTeleports
        3 -> if (wornVariant) portals else spells
        4 -> features
        else -> emptyList()
    }.getOrNull(subop - 1)

    fun worn(op: Int, subop: Int): CapeAction? = when (op) {
        4 -> guilds
        5 -> skilling
        6 -> portals
        7 -> spells
        8 -> features
        else -> emptyList()
    }.getOrNull(subop - 1)
}
