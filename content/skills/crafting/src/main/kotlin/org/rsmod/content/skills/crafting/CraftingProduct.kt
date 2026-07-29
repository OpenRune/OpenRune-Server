package org.rsmod.content.skills.crafting

import dev.openrune.types.ItemServerType
import dev.openrune.types.StatType
import org.rsmod.api.table.Tuple2
import org.rsmod.api.table.Tuple3
import org.rsmod.api.table.crafting.CraftingFacilitiesRow
import org.rsmod.api.table.crafting.CraftingGoldRow
import org.rsmod.api.table.crafting.CraftingHandRow
import org.rsmod.api.table.crafting.CraftingSilverRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.crafting.util.CraftingGamevals
import org.rsmod.content.skills.crafting.util.CraftingQuestReq
import org.rsmod.content.skills.crafting.util.CraftingVarbitReq
import org.rsmod.content.skills.crafting.util.craftingQuestReq
import org.rsmod.content.skills.crafting.util.craftingVarbitReq

/** A single crafting recipe, normalised from a table row and resolved against its section. */
data class CraftingProduct(
    val section: CraftingSection,
    val output: String,
    val outputCount: Int = 1,
    val inputs: List<Material>,
    val level: Int,
    /** Stored in tenths of a point so the int columns can carry fractional xp. CraftingWorker
     * divides by [CraftingConstants.FINE_XP_DIVISOR] */
    val xp: Double,
    val extraReqs: List<CraftingStatReq> = emptyList(),
    val extraXp: List<CraftingStatXp> = emptyList(),
    val triggers: List<String> = emptyList(),
    val ticks: List<Int>,
    val anims: List<String> = emptyList(),
    val imcandoAnim: String? = null,
    val locAnim: String? = null,
    val sound: String? = null,
    val spotanims: List<String> = emptyList(),
    val tools: List<String> = emptyList(),
    val consumesThread: Boolean = false,
    val byproducts: List<Material> = emptyList(),
    val failure: CraftingFailure? = null,
    val startMessage: String? = null,
    val successMessage: String? = null,
    val confirmTitles: List<String> = emptyList(),
    val confirmWarning: String? = null,
    val resultDialogue: String? = null,
    val questReqs: List<CraftingQuestReq> = emptyList(),
    val varbitReqs: List<CraftingVarbitReq> = emptyList(),
    val lockedMessage: String? = null,
    val missingInputMessage: String? = null,
    /** Shown when this recipe's output is used on the facility that made it. */
    val alreadyProcessedMessage: String? = null,
    val requiresMaterialsToShow: Boolean = false,
    val actionName: String,
) {
    /** This craft's tick timing, with the last entry carrying the rest of a batch. */
    fun ticksAt(cycle: Int): Int = ticks[minOf(cycle, ticks.lastIndex)]

    /** This craft's animation, cycling back to the first once the list runs out. */
    fun animAt(cycle: Int): String? = anims.getOrNull(cycle % anims.size.coerceAtLeast(1))

    fun spotanimAt(cycle: Int): String? = spotanims.getOrNull(cycle % spotanims.size.coerceAtLeast(1))

    /** How many crafts the given inventory counts allow. */
    fun maxCraftable(counts: (String) -> Int): Int = inputs.minOfOrNull { counts(it.internal) / it.count } ?: 0

    /** The first input the player is short on, or null when they have everything. */
    fun deficientInput(counts: (String) -> Int): Material? = inputs.firstOrNull { counts(it.internal) < it.count }
}

/**
 * A requirement in a skill other than Crafting. [stat] is the stat gameval ("stat.smithing");
 * [displayName] is what the player is shown ("Smithing").
 */
data class CraftingStatReq(val stat: String, val displayName: String, val level: Int)

/**
 * Experience granted in a skill other than Crafting on a successful craft. In tenths of a point,
 * like [CraftingProduct.xp]; CraftingWorker applies [CraftingConstants.FINE_XP_DIVISOR].
 */
