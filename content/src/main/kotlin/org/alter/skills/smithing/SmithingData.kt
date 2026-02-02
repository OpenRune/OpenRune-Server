package org.alter.skills.smithing

import org.alter.rscm.RSCM.asRSCM
import org.generated.tables.smithing.SmithingBarsRow

object SmithingData {

    val allBars: List<SmithingBarsRow> = SmithingBarsRow.all()
    val barsByOutput: Map<Int, SmithingBarsRow> = allBars.associateBy { it.output }

    const val FURNACE_CATEGORY = 215

    const val FURNACE_ANIMATION = "sequences.human_furnace"
    const val FURNACE_SOUND = 2725

    internal val shayzien: Map<String, String> by lazy {
        val items = mapOf(
            "gloves" to listOf(
                "components.smithing:dagger",
                "components.smithing:axe",
                "components.smithing:chainbody",
                "components.smithing:medhelm",
                "components.smithing:darttips"
            ),
            "boots" to listOf(
                "components.smithing:sword",
                "components.smithing:mace",
                "components.smithing:platelegs",
                "components.smithing:fullhelm",
                "components.smithing:arrowheads"
            ),
            "helm" to listOf(
                "components.smithing:scimitar",
                "components.smithing:warhammer",
                "components.smithing:plateskirt",
                "components.smithing:squareshield",
                "components.smithing:knives"
            ),
            "greaves" to listOf(
                "components.smithing:longsword",
                "components.smithing:battleaxe",
                "components.smithing:platebody",
                "components.smithing:kiteshield",
                "components.smithing:other_1"
            ),
            "platebody" to listOf(
                "components.smithing:2h",
                "components.smithing:claws",
                "components.smithing:nails",
                "components.smithing:other_2",
                "components.smithing:other_3"
            )
        )


        items.flatMap { (type, components) ->
            components.mapIndexed { index, component ->
                val level = index + 1
                component to "shayzien $type ($level)"
            }
        }.toMap()
    }

    internal fun typeForChild(child: String, bar: SmithingBarsRow) : String? = when {
        bar.output == "items.lovakite_bar".asRSCM() -> shayzien[child]
        child == "components.smithing:dagger" -> "${bar.prefix} dagger"
        child == "components.smithing:sword" -> "${bar.prefix} sword"
        child == "components.smithing:scimitar" -> "${bar.prefix} scimitar"
        child == "components.smithing:longsword" -> "${bar.prefix} longsword"
        child == "components.smithing:2h" -> "${bar.prefix} 2h sword"
        child == "components.smithing:axe" -> "${bar.prefix} axe"
        child == "components.smithing:mace" -> "${bar.prefix} mace"
        child == "components.smithing:warhammer" -> "${bar.prefix} warhammer"
        child == "components.smithing:battleaxe" -> "${bar.prefix} battleaxe"
        child == "components.smithing:claws" -> "${bar.prefix} claws"
        child == "components.smithing:chainbody" -> "${bar.prefix} chainbody"
        child == "components.smithing:platelegs" -> "${bar.prefix} platelegs"
        child == "components.smithing:plateskirt" -> "${bar.prefix} plateskirt"
        child == "components.smithing:platebody" -> "${bar.prefix} platebody"
        child == "components.smithing:nails" -> "${bar.prefix} nails"
        child == "components.smithing:medhelm" -> "${bar.prefix} med helm"
        child == "components.smithing:fullhelm" -> "${bar.prefix} full helm"
        child == "components.smithing:squareshield" -> "${bar.prefix} sq shield"
        child == "components.smithing:kiteshield" -> "${bar.prefix} kiteshield"
        child == "components.smithing:other_2" -> when (bar.output) {
            "items.iron_bar".asRSCM() -> "${bar.prefix} lantern frame"
            "items.steel_bar".asRSCM() -> "${bar.prefix} lantern (unf)"
            else -> null
        }
        child == "components.smithing:darttips" -> "${bar.prefix} dart tip"
        child == "components.smithing:arrowheads" -> "${bar.prefix} arrowtips"
        child == "components.smithing:knives" -> "${bar.prefix} knife"
        child == "components.smithing:other_1" -> when (bar.output) {
            "items.bronze_bar".asRSCM() -> "${bar.prefix} wire"
            "items.iron_bar".asRSCM() -> "${bar.prefix} spit"
            "items.steel_bar".asRSCM() -> "${bar.prefix} studs"
            "items.mithril_bar".asRSCM() -> "mith grapple tip"
            else -> null
        }
        child == "components.smithing:bolts" -> "${bar.prefix} bolts (unf)"
        child == "components.smithing:limbs" -> "${bar.prefix} limbs"
        child == "components.smithing:other_3" -> "${bar.prefix} javelin tips"
        else -> null
    }
}