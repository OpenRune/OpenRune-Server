package org.rsmod.content.other.home

import org.rsmod.map.CoordGrid

internal enum class TeleportCategory(val label: String, val icon: String) {
    Cities("Cities", "obj.lawrune"),
    Monsters("Monsters", "obj.dragon_scimitar"),
    Bosses("Bosses", "obj.dragon_warhammer"),
    Skilling("Skilling", "obj.rune_pickaxe"),
    Dungeons("Dungeons", "obj.spade"),
    Minigames("Minigames", "obj.coins"),
    Raids("Raids", "obj.shark"),
    Pvp("PvP", "obj.skull"),
    Other("Other", "obj.lawrune");
    val key get() = name.lowercase()
}

internal data class TeleportDestination(
    val key: String,
    val name: String,
    val category: TeleportCategory,
    val destination: CoordGrid?,
    val dangerous: Boolean,
    val description: String,
) {
    val available get() = destination != null
}

internal object TeleportCatalogue {
    val HOME = CoordGrid(3087, 3496)
    val WIZARD = CoordGrid(3084, 3499)
    const val WIZARD_TYPE = "npc.home_teleport_wizard"
    val all: List<TeleportDestination> =
        checkNotNull(javaClass.getResourceAsStream("/home-teleports.tsv")).bufferedReader().use { reader ->
            reader.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.map { line ->
                val p = line.split('\t')
                require(p.size == 9) { "Invalid home teleport: $line" }
                val tile = if (p[3] == "-") null else CoordGrid(p[3].toInt(), p[4].toInt(), p[5].toInt())
                TeleportDestination(p[0], p[1], TeleportCategory.valueOf(p[2]), tile, p[6].toBooleanStrict(), p[8])
            }.toList()
        }.also { entries ->
            require(entries.map { it.key }.distinct().size == entries.size)
            require(entries.all { it.key.matches(Regex("[a-z0-9_]+")) && it.name.length <= 40 && it.description.length <= 150 })
        }

    fun matches(category: TeleportCategory?, query: String): List<TeleportDestination> =
        all.filter { (category == null || it.category == category) &&
            (query.isBlank() || it.name.contains(query, true) || it.category.label.contains(query, true))
        }.sortedWith(compareByDescending<TeleportDestination> { it.available }.thenBy { it.name })
}
