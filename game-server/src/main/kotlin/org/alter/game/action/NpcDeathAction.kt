package org.alter.game.action

import dev.openrune.ServerCacheManager.getAnim
import org.alter.game.action.NpcDeathAction.reset
import org.alter.game.info.NpcInfo
import org.alter.game.model.LockState
import org.alter.game.model.attr.COMBAT_ATTACKERS_ATTR
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.KILLER_ATTR
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.move.moveTo
import org.alter.game.model.move.stopMovement
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.TaskPriority
import org.alter.game.model.weightedTableBuilder.roll
import org.alter.game.plugin.Plugin
import org.alter.game.service.log.LoggerService
import org.alter.rscm.RSCM.asRSCM
import java.lang.ref.WeakReference

/**
 * This class is responsible for handling npc death events.
 *
 * @author Tom <rspsmods@gmail.com>
 */
object NpcDeathAction {
    var deathPlugin: Plugin.() -> Unit = {
        val npc = ctx as Npc
        val world = npc.world

        // Efficiently clear combat targets from only the pawns that were attacking this NPC
        val attackers = npc.attr[COMBAT_ATTACKERS_ATTR]
        if (attackers != null) {
            attackers.forEach { attackerRef ->
                val attacker = attackerRef.get()
                if (attacker != null) {
                    attacker.attr.remove(COMBAT_TARGET_FOCUS_ATTR)
                    attacker.resetFacePawn()
                }
            }
            // Clear the attackers set since the NPC is dead
            npc.attr.remove(COMBAT_ATTACKERS_ATTR)
        }

        if (!world.plugins.executeNpcFullDeath(npc)) {
            npc.interruptQueues()
            npc.stopMovement()
            npc.lock()
            npc.queue(TaskPriority.STRONG) {
                death(npc)
            }
        }
    }

    suspend fun QueueTask.death(npc: Npc) {
        val world = npc.world
        val deathAnimation = npc.combatDef.deathAnimation
        val deathSound = npc.combatDef.defaultDeathSound
        val respawnDelay = npc.combatDef.respawnDelay
        var killer: Pawn? = null
        npc.damageMap.getMostDamage()?.let {
            if (it is Player) {
                killer = it
                world.getService(LoggerService::class.java, searchSubclasses = true)?.logNpcKill(it, npc)
            }
            npc.attr[KILLER_ATTR] = WeakReference(it)
        }

        // Efficiently clear combat targets from only the pawns that were attacking this NPC
        // (This is a safety check in case attackers weren't cleared in deathPlugin)
        val attackers = npc.attr[COMBAT_ATTACKERS_ATTR]
        if (attackers != null) {
            attackers.forEach { attackerRef ->
                val attacker = attackerRef.get()
                if (attacker != null) {
                    attacker.attr.remove(COMBAT_TARGET_FOCUS_ATTR)
                    attacker.resetFacePawn()
                }
            }
            npc.attr.remove(COMBAT_ATTACKERS_ATTR)
        }

        NpcInfo(npc).setAllOpsInvisible()
        world.plugins.executeNpcPreDeath(npc)
        npc.resetFacePawn()
        if (npc.combatDef.defaultDeathSoundArea) {
            world.spawn(AreaSound(npc.tile, deathSound, npc.combatDef.defaultDeathSoundRadius, npc.combatDef.defaultDeathSoundVolume))
        } else {
            (killer as? Player)?.playSound(deathSound, npc.combatDef.defaultDeathSoundVolume)
        }

        /**
         * @TODO add interruption for this block if we would want to execute a plugin during it's death animation
         */
        deathAnimation.forEach { anim ->
            val def = getAnim(anim.asRSCM())?: return

            npc.animate(anim, def.animationLength + 1)
            wait(def.animationLength + 1)
        }
        world.plugins.executeNpcDeath(npc)
        world.plugins.anyNpcDeath.forEach {
            npc.executePlugin(it)
        }
        if (npc.respawns) {
            NpcInfo(npc).setInaccessible(true)
            npc.reset()
            wait(respawnDelay)
            NpcInfo(npc).setAllOpsVisible()
            NpcInfo(npc).setInaccessible(false)
            world.plugins.executeNpcSpawn(npc)
        } else {
            world.remove(npc)
        }
    }
    private fun Npc.reset() {
        lock = LockState.NONE
        moveTo(spawnTile)
        attr.clear()
        timers.clear()
        world.setNpcDefaults(this)
    }
}
