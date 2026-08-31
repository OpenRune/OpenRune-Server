package org.rsmod.content.skills.crafting

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.crafting.util.CraftingConfig
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.crafting.util.hasCraftingTool
import org.rsmod.content.skills.crafting.util.holdsCostumeNeedle
import org.rsmod.content.skills.crafting.util.holdsImcandoHammer
import org.rsmod.content.skills.crafting.util.meetsUnlocks
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import skillSuccess

private var Player.craftingThreadUses by intVarBit("varbit.crafting_thread_uses")

class CraftingWorkerScript @Inject constructor(
    private val worldRepo: WorldRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        CraftingRuntime.worldRepo = worldRepo
        onPlayerQueueWithArgs<CraftingTask>(CraftingConstants.QUEUE_CRAFTING_MAKE) {
            processCraftingTask(it.args)
        }
    }

    private suspend fun ProtectedAccess.processCraftingTask(task: CraftingTask) {
        when (task.phase) {
            CraftingPhase.BEGIN -> processBegin(task)
            CraftingPhase.END -> processEnd(task)
        }
    }

    private suspend fun ProtectedAccess.processBegin(task: CraftingTask) {
        val product = task.product
        if (!canCraft(product, verbose = false)) {
            resetAnim()
            return
        }
        val anim = beginCycle(product, task.facility, task.completed, task.anim)
        weakQueue(
            CraftingConstants.QUEUE_CRAFTING_MAKE,
            (product.ticksAt(task.completed) - 1) + QUEUE_HANDLER_COMPENSATION,
            task.copy(phase = CraftingPhase.END, anim = anim),
        )
    }

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

                1 + QUEUE_HANDLER_COMPENSATION,
                task.copy(completed = completed, phase = CraftingPhase.BEGIN),
            )
        } else {
            val anim = beginCycle(product, task.facility, completed, task.anim)
            weakQueue(
                CraftingConstants.QUEUE_CRAFTING_MAKE,
                product.ticksAt(completed) + QUEUE_HANDLER_COMPENSATION,
                task.copy(completed = completed, phase = CraftingPhase.END, anim = anim),
            )
        }
    }
}

private const val QUEUE_HANDLER_COMPENSATION = 1

private const val CONFIRM_CRAFT_DELAY = 4

enum class CraftingPhase {
    BEGIN,
    END,
}

internal object CraftingRuntime {
    lateinit var worldRepo: WorldRepository
}

data class CraftAnimState(val seq: String? = null, val endsAt: Int = 0)

private fun ProtectedAccess.beginCycle(
    product: CraftingProduct,
    facility: BoundLocInfo?,
    cycle: Int = 0,
    anim: CraftAnimState = CraftAnimState(),
): CraftAnimState {
    val started = startCraftAnim(product, cycle, anim)
    if (started.restarted || product.section.repeatsSoundPerCraft) {
        product.sound?.let { soundSynth(it) }
    }
    product.spotanimAt(cycle)?.let { spotanim(it) }
    if (product.locAnim != null && facility != null) {
        locAnim(CraftingRuntime.worldRepo, facility, product.locAnim)
    }
    product.startMessage?.let { mes(it, ChatType.Spam) }
    return started.state
}

private data class CraftAnimResult(val state: CraftAnimState, val restarted: Boolean)

private fun seqTicks(seq: String): Int =
    ServerCacheManager.getAnim(seq.asRSCM(RSCMType.SEQ))?.tickDuration ?: 0

private fun ProtectedAccess.startCraftAnim(
    product: CraftingProduct,
    cycle: Int,
    anim: CraftAnimState,
): CraftAnimResult {
    val seq = craftAnim(product, cycle) ?: return CraftAnimResult(anim, restarted = true)
    val now = player.currentMapClock
    val playing = anim.seq == seq && now < anim.endsAt
    this.anim(seq)
    if (playing) {
        return CraftAnimResult(anim, restarted = false)
    }
    val ends = now + seqTicks(seq).coerceAtLeast(1)
    return CraftAnimResult(CraftAnimState(seq, ends), restarted = true)
}

