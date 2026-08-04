package org.rsmod.content.skills.crafting

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.crafting.util.CraftingConfig
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.crafting.util.CraftingGamevals
import org.rsmod.content.skills.crafting.util.meetsUnlocks
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.content.skills.crafting.util.hasCraftingTool
import org.rsmod.content.skills.crafting.util.holdsCostumeNeedle
import org.rsmod.content.skills.crafting.util.holdsImcandoHammer
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import skillSuccess

/** Remaining uses on the player's current spool of thread, where one spool covers five crafts. */
private val THREAD_USES_ATTR = AttributeKey<Int>(persistenceKey = "crafting_thread_uses")

private val CRAFT_ANIM_ATTR = AttributeKey<String>()

private val CRAFT_ANIM_END_ATTR = AttributeKey<Int>()

/** Uses left on the current spool. */
private fun ProtectedAccess.threadUses(): Int = player.attr.getOrDefault(THREAD_USES_ATTR, 0)

private fun ProtectedAccess.setThreadUses(value: Int) {
    player.attr[THREAD_USES_ATTR] = value
}

/** The shared crafting engine that every section funnels through. */
class CraftingWorkerScript @Inject constructor(
    private val worldRepo: WorldRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        CraftingRuntime.worldRepo = worldRepo
        onPlayerQueueWithArgs<CraftingTask>(CraftingConstants.QUEUE_CRAFTING_MAKE) {
            processCraftingTask(it.args)
        }
    }

    /** Dispatches a queued task to whichever half of the cycle it represents. */
    private suspend fun ProtectedAccess.processCraftingTask(task: CraftingTask) {
        when (task.phase) {
            CraftingPhase.BEGIN -> processBegin(task)
            CraftingPhase.END -> processEnd(task)
        }
    }

    /** BEGIN phase, used only by sections carrying a start message such as pottery firing. */
    private suspend fun ProtectedAccess.processBegin(task: CraftingTask) {
        val product = task.product
        if (!canCraft(product, verbose = false)) {
            resetAnim()
            return
        }
        beginCycle(product, task.facility, task.completed)
        weakQueue(
            CraftingConstants.QUEUE_CRAFTING_MAKE,
            (product.ticksAt(task.completed) - 1) + QUEUE_HANDLER_COMPENSATION,
            task.copy(phase = CraftingPhase.END),
        )
    }

    /** END phase, which consumes inputs, rolls failure, adds the output and sends the success message. */
    private suspend fun ProtectedAccess.processEnd(task: CraftingTask) {
        val product = task.product
        if (!craftOnce(product)) {
            resetAnim()
            return
        }
        val completed = task.completed + 1
        if (completed >= task.amount) {
            return
        }
        if (product.startMessage != null) {
            weakQueue(
                CraftingConstants.QUEUE_CRAFTING_MAKE,
                // Sections with a start message need it a tick after this cycle's end message, so
                // the next cycle gets its own BEGIN instead of folding into the END below.
                1 + QUEUE_HANDLER_COMPENSATION,
                task.copy(completed = completed, phase = CraftingPhase.BEGIN),
            )
        } else {
            beginCycle(product, task.facility, completed)
            weakQueue(
                CraftingConstants.QUEUE_CRAFTING_MAKE,
                product.ticksAt(completed) + QUEUE_HANDLER_COMPENSATION,
                task.copy(completed = completed, phase = CraftingPhase.END),
            )
        }
    }
}

/** Queues scheduled from inside a queue handler lose a tick to the same cycle's decrement. */
private const val QUEUE_HANDLER_COMPENSATION = 1

private const val CONFIRM_CRAFT_DELAY = 4

/** Which half of a craft cycle a queued task represents. */
enum class CraftingPhase {
    BEGIN,
    END,
}

/** Runtime references the pipeline reaches for during a craft but does not own itself. */
internal object CraftingRuntime {
    lateinit var worldRepo: WorldRepository
}

/** Everything that fires at the start of a craft cycle, from animations to the start message. */
private fun ProtectedAccess.beginCycle(
    product: CraftingProduct,
    facility: BoundLocInfo?,
    cycle: Int = 0,
) {
    // The client drops an animation already playing, so a sound fired every craft can outpace it.
    val restarted = startCraftAnim(product, cycle)
    if (restarted || product.section.repeatsSoundPerCraft) {
        product.sound?.let { soundSynth(it) }
    }
    product.spotanimAt(cycle)?.let { spotanim(it) }
    if (product.locAnim != null && facility != null) {
        locAnim(CraftingRuntime.worldRepo, facility, product.locAnim)
    }
    product.startMessage?.let { mes(it, ChatType.Spam) }
}

