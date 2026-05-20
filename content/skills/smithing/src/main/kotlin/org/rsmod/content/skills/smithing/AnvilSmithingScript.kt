package org.rsmod.content.skills.smithing

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLocCategory1
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.smithing.SmithingBarsRow
import org.rsmod.content.skills.smithing.util.SmithingData
import org.rsmod.content.skills.smithing.util.SmithingProductMeta
import org.rsmod.content.skills.smithing.util.SmithingUtils
import org.rsmod.content.skills.smithing.util.SmithingUtils.hasHammer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AnvilSmithingScript @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {

    private val hammers = setOf(
        "obj.hammer",
        "obj.imcando_hammer",
        "obj.imcando_hammer_offhand"
    )

    private var ProtectedAccess.smithingBarType by intVarBit(SmithingData.SMITHING_BAR_TYPE_VARBIT)

    override fun ScriptContext.startup() {
        hammers.forEach { hammer ->
            onOpLocCategoryU(SmithingData.ANVIL_CATEGORY, hammer) {
                mes(
                    "To smith a metal bar, click on the anvil while you have the bar in your inventory.",
                )
            }
        }

        SmithingData.barOutputInternals.forEach { barInternal ->
            onOpLocCategoryU(SmithingData.ANVIL_CATEGORY, barInternal) {
                SmithingData.barsByOutput[barInternal]?.let { queueOpenSmithing(it) }
            }
        }

        onOpLocCategory1(SmithingData.ANVIL_CATEGORY) {
            val bar = selectedBar()
            if (bar == null) {
                mesbox("You should select an item from your inventory and use it on the anvil.")
                return@onOpLocCategory1
            }
            queueOpenSmithing(bar)
        }

        SmithingData.smithingButtonComponents.forEach { component ->
            onIfModalButton(component) { handleSmithingInterfaceClick(it.component.packed) }
        }

        onPlayerQueueWithArgs<OpenAnvilTask>("queue.smithing_anvil_open") {
            openAnvilSmithing(it.args.barInternal)
        }

        onPlayerQueueWithArgs<AnvilSmithTask>("queue.smithing_anvil") {
            processSmithTask(it.args)
        }
    }

    private fun ProtectedAccess.queueOpenSmithing(bar: SmithingBarsRow) {
        weakQueue("queue.smithing_anvil_open", 1, OpenAnvilTask(bar.output.internalName))
    }

    private suspend fun ProtectedAccess.openAnvilSmithing(barInternal: String) {
        val bar = SmithingData.barsByOutput[barInternal] ?: return
        if (!canSmithBar(bar)) {
            return
        }
        openSmithingInterface(bar)
    }

    private suspend fun ProtectedAccess.handleSmithingInterfaceClick(componentPacked: Int) {
        val bar = SmithingData.barAtIndex(smithingBarType) ?: return
        val componentString = RSCM.getReverseMapping(RSCMType.COMPONENT, componentPacked)
        val itemName = SmithingData.typeForChild(componentString, bar) ?: return
        val meta = SmithingData.metaForName(bar, itemName) ?: return

        ifCloseSub(SmithingData.SMITHING_INTERFACE)
        startSmithing(meta, amount = 1, strong = true)
    }

    private suspend fun ProtectedAccess.canSmithBar(bar: SmithingBarsRow): Boolean {
        if (!hasHammer()) {
            mes("You need a hammer to work metal with an anvil.")
            return false
        }
        val barName = SmithingUtils.itemName(bar.output, "bar").lowercase()
        return SmithingUtils.requireSmithingLevel(
            this,
            bar.level,
            "work $barName",
        )
    }

    private suspend fun ProtectedAccess.openSmithingInterface(bar: SmithingBarsRow) {
        SmithingData.barIndexFor(bar)?.let { smithingBarType = it }
        ifOpenMainModal(SmithingData.SMITHING_INTERFACE)
    }

    private fun ProtectedAccess.selectedBar(): SmithingBarsRow? {
        val smithingLevel = player.smithingLvl

        SmithingData.barAtIndex(smithingBarType)
            ?.takeIf { inv.contains(it.output.internalName) && smithingLevel >= it.level }
            ?.let {
                return it
            }

        val bestBar =
            SmithingData.allBars
                .filter { inv.contains(it.output.internalName) && smithingLevel >= it.level }
                .maxByOrNull { it.level }

        if (bestBar != null) {
            SmithingData.barIndexFor(bestBar)?.let { smithingBarType = it }
        }

        return bestBar
    }

    private fun ProtectedAccess.startSmithing(
        meta: SmithingProductMeta,
        amount: Int,
        strong: Boolean = false,
    ) {
        val maxAmount = inv.count(meta.bar.output.internalName) / meta.barCount
        val craftAmount = amount.coerceAtMost(maxAmount)
        if (craftAmount <= 0) {
            return
        }

        val task = AnvilSmithTask(meta, craftAmount, completed = 0)
        if (strong) {
            strongQueue("queue.smithing_anvil", ANVIL_INITIAL_DELAY, task)
        } else {
            weakQueue("queue.smithing_anvil", ANVIL_INITIAL_DELAY, task)
        }
    }

    private suspend fun ProtectedAccess.processSmithTask(task: AnvilSmithTask) {
        if (!canSmithProduct(task.meta)) {
            resetAnim()
            return
        }

        performSmith(task.meta)

        val completed = task.completed + 1
        if (completed >= task.amount) {
            return
        }

        if (!canSmithProduct(task.meta)) {
            resetAnim()
            return
        }

        weakQueue(
            "queue.smithing_anvil",
            ANVIL_CYCLE_DELAY,
            task.copy(completed = completed),
        )
    }

    private suspend fun ProtectedAccess.performSmith(meta: SmithingProductMeta) {
        delay(2)
        anim("seq.human_smithing")
        soundSynth(3771)

        val anvilDelay = SmithingUtils.anvilActionDelay(player)
        delay(anvilDelay)

        if (!canSmithProduct(meta)) {
            resetAnim()
            return
        }

        val barRemoved =
            invDel(inv, meta.bar.output.internalName, meta.barCount, strict = true).success
        if (!barRemoved) {
            resetAnim()
            return
        }

        if (invAdd(inv, meta.product.internalName, meta.numProduced).success) {
            val xp = meta.barCount * meta.bar.smithxp * xpMods.get(player, "stat.smithing")
            statAdvance("stat.smithing", xp.toDouble())
        }
    }

    private suspend fun ProtectedAccess.canSmithProduct(meta: SmithingProductMeta): Boolean {
        if (!canSmithBar(meta.bar)) {
            return false
        }

        if (!SmithingUtils.requireSmithingLevel(
                this,
                meta.level,
                "make ${SmithingUtils.prefixAn(meta.name)}",
            )
        ) {
            return false
        }

        if (meta.barCount > inv.count(meta.bar.output.internalName)) {
            val barName = SmithingUtils.itemName(meta.bar.output, "bar").lowercase()
            mesbox(
                "You don't have enough ${SmithingUtils.pluralize(barName, meta.barCount)} to make " +
                    SmithingUtils.prefixAn(meta.name) + ".",
            )
            return false
        }

        return true
    }

    private data class OpenAnvilTask(
        val barInternal: String,
    )

    private data class AnvilSmithTask(
        val meta: SmithingProductMeta,
        val amount: Int,
        val completed: Int,
    )

    private companion object {
        private const val ANVIL_INITIAL_DELAY = 1
        /** Matches Alter [repeatWhile] delay between smith cycles. */
        private const val ANVIL_CYCLE_DELAY = 5
    }
}
