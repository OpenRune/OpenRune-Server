package org.rsmod.content.skills.fletching

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType

internal const val STAT_FLETCHING: String = "stat.fletching"
internal const val KNIFE: String = "obj.knife"
internal const val CHISEL: String = "obj.chisel"
internal const val ANIM_FLETCH: String = "seq.human_fletching"
internal const val ANIM_GEM_CUT: String = "seq.human_cutting"
internal const val ANIM_ADD_FEATHER: String = "seq.human_fletching_add_feather"
internal const val ANIM_ADD_ARROW_TIPS: String = "seq.human_fletching_add_arrow_tips"

internal const val QUEUE_CARVE: String = "queue.fletching_carve"
internal const val QUEUE_COMBINE: String = "queue.fletching_combine"

data class FletchProduct(val output: String, val count: Int, val level: Int, val xp: Double)

data class CarveRecipe(
    val tool: String,
    val input: String,
    val products: List<FletchProduct>,
    val anim: String = ANIM_FLETCH,
    val ticks: Int = CARVE_TICKS,
    val message: String = "You carefully cut the wood into",
)

internal fun objDisplayName(internal: String, count: Int): String {
    val type = ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ))
    val name = type?.name?.lowercase() ?: internal
    if (count <= 1) {
        val article = if (name.first() in "aeiou") "an" else "a"
        return "$article $name"
    }
    return if (name.endsWith('s')) "$count $name" else "$count ${name}s"
}

data class CombineRecipe(
    val first: String,
    val second: String,
    val output: String,
    val level: Int,
    val xp: Double,
    val setSize: Int = 1,
    val ticks: Int = 1,
    val anim: String = ANIM_FLETCH,
    val slayerLevel: Int = 0,
)

// ponytail: these two are the tuning knobs. Carving and stringing are one item per CARVE_TICKS /
// STRING_TICKS; everything that batches makes one set per tick. Change these, not the call sites.
internal const val CARVE_TICKS: Int = 3
internal const val STRING_TICKS: Int = 3

private fun shafts(count: Int, level: Int, xp: Double) =
    FletchProduct("obj.arrow_shaft", count, level, xp)

private fun single(output: String, level: Int, xp: Double) = FletchProduct(output, 1, level, xp)

private fun logs(input: String, vararg products: FletchProduct) =
    CarveRecipe(KNIFE, input, products.sortedBy(FletchProduct::level))

private fun gem(input: String, tips: String, count: Int, level: Int, xp: Double) =
    CarveRecipe(
        tool = CHISEL,
        input = input,
        products = listOf(FletchProduct(tips, count, level, xp)),
        anim = ANIM_GEM_CUT,
        ticks = 2,
        message = "You cut the gem into",
    )

private fun stringBow(unstrung: String, strung: String, level: Int, xp: Double, anim: String) =
    CombineRecipe("obj.bow_string", unstrung, strung, level, xp, ticks = STRING_TICKS, anim = anim)

private fun arrows(head: String, arrow: String, level: Int, xp: Double, slayer: Int = 0) =
    CombineRecipe(
        first = head,
        second = "obj.headless_arrow",
        output = arrow,
        level = level,
        xp = xp,
        setSize = 15,
        anim = ANIM_ADD_ARROW_TIPS,
        slayerLevel = slayer,
    )

private fun darts(tip: String, dart: String, level: Int, xp: Double, metal: String) =
    CombineRecipe(
        first = tip,
        second = "obj.feather",
        output = dart,
        level = level,
        xp = xp,
        setSize = 10,
        anim = "seq.human_fletching_add_dart_feathers_" + metal,
    )

private fun bolts(
    unfeathered: String,
    bolt: String,
    level: Int,
    xp: Double,
    metal: String,
    slayer: Int = 0,
) =
    CombineRecipe(
        first = "obj.feather",
        second = unfeathered,
        output = bolt,
        level = level,
        xp = xp,
        setSize = 10,
        anim = "seq.human_fletching_add_bolt_feathers_" + metal,
        slayerLevel = slayer,
    )

private fun tippedBolts(
    tip: String,
    bolt: String,
    output: String,
    level: Int,
    xp: Double,
    metal: String,
) =
    CombineRecipe(
        first = tip,
        second = bolt,
        output = output,
        level = level,
        xp = xp,
        setSize = 10,
        anim = "seq.human_fletching_add_bolt_tips_" + metal,
    )

