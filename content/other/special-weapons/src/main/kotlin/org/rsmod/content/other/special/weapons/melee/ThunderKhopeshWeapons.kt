package org.rsmod.content.other.special.weapons.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.weapons.MeleeWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Ordinary attacks (Chop/Slash/Lunge/Block) - the item's own cache-defined bonuses, no bespoke
 * accuracy or damage. The weapon's only unusual behavior outside its special attack (see
 * `ThunderKhopeshSpecialAttack.kt`) is its passive. Wiki: "grants standard, successful hits a 20%
 * chance to summon a delayed lightning bolt at the target's location, damaging enemies in a 3x3
 * area for up to 50% of the player's max hit... The lightning bolt does not roll accuracy checks...
 * No experience is gained from the damage dealt by the lightning bolts."
 *
 * NPC-only, matching the special attack's own PvM lightning branch
 * (`ThunderKhopeshLightning.schedulePvm`, duplicated here rather than shared since this module
 * doesn't otherwise depend on `special-attacks`) - the wiki never documents a PvP variant of the
 * passive, and this weapon was PvM-exclusive Leagues/Grid Master content.
 */
class ThunderKhopeshWeapons
@Inject
constructor(
    private val worldQueues: WorldQueueList,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcHitModifier: NpcHitModifier,
    private val npcAttackValidateHooks: Set<NpcAttackValidateHook>,
) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val khopesh = ThunderKhopesh(manager, this@ThunderKhopeshWeapons)
        register("obj.thunder_khopesh", khopesh)
        register("obj.deadman_thunder_khopesh", khopesh)
    }

    private class ThunderKhopesh(
        private val manager: WeaponAttackManager,
        private val passive: ThunderKhopeshWeapons,
    ) : MeleeWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = swing(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = swing(target, attack)

        private fun ProtectedAccess.swing(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            playSwingFx(attack)
            val maximum =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.0,
                    maxHitMultiplier = 1.0,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            if (damage > 0 && random.of(100) < PASSIVE_PROC_CHANCE_PERCENT) {
                val maximumLightningHit = maximum * PASSIVE_LIGHTNING_MAX_HIT_PERCENT / 100
                passive.scheduleLightning(player, target.coords, maximumLightningHit)
            }
            manager.continueCombat(this, target)
            return true
        }

        /**
         * Both Thunder khopesh variants' cache entries are missing `attack_anim_stance*`/
         * `attack_sound_stance*` params entirely, so `WeaponAttackManager.playWeaponFx` has nothing
         * to read and falls back to the generic unarmed punch/kick. Played directly here instead,
         * with no cache dependency at all - same real animations/sounds Dragon scimitar uses (an
         * identical SlashSword weapon per the wiki's own combat-style table: Chop/Slash/Lunge/
         * Block).
         */
        private fun ProtectedAccess.playSwingFx(attack: CombatAttack.Melee) {
            val stab = attack.stance == CombatStance.Stance3
            val anim = if (stab) "seq.human_sword_stab" else "seq.human_sword_slash"
            val soundId = if (stab) STAB_SOUND else SLASH_SOUND
            player.anim(anim, priority = 6)
            player.soundSynth(soundId)
        }
    }

    private fun scheduleLightning(source: Player, centre: CoordGrid, maximumHit: Int) {
        if (maximumHit <= 0) {
            return
        }
        val sourceUid = source.uid.packed
        worldQueues.add(LIGHTNING_DELAY) {
            if (!source.isSlotAssigned || source.uid.packed != sourceUid) {
                return@add
            }

            val zone = ZoneKey.from(centre)
            for (target in npcs.findAll(zone, zoneRadius = SEARCH_ZONE_RADIUS)) {
                if (!target.occupiesLightningTile(centre) || !canAttack(source, target)) {
                    continue
                }
                target.spotanim("spotanim.fx_khopesh_lightning_special_extra")
                val damage = random.of(0..maximumHit)
                target.queueHit(
                    source = source,
                    delay = TARGET_HIT_DELAY,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = npcHitModifier,
                )
            }
        }
    }

    private fun canAttack(source: Player, target: Npc): Boolean {
        if (!target.isValidTarget() || !target.visType.hasOp(InteractionOp.Op2.slot)) {
            return false
        }
        return npcAttackValidateHooks.all { hook ->
            hook.validate(source, target) !is NpcAttackValidateResult.Deny
        }
    }

    private fun Npc.occupiesLightningTile(centre: CoordGrid): Boolean =
        bounds().asSequence().any { it.chebyshevDistance(centre) <= LIGHTNING_RADIUS }

    private companion object {
        const val SLASH_SOUND: Int = 2500
        const val STAB_SOUND: Int = 2501
        const val PASSIVE_PROC_CHANCE_PERCENT: Int = 20
        const val PASSIVE_LIGHTNING_MAX_HIT_PERCENT: Int = 50
        const val LIGHTNING_DELAY: Int = 1
        const val TARGET_HIT_DELAY: Int = 1
        const val LIGHTNING_RADIUS: Int = 1
        const val SEARCH_ZONE_RADIUS: Int = 1
    }
}