data class CraftingStatXp(val stat: String, val xp: Double)

/** Failure odds expressed through the standard skilling success roll. */
data class CraftingFailure(
    /** Success numerators out of 256 at level 1 and at level 99. Above 256 succeeds before 99. */
    val low: Int,
    val high: Int,
    val item: String? = null,
    val itemCount: Int = 1,
    val xp: Double = 0.0,
    val message: String? = null,
    val sound: String? = null,
)

/** The one place a table row becomes a [CraftingProduct], resolving it against its [section] for defaults. */
fun craftingProduct(
    section: CraftingSection,
    output: ItemServerType,
    outputCount: Int,
    input: List<ItemServerType>,
    inputAmount: List<Int>,
    statReq: List<Tuple2<StatType, Int>>,
    fineXp: Int,
    anims: List<String> = emptyList(),
    ticks: List<Int> = emptyList(),
    sound: String? = null,
    locAnim: String? = null,
    spotanims: List<String> = emptyList(),
    successLow: Int? = null,
    successHigh: Int? = null,
    failItem: ItemServerType? = null,
    failItemCount: Int = 1,
    failXp: Int? = null,
    extraTools: List<String> = emptyList(),
    byproducts: List<Material> = emptyList(),
    requiresMaterialsToShow: Boolean = false,
    xpExtra: List<CraftingStatXp> = emptyList(),
    triggers: List<ItemServerType> = emptyList(),
    message: String? = null,
    actionName: String? = null,
    confirmTitles: List<String> = emptyList(),
    confirmWarning: String? = null,
    resultDialogue: String? = null,
    questReqs: List<CraftingQuestReq> = emptyList(),
    varbitReqs: List<CraftingVarbitReq> = emptyList(),
    lockedMessage: String? = null,
): CraftingProduct {
    val names = CraftingNames(
        input = input.firstOrNull()?.name?.lowercase().orEmpty(),
        output = output.name.lowercase(),
    )
    val failure = if (successLow != null && successLow > 0) {
        CraftingFailure(
            low = successLow,
            high = successHigh ?: successLow,
            item = failItem?.internalName,
            itemCount = failItemCount,
            xp = (failXp ?: 0).toDouble(),
            message = section.failureMessage(names),
            sound = CraftingGamevals.optional(section.failureSound),
        )
    } else {
        null
    }
    val (craftingLevel, extraReqs) = statReq.splitCraftingReq()
    return CraftingProduct(
        section = section,
        output = output.internalName,
        outputCount = outputCount,
        inputs = input.mapIndexed { i, obj -> Material(obj.internalName, inputAmount.getOrElse(i) { 1 }) },
        level = craftingLevel,
        xp = fineXp.toDouble(),
        extraReqs = extraReqs,
        extraXp = xpExtra,
        triggers = triggers.map { it.internalName },
        ticks = ticks.ifEmpty { listOf(section.ticks) }.map { it.coerceAtLeast(1) },
        anims = CraftingGamevals.filterResolvable(anims.ifEmpty { listOfNotNull(section.anim) }),
        imcandoAnim = CraftingGamevals.optional(section.imcandoAnim),
        locAnim = CraftingGamevals.optional(locAnim ?: section.locAnim),
        sound = CraftingGamevals.optional(sound ?: section.sound),
        spotanims = CraftingGamevals.filterResolvable(spotanims),
        tools = section.tools + extraTools,
        consumesThread = section.consumesThread,
        byproducts = byproducts,
        failure = failure,
        startMessage = section.startMessage(names),
        successMessage =
            when {
                message == null -> section.successMessage(names)
                message.isBlank() -> null
                else -> message.render(names)
            },
        missingInputMessage = section.missingInputMessage(names),
        alreadyProcessedMessage = section.alreadyProcessedMessage(names),
        requiresMaterialsToShow = requiresMaterialsToShow,
        actionName = actionName?.render(names) ?: section.actionName(names),
        confirmTitles = confirmTitles,
        confirmWarning = confirmWarning,
        resultDialogue = resultDialogue,
        questReqs = questReqs,
        varbitReqs = varbitReqs,
        lockedMessage = lockedMessage,
    ).also(CraftingRecipes::register)
}

