package org.rsmod.content.skills.smithing.util

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum
import org.rsmod.api.enums.SmithingProductToEnums
import org.rsmod.api.table.smithing.SmithingBarsRow

object SmithingData {
    const val ANVIL_CATEGORY = "category.anvil"
    const val SMITHING_INTERFACE = "interface.smithing"
    const val SMITHING_BAR_TYPE_VARBIT = "varbit.smithing_bar_type"

    val barsByOutput: Map<String, SmithingBarsRow> =
        SmithingBarsRow.all().associateBy { it.output.internalName }

    private val barEnum = SmithingProductToEnums.smithing_product_to_item

    val barOutputInternals: Set<String> by lazy {
        barEnum.values.mapNotNull { it?.internalName }.toSet()
    }

    val allBars: List<SmithingBarsRow> by lazy {
        SmithingBarsRow.all().filter { it.output.internalName in barOutputInternals }
    }

    val productMeta: List<SmithingProductMeta> by lazy {
        SmithingProductToEnums.smithing_product_to_requirement.keys.mapNotNull { product ->
            val level = SmithingProductToEnums.smithing_product_to_requirement[product] ?: return@mapNotNull null
            val barCount = SmithingProductToEnums.smithing_product_to_bars_required[product] ?: return@mapNotNull null
            val numProduced = SmithingProductToEnums.smithing_product_to_quantity[product] ?: return@mapNotNull null
            val bar = barForProduct(product.name) ?: return@mapNotNull null
            if (bar.output.internalName !in barOutputInternals) {
                return@mapNotNull null
            }
            SmithingProductMeta(
                product = product,
                name = product.name.lowercase(),
                level = level,
                barCount = barCount,
                numProduced = numProduced,
                bar = bar,
            )
        }
    }

    private val productMetaByBarAndName: Map<Pair<Int, String>, SmithingProductMeta> by lazy {
        productMeta.associateBy { it.bar.output.id to it.name }
    }

    /** Every smithing interface item button (see [typeForChild]). */
    val smithingButtonComponents: List<String> =
        listOf(
            "component.smithing:dagger",
            "component.smithing:axe",
            "component.smithing:chainbody",
            "component.smithing:medhelm",
            "component.smithing:darttips",
            "component.smithing:sword",
            "component.smithing:mace",
            "component.smithing:platelegs",
            "component.smithing:fullhelm",
            "component.smithing:arrowheads",
            "component.smithing:scimitar",
            "component.smithing:warhammer",
            "component.smithing:plateskirt",
            "component.smithing:squareshield",
            "component.smithing:knives",
            "component.smithing:longsword",
            "component.smithing:battleaxe",
            "component.smithing:platebody",
            "component.smithing:kiteshield",
            "component.smithing:other_1",
            "component.smithing:2h",
            "component.smithing:claws",
            "component.smithing:nails",
            "component.smithing:other_2",
            "component.smithing:other_3",
            "component.smithing:bolts",
            "component.smithing:limbs",
        )


    fun metaForName(bar: SmithingBarsRow, itemName: String): SmithingProductMeta? =
        productMetaByBarAndName[bar.output.id to itemName]

    fun barIndexFor(bar: SmithingBarsRow): Int? {
        for ((index, barType) in barEnum.backing) {
            if (barType?.id == bar.output.id) {
                return index
            }
        }
        return null
    }

    fun barAtIndex(index: Int): SmithingBarsRow? =
        barEnum[index]?.internalName?.let { barsByOutput[it] }

    fun barForProduct(productName: String): SmithingBarsRow? {
        val lower = productName.lowercase()
        allBars.firstOrNull { lower.startsWith(it.prefix) }?.let {
            return it
        }
        return when {
            lower.endsWith("lantern frame") -> barsByOutput["obj.iron_bar"]
            lower.endsWith("lantern (unf)") -> barsByOutput["obj.steel_bar"]
            lower.endsWith("grapple tip") -> barsByOutput["obj.mithril_bar"]
            else -> null
        }
    }

