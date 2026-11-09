package org.rsmod.content.travel.maptransports

import org.rsmod.map.CoordGrid

data class MapTransport(
    val origin: CoordGrid,
    val destination: CoordGrid,
    val option: String,
    val locId: Int,
    val skills: List<Pair<String, Int>>,
    val duration: Int,
)

/**
 * Reads the Shortest Path plugin's transport tables: tab-separated rows whose columns are named
 * by the `# Origin Destination ...` header, which each file orders differently. Only the
 * columns a click needs are kept; quests are already treated as complete on this server, and
 * item and varbit conditions are left out for now.
 */
object MapTransports {
    private val SKILLS =
        setOf(
            "attack", "defence", "strength", "hitpoints", "ranged", "prayer", "magic", "cooking",
            "woodcutting", "fletching", "fishing", "firemaking", "crafting", "smithing", "mining",
            "herblore", "agility", "thieving", "slayer", "farming", "runecrafting", "hunter",
            "construction",
        )

    fun parse(text: String): List<MapTransport> {
        val lines = text.lines()
        val header = lines.firstOrNull { it.startsWith("# Origin") } ?: return emptyList()
        val columns = header.removePrefix("#").split('\t').map { it.trim().lowercase() }
        val origin = columns.indexOf("origin")
        val destination = columns.indexOf("destination")
        val menu = columns.indexOfFirst { it.startsWith("menuoption") }
        val skills = columns.indexOf("skills")
        val duration = columns.indexOf("duration")
        return lines.mapNotNull { line ->
            if (line.isBlank() || line.startsWith("#")) return@mapNotNull null
            val cells = line.split('\t')
            val from = coords(cells.getOrNull(origin)) ?: return@mapNotNull null
            val to = coords(cells.getOrNull(destination)) ?: return@mapNotNull null
            val words = cells.getOrNull(menu)?.trim()?.split(' ')?.filter { it.isNotEmpty() }.orEmpty()
            val locId = words.lastOrNull()?.toIntOrNull() ?: return@mapNotNull null
            if (words.size < 2) return@mapNotNull null
            MapTransport(
                origin = from,
                destination = to,
                option = words.first(),
                locId = locId,
                skills = skillsOf(cells.getOrNull(skills)),
                duration = cells.getOrNull(duration)?.trim()?.toIntOrNull() ?: 1,
            )
        }
    }

    /** The row for a click on [locId]'s [option] whose origin lies closest to [standing]. */
    fun pick(rows: List<MapTransport>, standing: CoordGrid): MapTransport? =
        rows.filter { it.origin.level == standing.level }
            .minByOrNull { distance(it.origin, standing) }
            ?: rows.minByOrNull { distance(it.origin, standing) }

    private fun coords(cell: String?): CoordGrid? {
        val parts = cell?.trim()?.split(' ')?.mapNotNull { it.toIntOrNull() } ?: return null
        if (parts.size != 3) return null
        return CoordGrid(parts[0], parts[1], parts[2])
    }

    private fun skillsOf(cell: String?): List<Pair<String, Int>> =
        cell.orEmpty().split(';').mapNotNull { requirement ->
            val parts = requirement.trim().split(' ')
            val level = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val skill = parts.getOrNull(1)?.lowercase() ?: return@mapNotNull null
            if (skill in SKILLS) "stat.$skill" to level else null
        }

    private fun distance(a: CoordGrid, b: CoordGrid): Int =
        (a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z) + if (a.level == b.level) 0 else 1_000_000
}
