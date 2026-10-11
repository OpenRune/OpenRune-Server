package org.rsmod.content.travel.shortcuts

import org.rsmod.map.CoordGrid

internal data class ShortcutRoute(
    val loc: String, val option: String, val origin: CoordGrid, val destination: CoordGrid,
    val level: Int, val xp: Double, val ticks: Int, val ranged: Int, val strength: Int,
    val gear: String, val quest: String?, val gate: String?, val gateValue: Int,
    val gateExact: Boolean, val bareLevel: Int, val failure: String, val stages: List<CoordGrid>,
) {
    val animation: String get() = when {
        loc.contains("railing") && option.startsWith("Squeeze", true) -> "seq.railing_squeeze"
        loc.contains("pipe") -> "seq.human_pipesqueeze"
        loc.contains("zqrockjump") || loc.contains("stepping") || loc.contains("stepstone") -> "seq.human_steppingstonejump"
        loc.contains("fenk_bridge") -> "seq.human_longjump"
        option.startsWith("Swim", true) -> "seq.human_real_swimming"
        option.startsWith("Grapple", true) -> "seq.xbows_human_fire_and_climb_grapple"
        option.startsWith("Teeth", true) -> "seq.zipline_slide"
        option.startsWith("Squeeze", true) -> "seq.human_squeeze"
        option.startsWith("Crawl", true) -> "seq.human_crawling"
        option.startsWith("Jump", true) || option.startsWith("Step", true) -> "seq.human_longjump"
        option.startsWith("Cross", true) || option.startsWith("Walk", true) -> "seq.human_walk_logbalance_loop"
        option.startsWith("Climb-over", true) -> "seq.human_walk_crumbledwall"
        option.startsWith("Climb-down", true) -> "seq.human_climbing_down"
        else -> "seq.human_climbing"
    }
}

internal object ShortcutRoutes {
    val all: List<ShortcutRoute> = javaClass.getResourceAsStream("/shortcuts.tsv")!!.bufferedReader().useLines { lines ->
        lines.drop(1).filter { it.isNotBlank() }.map { line ->
            val v = line.split('\t')
            fun coord(index: Int): CoordGrid { val c = v[index].split(',').map(String::toInt); return CoordGrid(c[0], c[1], c[2]) }
            ShortcutRoute(v[0], v[1], coord(2), coord(3), v[4].toInt(), v[5].toDouble(), v[6].toInt(),
                v[7].toInt(), v[8].toInt(), v[9], v[10].ifBlank { null }, v[11].ifBlank { null },
                v[12].toInt(), v[13].toBooleanStrict(), v[14].toInt(), v[15], v.getOrElse(16) { "" }.split(';').filter { it.isNotBlank() }.map { value -> val c = value.split(',').map(String::toInt); CoordGrid(c[0], c[1], c[2]) })
        }.toList()
    }

    // These native area scripts retain ownership of their accepted routes.
    val external = setOf("loc.falador_sc_castlewall_south", "loc.falador_sc_castlewall_north",
        "loc.lumbridge_sc_fencejump", "loc.lumbridge_sc_stepstone", "loc.lumbridge_diary_desert_shortcut",
        "loc.dwarf_mines_sc_wall_crack", "loc.xbows_raft_tr", "loc.xbows_raft_tl", "loc.xbows_raft_br",
        "loc.wild_cave_agility_jump")
}
