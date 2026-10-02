package org.rsmod.content.skills.magic.spell.attacks.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.WorldQueueList

internal const val GRASP_CAST_ANIM = "seq.human_spellcast_grasp"

class GraspSpells
@Inject
constructor(private val random: GameRandom, private val worldQueues: WorldQueueList) :
    SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        for (spell in GraspSpell.entries) {
            register(spell.obj, GraspSpellAttack(manager, spell))
        }
    }

    internal enum class GraspSpell(
        val obj: String,
        val maxHit: Int,
        val bindChance: Int,
        val markedBindChance: Int,
        val bindTicks: Int,
        val cast: String,
        val bind: String,
        val impact: String,
    ) {
        Ghostly(
            obj = "obj.dmm_ref_hat",
            maxHit = 12,
            bindChance = 10,
            markedBindChance = 20,
            bindTicks = 2,
            cast = "spotanim.ghostly_grasp_cast_spotanim",
            bind = "spotanim.ghostly_grasp_hit_spotanim",
            impact = "spotanim.ghostly_grasp_miss_spotanim",
        ),
        Skeletal(
            obj = "obj.dmm_ref_torso",
            maxHit = 17,
            bindChance = 25,
            markedBindChance = 50,
            bindTicks = 3,
            cast = "spotanim.skeletal_grasp_cast_spotanim",
            bind = "spotanim.skeletal_grasp_hit_spotanim",
            impact = "spotanim.skeletal_grasp_miss_spotanim",
        ),
        Undead(
            obj = "obj.dmm_ref_legs",
            maxHit = 24,
            bindChance = 50,
            markedBindChance = 100,
            bindTicks = 4,
            cast = "spotanim.undead_grasp_cast_spotanim",
            bind = "spotanim.undead_grasp_hit_spotanim",
            impact = "spotanim.undead_grasp_miss_spotanim",
        ),
    }

    private inner class GraspSpellAttack(
        private val manager: SpellAttackManager,
        private val spell: GraspSpell,
    ) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            cast(target, ProjAnim.fromBoundsToNpc(player.bounds(), target, -1, PROJANIM), attack)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            cast(target, ProjAnim.fromBoundsToPlayer(player.bounds(), target, -1, PROJANIM), attack)
        }

        private fun ProtectedAccess.cast(
            target: PathingEntity,
            proj: ProjAnim,
            attack: CombatAttack.Spell,
        ) {
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            player.anim(GRASP_CAST_ANIM, priority = 6)
            spotanim(spell.cast)

            val (serverDelay, clientDelay) = proj.durations
            val spellObj = attack.spell.obj

            if (manager.rollSplash(this, target, attack, castResult)) {
                manager.playSplashFx(this, target, clientDelay, castSound = null, soundRadius = 8)
                manager.queueSplashHit(this, target, spellObj, clientDelay, serverDelay)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            val damage = manager.rollMaxHit(this, target, attack, castResult, spell.maxHit)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = null,
                soundRadius = 8,
                hitSpot = spell.impact,
                hitSpotHeight = 0,
                hitSound = null,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spellObj, damage, clientDelay, serverDelay)
            queueBind(target, serverDelay)
            manager.continueCombatIfAutocast(this, target)
        }

        private fun ProtectedAccess.queueBind(target: PathingEntity, serverDelay: Int) {
            val marked = player.vars["varbit.mark_of_darkness_active"] == 1
            val chance = if (marked) spell.markedBindChance else spell.bindChance
            if (random.of(100) >= chance) {
                return
            }
            val caster = player
            val stillValid = target.validityCheck()
            worldQueues.add(serverDelay) {
                if (stillValid() && caster.isSlotAssigned && bind(target)) {
                    target.spotanim(spell.bind, slot = BIND_SPOTANIM_SLOT)
                }
            }
        }

        private fun bind(target: PathingEntity): Boolean =
            when (target) {
                is Npc -> {
                    val resistance = target.visType.paramOrNull(params.freeze_resistance) ?: 0
                    val resisted = resistance > 0 && random.of(100) < resistance
                    !resisted && CombatEffects.freeze(target, spell.bindTicks)
                }
                is Player -> {
                    val warded = target.vars["varbit.ward_of_arceuus_active"] == 1
                    CombatEffects.freeze(target, if (warded) WARDED_BIND_TICKS else spell.bindTicks)
                }
            }

        private fun PathingEntity.validityCheck(): () -> Boolean =
            when (this) {
                is Npc -> {
                    val castUid = uid
                    val check = { uid == castUid && hitpoints > 0 }
                    check
                }
                is Player -> ::isValidTarget
            }
    }

    private companion object {
        private const val PROJANIM = "projanim.magic_spell"
        private const val BIND_SPOTANIM_SLOT = 1
        private const val WARDED_BIND_TICKS = 1
    }
}
