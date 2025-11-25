package org.alter.game.action

import dev.openrune.ServerCacheManager.getAnim
import net.rsprot.protocol.game.outgoing.sound.MidiJingle
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.model.move.stopMovement
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.plugin.Plugin
import org.alter.game.service.log.LoggerService
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import java.lang.ref.WeakReference

/**
 * @author Tom <rspsmods@gmail.com>
 */
object PlayerDeathAction {
    private const val DEATH_ANIMATION = "sequences.human_death"

    val deathPlugin: Plugin.() -> Unit = {
        val player = ctx as Player

        player.interruptQueues()
        player.stopMovement()
        player.lock()

        // Reset combat state on death using reflection (Combat is in plugins module)
        try {
            val combatClass = Class.forName("org.alter.combat.Combat")
            val resetMethod = combatClass.getDeclaredMethod("reset", org.alter.game.model.entity.Pawn::class.java)
            resetMethod.invoke(null, player)
        } catch (e: Exception) {
            // If reflection fails, manually clear combat attributes
            player.attr.remove(org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR)
            player.resetFacePawn()
        }

        player.queue(TaskPriority.STRONG) {
            death(player)
        }
    }

    private suspend fun QueueTask.death(player: Player) {
        val world = player.world
        val deathAnim = getAnim(DEATH_ANIMATION.asRSCM())?: return
        val instancedMap = world.instanceAllocator.getMap(player.tile)
        player.write(MidiJingle(90))
        player.damageMap.getMostDamage()?.let { killer ->
            if (killer is Player) {
                world.getService(LoggerService::class.java, searchSubclasses = true)?.logPlayerKill(killer, player)
            }
            player.attr[KILLER_ATTR] = WeakReference(killer)
        }

        world.plugins.executePlayerPreDeath(player)
        player.resetFacePawn()
        wait(2)
        player.animate(DEATH_ANIMATION)
        wait(deathAnim.animationLength + 1)
        player.getSkills().restoreAll()
        player.animate(RSCM.NONE)
        if (instancedMap == null) {
            // Note: maybe add a player attribute for death locations
            player.moveTo(player.world.gameContext.home)
        } else {
            player.moveTo(instancedMap.exitTile)
            world.instanceAllocator.death(player)
        }
        player.writeMessage("Oh dear, you are dead!")
        player.unlock()

        player.attr.removeIf { it.resetOnDeath }
        player.timers.removeIf { it.resetOnDeath }

        world.plugins.executePlayerDeath(player)
    }
}