data class CraftingTask(
    val product: CraftingProduct,
    val amount: Int,
    val completed: Int,
    val facility: BoundLocInfo? = null,
    val phase: CraftingPhase = CraftingPhase.END,
    val anim: CraftAnimState = CraftAnimState(),
)

suspend fun ProtectedAccess.canCraft(product: CraftingProduct, verbose: Boolean): Boolean {
    if (player.craftingLvl < product.level) {
        if (verbose) {
            mesbox(
                "You need a Crafting level of at least ${product.level} to ${product.actionName}."
            )
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

    val missingThread =
        product.consumesThread && !holdsCostumeNeedle() && !inv.contains(CraftingConstants.THREAD)
    if (missingThread) {
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
                mes(
                    "You don't have enough ${itemName(deficient.internal)} for that!",
                    ChatType.Spam,
                )
            }
        }
        return false
    }

    return true
}

suspend fun ProtectedAccess.craftOnce(product: CraftingProduct): Boolean {
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

private fun ProtectedAccess.advanceCraftingXp(stat: String, fineXp: Double) {
    if (fineXp <= 0.0) return
    statAdvance(stat, fineXp / CraftingConstants.FINE_XP_DIVISOR)
}

private fun ProtectedAccess.consumeThreadCharge() {
    val uses =
        player.craftingThreadUses.takeIf { it > 0 } ?: CraftingConstants.THREAD_USES_PER_SPOOL
    if (uses <= 1) {
        if (invDel(inv, CraftingConstants.THREAD, 1).success) {
            mes("You use up one of your reels of thread.", ChatType.Spam)
        }
        player.craftingThreadUses = CraftingConstants.THREAD_USES_PER_SPOOL
    } else {
        player.craftingThreadUses = uses - 1
    }
}

private fun itemName(internal: String): String =
    ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ))?.name?.lowercase() ?: internal

fun ProtectedAccess.hasCraftingMaterials(product: CraftingProduct): Boolean {
    val hasTools = product.tools.all { hasCraftingTool(it) }
    val hasThread =
        !product.consumesThread ||
            holdsCostumeNeedle() ||
            inv.contains(CraftingConstants.THREAD)
    val hasInputs = product.inputs.all { inv.count(it.internal) >= it.count }
    return hasTools && hasThread && hasInputs
}

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
    val anim = beginCycle(product, facility)

    val firstCycleTicks: Int
    if (product.section.shortensFirstCraft) {
        firstCycleTicks = maxOf(1, cycleTicks - 1)
    } else {
        firstCycleTicks = cycleTicks
    }

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
            anim = anim,
        ),
    )
}

private fun ProtectedAccess.craftAnim(product: CraftingProduct, cycle: Int): String? =
    product.imcandoAnim?.takeIf { holdsImcandoHammer() } ?: product.animAt(cycle)

suspend fun ProtectedAccess.craftInstantly(product: CraftingProduct) {
    if (!canCraft(product, verbose = true)) {
        return
    }
    if (!confirmCraft(product)) {
        return
    }
    val anim = beginCycle(product, facility = null)
    if (product.confirmTitles.isNotEmpty()) {
        delayForCraftAnims(product, anim)
    }
    craftOnce(product)
}

private suspend fun ProtectedAccess.delayForCraftAnims(
    product: CraftingProduct,
    anim: CraftAnimState,
) {
    val anims = product.anims
    if (anims.isEmpty()) {
        delay(CONFIRM_CRAFT_DELAY)
        return
    }
    var state = anim
    delay(seqTicks(anims.first()).coerceAtLeast(1))
    for (cycle in 1 until anims.size) {
        state = beginCycle(product, null, cycle, state)
        delay(seqTicks(anims[cycle]).coerceAtLeast(1))
    }
}

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
