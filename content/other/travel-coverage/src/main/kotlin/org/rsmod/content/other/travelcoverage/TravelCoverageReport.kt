package org.rsmod.content.other.travelcoverage

import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import org.rsmod.map.CoordGrid

object TravelCoverageReport {
    private const val TOP_SQUARES = 60
    private const val TOP_TYPES = 60

    fun write(result: TravelCoverageResult, dir: Path, internalName: (Int) -> String): Path {
        Files.createDirectories(dir)
        Files.writeString(dir.resolve("unhandled-travel.csv"), csv(result, internalName))
        val summary = dir.resolve("travel-coverage.md")
        Files.writeString(summary, markdown(result, internalName))
        return summary
    }

    private fun mapLink(coords: CoordGrid): String =
        "https://mejrs.github.io/osrs?m=-1&z=3&p=${coords.level}&x=${coords.x}&y=${coords.z}"

    private fun square(coords: CoordGrid): Pair<Int, Int> = (coords.x shr 6) to (coords.z shr 6)

    private fun csv(result: TravelCoverageResult, internalName: (Int) -> String): String =
        buildString {
            appendLine("square_x,square_z,x,z,level,loc_id,internal_name,name,op,verb,map")
            for (travel in result.unhandled.sortedBy { it.locId }) {
                for (coords in travel.placements) {
                    val (sx, sz) = square(coords)
                    val name = travel.name.replace("\"", "\"\"")
                    append("$sx,$sz,${coords.x},${coords.z},${coords.level},${travel.locId},")
                    append("${internalName(travel.locId)},\"$name\",${travel.op},${travel.verb},")
                    appendLine(mapLink(coords))
                }
            }
        }

    private fun markdown(result: TravelCoverageResult, internalName: (Int) -> String): String =
        buildString {
            appendLine("# Travel coverage")
            appendLine()
            appendLine("Generated ${LocalDate.now()} from the live server's map and loaded scripts.")
            appendLine("A travel op is a ladder, staircase, entrance, trapdoor or similar option")
            appendLine("on a loc placed on the map. Unhandled means clicking it does nothing.")
            appendLine()
            val types = result.unhandled.map { it.locId }.distinct().size
            appendLine("| | Count |")
            appendLine("|---|---|")
            appendLine("| Travel ops on placed loc types | ${result.travelOps} |")
            appendLine("| Handled | ${result.handledOps} |")
            appendLine("| Unhandled ops | ${result.unhandled.size} (on $types loc types) |")
            appendLine("| Unhandled placements on the map | ${result.unhandledPlacements} |")
            appendLine()

            appendLine("## By option")
            appendLine()
            appendLine("| Option | Loc types | Placements |")
            appendLine("|---|---|---|")
            result.unhandled
                .groupBy { it.verb.lowercase() }
                .entries
                .sortedByDescending { (_, list) -> list.sumOf { it.placements.size } }
                .forEach { (verb, list) ->
                    appendLine("| $verb | ${list.size} | ${list.sumOf { it.placements.size }} |")
                }
            appendLine()

            appendLine("## Map squares with the most unhandled travel ($TOP_SQUARES shown)")
            appendLine()
            appendLine("A map square is 64x64 tiles; `x_z` matches the wiki's map square ids.")
            appendLine()
            appendLine("| Square | Placements | Examples | Map |")
            appendLine("|---|---|---|---|")
            result.unhandled
                .flatMap { travel -> travel.placements.map { it to travel } }
                .groupBy { (coords, _) -> square(coords) }
                .entries
                .sortedByDescending { it.value.size }
                .take(TOP_SQUARES)
                .forEach { (sq, hits) ->
                    val examples =
                        hits.map { (_, t) -> "${t.name} (${t.verb})" }.distinct().take(4)
                    val first = hits.first().first
                    appendLine(
                        "| ${sq.first}_${sq.second} | ${hits.size} | " +
                            "${examples.joinToString(", ")} | [map](${mapLink(first)}) |"
                    )
                }
            appendLine()

            appendLine("## Most-placed unhandled loc types ($TOP_TYPES shown)")
            appendLine()
            appendLine("One fix per row covers every placement, usually by tagging the loc's")
            appendLine("content group so a shared ladder or staircase script picks it up.")
            appendLine()
            appendLine("| Loc | Symbol | Option | Placements | First placement |")
            appendLine("|---|---|---|---|---|")
            result.unhandled
                .sortedByDescending { it.placements.size }
                .take(TOP_TYPES)
                .forEach { t ->
                    val first = t.placements.first()
                    appendLine(
                        "| ${t.name} (${t.locId}) | `${internalName(t.locId)}` | ${t.verb} | " +
                            "${t.placements.size} | [${first.x},${first.z},${first.level}]" +
                            "(${mapLink(first)}) |"
                    )
                }
            appendLine()
            appendLine("Every placement is listed in `unhandled-travel.csv` next to this file.")
        }
}
