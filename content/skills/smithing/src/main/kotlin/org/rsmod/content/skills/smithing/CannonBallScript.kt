package org.rsmod.content.skills.smithing

import org.rsmod.api.player.events.interact.LocUCategoryEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLocCategoryU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.smithing.SmithingCannonBallsRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.smithing.util.SmithingUtils
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CannonBallScript : PluginScript() {

    private val moulds = arrayOf("obj.ammo_mould", "obj.double_ammo_mould")
    private val allCannonBalls = SmithingCannonBallsRow.all()

    override fun ScriptContext.startup() {
        allCannonBalls.forEach { ball ->
            onOpLocCategoryU("category.furnace", ball.bar.internalName) {
                if (hasAmmoMould()) {
                    startSmelting(it, ball)
                }
            }
        }

        onOpHeldU("obj.mcannonball", "obj.granite_dust") {
            coatGraniteCannonballs()
        }

        onPlayerQueueWithArgs("queue.smithing_cannonball_smelt") {
            processSmeltTick(it.args)
        }
    }

    private fun ProtectedAccess.hasAmmoMould(): Boolean =
        moulds.any(inv::contains)

    private fun ProtectedAccess.startSmelting(
        event: LocUCategoryEvents.Op,
        ball: SmithingCannonBallsRow,
    ) {
        startSmelting(event.type.internalName, ball, amount = 28)
    }

    private fun ProtectedAccess.startSmelting(
        locInternal: String,
        ball: SmithingCannonBallsRow,
        amount: Int,
    ) {
        val barsPerSmelt = barsPerSmelt(inv)
        val maxSmelts = minOf(amount, inv.count(ball.bar.internalName) / barsPerSmelt)
        if (maxSmelts <= 0) {
            return
        }

        val task =
            CannonballSmeltTask(
                ball = ball,
                locInternal = locInternal,
                targetSmelts = maxSmelts,
                completedSmelts = 0,
            )

        weakQueue("queue.smithing_cannonball_smelt", 1, task)
    }

    private suspend fun ProtectedAccess.processSmeltTick(task: CannonballSmeltTask) {
        if (!canSmelt(task.ball)) {
            resetAnim()
            return
        }

        performSmelt(task.ball)

        val completed = task.completedSmelts + 1
        if (completed < task.targetSmelts && canSmelt(task.ball)) {
            weakQueue(
                "queue.smithing_cannonball_smelt",
                smeltCycleDelay(task.locInternal),
                task.copy(completedSmelts = completed),
            )
        }
    }

    private suspend fun ProtectedAccess.performSmelt(ball: SmithingCannonBallsRow) {
        val barsPerSmelt = barsPerSmelt(inv)
        val ballsPerSmelt = ballsPerSmelt(inv)
        val barName = SmithingUtils.itemName(ball.bar, "bar")
        val ballName = SmithingUtils.itemName(ball.output, "cannonball")

        anim("seq.human_furnace")
        soundSynth("synth.furnace")

        invDel(inv, ball.bar.internalName, barsPerSmelt)

        mes("You heat the $barName into a liquid state.")
        delay(2)

        mes("You pour the molten metal into your cannonball mould.")
        delay(1)

        mes(
            "The molten metal cools slowly to form $ballsPerSmelt " +
                SmithingUtils.prefixAn(ballName) + ".",
        )
        delay(1)

        mes("You remove the cannonballs from the mould.")
        if (invAdd(inv, ball.output.internalName, ballsPerSmelt).success) {
            statAdvance("stat.smithing", ball.xp * barsPerSmelt.toDouble())
        }
    }

    private suspend fun ProtectedAccess.canSmelt(ball: SmithingCannonBallsRow): Boolean {
        if (!hasAmmoMould()) {
            mesbox("You need a mould to do this.")
            return false
        }

        val barsPerSmelt = barsPerSmelt(inv)
        val ballName = ball.output.name
        val barName = ball.bar.name

        if (inv.count(ball.bar.internalName) < barsPerSmelt) {
            mesbox(
                "You need ${SmithingUtils.countLiteral(barsPerSmelt)} $barName to make " +
                    SmithingUtils.prefixAn(ballName) + ".",
            )
            return false
        }

        return SmithingUtils.requireSmithingLevel(
            this,
            ball.level,
            "smelt ${SmithingUtils.prefixAn(ballName)}",
        )
    }

    private suspend fun ProtectedAccess.coatGraniteCannonballs() {
        if (!SmithingUtils.requireSmithingLevel(this, 50, "coat the cannonballs")) {
            return
        }

        val totalBalls = inv.count("obj.mcannonball")
        val graniteDust = inv.count("obj.granite_dust")
        val toCreate = minOf(totalBalls, graniteDust)
        if (toCreate <= 0) {
            return
        }

        val removedBalls = invDel(inv, "obj.mcannonball", toCreate).success
        val removedDust = invDel(inv, "obj.granite_dust", toCreate).success

        if (removedBalls && removedDust && invAdd(inv, "obj.granite_cannonball", toCreate).success) {
            mes("You apply a thick coating of granite dust to your cannonballs.")
        }
    }

    private fun barsPerSmelt(inv: org.rsmod.game.inv.Inventory): Int =
        if (inv.contains("obj.double_ammo_mould")) 2 else 1

    private fun ballsPerSmelt(inv: org.rsmod.game.inv.Inventory): Int =
        if (inv.contains("obj.double_ammo_mould")) 8 else 4

    private fun smeltCycleDelay(locInternal: String): Int =
        if (locInternal.contains("grimstone_furnace")) 1 else 4
}

