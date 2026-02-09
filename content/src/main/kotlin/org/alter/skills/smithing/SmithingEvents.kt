package org.alter.skills.smithing

import dev.openrune.ServerCacheManager
import org.alter.api.Skills
import org.alter.api.ext.*
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ButtonClickEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.game.util.enum
import org.alter.game.util.vars.IntType
import org.alter.game.util.vars.ObjType
import org.alter.interfaces.ifCloseModal
import org.alter.interfaces.ifOpenMainModal
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType
import org.alter.skills.smithing.SmithingData.barsByOutput
import org.alter.skills.smithing.SmithingData.typeForChild
import org.generated.tables.smithing.SmithingBarsRow
import kotlin.random.Random

data class SmithingMetaData(
    val id: Int,
    val name: String,
    val bar: SmithingBarsRow?,
    val barCount: Int,
    val numProduced: Int,
    val level: Int
)

class SmithingEvents : PluginEvent() {

    val hammers = listOf(
        "items.hammer".asRSCM(),
        "items.imcando_hammer".asRSCM(),
        "items.imcando_hammer_offhand".asRSCM()
    )

    private val smithsUniformPieces = listOf(
        "items.smithing_uniform_torso".asRSCM(),
        "items.smithing_uniform_legs".asRSCM(),
        "items.smithing_uniform_gloves".asRSCM(),
        "items.smithing_uniform_boots".asRSCM()
    )

    private val levelReq = enum("enums.smithing_level_req", ObjType, IntType)
    private val barCount = enum("enums.smithing_bar_count", ObjType, IntType)
    private val producedCountEnum = enum("enums.smithing_bar_produced_count", ObjType, IntType)

    private val itemNames = levelReq.associate { it.key to ServerCacheManager.getItem(it.key)!!.name.lowercase() }

    private val barEnum = enum("enums.smithing_bars", IntType, ObjType)
    private val barIds: Set<Int> = barEnum.mapTo(hashSetOf()) { it.value }

    private val allBars: List<SmithingBarsRow> = SmithingData.allBars.filter { it.output in barIds }

    private val metaData: List<SmithingMetaData> = levelReq.map { (id, level) ->
        SmithingMetaData(
            id = id,
            name = itemNames.getValue(id),
            bar = getBarForSmithing(itemNames.getValue(id)),
            barCount = barCount.find { it.key == id }!!.value,
            numProduced = producedCountEnum.find { it.key == id }!!.value,
            level = level
        )
    }

    internal fun getBarForSmithing(name: String): SmithingBarsRow? {
        return allBars.firstOrNull { name.startsWith(it.prefix) } ?: when {
            name.endsWith("lantern frame") -> SmithingBarsRow.getRow("dbrows.iron".asRSCM())
            name.endsWith("lantern (unf)") -> SmithingBarsRow.getRow("dbrows.steel".asRSCM())
            name.endsWith("grapple tip") -> SmithingBarsRow.getRow("dbrows.mithril".asRSCM())
            else -> null
        }
    }

    override fun init() {

        // Clicking a hammer on an anvil
        on<ItemOnObject> {
            where { hammers.contains(item.id) && gameObject.getDef().category == SmithingUtils.ANVIL_CATEGORY }
            then {
                player.message(
                    "To smith a metal bar, click on the anvil while you have the bar in your inventory."
                )
            }
        }

        // Clicking a bar on an anvil
        on<ItemOnObject> {
            where { item.id in barIds && gameObject.getDef().category == SmithingUtils.ANVIL_CATEGORY }
            then {
                barsByOutput[item.id]?.let { queueSmithing(player, it) }
            }
        }

        // Clicking buttons in the smithing interface
        on<ButtonClickEvent> {
            where { component.interfaceId == "interfaces.smithing".asRSCM() }
            then {
                val barID = barEnum.find { it.key == player.getVarbit("varbits.smithing_bar_type") }?.value
                val resultList = barID?.let { id -> metaData.filter { it.bar?.output == id } } ?: emptyList()
                val componentString = RSCM.getReverseMapping(RSCMType.COMPONENTS, component.combinedId)
                val item = typeForChild(componentString, barsByOutput[barID]!!)
                val metaData = resultList.find { it.name == item }
                metaData?.let {
                    player.ifCloseModal("interfaces.smithing")
                    player.queue(TaskPriority.STRONG) {
                        smelt(player,this,metaData.bar!!,metaData,1)
                    }
                }
            }
        }

        // Clicking an anvil object
        on<ObjectClickEvent> {
            where { gameObject.getDef().category == SmithingUtils.ANVIL_CATEGORY }
            then {
                val bar = getBar(player)
                if (bar == null) {
                    player.queue {
                        messageBox(player, "You should select an item from your inventory and use it on the anvil.")
                    }
                    return@then
                }
                queueSmithing(player, bar)
            }
        }
    }

