package org.rsmod.content.skills.construction.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

private class Destination(
    val key: String,
    val name: String,
    val spell: String,
    val nexusId: Int,
    val portal: String,
)

/**
 * Where a portal chamber portal or the portal nexus can lead.
 *
 * `spell` is the teleport the destination shares, which is where its Magic level, quest and runes
 * come from. `nexus_id` is the destination's key in the client's nexus list (`enum_1377`), which is
 * the value the `poh_nexus_tele_*` varbits hold. `portals` pairs each empty portal frame with the
 * loc it becomes once directed here; the loc names do not follow the destination names - the
 * mahogany frames are `poh_portal_mag_*`, Watchtower is `_yanille` and Ape Atoll is `_marim`.
 */
object ConstructionPortals {
    const val SPELL = 0
    const val NEXUS_ID = 1
    const val NAME = 2
    const val PORTALS = 3

    private val FRAMES =
        listOf(
            "loc.poh_portal_teak_empty" to "loc.poh_portal_teak_",
            "loc.poh_portal_mag_empty" to "loc.poh_portal_mag_",
            "loc.poh_portal_marble_empty" to "loc.poh_portal_marble_",
        )

    private val DESTINATIONS =
        listOf(
            Destination("varrock", "Varrock", "obj.25_varrock_teleport", 1, "varrock"),
            Destination("lumbridge", "Lumbridge", "obj.31_lumbridge_teleport", 2, "lumbridge"),
            Destination("falador", "Falador", "obj.37_falador_teleport", 3, "falador"),
            Destination("camelot", "Camelot", "obj.45_camelot_teleport", 4, "camelot"),
            Destination("ardougne", "Ardougne", "obj.51_ardougne_teleport", 5, "ardougne"),
            Destination("watchtower", "Watchtower", "obj.58_watchtower_teleport", 6, "yanille"),
            Destination("trollheim", "Trollheim", "obj.61_trollheim_teleport", 32, "trollheim"),
            Destination("marim", "Marim", "obj.64_ape_atoll_teleport", 8, "marim"),
        )

    fun table() =
        dbTable("dbtable.construction_portal_destination", serverOnly = true) {
            column("spell", SPELL, VarType.OBJ)
            column("nexus_id", NEXUS_ID, VarType.INT)
            column("name", NAME, VarType.STRING)
            column("portals", PORTALS, VarType.LOC, VarType.LOC)

            for (destination in DESTINATIONS) {
                row("dbrow.construction_portal_${destination.key}") {
                    columnRSCM(SPELL, destination.spell)
                    column(NEXUS_ID, destination.nexusId)
                    column(NAME, destination.name)
                    columnRSCM(
                        PORTALS,
                        *FRAMES.flatMap { (frame, prefix) ->
                                listOf(frame, prefix + destination.portal)
                            }
                            .toTypedArray(),
                    )
                }
            }
        }
}
