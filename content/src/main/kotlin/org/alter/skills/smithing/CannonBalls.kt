package org.alter.skills.smithing

import dev.openrune.ServerCacheManager
import org.alter.api.Skills
import org.alter.api.ext.itemMessageBox
import org.alter.api.ext.message
import org.alter.api.ext.messageBox
import org.alter.api.ext.prefixAn
import org.alter.api.ext.produceItemBox
import org.alter.api.ext.toLiteral
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnObject
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.game.pluginnew.event.impl.onItemOnItem
import org.alter.rscm.RSCM
import org.generated.tables.smithing.SmithingCannonBallsRow

class CannonBalls : PluginEvent() {

    private val moulds = arrayOf("items.ammo_mould", "items.double_ammo_mould")
    private val allCannonBalls = SmithingCannonBallsRow.all()
    private val barsByCannonBalls = allCannonBalls.associateBy { it.output }

    override fun init() {
        on<ItemOnObject> {
            where {
                gameObject.getDef().category == 215 &&
                        player.inventory.containsAny(*moulds) &&
                        allCannonBalls.any { it.bar == item.id }
            }
            then { smeltCannonBalls(player,gameObject.id) }
        }

        on<ObjectClickEvent> {
            where {
                gameObject.getDef().category == 215 &&
                        player.inventory.containsAny(*moulds)
            }
            then { smeltCannonBalls(player,gameObject.id) }
        }

        onItemOnItem("items.mcannonball", "items.granite_dust") {
            val totalBalls = player.inventory.getItemCount("items.mcannonball")
            val graniteDust = player.inventory.getItemCount("items.granite_dust")

            player.queue {
                val smithingLevel = player.getSkills().getCurrentLevel(Skills.SMITHING)
                if (smithingLevel < 50) {
                    messageBox(player, "You need a ${Skills.getSkillName(Skills.SMITHING)} level of at least 50 to coat the cannonballs.")
                    return@queue
                }

                val toCreate = minOf(totalBalls, graniteDust)
                if (toCreate <= 0) return@queue

                if (player.inventory.add("items.granite_cannonball", toCreate).hasSucceeded()) {
                    val removedBalls = player.inventory.remove("items.mcannonball", toCreate).hasSucceeded()
                    val removedDust = player.inventory.remove("items.granite_dust", toCreate).hasSucceeded()

                    if (removedBalls && removedDust) {
                        player.message("You apply a thick coating of granite dust to your cannonballs.")
                    }
                }
            }
        }
    }

    private fun smeltCannonBalls(player: Player,objectID : String) {
        player.queue {
            val availableBalls = allCannonBalls
                .filter { player.inventory.contains(it.bar) }
                .map { it.output }

            if (availableBalls.isEmpty()) return@queue

            produceItemBox(
                player,
                *availableBalls.toIntArray(),
                title = "What would you like to smelt?",
            ) { selectedItemId: Int, quantity: Int ->
                startSmelting(player, selectedItemId, quantity, objectID)
            }

        }
    }

    private fun startSmelting(player: Player, output: Int, amount: Int = 28, objectID : String) {
        val ball = barsByCannonBalls[output] ?: return

        player.queue {
            if (!canSmelt(this, player, ball)) return@queue

            val doubleBalls = player.inventory.contains("items.double_ammo_mould")
            val barsNeeded = if (doubleBalls) 2 else 1
            val ballsPerSmelt = if (doubleBalls) 8 else 4
            var maxSmelts = minOf(amount, player.inventory.getItemCount(ball.bar) / barsNeeded)
            val delay = if (objectID == "objects.grimstone_furnace") 1 else 4
            repeatWhile(delay = delay, immediate = true, canRepeat = { maxSmelts != 0 }) {
                player.lock()
                if (!canSmelt(task, player, ball)) {
                    player.animate(RSCM.NONE)
                    player.unlock()
                    return@repeatWhile
                }

                performSmelting(task,player, ball, barsNeeded, ballsPerSmelt)
                maxSmelts--
            }
        }
    }

    private suspend fun performSmelting(task: QueueTask, player: Player, ball: SmithingCannonBallsRow, barsNeeded: Int, ballsPerSmelt: Int) {
        val barName = ServerCacheManager.getItem(ball.bar)?.name ?: "bar"
        val ballName = ServerCacheManager.getItem(ball.output)?.name ?: "cannonball"

        player.animate("sequences.human_furnace")
        player.playSound(2725)
        player.message("You heat the $barName into a liquid state.")
        task.wait(2)

        player.animate("sequences.human_furnace")
        player.inventory.remove(ball.bar, barsNeeded)
        player.message("You pour the molten metal into your cannonball mould")
        task.wait(1)

        player.message("The molten metal cools slowly to form $ballsPerSmelt ${ballName.prefixAn()}")
        task.wait(1)

        player.message("You remove the cannonballs from the mould")
        player.inventory.add(ball.output, ballsPerSmelt)
        player.addXp(Skills.SMITHING, ball.xp * barsNeeded)
        player.unlock()
    }

    private suspend fun canSmelt(task: QueueTask, player: Player, ball: SmithingCannonBallsRow): Boolean {
        val doubleBalls = player.inventory.contains("items.double_ammo_mould")

        if (!player.inventory.containsAny(*moulds)) {
            task.messageBox(player, "You need a mould to do this.")
            return false
        }

        val barsNeeded = if (doubleBalls) 2 else 1
        val hasBars = player.inventory.getItemCount(ball.bar) >= barsNeeded
        val ballName = ServerCacheManager.getItem(ball.output)?.name ?: "cannonball"

        if (!hasBars) {
            val message = "You need ${barsNeeded.toLiteral()} $ballName to make ${ballName.prefixAn()}."
            task.messageBox(player, message)
            return false
        }

        val smithingLevel = player.getSkills().getCurrentLevel(Skills.SMITHING)
        if (smithingLevel < ball.level) {
            task.messageBox(player, "You need a ${Skills.getSkillName(Skills.SMITHING)} level of at least ${ball.level} to smelt ${ballName.prefixAn()}.")
            return false
        }

        return true
    }
}