/** Fills a row's message template, where `{input}` and `{output}` become the item names. */
private fun String.render(names: CraftingNames): String = replace("{input}", names.input).replace("{output}", names.output)

/** Separates a row's first output, which is the recipe's product, from any byproducts after it. */
private fun splitOutputs(
    output: List<ItemServerType>,
    outputAmount: List<Int>,
): Triple<ItemServerType, Int, List<Material>> {
    val main = output.first()
    val mainCount = outputAmount.firstOrNull() ?: 1
    val byproducts = output.drop(1).mapIndexed { i, obj -> Material(obj.internalName, outputAmount.getOrElse(i + 1) { 1 }) }
    return Triple(main, mainCount, byproducts)
}

// A column no row fills generates an optional scalar, and one some row fills twice generates a
// list, so every multi capable column is read through an overload pair that accepts either one.
private fun objValues(value: ItemServerType?): List<ItemServerType> = listOfNotNull(value)

private fun objValues(value: List<ItemServerType>): List<ItemServerType> = value

private fun strValues(value: String?): List<String> = listOfNotNull(value)

private fun strValues(value: List<String>): List<String> = value

private fun intValues(value: Int?): List<Int> = listOfNotNull(value)

private fun intValues(value: List<Int>): List<Int> = value

private fun questReqs(value: Tuple2<String?, Int?>?): List<CraftingQuestReq> =
    listOfNotNull(craftingQuestReq(value?.t0, value?.t1))

private fun questReqs(value: List<Tuple2<String, Int>>): List<CraftingQuestReq> =
    value.mapNotNull { craftingQuestReq(it.t0, it.t1) }

private fun varbitReqs(value: Tuple3<String?, Int?, Int?>?): List<CraftingVarbitReq> =
    listOfNotNull(craftingVarbitReq(value?.t0, value?.t1, value?.t2))

private fun varbitReqs(value: List<Tuple3<String, Int, Int>>): List<CraftingVarbitReq> =
    value.mapNotNull { craftingVarbitReq(it.t0, it.t1, it.t2) }

private fun statXp(value: Tuple2<StatType?, Int?>?): List<CraftingStatXp> {
    val stat = value?.t0 ?: return emptyList()
    return listOf(CraftingStatXp(stat.internalName, (value.t1 ?: 0).toDouble()))
}

private fun statXp(value: List<Tuple2<StatType, Int>>): List<CraftingStatXp> =
    value.map { CraftingStatXp(it.t0.internalName, it.t1.toDouble()) }

/** A `crafting_hand` row, covering needlework, gems, combines, birdhouses and the rest. */
fun CraftingHandRow.toCraftingProduct(): CraftingProduct {
    val (mainOutput, mainCount, extraOutputs) = splitOutputs(output, outputAmount)
    return craftingProduct(
        section = CraftingSection.byId(section),
        output = mainOutput,
        outputCount = mainCount,
        input = input,
        inputAmount = inputAmount,
        statReq = statReq,
        fineXp = xp,
        anims = strValues(anim),
        ticks = intValues(ticks),
        sound = sound,
        spotanims = strValues(spotanim),
        successLow = successLow,
        successHigh = successHigh,
        failItem = failItem,
        failXp = failXp,
        extraTools = objValues(tool).map { it.internalName },
        xpExtra = statXp(xpExtra),
        triggers = triggers,
        message = message,
        actionName = actionName,
        confirmTitles = strValues(confirmTitle),
        confirmWarning = confirmWarning,
        resultDialogue = resultDialogue,
        questReqs = questReqs(questReq),
        varbitReqs = varbitReqs(unlockVarbit),
        lockedMessage = lockedMessage,
        byproducts = extraOutputs,
    )
}