    private fun queueSmithing(player: Player, bar: SmithingBarsRow) {
        player.queue(TaskPriority.WEAK) {
            if (!canSmithBar(player, this, bar)) return@queue
            openSmithingInterface(player, bar)
        }
    }

    suspend fun smelt(player: Player, task: QueueTask, bar: SmithingBarsRow, meta: SmithingMetaData, amount: Int) {
        if (!canSmith(player,task, meta))
            return

        val barAmount = player.inventory.getItemCount(meta.bar!!.output)
        val maxAmount = (barAmount / meta.barCount)
        var craftAmount = amount.coerceAtMost(maxAmount)

        task.repeatWhile(delay = 5, immediate = true, canRepeat = { craftAmount != 0 }) {
            task.wait(2)
            player.lock()
            player.animate("sequences.human_smithing")
            player.playSound(3771)
            val anvilDelay = anvilActionDelay(player)
            task.wait(anvilDelay)

            if (!canSmith(player,task, meta)) {
                player.animate(RSCM.NONE)
                player.unlock()
                return@repeatWhile
            }

            val transaction = player.inventory.remove(item = meta.bar.output, amount = meta.barCount, assureFullRemoval = true)
            if (transaction.hasSucceeded()) {
                player.inventory.add(meta.id, meta.numProduced)
                player.addXp(Skills.SMITHING, (meta.barCount * meta.bar.smithxp))
            }
            player.unlock()
            craftAmount--
        }

    }

    private fun openSmithingInterface(player: Player, bar: SmithingBarsRow) {
        val enumIndex = barEnum.first { it.value == bar.output }.key
        player.setVarbit("varbits.smithing_bar_type", enumIndex)
        player.ifOpenMainModal("interfaces.smithing")
    }

    private suspend fun canSmithBar(player: Player, task: QueueTask, bar: SmithingBarsRow): Boolean {
        val barDef = ServerCacheManager.getItem(bar.output) ?: return false
        if (!SmithingUtils.requireSmithingLevel(task, player, bar.level, "work ${barDef.name.lowercase()}")) return false
        return SmithingUtils.hasHammer(player)
    }


    /**
     * Checks if the player can smith an item
     *
     * @param task  The queued task
     * @param meta  The item meta data
     * @return      If the item can be smithed
     */
    private suspend fun canSmith(player: Player, task: QueueTask, meta: SmithingMetaData): Boolean {
        if (!canSmithBar(player, task, meta.bar!!)) return false
        if (!SmithingUtils.requireSmithingLevel(task, player, meta.level, "make ${meta.name.prefixAn()}")) return false
        if (meta.barCount > player.inventory.getItemCount(meta.bar.output)) {
            val barDef = ServerCacheManager.getItem(meta.bar.output) ?: return false
            task.messageBox(player, "You don't have enough ${barDef.name.lowercase()}s to make ${meta.name.prefixAn()}.")
            return false
        }
        return true
    }

    /**
     * Anvil action delay in ticks. Base 3; Smith's uniform gives 20% per piece to reduce by 1 tick (full set = 100%).
     */
    private fun anvilActionDelay(player: Player): Int {
        val piecesWorn = smithsUniformPieces.count { player.equipment.contains(it) }
        return when {
            piecesWorn >= 4 -> 2
            piecesWorn > 0 && Random.nextDouble() < piecesWorn * 0.2 -> 2
            else -> 3
        }
    }

    /**
     * Returns the smithing bar the player is currently able to use.
     *
     * Priority is given to the last selected bar type (stored in a varbit) if the player
     * still has it in their inventory. Otherwise, returns the highest-level bar the player can smith.
     */
    private fun getBar(player: Player): SmithingBarsRow? {
        val inventory = player.inventory
        val smithingLevel = player.getSkills().getCurrentLevel(Skills.SMITHING)

        val varbitValue = player.getVarbit("varbits.smithing_bar_type")

        val lastBar = barEnum.getOrNull(varbitValue)?.value?.let { barsByOutput[it] }
            ?.takeIf { inventory.contains(it.output) && smithingLevel >= it.level }

        if (lastBar != null) {
            return lastBar
        }

        val bestBar = inventory.asSequence()
            .mapNotNull { it?.id }
            .distinct()
            .mapNotNull { barsByOutput[it] }
            .filter { smithingLevel >= it.level }
            .maxByOrNull { it.level }

        if (bestBar != null) {
            val enumIndex = barEnum.firstOrNull { it.value == bestBar.output }?.key
            if (enumIndex != null) {
                player.setVarbit("varbits.smithing_bar_type", enumIndex)
            }
        }

        return bestBar
    }
}
