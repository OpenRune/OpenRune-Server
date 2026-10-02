package org.rsmod.content.skills.magic.spell.attacks.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.npc.isDarkLured
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.movement.RouteRequestPathingEntity
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DarkLure {
    const val SPELL_OBJ = "obj.placeholder_snakepet_orange"
    const val CAST_ANIM = "seq.human_cast_vicious_strike"
    const val CAST_SPOTANIM = "spotanim.dark_lure_cast_spotanim"
    const val TRAVEL_SPOTANIM = "spotanim.dark_lure_travel_projanim"
    const val HIT_SPOTANIM = "spotanim.dark_lure_hit_spotanim"
    const val PROJANIM = "projanim.magic_spell"
    const val COOLDOWN_QUEUE = "queue.dark_lure_cooldown"
    const val LURED_VARN = "varn.dark_lure_end_clock"
    const val COOLDOWN_TICKS = 17
    const val LURE_TICKS = 102
    const val HIT_SPOTANIM_HEIGHT = 124
}

internal var Player.darkLureCooldown by boolVarBit("varbit.arceuus_dark_lure_cooldown")

class DarkLureScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueue(DarkLure.COOLDOWN_QUEUE) { player.darkLureCooldown = false }
    }
}

class DarkLureSpell @Inject constructor(private val worldQueues: WorldQueueList) : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        register(DarkLure.SPELL_OBJ, DarkLureAttack(manager))
    }

    private inner class DarkLureAttack(private val manager: SpellAttackManager) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            if (player.darkLureCooldown) {
                reject("You can only cast Dark Lure every 10 seconds.")
                return
            }
            if (target.isDarkLured()) {
                reject("That creature is already under the effect of a Dark Lure.")
                return
            }

            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            player.anim(DarkLure.CAST_ANIM, priority = 6)
            spotanim(DarkLure.CAST_SPOTANIM)

            player.darkLureCooldown = true
            clearQueue(DarkLure.COOLDOWN_QUEUE)
            queue(DarkLure.COOLDOWN_QUEUE, DarkLure.COOLDOWN_TICKS)

            val proj = manager.spawnProjectile(this, target, DarkLure.TRAVEL_SPOTANIM, DarkLure.PROJANIM)
            val (serverDelay, clientDelay) = proj.durations

            if (manager.rollSplash(this, target, attack, castResult)) {
                manager.playSplashFx(this, target, clientDelay, castSound = null, soundRadius = 8)
                manager.queueSplashHit(this, target, attack.spell.obj, clientDelay, serverDelay)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = null,
                soundRadius = 8,
                hitSpot = DarkLure.HIT_SPOTANIM,
                hitSpotHeight = DarkLure.HIT_SPOTANIM_HEIGHT,
                hitSound = null,
            )

            val caster = player
            val lured = target.uid
            worldQueues.add(serverDelay) {
                if (target.uid == lured && target.hitpoints > 0 && caster.isSlotAssigned) {
                    lure(target, caster)
                }
            }
            manager.continueCombatIfAutocast(this, target)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            reject("This spell can only be cast on monsters.")
        }

        private fun ProtectedAccess.reject(message: String) {
            manager.stopCombat(this)
            mes(message)
        }

        private fun lure(target: Npc, caster: Player) {
            target.vars[DarkLure.LURED_VARN] = target.currentMapClock + DarkLure.LURE_TICKS
            target.routeRequest = RouteRequestPathingEntity(caster.avatar)
        }
    }
}