/** A `crafting_facilities` row, covering spinning, weaving, pottery and glass smelting. */
fun CraftingFacilitiesRow.toCraftingProduct(
    anim: String? = null,
    requiresMaterialsToShow: Boolean = false,
): CraftingProduct {
    val (mainOutput, mainCount, extraOutputs) = splitOutputs(output, outputAmount)
    return craftingProduct(
        section = CraftingSection.byId(section),
        output = mainOutput,
        outputCount = mainCount,
        input = input,
        inputAmount = inputAmount,
        statReq = statReq,
        fineXp = xp,
        anims = strValues(this.anim).ifEmpty { listOfNotNull(anim) },
        ticks = intValues(ticks),
        sound = sound,
        spotanims = strValues(spotanim),
        successLow = successLow,
        successHigh = successHigh,
        failItem = failItem,
        failXp = failXp,
        extraTools = objValues(tool).map { it.internalName },
        xpExtra = statXp(xpExtra),
        message = message,
        actionName = actionName,
        confirmTitles = strValues(confirmTitle),
        confirmWarning = confirmWarning,
        resultDialogue = resultDialogue,
        questReqs = questReqs(questReq),
        varbitReqs = varbitReqs(unlockVarbit),
        lockedMessage = lockedMessage,
        byproducts = extraOutputs,
        requiresMaterialsToShow = requiresMaterialsToShow,
    )
}

/** A `crafting_silver` row, whose mould rides along as an extra tool. */
fun CraftingSilverRow.toCraftingProduct(): CraftingProduct =
    craftingProduct(
        section = CraftingSection.byId(section),
        output = output,
        outputCount = outputAmount,
        input = input,
        inputAmount = inputAmount,
        statReq = statReq,
        fineXp = xp,
        anims = strValues(anim),
        ticks = intValues(ticks),
        sound = sound,
        spotanims = strValues(spotanim),
        extraTools = objValues(tool).map { it.internalName },
        message = message,
        actionName = actionName,
        confirmTitles = strValues(confirmTitle),
        confirmWarning = confirmWarning,
        resultDialogue = resultDialogue,
        questReqs = questReqs(questReq),
        varbitReqs = varbitReqs(unlockVarbit),
        lockedMessage = lockedMessage,
    )

/** A `crafting_gold` row, whose mould rides along as an extra tool. */
fun CraftingGoldRow.toCraftingProduct(): CraftingProduct =
    craftingProduct(
        section = CraftingSection.byId(section),
        output = output,
        outputCount = outputAmount,
        input = input,
        inputAmount = inputAmount,
        statReq = statReq,
        fineXp = xp,
        anims = strValues(anim),
        ticks = intValues(ticks),
        sound = sound,
        spotanims = strValues(spotanim),
        extraTools = objValues(tool).map { it.internalName },
        message = message,
        actionName = actionName,
        confirmTitles = strValues(confirmTitle),
        confirmWarning = confirmWarning,
        resultDialogue = resultDialogue,
        questReqs = questReqs(questReq),
        varbitReqs = varbitReqs(unlockVarbit),
        lockedMessage = lockedMessage,
    )

/** Player facing name for a stat, falling back to the title cased gameval suffix. */
private fun StatType.playerName(): String =
    displayName.ifBlank {
        internalName.substringAfterLast('.').replaceFirstChar(Char::uppercase)
    }

/**
 * Pulls the Crafting entry out of a row's stat_req column. Its level becomes the recipe's
 * level and every other skill becomes an extra requirement.
 */
private fun List<Tuple2<StatType, Int>>.splitCraftingReq(): Pair<Int, List<CraftingStatReq>> {
    val craftingReq = firstOrNull { req -> req.t0.isType(CraftingConstants.STAT_CRAFTING) }
    val otherReqs = filter { req -> req !== craftingReq }
    val extraReqs = otherReqs.map { req ->
        CraftingStatReq(req.t0.internalName, req.t0.playerName(), req.t1)
    }
    val craftingLevel = craftingReq?.t1 ?: 1
    return craftingLevel to extraReqs
}