/**
 * Duration of the sequence gameval [seq] in server ticks, or `0` when the seq carries no duration
 * data. `tickDuration` is game cycles; `totalDelay` is client frames and must not be used here.
 */
private fun seqTicks(seq: String): Int =
    ServerCacheManager.getAnim(seq.asRSCM(RSCMType.SEQ))?.tickDuration ?: 0

/** Plays this cycle's animation and reports whether it restarted rather than overlapping. */
private fun ProtectedAccess.startCraftAnim(product: CraftingProduct, cycle: Int): Boolean {
    val seq = craftAnim(product, cycle) ?: return true
    val now = player.currentMapClock
    val playing = player.attr[CRAFT_ANIM_ATTR] == seq && now < player.attr.getOrDefault(CRAFT_ANIM_END_ATTR, 0)
    anim(seq)
    if (!playing) {
        player.attr[CRAFT_ANIM_ATTR] = seq
        player.attr[CRAFT_ANIM_END_ATTR] = now + seqTicks(seq).coerceAtLeast(1)
    }
    return !playing
}

/** A queued crafting job, where [facility] is set only for facility based sections. */
data class CraftingTask(
    val product: CraftingProduct,
    val amount: Int,
    val completed: Int,
    val facility: BoundLocInfo? = null,
    val phase: CraftingPhase = CraftingPhase.END,
)

/** Validates that [product] can currently be crafted, messaging the reason when [verbose]. */
suspend fun ProtectedAccess.canCraft(product: CraftingProduct, verbose: Boolean): Boolean {
    if (player.craftingLvl < product.level) {
        if (verbose) {
            mesbox("You need a Crafting level of at least ${product.level} to ${product.actionName}.")
        }
        return false
    }

    for (req in product.extraReqs) {
        if (stat(req.stat) < req.level) {
            if (verbose) {
                mesbox(
                    "You need a ${req.displayName} level of at least ${req.level} " +
                        "to ${product.actionName}.",
                )
            }
            return false
        }
    }

    for (tool in product.tools) {
        if (!hasCraftingTool(tool)) {
            if (verbose) {
                mes("You don't have the required tool to do that.", ChatType.Spam)
            }
            return false
        }
    }

    if (product.consumesThread && !holdsCostumeNeedle() && !inv.contains(CraftingConstants.THREAD)) {
        if (verbose) {
            mes("You need some thread to make that.", ChatType.Spam)
        }
        return false
    }

    val deficient = product.deficientInput { inv.count(it) }
    if (deficient != null) {
        if (verbose) {
            val dialogue = product.missingInputMessage
            if (dialogue != null) {
                mesbox(dialogue)
            } else {
                mes("You don't have enough ${itemName(deficient.internal)} for that!", ChatType.Spam)
            }
        }
        return false
    }

    return true
}

/** Performs a single craft of [product], consuming inputs, rolling failure and granting xp. */
suspend fun ProtectedAccess.craftOnce(product: CraftingProduct): Boolean {
    // Consume inputs, rolling back on partial failure.
    val removed = mutableListOf<Pair<String, Int>>()
    for (material in product.inputs) {
        if (invDel(inv, material.internal, material.count).success) {
            removed += material.internal to material.count
        } else {
            removed.forEach { (obj, count) -> invAdd(inv, obj, count) }
            return false
        }
    }

    val failure = product.failure

    if (failure != null && !skillSuccess(failure.low, failure.high, player.craftingLvl)) {
        failure.sound?.let { soundSynth(it) }
        failure.item?.let { invAdd(inv, it, failure.itemCount) }
        advanceCraftingXp(CraftingConstants.STAT_CRAFTING, failure.xp)
        failure.message?.let { mes(it, ChatType.Spam) }
        return true
    }

    if (invAdd(inv, product.output, product.outputCount).failure) {
        removed.forEach { (obj, count) -> invAdd(inv, obj, count) }
        mes("You don't have enough inventory space to do that.", ChatType.Spam)
        return false
    }

    product.byproducts.forEach { invAdd(inv, it.internal, it.count) }

    if (product.consumesThread && !holdsCostumeNeedle()) {
        consumeThreadCharge()
    }

    advanceCraftingXp(CraftingConstants.STAT_CRAFTING, product.xp)
    product.extraXp.forEach { advanceCraftingXp(it.stat, it.xp) }
    val resultDialogue = product.resultDialogue
    if (resultDialogue != null) {
        objbox(product.output, resultDialogue)
    } else {
        product.successMessage?.let { mes(it, ChatType.Spam) }
    }
    return true
}

