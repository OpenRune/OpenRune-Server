package org.rsmod.content.skills.crafting

import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.crafting.util.CraftingConstants

/** Display names a section's message templates interpolate. */
data class CraftingNames(val input: String, val output: String)

/** How a section's recipes are started and produced. */
enum class CraftingMode {
    MENU,
    INSTANT,
    COMBINE,
    SERVICE,
}

/** The defaults a group of recipes share, covering pacing, cosmetics, tools and messages. */
enum class CraftingSection(
    val id: String,
    val verb: String,
    val actionType: SkillingActionType,
    /** Default ticks per craft, which a recipe's ticks column overrides. */
    val ticks: Int,
    /** Whether the first craft of a batch runs a tick short, as most crafting does. */
    val shortensFirstCraft: Boolean = true,
    val mode: CraftingMode = CraftingMode.MENU,
    val anim: String? = null,
    val imcandoAnim: String? = null,
    val locAnim: String? = null,
    val sound: String? = null,
    val failureSound: String? = null,
    val tools: List<String> = emptyList(),
    val consumesThread: Boolean = false,
    val ownsDefaultHandler: (input: String) -> Boolean = { false },
    val actionName: (CraftingNames) -> String,
    val startMessage: (CraftingNames) -> String? = { null },
    val successMessage: (CraftingNames) -> String? = { null },
    val failureMessage: (CraftingNames) -> String? = { null },
    val emptyMenuMessage: () -> String? = { null },
    val missingInputMessage: (CraftingNames) -> String? = { null },

    /** Message when product is used on the loc */
    val alreadyProcessedMessage: (CraftingNames) -> String? = { null },
) {
    SPINNING(
        id = "Spinning",
        verb = "spin",
        actionType = SkillingActionType.SPIN,
        ticks = 3,
        shortensFirstCraft = false,
        anim = CraftingConstants.ANIM_SPINNING,
        locAnim = CraftingConstants.LOC_ANIM_SPINNING,
        sound = CraftingConstants.SOUND_SPINNING,
        actionName = { "spin ${it.output}" },
        successMessage = { "You spin the ${it.input} into ${it.output}." },
        emptyMenuMessage = { "You don't have anything suitable to spin at this spinning wheel." },
        alreadyProcessedMessage = { "You have already spun this ${it.input}." },
    ),

    WEAVING(
        id = "Weaving",
        verb = "weave",
        actionType = SkillingActionType.WEAVE,
        ticks = 3,
        shortensFirstCraft = false,
        anim = CraftingConstants.ANIM_WEAVING,
        locAnim = CraftingConstants.LOC_ANIM_WEAVING,
        sound = CraftingConstants.SOUND_WEAVING,
        actionName = { "weave ${it.output}" },
        successMessage = { null },
        emptyMenuMessage = {
            "You either don't have the required items or don't have enough of them to weave " +
                "anything at this loom."
        },
    ),

    POTTERY_SHAPING(
        id = "PotteryShaping",
        verb = "make",
        actionType = SkillingActionType.MAKE,
        ticks = 3,
        anim = CraftingConstants.ANIM_POTTERY_WHEEL,
        locAnim = CraftingConstants.LOC_ANIM_POTTERY_WHEEL,
        sound = CraftingConstants.SOUND_POTTERY_WHEEL,
        actionName = { "make ${it.output}" },
        successMessage = { "You make the clay into ${it.output.removePrefix("unfired ").withArticle()}." }, // Strips the "unfired " prefix off the output name
        emptyMenuMessage = { "You don't have anything suitable to craft with." },
    ),

    POTTERY_FIRING(
        id = "PotteryFiring",
        verb = "fire",
        actionType = SkillingActionType.FIRE,
        ticks = 7,
        anim = CraftingConstants.ANIM_POTTERY_OVEN,
        sound = CraftingConstants.SOUND_FURNACE,
        actionName = { "fire ${it.output}" },
        startMessage = { "You put the ${it.output} in the oven." },
        successMessage = { "You remove the ${it.output} from the oven." },
        failureMessage = { "The clay cracks in the oven and is ruined." },
        missingInputMessage = { "You don't have any ${it.output.plural()} which need firing." },
    ),

    GLASS_SMELTING(
        id = "GlassSmelting",
        verb = "smelt",
        actionType = SkillingActionType.SMELT,
        ticks = 3,
        anim = CraftingConstants.ANIM_FURNACE,
        sound = CraftingConstants.SOUND_FURNACE,
        actionName = { "smelt molten glass" },
        successMessage = { "You heat the sand and soda ash in the furnace to make glass." },
    ),

    NEEDLEWORK(
        id = "Needlework",
        verb = "make",
        actionType = SkillingActionType.MAKE,
        ticks = 3,
        anim = CraftingConstants.ANIM_LEATHER_CRAFT,
        sound = CraftingConstants.SOUND_LEATHER_CRAFT,
        tools = listOf(CraftingConstants.NEEDLE),
        consumesThread = true,
        actionName = { "make ${it.output}" },
        successMessage = { "You make ${it.output}." },
    ),

    PHEASANT_COSTUME(
        id = "PheasantCostume",
        verb = "make",
        actionType = SkillingActionType.MAKE,
        ticks = 3,
        anim = CraftingConstants.ANIM_PHEASANT_COSTUME,
        sound = CraftingConstants.SOUND_LEATHER_CRAFT,
        tools = listOf(CraftingConstants.NEEDLE),
        consumesThread = true,
        actionName = { "make ${it.output}" },
        successMessage = { "You make ${it.output}." },
    ),

    SHIELDS(
        id = "Shields",
        verb = "make",
        actionType = SkillingActionType.MAKE,
        ticks = 3,
        sound = CraftingConstants.SOUND_LEATHER_CRAFT,
        tools = listOf(CraftingConstants.HAMMER),
        actionName = { "make a ${it.output}" },
        successMessage = { "You nail the pieces together to make a ${it.output}." },
    ),

    CARVING(
        id = "Carving",
        verb = "cut",
        actionType = SkillingActionType.CUT,
        ticks = 3,
        anim = CraftingConstants.ANIM_SNAIL_SHELL_CUT,
        sound = CraftingConstants.SOUND_GEM_CUTTING,
        tools = listOf(CraftingConstants.CHISEL),
        actionName = { "carve a ${it.output}" },
        successMessage = { "You carve the ${it.input} into a ${it.output}." },
    ),

    KNIFE(
        id = "Knife",
        verb = "cut",
        actionType = SkillingActionType.CUT,
        ticks = 1,
        anim = CraftingConstants.ANIM_KNIFE_CUTTING,
        tools = listOf(CraftingConstants.KNIFE),
        actionName = { "make a ${it.output}" },
        successMessage = { null },
    ),

    GEMS(
        id = "Gems",
        verb = "cut",
        actionType = SkillingActionType.CUT,
        ticks = 2,
        anim = CraftingConstants.ANIM_GEM_CUTTING,
        sound = CraftingConstants.SOUND_GEM_CUTTING,
        failureSound = CraftingConstants.SOUND_GEM_CRUSH,
        tools = listOf(CraftingConstants.CHISEL),
        actionName = { "cut ${it.output}s" },
        successMessage = { "You cut the ${it.output}." },
        failureMessage = { "You mis-hit the chisel and smash the ${it.output} to pieces!" },
    ),

    AMETHYST(
        id = "Amethyst",
        verb = "cut",
        actionType = SkillingActionType.CUT,
        ticks = 2,
        anim = CraftingConstants.ANIM_AMETHYST_CUT,
        sound = CraftingConstants.SOUND_GEM_CUTTING,
        tools = listOf(CraftingConstants.CHISEL),
        actionName = { "cut ${it.output}" },
        successMessage = { "You carefully cut the amethyst into ${it.output}." },
    ),

    /** One brick per click so we use [CraftingMode.INSTANT] here */
    LIMESTONE(
        id = "Limestone",
        verb = "cut",
        actionType = SkillingActionType.CUT,
        ticks = 0, //same as mode = CraftingMode.INSTANT
        anim = CraftingConstants.ANIM_LIMESTONE_CUT,
        sound = CraftingConstants.SOUND_GEM_CUTTING,
        tools = listOf(CraftingConstants.CHISEL),
        actionName = { "cut the limestone" },
        successMessage = { "You cut the limestone into a brick." },
        failureMessage = { "You accidentally crush the limestone to bits." },
    ),

    GLASSBLOWING(
        id = "Glassblowing",
        verb = "make",
        actionType = SkillingActionType.MAKE,
        ticks = 3,
        anim = CraftingConstants.ANIM_GLASSBLOWING,
        sound = CraftingConstants.SOUND_GLASSBLOWING,
        tools = listOf(CraftingConstants.GLASSBLOWING_PIPE),
        actionName = { "make ${it.output}" },
        successMessage = { "You make ${it.output.withArticle()}." },
    ),

    BATTLESTAVES(
        id = "Battlestaves",
        verb = "make",
        actionType = SkillingActionType.MAKE,
        ticks = 2,
        shortensFirstCraft = false,
        anim = CraftingConstants.ANIM_BATTLESTAFF,
        sound = CraftingConstants.SOUND_BATTLESTAFF_ATTACH,
        actionName = { "make a ${it.output}" },
    ),

    AMULET_STRINGING(
        id = "AmuletStringing",
        verb = "string",
        actionType = SkillingActionType.MAKE,
        ticks = 2,
        sound = CraftingConstants.SOUND_AMULET_STRINGING,
        actionName = { "string ${it.output}" },
        successMessage = { "You string the amulet." },
    ),

    BIRDHOUSES(
        id = "Birdhouses",
        verb = "make",
        actionType = SkillingActionType.MAKE,
        ticks = 3,
        anim = CraftingConstants.ANIM_BIRDHOUSE,
        imcandoAnim = CraftingConstants.ANIM_BIRDHOUSE_IMCANDO,
        tools = listOf(CraftingConstants.HAMMER, CraftingConstants.CHISEL),
        ownsDefaultHandler = { it != CraftingConstants.CLOCKWORK },
        actionName = { "make a ${it.output}" },
    ),

    SOFT_CLAY_MIXING(
        id = "SoftClayMixing",
        verb = "mix",
        actionType = SkillingActionType.MAKE,
        ticks = 2,
        actionName = { "mix soft clay" },
        successMessage = { "You mix the clay and water.<br>You now have some soft workable clay." },
    ),

    COMBINING(
        id = "Combining",
        verb = "make",
        actionType = SkillingActionType.MAKE,
        ticks = 1,
        mode = CraftingMode.COMBINE,
        actionName = { "make a ${it.output}" },
        successMessage = { "You attach the ${it.input}, making a ${it.output}." },
    ),

    /** Both the silver and gold tables. Their `category` column tells the two apart. */
    JEWELLERY(
        id = "Jewellery",
        verb = "make",
        actionType = SkillingActionType.MAKE,
        ticks = 3,
        anim = CraftingConstants.ANIM_FURNACE,
        sound = CraftingConstants.SOUND_FURNACE,
        actionName = { "make ${it.output}" },
    ),

    SAND_PIT(
        id = "SandPit",
        verb = "fill",
        actionType = SkillingActionType.MAKE,
        ticks = 3,
        anim = CraftingConstants.ANIM_SAND_PIT,
        sound = CraftingConstants.SOUND_SAND_BUCKET,
        actionName = { "fill a bucket with sand" },
        successMessage = { "You fill the bucket with sand." },
    ),

    TANNING(
        id = "Tanning",
        verb = "tan",
        actionType = SkillingActionType.MAKE,
        ticks = 0,
        mode = CraftingMode.SERVICE,
        actionName = { "tan ${it.output}" },
    );

    companion object {
        private val byId: Map<String, CraftingSection> = entries.associateBy { it.id }

        /** The section a table row names. */
        fun byId(id: String): CraftingSection =
            requireNotNull(byId[id]) { "Unknown crafting section: '$id'" }
    }
}

/** Simple English pluralization for the item names used in section message templates. */
internal fun String.plural(): String = when {
    endsWith("s") || endsWith("x") || endsWith("z") ||
        endsWith("sh") || endsWith("ch") -> "${this}es"
    else -> "${this}s"
}

/** Prepends the indefinite article, giving an orb or a beer glass. */
internal fun String.withArticle(): String = if (firstOrNull()?.lowercaseChar() in setOf('a', 'e', 'i', 'o', 'u')) "an $this" else "a $this"