private fun javelin(head: String, output: String, level: Int, xp: Double) =
    CombineRecipe(head, "obj.javelin_shaft", output, level, xp, setSize = 15)

private fun xbowLimbs(limbs: String, stock: String, unstrung: String, level: Int, xp: Double) =
    CombineRecipe(limbs, stock, unstrung, level, xp, ticks = CARVE_TICKS)

private fun xbowString(unstrung: String, strung: String, level: Int, xp: Double, metal: String) =
    CombineRecipe(
        first = "obj.xbows_crossbow_string",
        second = unstrung,
        output = strung,
        level = level,
        xp = xp,
        ticks = STRING_TICKS,
        anim = "seq.xbows_stringing_crossbow_" + metal,
    )

object FletchingRecipes {
    val carving: List<CarveRecipe> =
        listOf(
            logs(
                "obj.logs",
                shafts(15, 1, 5.0),
                single("obj.unstrung_shortbow", 5, 5.0),
                single("obj.xbows_crossbow_stock_wood", 9, 6.0),
                single("obj.unstrung_longbow", 10, 10.0),
            ),
            logs("obj.achey_tree_logs", FletchProduct("obj.ogre_arrow_shaft", 6, 5, 10.0)),
            logs(
                "obj.oak_logs",
                shafts(30, 15, 10.0),
                single("obj.unstrung_oak_shortbow", 20, 16.5),
                single("obj.xbows_crossbow_stock_oak", 24, 16.0),
                single("obj.unstrung_oak_longbow", 25, 25.0),
            ),
            logs(
                "obj.willow_logs",
                shafts(45, 30, 15.0),
                single("obj.unstrung_willow_shortbow", 35, 33.3),
                single("obj.xbows_crossbow_stock_willow", 39, 22.0),
                single("obj.unstrung_willow_longbow", 40, 41.5),
            ),
            logs(
                "obj.teak_logs",
                shafts(60, 35, 20.0),
                single("obj.xbows_crossbow_stock_teak", 46, 27.0),
            ),
            logs(
                "obj.maple_logs",
                shafts(75, 45, 25.0),
                single("obj.unstrung_maple_shortbow", 50, 50.0),
                single("obj.xbows_crossbow_stock_maple", 54, 32.0),
                single("obj.unstrung_maple_longbow", 55, 58.3),
            ),
            logs(
                "obj.mahogany_logs",
                shafts(90, 50, 30.0),
                single("obj.xbows_crossbow_stock_mahogany", 61, 41.0),
            ),
            logs(
                "obj.yew_logs",
                shafts(105, 60, 35.0),
                single("obj.unstrung_yew_shortbow", 65, 67.5),
                single("obj.xbows_crossbow_stock_yew", 69, 50.0),
                single("obj.unstrung_yew_longbow", 70, 75.0),
            ),
            logs(
                "obj.magic_logs",
                shafts(120, 75, 40.0),
                single("obj.xbows_crossbow_stock_magic", 78, 70.0),
                single("obj.unstrung_magic_shortbow", 80, 83.3),
                single("obj.unstrung_magic_longbow", 85, 91.5),
            ),
            logs("obj.redwood_logs", shafts(135, 90, 45.0)),
            gem("obj.opal", "obj.opal_bolttips", 12, 11, 1.5),
            gem("obj.jade", "obj.xbows_bolt_tips_jade", 12, 26, 2.4),
            gem("obj.red_topaz", "obj.xbows_bolt_tips_redtopaz", 12, 48, 3.9),
            gem("obj.sapphire", "obj.xbows_bolt_tips_sapphire", 12, 56, 4.7),
            gem("obj.emerald", "obj.xbows_bolt_tips_emerald", 12, 58, 5.5),
            gem("obj.ruby", "obj.xbows_bolt_tips_ruby", 12, 63, 6.3),
            gem("obj.diamond", "obj.xbows_bolt_tips_diamond", 12, 65, 7.0),
            gem("obj.dragonstone", "obj.xbows_bolt_tips_dragonstone", 12, 71, 8.2),
            gem("obj.onyx", "obj.xbows_bolt_tips_onyx", 24, 73, 9.4),
        )