private val cannonballMoulds = arrayOf("obj.ammo_mould", "obj.double_ammo_mould")

internal fun ProtectedAccess.hasCannonballFurnaceMould(): Boolean =
    cannonballMoulds.any(inv::contains)

/** @return `true` if a cannonball smelt menu was opened. */
internal suspend fun ProtectedAccess.openCannonballFurnaceMenu(locInternal: String): Boolean {
    val allCannonBalls = SmithingCannonBallsRow.all()
    val available = allCannonBalls.filter { inv.contains(it.bar.internalName) }
    if (available.isEmpty()) {
        return false
    }

    val ballsByOutput = allCannonBalls.associateBy { it.output.internalName }
    openSkillMulti(
        SkillMultiConfig(
            verb = "smelt",
            actionType = SkillingActionType.SMELT,
            entries =
                available.map { ball ->
                    SkillMultiEntry(
                        ball.output.internalName,
                        listOf(Material(ball.bar.internalName)),
                    )
                },
            maxCountProvider = { inventory, entry ->
                val ball = ballsByOutput[entry.internal] ?: return@SkillMultiConfig 0
                val barsPerSmelt = cannonballBarsPerSmelt(inventory)
                inventory.count(ball.bar.internalName) / barsPerSmelt
            },
        ),
    ) { selection ->
        val ball = ballsByOutput[selection.entry.internal] ?: return@openSkillMulti
        startCannonballFurnaceSmelting(locInternal, ball, selection.amount)
    }
    return true
}

private fun cannonballBarsPerSmelt(inv: org.rsmod.game.inv.Inventory): Int =
    if (inv.contains("obj.double_ammo_mould")) 2 else 1

private suspend fun ProtectedAccess.startCannonballFurnaceSmelting(
    locInternal: String,
    ball: SmithingCannonBallsRow,
    amount: Int,
) {
    val barsPerSmelt = cannonballBarsPerSmelt(inv)
    val maxSmelts = minOf(amount, inv.count(ball.bar.internalName) / barsPerSmelt)
    if (maxSmelts <= 0) {
        return
    }

    weakQueue(
        "queue.smithing_cannonball_smelt",
        1,
        CannonballSmeltTask(
            ball = ball,
            locInternal = locInternal,
            targetSmelts = maxSmelts,
            completedSmelts = 0,
        ),
    )
}

private data class CannonballSmeltTask(
    val ball: SmithingCannonBallsRow,
    val locInternal: String,
    val targetSmelts: Int,
    val completedSmelts: Int,
)
