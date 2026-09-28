package org.rsmod.content.skills.magic.spell.attacks.ancient

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.hunt.HuntVis
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.death.PvPAttackValidateResult
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.npc.mapMultiway
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.mapMultiway
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getOrNull

class AncientSpells
@Inject
constructor(
    private val hunt: Hunt,
    private val rayCast: RayCastValidator,
    private val areaChecker: AreaChecker,
    private val worldRepo: WorldRepository,
    private val worldQueues: WorldQueueList,
    private val random: GameRandom,
    private val npcValidators: Set<NpcAttackValidateHook>,
    private val pvpValidators: Set<PvPAttackValidateHook>,
) : SpellAttackMap {
    private val effects = AncientSpellEffects(random)

    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        for (spell in AncientSpell.entries) {
            register(spell.obj, AncientSpellAttack(manager, spell))
        }
    }

    private inner class AncientSpellAttack(
        private val manager: SpellAttackManager,
        private val spell: AncientSpell,
    ) : SpellAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
            cast(target, attack)
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
            val castResult = manager.attemptCast(this, attack)
            if (castResult.isFailure()) {
                return
            }

            player.anim(spell.tier.anim, priority = 6)
            spell.launch?.let { spotanim(it, height = 92) }
            soundSynth(spell.castSound)

            val primary = baseProjectile(target)
            val (serverDelay, clientDelay) = primary.durations
            val targets = if (spell.tier.multiTarget) collectTargets(target) else listOf(target)
            val sceptre = player.wieldsAncientSceptre()

            for (victim in targets) {
                sendTravel(victim, primary)
                hitTarget(victim, attack, castResult, clientDelay, serverDelay, sceptre)
            }
            manager.continueCombatIfAutocast(this, target)
        }

        private fun ProtectedAccess.hitTarget(
            target: PathingEntity,
            attack: CombatAttack.Spell,
            castResult: MagicRuneManager.CastResult,
            clientDelay: Int,
            serverDelay: Int,
            sceptre: Boolean,
        ) {
            val spellObj = attack.spell.obj
            val guaranteedFreeze = effects.claimGuaranteedFreeze(spell, target)
            if (!guaranteedFreeze && manager.rollSplash(this, target, attack, castResult)) {
                manager.playSplashFx(this, target, clientDelay, castSound = null, soundRadius = 8)
                manager.queueSplashHit(this, target, spellObj, clientDelay, serverDelay)
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
                hitSpotHeight = spell.impactHeight,
                hitSound = spell.hitSound,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spellObj, damage, clientDelay, serverDelay)

            val caster = player
            val effect = {
                effects.apply(spell, caster, target, damage, sceptre, guaranteedFreeze)
            }
            val targetStillValid = target.validityCheck()
            worldQueues.add(serverDelay) {
                if (targetStillValid() && caster.isSlotAssigned) {
                    effect()
                }
            }
        }

        private fun ProtectedAccess.baseProjectile(target: PathingEntity): ProjAnim =
            when (target) {
                is Npc -> ProjAnim.fromBoundsToNpc(player.bounds(), target, NULL_SPOTANIM, PROJANIM)
                is Player ->
                    ProjAnim.fromBoundsToPlayer(player.bounds(), target, NULL_SPOTANIM, PROJANIM)
            }

        private fun ProtectedAccess.sendTravel(target: PathingEntity, primary: ProjAnim) {
            when (val travel = spell.travel) {
                Travel.None -> Unit
                is Travel.FromCaster -> {
                    val proj = baseProjectile(target)
                    worldRepo.projAnim(
                        proj.copy(
                            spotanim = travel.spotanim.asRSCM(RSCMType.SPOTANIM),
                            endHeight = travel.endHeight,
                        )
                    )
                }
                is Travel.FromTarget -> {
                    val proj = baseProjectile(target)
                    worldRepo.projAnim(
                        proj.copy(
                            spotanim = travel.spotanim.asRSCM(RSCMType.SPOTANIM),
                            endHeight = travel.endHeight,
                            endTime = primary.endTime,
                            startCoord = target.coords,
                        )
                    )
                }
            }
        }

        private fun ProtectedAccess.collectTargets(primary: PathingEntity): List<PathingEntity> {
            val targets = mutableListOf(primary)
            if (!primary.inMultiway()) {
                return targets
            }
            val centre = primary.coords
            for (npc in hunt.findNpcs(centre, AOE_SEARCH_RADIUS, HuntVis.Off)) {
                if (targets.size >= MAX_TARGETS) {
                    return targets
                }
                if (
                    npc !== primary &&
                        AncientSpellEffects.overlapsAoe(centre, npc.coords, npc.size) &&
                        primary.hasLineOfSightTo(npc) &&
                        canHitSecondary(npc)
                ) {
                    targets += npc
                }
            }
            for (other in hunt.findPlayers(centre, AncientSpellEffects.AOE_RADIUS, HuntVis.Off)) {
                if (targets.size >= MAX_TARGETS) {
                    return targets
                }
                if (
                    other !== primary &&
                        other !== player &&
                        primary.hasLineOfSightTo(other) &&
                        canHitSecondary(other)
                ) {
                    targets += other
                }
            }
            return targets
        }

        private fun ProtectedAccess.canHitSecondary(npc: Npc): Boolean {
            if (!npc.isValidTarget() || !npc.visType.hasOp(InteractionOp.Op2.slot)) {
                return false
            }
            if (!npc.mapMultiway(areaChecker)) {
                return false
            }
            return npcValidators.none { it.validate(player, npc) is NpcAttackValidateResult.Deny }
        }

        private fun ProtectedAccess.canHitSecondary(other: Player): Boolean {
            if (!other.isValidTarget() || !other.mapMultiway(areaChecker)) {
                return false
            }
            if (pvpValidators.isEmpty()) {
                return false
            }
            return pvpValidators.none { it.validate(player, other) is PvPAttackValidateResult.Deny }
        }

        private fun PathingEntity.hasLineOfSightTo(other: PathingEntity): Boolean {
            if (coords.level != other.coords.level) {
                return false
            }
            return rayCast.hasLineOfSight(
                source = coords,
                destination = other.coords,
                srcWidth = size,
                srcLength = size,
                destWidth = other.size,
                destLength = other.size,
            )
        }

        private fun PathingEntity.inMultiway(): Boolean =
            when (this) {
                is Npc -> mapMultiway(areaChecker)
                is Player -> mapMultiway(areaChecker)
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

        private fun Player.wieldsAncientSceptre(): Boolean {
            val weapon = getOrNull(righthand) ?: return false
            return ANCIENT_SCEPTRES.any(weapon::isType)
        }
    }

    private companion object {
        private const val PROJANIM = "projanim.magic_spell"
        private const val NULL_SPOTANIM = -1
        private const val MAX_TARGETS = 9
        private const val AOE_SEARCH_RADIUS = 6

        private val ANCIENT_SCEPTRES =
            listOf(
                "obj.ancient_sceptre",
                "obj.ancient_sceptre_trouver",
                "obj.ancient_sceptre_blood",
                "obj.ancient_sceptre_ice",
                "obj.ancient_sceptre_smoke",
                "obj.ancient_sceptre_shadow",
                "obj.ancient_sceptre_blood_trouver",
                "obj.ancient_sceptre_ice_trouver",
                "obj.ancient_sceptre_smoke_trouver",
                "obj.ancient_sceptre_shadow_trouver",
            )
    }
}
