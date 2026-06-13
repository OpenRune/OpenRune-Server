package org.rsmod.content.skills.smithing

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.smithing.SmithingCrystalSingingRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.smithing.util.SmithingUtils
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CrystalSingingScript : PluginScript() {

    private val crystalShardInternal = "obj.prif_crystal_shard"

    private val crystalItems = SmithingCrystalSingingRow.all()

    private val itemsByOutput = crystalItems.associateBy { it.output.internalName }

    override fun ScriptContext.startup() {
        onOpLoc1("loc.prif_singing_bowl") {
            openCrystalSinging()
        }

        onPlayerQueueWithArgs<CrystalSingTask>("queue.smithing_crystal_singing") {
            processSingTask(it.args)
        }
    }

    private suspend fun ProtectedAccess.openCrystalSinging() {
        val available = crystalItems.filter { canUseItem(it) }
        if (available.isEmpty()) {
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                verb = "make",
                entries = available.map(::toEntry),
                maxCountProvider = { inventory, entry ->
                    itemsByOutput[entry.internal]
                        ?.let { maxCraftCount(inventory, it) }
                        ?: 0
                },
            ),
        ) { selection ->
            val item = itemsByOutput[selection.entry.internal] ?: return@openSkillMulti
            startSinging(item, selection.amount)
        }
    }

    private fun toEntry(item: SmithingCrystalSingingRow): SkillMultiEntry = SkillMultiEntry(
        item.output.internalName,
        item.input.zip(item.inputAmount).map { (material, amount) ->
                Material(material.internalName, amount)
            },
        )

    private fun ProtectedAccess.canUseItem(item: SmithingCrystalSingingRow): Boolean {
        if (player.smithingLvl < item.statReq.first().t1 || player.craftingLvl < item.statReq.first().t1) {
            return false
        }

        return item.input.zip(item.inputAmount).any { (material, _) ->
            material.internalName != crystalShardInternal &&
                inv.contains(material.internalName)
        }
    }

    private suspend fun ProtectedAccess.startSinging(
        item: SmithingCrystalSingingRow,
        requestedAmount: Int,
    ) {
        val amount = minOf(requestedAmount, maxCraftCount(inv, item))
        if (amount <= 0) {
            return
        }

        if (!confirmSinging(item)) {
            return
        }

        queueNext(item, amount, completed = 0)
    }

    private suspend fun ProtectedAccess.confirmSinging(
        item: SmithingCrystalSingingRow,
    ): Boolean {
        if (player.attr[CRYSTAL_DONT_ASK_AGAIN] == true) {
            return true
        }

        val materialsText = item.input.zip(item.inputAmount)
            .filterNot { (material, _) ->
                material.internalName == crystalShardInternal
            }.joinToString(" and ") { (material, amount) ->
                val name = SmithingUtils.itemName(material)
                "${SmithingUtils.countLiteral(amount)} ${SmithingUtils.pluralize(name, amount)}"
            }

        val shortName = item.shortname ?: SmithingUtils.itemName(item.output)

        objbox(
            item.output.internalName,
            "This will consume $materialsText. " +
                "Reverting the $shortName will not give you any materials back.",
        )

        return when (
            choice3(
                "Yes",
                1,
                "Yes, and don't ask again.",
                2,
                "No",
                3,
                title = "Are you sure you wish to proceed?",
            )
        ) {
            1 -> true
            2 -> {
                player.attr[CRYSTAL_DONT_ASK_AGAIN] = true
                true
            }
            else -> false
        }
    }

    private suspend fun ProtectedAccess.processSingTask(
        task: CrystalSingTask,
    ) {
        if (!canProduce(task.item)) {
            resetAnim()
            return
        }

        performSing(task.item)

        val completed = task.completed + 1
        if (completed >= task.amount || !canProduce(task.item)) {
            return
        }

        queueNext(task.item, task.amount, completed)
    }

    private fun ProtectedAccess.queueNext(item: SmithingCrystalSingingRow, amount: Int, completed: Int, ) {
        weakQueue(
            "queue.smithing_crystal_singing",
            1,
            CrystalSingTask(item, amount, completed),
        )
    }

    private fun ProtectedAccess.performSing(item: SmithingCrystalSingingRow, ) {
        anim("seq.prif_crystal_singing")
        soundSynth("synth.crystal_sing")

        val removed = item.input.zip(item.inputAmount).all { (material, amount) ->
            invDel(inv, material.internalName, amount).success
        }

        if (!removed) {
            return
        }

        invAdd(inv, item.output.internalName, 1)

        statAdvance("stat.smithing", item.xp.toDouble())
        statAdvance("stat.crafting", item.xp.toDouble())
    }

    private suspend fun ProtectedAccess.canProduce(item: SmithingCrystalSingingRow, ): Boolean {
        val missingMaterials = item.input.zip(item.inputAmount).mapNotNull { (material, required) ->
            val missing = required - inv.count(material.internalName)

            if (missing > 0) {
                SmithingUtils.itemName(material) to missing
            } else {
                null
            }
        }

        if (missingMaterials.isNotEmpty()) {
            val missingText = missingMaterials.joinToString(", ") { (name, amount) ->
                "${SmithingUtils.countLiteral(amount)} ${name.lowercase()}"
            }

            val outputName = SmithingUtils.itemName(item.output)

            mesbox(
                "You need $missingText to make " +
                    "${SmithingUtils.prefixAn(outputName).lowercase()}.",
            )

            return false
        }

        return SmithingUtils.requireSmithingAndCraftingLevel(
            this,
            item.statReq.first().t1,
            "create ${SmithingUtils.itemName(item.output).lowercase()}",
        )
    }

    private fun maxCraftCount(inventory: Inventory, item: SmithingCrystalSingingRow, ): Int =
        item.input.zip(item.inputAmount).minOfOrNull { (material, required) ->
            inventory.count(material.internalName) / required
        }?: 0

    private data class CrystalSingTask(
        val item: SmithingCrystalSingingRow,
        val amount: Int,
        val completed: Int,
    )

    private companion object {
        private val CRYSTAL_DONT_ASK_AGAIN = AttributeKey<Boolean>(persistenceKey = "crystal_singing_dont_ask_again")
    }
}