    val combining: List<CombineRecipe> =
        listOf(
            stringBow("obj.unstrung_shortbow", "obj.shortbow", 5, 5.0, "seq.stringing_shortbow"),
            stringBow("obj.unstrung_longbow", "obj.longbow", 10, 10.0, "seq.stringing_longbow"),
            stringBow(
                "obj.unstrung_oak_shortbow",
                "obj.oak_shortbow",
                20,
                16.5,
                "seq.stringing_oak_shortbow",
            ),
            stringBow(
                "obj.unstrung_oak_longbow",
                "obj.oak_longbow",
                25,
                25.0,
                "seq.stringing_oak_longbow",
            ),
            stringBow(
                "obj.unstrung_willow_shortbow",
                "obj.willow_shortbow",
                35,
                33.3,
                "seq.stringing_willow_shortbow",
            ),
            stringBow(
                "obj.unstrung_willow_longbow",
                "obj.willow_longbow",
                40,
                41.5,
                "seq.stringing_willow_longbow",
            ),
            stringBow(
                "obj.unstrung_maple_shortbow",
                "obj.maple_shortbow",
                50,
                50.0,
                "seq.stringing_maple_shortbow",
            ),
            stringBow(
                "obj.unstrung_maple_longbow",
                "obj.maple_longbow",
                55,
                58.3,
                "seq.stringing_maple_longbow",
            ),
            stringBow(
                "obj.unstrung_yew_shortbow",
                "obj.yew_shortbow",
                65,
                67.5,
                "seq.stringing_yew_shortbow",
            ),
            stringBow(
                "obj.unstrung_yew_longbow",
                "obj.yew_longbow",
                70,
                75.0,
                "seq.stringing_yew_longbow",
            ),
            stringBow(
                "obj.unstrung_magic_shortbow",
                "obj.magic_shortbow",
                80,
                83.3,
                "seq.stringing_magic_shortbow",
            ),
            stringBow(
                "obj.unstrung_magic_longbow",
                "obj.magic_longbow",
                85,
                91.5,
                "seq.stringing_magic_longbow",
            ),
            CombineRecipe(
                first = "obj.feather",
                second = "obj.arrow_shaft",
                output = "obj.headless_arrow",
                level = 1,
                xp = 1.0,
                setSize = 15,
                anim = ANIM_ADD_FEATHER,
            ),
            arrows("obj.bronze_arrowheads", "obj.bronze_arrow", 1, 1.3),
            arrows("obj.iron_arrowheads", "obj.iron_arrow", 15, 2.5),
            arrows("obj.steel_arrowheads", "obj.steel_arrow", 30, 5.0),
            arrows("obj.mithril_arrowheads", "obj.mithril_arrow", 45, 7.5),
            arrows("obj.slayer_broad_arrowhead", "obj.slayer_broad_arrows", 52, 15.0, slayer = 55),
            arrows("obj.adamant_arrowheads", "obj.adamant_arrow", 60, 10.0),
            arrows("obj.rune_arrowheads", "obj.rune_arrow", 75, 12.5),
            arrows("obj.amethyst_arrowheads", "obj.amethyst_arrow", 82, 13.5),
            arrows("obj.dragon_arrowheads", "obj.dragon_arrow", 90, 15.0),
            darts("obj.bronze_dart_tip", "obj.bronze_dart", 1, 1.8, "bronze"),
            darts("obj.iron_dart_tip", "obj.iron_dart", 22, 3.8, "iron"),
            darts("obj.steel_dart_tip", "obj.steel_dart", 37, 7.5, "steel"),
            darts("obj.mithril_dart_tip", "obj.mithril_dart", 52, 11.2, "mithril"),
            darts("obj.adamant_dart_tip", "obj.adamant_dart", 67, 15.0, "adamant"),
            darts("obj.rune_dart_tip", "obj.rune_dart", 81, 18.8, "rune"),
            darts("obj.amethyst_dart_tip", "obj.amethyst_dart", 90, 21.2, "amethyst"),
            darts("obj.dragon_dart_tip", "obj.dragon_dart", 95, 25.0, "dragon"),
            bolts("obj.xbows_crossbow_bolts_bronze_unfeathered", "obj.bolt", 9, 0.5, "bronze"),
            bolts(
                "obj.xbows_crossbow_bolts_blurite_unfeathered",
                "obj.xbows_crossbow_bolts_blurite",
                24,
                1.0,
                "blurite",
            ),
            bolts(
                "obj.xbows_crossbow_bolts_iron_unfeathered",
                "obj.xbows_crossbow_bolts_iron",
                39,
                1.5,
                "iron",
            ),
            bolts(
                "obj.xbows_crossbow_bolts_silver_unfeathered",
                "obj.xbows_crossbow_bolts_silver",
                43,
                2.5,
                "silver",
            ),
            bolts(
                "obj.xbows_crossbow_bolts_steel_unfeathered",
                "obj.xbows_crossbow_bolts_steel",
                46,
                3.5,
                "steel",
            ),
            bolts(
                "obj.xbows_crossbow_bolts_mithril_unfeathered",
                "obj.xbows_crossbow_bolts_mithril",
                54,
                5.0,
                "mithril",
            ),
            bolts(
                "obj.slayer_broad_bolt_unfinished",
                "obj.slayer_broad_bolt",
                55,
                3.0,
                "adamant",
                slayer = 55,
            ),
            bolts(
                "obj.xbows_crossbow_bolts_adamantite_unfeathered",
                "obj.xbows_crossbow_bolts_adamantite",
                61,
                7.0,
                "adamant",
            ),
            bolts(
                "obj.xbows_crossbow_bolts_runite_unfeathered",
                "obj.xbows_crossbow_bolts_runite",
                69,
                10.0,
                "rune",
            ),
            bolts("obj.dragon_bolts_unfeathered", "obj.dragon_bolts", 84, 12.0, "dragon"),
            tippedBolts("obj.opal_bolttips", "obj.bolt", "obj.opal_bolt", 11, 1.6, "bronze"),
            tippedBolts(
                "obj.xbows_bolt_tips_jade",
                "obj.xbows_crossbow_bolts_blurite",
                "obj.xbows_crossbow_bolts_blurite_tipped_jade",
                26,
                2.4,
                "blurite",
            ),
            tippedBolts(
                "obj.pearl_bolttips",
                "obj.xbows_crossbow_bolts_iron",
                "obj.pearl_bolt",
                41,
                3.2,
                "iron",
            ),
            tippedBolts(
                "obj.xbows_bolt_tips_redtopaz",
                "obj.xbows_crossbow_bolts_steel",
                "obj.xbows_crossbow_bolts_steel_tipped_redtopaz",
                48,
                3.9,
                "steel",
            ),
            tippedBolts(
                "obj.xbows_bolt_tips_sapphire",
                "obj.xbows_crossbow_bolts_mithril",
                "obj.xbows_crossbow_bolts_mithril_tipped_sapphire",
                56,
                4.7,
                "mithril",
            ),
            tippedBolts(
                "obj.xbows_bolt_tips_emerald",
                "obj.xbows_crossbow_bolts_mithril",
                "obj.xbows_crossbow_bolts_mithril_tipped_emerald",
                58,
                5.5,
                "mithril",
            ),
            tippedBolts(
                "obj.xbows_bolt_tips_ruby",
                "obj.xbows_crossbow_bolts_adamantite",
                "obj.xbows_crossbow_bolts_adamantite_tipped_ruby",
                63,
                6.3,
                "adamant",
            ),
            tippedBolts(
                "obj.xbows_bolt_tips_diamond",
                "obj.xbows_crossbow_bolts_adamantite",
                "obj.xbows_crossbow_bolts_adamantite_tipped_diamond",
                65,
                7.0,
                "adamant",
            ),
            tippedBolts(
                "obj.xbows_bolt_tips_dragonstone",
                "obj.xbows_crossbow_bolts_runite",
                "obj.xbows_crossbow_bolts_runite_tipped_dragonstone",
                71,
                8.2,
                "rune",
            ),
            tippedBolts(
                "obj.xbows_bolt_tips_onyx",
                "obj.xbows_crossbow_bolts_runite",
                "obj.xbows_crossbow_bolts_runite_tipped_onyx",
                73,
                9.4,
                "rune",
            ),
            tippedBolts(
                "obj.xbows_bolt_tips_amethyst",
                "obj.slayer_broad_bolt",
                "obj.slayer_broad_bolt_amethyst",
                76,
                10.6,
                "adamant",
            ),
            javelin("obj.bronze_javelin_head", "obj.bronze_javelin", 3, 1.0),
            javelin("obj.iron_javelin_head", "obj.iron_javelin", 17, 2.0),
            javelin("obj.steel_javelin_head", "obj.steel_javelin", 32, 5.0),
            javelin("obj.mithril_javelin_head", "obj.mithril_javelin", 47, 8.0),
            javelin("obj.adamant_javelin_head", "obj.adamant_javelin", 62, 10.0),
            javelin("obj.rune_javelin_head", "obj.rune_javelin", 77, 12.4),
            javelin("obj.amethyst_javelin_head", "obj.amethyst_javelin", 84, 14.0),
            javelin("obj.dragon_javelin_head", "obj.dragon_javelin", 92, 15.0),
            xbowLimbs(
                "obj.xbows_crossbow_limbs_bronze",
                "obj.xbows_crossbow_stock_wood",
                "obj.xbows_crossbow_unstrung_bronze",
                9,
                12.0,
            ),
            xbowLimbs(
                "obj.xbows_crossbow_limbs_blurite",
                "obj.xbows_crossbow_stock_oak",
                "obj.xbows_crossbow_unstrung_blurite",
                24,
                32.0,
            ),
            xbowLimbs(
                "obj.xbows_crossbow_limbs_iron",
                "obj.xbows_crossbow_stock_willow",
                "obj.xbows_crossbow_unstrung_iron",
                39,
                44.0,
            ),
            xbowLimbs(
                "obj.xbows_crossbow_limbs_steel",
                "obj.xbows_crossbow_stock_teak",
                "obj.xbows_crossbow_unstrung_steel",
                46,
                54.0,
            ),
            xbowLimbs(
                "obj.xbows_crossbow_limbs_mithril",
                "obj.xbows_crossbow_stock_maple",
                "obj.xbows_crossbow_unstrung_mithril",
                54,
                64.0,
            ),
            xbowLimbs(
                "obj.xbows_crossbow_limbs_adamantite",
                "obj.xbows_crossbow_stock_mahogany",
                "obj.xbows_crossbow_unstrung_adamantite",
                61,
                82.0,
            ),
            xbowLimbs(
                "obj.xbows_crossbow_limbs_runite",
                "obj.xbows_crossbow_stock_yew",
                "obj.xbows_crossbow_unstrung_runite",
                69,
                100.0,
            ),
            xbowLimbs(
                "obj.xbows_crossbow_limbs_dragon",
                "obj.xbows_crossbow_stock_magic",
                "obj.xbows_crossbow_unstrung_dragon",
                78,
                135.0,
            ),
            xbowString(
                "obj.xbows_crossbow_unstrung_bronze",
                "obj.xbows_crossbow_bronze",
                9,
                6.0,
                "bronze",
            ),
            xbowString(
                "obj.xbows_crossbow_unstrung_blurite",
                "obj.xbows_crossbow_blurite",
                24,
                16.0,
                "blurite",
            ),
            xbowString(
                "obj.xbows_crossbow_unstrung_iron",
                "obj.xbows_crossbow_iron",
                39,
                22.0,
                "iron",
            ),
            xbowString(
                "obj.xbows_crossbow_unstrung_steel",
                "obj.xbows_crossbow_steel",
                46,
                27.0,
                "steel",
            ),
            xbowString(
                "obj.xbows_crossbow_unstrung_mithril",
                "obj.xbows_crossbow_mithril",
                54,
                32.0,
                "mithril",
            ),
            xbowString(
                "obj.xbows_crossbow_unstrung_adamantite",
                "obj.xbows_crossbow_adamantite",
                61,
                41.0,
                "adamantite",
            ),
            xbowString(
                "obj.xbows_crossbow_unstrung_runite",
                "obj.xbows_crossbow_runite",
                69,
                50.0,
                "runite",
            ),
            xbowString(
                "obj.xbows_crossbow_unstrung_dragon",
                "obj.xbows_crossbow_dragon",
                78,
                70.0,
                "dragon",
            ),
        )
}