    private val shayzien: Map<String, String> by lazy {
        val items =
            mapOf(
                "gloves" to
                    listOf(
                        "component.smithing:dagger",
                        "component.smithing:axe",
                        "component.smithing:chainbody",
                        "component.smithing:medhelm",
                        "component.smithing:darttips",
                    ),
                "boots" to
                    listOf(
                        "component.smithing:sword",
                        "component.smithing:mace",
                        "component.smithing:platelegs",
                        "component.smithing:fullhelm",
                        "component.smithing:arrowheads",
                    ),
                "helm" to
                    listOf(
                        "component.smithing:scimitar",
                        "component.smithing:warhammer",
                        "component.smithing:plateskirt",
                        "component.smithing:squareshield",
                        "component.smithing:knives",
                    ),
                "greaves" to
                    listOf(
                        "component.smithing:longsword",
                        "component.smithing:battleaxe",
                        "component.smithing:platebody",
                        "component.smithing:kiteshield",
                        "component.smithing:other_1",
                    ),
                "platebody" to
                    listOf(
                        "component.smithing:2h",
                        "component.smithing:claws",
                        "component.smithing:nails",
                        "component.smithing:other_2",
                        "component.smithing:other_3",
                    ),
            )

        items
            .flatMap { (type, components) ->
                components.mapIndexed { index, component ->
                    val level = index + 1
                    component to "shayzien $type ($level)"
                }
            }.toMap()
    }

    fun typeForChild(child: String, bar: SmithingBarsRow): String? =
        when {
            bar.output.internalName == "obj.lovakite_bar" -> shayzien[child]
            child == "component.smithing:dagger" -> "${bar.prefix} dagger"
            child == "component.smithing:sword" -> "${bar.prefix} sword"
            child == "component.smithing:scimitar" -> "${bar.prefix} scimitar"
            child == "component.smithing:longsword" -> "${bar.prefix} longsword"
            child == "component.smithing:2h" -> "${bar.prefix} 2h sword"
            child == "component.smithing:axe" -> "${bar.prefix} axe"
            child == "component.smithing:mace" -> "${bar.prefix} mace"
            child == "component.smithing:warhammer" -> "${bar.prefix} warhammer"
            child == "component.smithing:battleaxe" -> "${bar.prefix} battleaxe"
            child == "component.smithing:claws" -> "${bar.prefix} claws"
            child == "component.smithing:chainbody" -> "${bar.prefix} chainbody"
            child == "component.smithing:platelegs" -> "${bar.prefix} platelegs"
            child == "component.smithing:plateskirt" -> "${bar.prefix} plateskirt"
            child == "component.smithing:platebody" -> "${bar.prefix} platebody"
            child == "component.smithing:nails" -> "${bar.prefix} nails"
            child == "component.smithing:medhelm" -> "${bar.prefix} med helm"
            child == "component.smithing:fullhelm" -> "${bar.prefix} full helm"
            child == "component.smithing:squareshield" -> "${bar.prefix} sq shield"
            child == "component.smithing:kiteshield" -> "${bar.prefix} kiteshield"
            child == "component.smithing:other_2" ->
                when (bar.output.internalName) {
                    "obj.iron_bar" -> "${bar.prefix} lantern frame"
                    "obj.steel_bar" -> "${bar.prefix} lantern (unf)"
                    else -> null
                }
            child == "component.smithing:darttips" -> "${bar.prefix} dart tip"
            child == "component.smithing:arrowheads" -> "${bar.prefix} arrowtips"
            child == "component.smithing:knives" -> "${bar.prefix} knife"
            child == "component.smithing:other_1" ->
                when (bar.output.internalName) {
                    "obj.bronze_bar" -> "${bar.prefix} wire"
                    "obj.iron_bar" -> "${bar.prefix} spit"
                    "obj.steel_bar" -> "${bar.prefix} studs"
                    "obj.mithril_bar" -> "mith grapple tip"
                    else -> null
                }
            child == "component.smithing:bolts" -> "${bar.prefix} bolts (unf)"
            child == "component.smithing:limbs" -> "${bar.prefix} limbs"
            child == "component.smithing:other_3" -> "${bar.prefix} javelin tips"
            else -> null
        }
}

data class SmithingProductMeta(
    val product: ItemServerType,
    val name: String,
    val level: Int,
    val barCount: Int,
    val numProduced: Int,
    val bar: SmithingBarsRow,
)