/** Grants pipeline experience, converting out of the tenths that every xp value is stored in. */
private fun ProtectedAccess.advanceCraftingXp(stat: String, fineXp: Double) {
    if (fineXp <= 0.0) return
    statAdvance(stat, fineXp / CraftingConstants.FINE_XP_DIVISOR)
}

/** One spool of thread lasts [CraftingConstants.THREAD_USES_PER_SPOOL] crafts. */
private fun ProtectedAccess.consumeThreadCharge() {
    // A value of 0 means no spool has been started, so treat it as a fresh one.
    val uses = threadUses().takeIf { it > 0 } ?: CraftingConstants.THREAD_USES_PER_SPOOL
    if (uses <= 1) {
        if (invDel(inv, CraftingConstants.THREAD, 1).success) {
            mes("You use up one of your reels of thread.", ChatType.Spam)
        }
        setThreadUses(CraftingConstants.THREAD_USES_PER_SPOOL)
    } else {
        setThreadUses(uses - 1)
    }
}

/** Human-readable item name for messages, falling back to the raw gameval when unresolved. */
private fun itemName(internal: String): String {
    val id = CraftingGamevals.objOrNull(internal) ?: return internal
    return ServerCacheManager.getItem(id)?.name?.lowercase() ?: internal
}

/** Materials and tools check without level gating, used to decide what a prompt should show. */
fun ProtectedAccess.hasCraftingMaterials(product: CraftingProduct): Boolean {
    val hasTools = product.tools.all { hasCraftingTool(it) }
    val hasThread = !product.consumesThread || holdsCostumeNeedle() || inv.contains(CraftingConstants.THREAD)
    val hasInputs = product.inputs.all { inv.count(it.internal) >= it.count }
    return hasTools && hasThread && hasInputs
}

/**
 * Starts the production loop for [amount] of [product], optionally animating [facility]. Pacing
 * is the product's resolved [CraftingProduct.ticksAt] value for that craft.
 *
 * Cycle 1 begins inline on this same tick (anim/sound/start message fire immediately), and END
 * is queued for T + ticks, unless [CraftingSection.shortensFirstCraft] is off.
 * Subsequent cycles run through the queue handler.
 */
suspend fun ProtectedAccess.startCrafting(
    product: CraftingProduct,
    amount: Int,
    facility: BoundLocInfo? = null,
) {
    val capped = minOf(amount, product.maxCraftable { inv.count(it) })
    if (capped <= 0) {
        return
    }
    val cycleTicks = product.ticksAt(0)
    if (cycleTicks <= 0) {
        beginCycle(product, facility)
        repeat(capped) {
            if (!craftOnce(product)) {
                return
            }
        }
        return
    }
    beginCycle(product, facility)

    // Cycle 1 spans ticks-1 rather than ticks, so the first craft lands a tick sooner than the
    // steady-state product-to-product spacing. Sections that clear `shortensFirstCraft` keep the
    // full cycle, because an effect that runs the whole cycle has no room to play out in a short
    // one and is cut off when the next cycle restarts it.
    val firstCycleTicks: Int
    if (product.section.shortensFirstCraft) {
        firstCycleTicks = maxOf(1, cycleTicks - 1)
    } else {
        firstCycleTicks = cycleTicks
    }

    // Menu-resume paths run in PlayerInputProcess, before PlayerMainProcess.processQueues, so a
    // queue added there loses a tick to that same cycle's decrement, while direct op-handler paths
    // run after it and lose nothing. processedMapClock only matches currentMapClock once we are
    // inside PlayerMainProcess, which tells the two apart.
    val queuedBeforeProcessing = player.currentMapClock != player.processedMapClock
    val phaseCompensation: Int
    if (queuedBeforeProcessing) {
        phaseCompensation = 1
    } else {
        phaseCompensation = 0
    }

    weakQueue(
        CraftingConstants.QUEUE_CRAFTING_MAKE,
        firstCycleTicks + phaseCompensation,
        CraftingTask(
            product = product,
            amount = capped,
            completed = 0,
            facility = facility,
            phase = CraftingPhase.END,
        ),
    )
}

/** This craft's animation, preferring the imcando variant while an imcando hammer is held. */
private fun ProtectedAccess.craftAnim(product: CraftingProduct, cycle: Int): String? =
    product.imcandoAnim?.takeIf { holdsImcandoHammer() } ?: product.animAt(cycle)

/** Performs exactly one craft of [product] now, with no menu, queue or tick pacing. */
suspend fun ProtectedAccess.craftInstantly(product: CraftingProduct) {
    if (!canCraft(product, verbose = true)) {
        return
    }
    if (!confirmCraft(product)) {
        return
    }
    beginCycle(product, facility = null)
    if (product.confirmTitles.isNotEmpty()) {
        delayForCraftAnims(product)
    }
    craftOnce(product)
}

/** Holds a confirmed craft for as long as its animations take, so the item lands when they end. */
private suspend fun ProtectedAccess.delayForCraftAnims(product: CraftingProduct) {
    val anims = product.anims
    if (anims.isEmpty()) {
        delay(CONFIRM_CRAFT_DELAY)
        return
    }
    delay(seqTicks(anims.first()).coerceAtLeast(1))
    for (cycle in 1 until anims.size) {
        beginCycle(product, null, cycle)
        delay(seqTicks(anims[cycle]).coerceAtLeast(1))
    }
}

/** Runs a recipe's confirmation prompts, stopping as soon as one is declined. */
suspend fun ProtectedAccess.confirmCraft(product: CraftingProduct): Boolean {
    if (product.confirmTitles.isEmpty()) {
        return true
    }
    product.confirmWarning?.let { mesbox(it) }
    product.confirmTitles.forEachIndexed { index, title ->
        val confirmed =
            if (index == 0) {
                choice2("Yes.", true, "No.", false, title = title)
            } else {
                choice2("No.", false, "Yes.", true, title = title)
            }
        if (!confirmed) {
            return false
        }
    }
    return true
}

/**
 * Presents the "what would you like to make / how many" prompt for [products] and starts
 * production with the player's selection.
 * The prompt's verb and action type come from [section].
 */
suspend fun ProtectedAccess.selectCraftingProduct(
    section: CraftingSection,
    products: List<CraftingProduct>,
    facility: BoundLocInfo? = null,
) {
    val unlocked = products.filter { player.meetsUnlocks(it) }
    val shown = unlocked.filter { !it.requiresMaterialsToShow || hasCraftingMaterials(it) }
    val emptyMessage = section.emptyMenuMessage()
    if (emptyMessage != null && products.none { hasCraftingMaterials(it) }) {
        mesbox(emptyMessage)
        return
    }
    if (shown.isEmpty()) {
        return
    }

    val productsByOutput = shown.associateBy { it.output }

    if (shown.size == 1 && CraftingConfig.SKIP_SINGLE_RECIPE_PROMPT) {
        beginCraft(shown.single(), Int.MAX_VALUE, facility)
        return
    }

    openSkillMultiForProducts(section, shown) { selected, amount ->
        productsByOutput[selected]?.let { beginCraft(it, amount, facility) }
    }
}

/** Validates the chosen [product] and either starts crafting or messages why it cannot. */
suspend fun ProtectedAccess.beginCraft(
    product: CraftingProduct,
    amount: Int,
    facility: BoundLocInfo? = null,
) {
    if (!canCraft(product, verbose = true)) {
        return
    }
    if (!confirmCraft(product)) {
        return
    }
    startCrafting(product, amount, facility)
}

/** Opens the multiskill prompt, showing at least one of each recipe so none are filtered out. */
private suspend fun ProtectedAccess.openSkillMultiForProducts(
    section: CraftingSection,
    shown: List<CraftingProduct>,
    onSelect: suspend (output: String, amount: Int) -> Unit,
) {
    val entries = shown.map { product -> SkillMultiEntry(product.output, product.inputs) }
    val byOutput = shown.associateBy { it.output }
    openSkillMulti(
        SkillMultiConfig(
            verb = section.verb,
            actionType = section.actionType,
            entries = entries,
            maxCountProvider = { inventory, entry ->
                val product = byOutput[entry.internal]
                product?.maxCraftable { inventory.count(it) }?.coerceAtLeast(1) ?: 1
            },
        ),
    ) {
        selection -> onSelect(selection.entry.internal, selection.amount)
    }
}
