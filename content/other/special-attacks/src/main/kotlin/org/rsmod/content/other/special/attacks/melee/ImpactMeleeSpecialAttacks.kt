package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.combat.commons.BindEffectService
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Real OSRS doesn't clamp damage after the roll, so every effect below uses the already-known,
 * pre-mitigation damage value computed a few lines earlier instead of waiting on an impact
 * callback (`HitImpactHandler`, which isn't a real type anywhere in this engine - same fake-type
 * bug as elsewhere in this project). This matters beyond convenience for the Saradomin godsword:
 * the wiki is explicit its heal/prayer-restore is "calculated from the potential damage of a swing
 * *before* some types of damage immunities are applied" and "before flat armour is applied" - the
 * pre-mitigation roll is the *correct* value to use, not an engine-diff workaround.
 */
class ImpactMeleeSpecialAttacks
@Inject
constructor(private val binds: BindEffectService) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val bandos = BandosGodsword(manager, "seq.bgs_special_player", BANDOS_STANDARD_SPOTANIM)
        registerMelee("obj.bgs", bandos)
        registerMelee(
            "obj.bgsg",
            BandosGodsword(manager, "seq.bgs_special_ornate_player", BANDOS_ORNATE_SPOTANIM),
        )

        val saradomin =
            SaradominGodsword(manager, "seq.sgs_special_player", SARADOMIN_STANDARD_SPOTANIM)
        registerMelee("obj.sgs", saradomin)
        registerMelee(
            "obj.sgsg",
            SaradominGodsword(manager, "seq.sgs_special_ornate_player", SARADOMIN_ORNATE_SPOTANIM),
        )

        val zamorak =
            ZamorakGodsword(manager, binds, "seq.zgs_special_player", ZAMORAK_STANDARD_SPOTANIM)
        registerMelee("obj.zgs", zamorak)
        registerMelee(
            "obj.zgsg",
            ZamorakGodsword(
                manager = manager,
                binds = binds,
                sequence = "seq.zgs_special_ornate_player",
                graphic = ZAMORAK_ORNATE_SPOTANIM,
            ),
        )

        val warhammer = DragonWarhammer(manager)
        registerMelee("obj.dragon_warhammer", warhammer)
        registerMelee("obj.br_dragon_warhammer", warhammer)
        registerMelee("obj.dragon_warhammer_ornament", warhammer)
        registerMelee("obj.bh_dragon_warhammer_corrupted", warhammer)
    }

    private class BandosGodsword(
        private val manager: SpecialAttackManager,
        private val sequence: String,
        private val graphic: String,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            bandosStrike(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            bandosStrike(target, attack)
            return true
        }

        private fun ProtectedAccess.bandosStrike(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim(sequence)
            spotanim(
                spot = graphic,
                slot = constants.spotanim_slot_combat,
                // 96 (blindly copied from Dragon claws) still sat too high even at 48 per live
                // feedback - dropped to ground level. Visual-tuning guess, not verified against a
                // real screenshot; needs your eyes to confirm.
                height = 0,
            )
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 2.0,
                    maxHitMultiplier = 1.21,
                    blockType = MeleeAttackType.Slash,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(source = this, target = target, damage = damage)
            if (damage > 0) {
                drainBandosStats(target, damage)
            }
            manager.continueCombat(this, target)
        }
    }

    private class SaradominGodsword(
        private val manager: SpecialAttackManager,
        private val sequence: String,
        private val graphic: String,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            healingBlade(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            healingBlade(target, attack)
            return true
        }

        private fun ProtectedAccess.healingBlade(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim(sequence)
            spotanim(
                spot = graphic,
                slot = constants.spotanim_slot_combat,
                // 96 (blindly copied from Dragon claws) still sat too high even at 48 per live
                // feedback - dropped to ground level. Visual-tuning guess, not verified against a
                // real screenshot; needs your eyes to confirm.
                height = 0,
            )
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 2.0,
                    maxHitMultiplier = 1.1,
                    blockType = MeleeAttackType.Slash,
                )
            val source = player
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(source = this, target = target, damage = damage)
            // Wiki: restoration is calculated from the pre-immunity, pre-armour roll above, not the
            // final post-mitigation hit - `damage` (not a post-modifier `impact.damage`) is exactly
            // that value, so a target absorbing the hit down to 0 still restores normally.
            if (damage > 0) {
                source.statHeal("stat.hitpoints", constant = max(10, (damage + 1) / 2), percent = 0)
                source.statHeal("stat.prayer", constant = max(5, (damage + 3) / 4), percent = 0)
            }
            manager.continueCombat(this, target)
        }
    }

    private class ZamorakGodsword(
        private val manager: SpecialAttackManager,
        private val binds: BindEffectService,
        private val sequence: String,
        private val graphic: String,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            iceCleave(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            iceCleave(target, attack)
            return true
        }

        private fun ProtectedAccess.iceCleave(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim(sequence)
            spotanim(
                spot = graphic,
                slot = constants.spotanim_slot_combat,
                // 96 (blindly copied from Dragon claws) still sat too high even at 48 per live
                // feedback - dropped to ground level. Visual-tuning guess, not verified against a
                // real screenshot; needs your eyes to confirm.
                height = 0,
            )
            val accurate =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Slash,
                    multiplier = 2.0,
                )
            val damage =
                if (accurate) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = ZGS_MAX_HIT_MULTIPLIER,
                    )
                } else {
                    0
                }
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(source = this, target = target, damage = damage)
            // Wiki: "The sword's special attack must produce a successful hit in order to have the
            // freezing effect... Protect from Melee... does not affect the freeze" - gated on the
            // accuracy roll itself, not the post-mitigation damage.
            if (accurate) {
                // Wiki: "Freezes opponent... with a similar animation to Ice Barrage."
                target.spotanim("spotanim.ice_barrage_impact")
                when (target) {
                    is Player -> CombatEffects.freeze(target, ZGS_FREEZE_TICKS)
                    is Npc -> binds.bind(target, ZGS_FREEZE_TICKS)
                }
            }
            manager.continueCombat(this, target)
        }
    }

    private class DragonWarhammer(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            smash(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            smash(target, attack)
            return true
        }

        private fun ProtectedAccess.smash(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.dragon_warhammer_sa_player")
            spotanim(
                spot = "spotanim.dragon_warhammer_sa_spotanim",
                slot = constants.spotanim_slot_combat,
                // 96 (blindly copied from Dragon claws) still sat too high even at 48 per live
                // feedback - dropped to ground level. Visual-tuning guess, not verified against a
                // real screenshot; needs your eyes to confirm.
                height = 0,
            )
            val accurate =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Crush,
                    multiplier = 1.0,
                )
            val damage =
                if (accurate) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = DWH_MAX_HIT_MULTIPLIER,
                    )
                } else {
                    0
                }
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(source = this, target = target, damage = damage)
            // Wiki: "unlike other weapons such as the Bandos godsword... the dragon warhammer only
            // needs to roll a successful hit to reduce 30%" - gated on accuracy, not damage.
            if (accurate) {
                reduceDefence(target)
            }
            manager.continueCombat(this, target)
        }
    }

    internal companion object {
        // Wiki: "it freezes the target in place for 19.2 seconds (32 ticks)".
        const val ZGS_FREEZE_TICKS = 32
        const val ZGS_MAX_HIT_MULTIPLIER: Double = 1.1
        const val DWH_MAX_HIT_MULTIPLIER: Double = 1.5
        const val BANDOS_STANDARD_SPOTANIM = "spotanim.dh_sword_update_bandos_special_spotanim"
        const val BANDOS_ORNATE_SPOTANIM = "spotanim.bandos_special_spotanim_gold"
        const val SARADOMIN_STANDARD_SPOTANIM = "spotanim.dh_sword_update_saradomin_special_spotanim"
        const val SARADOMIN_ORNATE_SPOTANIM = "spotanim.saradomin_special_spotanim_gold"
        const val ZAMORAK_STANDARD_SPOTANIM = "spotanim.dh_sword_update_zamorak_special_spotanim"
        const val ZAMORAK_ORNATE_SPOTANIM = "spotanim.zamorak_special_spotanim_gold"

        val BANDOS_PLAYER_STAT_ORDER =
            listOf(
                "stat.defence",
                "stat.strength",
                "stat.prayer",
                "stat.attack",
                "stat.magic",
                "stat.ranged",
            )
    }
}

private fun drainBandosStats(target: PathingEntity, amount: Int) {
    when (target) {
        is Player -> {
            val currentStats = ImpactMeleeSpecialAttacks.BANDOS_PLAYER_STAT_ORDER.map { target.stat(it) }
            val drains = BandosStatDrain.distribute(amount, currentStats)
            for (i in drains.indices) {
                if (drains[i] > 0) {
                    target.statSub(
                        ImpactMeleeSpecialAttacks.BANDOS_PLAYER_STAT_ORDER[i],
                        constant = drains[i],
                        percent = 0,
                    )
                }
            }
        }
        is Npc -> {
            val currentStats =
                listOf(target.defenceLvl, target.strengthLvl, target.attackLvl, target.magicLvl, target.rangedLvl)
            val drains = BandosStatDrain.distribute(amount, currentStats)
            target.defenceLvl -= drains[0]
            target.strengthLvl -= drains[1]
            target.attackLvl -= drains[2]
            target.magicLvl -= drains[3]
            target.rangedLvl -= drains[4]
        }
    }
}

private fun reduceDefence(target: PathingEntity) {
    when (target) {
        is Player -> {
            val drain = DragonWarhammerDefenceReduction.playerDrain(target.stat("stat.defence"))
            if (drain > 0) {
                target.statSub("stat.defence", constant = drain, percent = 0)
            }
        }
        is Npc -> target.defenceLvl = DragonWarhammerDefenceReduction.npcRemaining(target.defenceLvl)
    }
}

/**
 * Warstrike's stat drain: [amount] is spent down the wiki's documented order (Defence, Strength,
 * Prayer, Attack, Magic, Ranged for players; NPCs have no Prayer, so it's skipped), moving to the
 * next stat only once the current one hits 0. Pure - takes each stat's *current* value in that
 * order and returns how much to drain from each, leaving the actual mutation to the caller.
 */
internal object BandosStatDrain {
    fun distribute(amount: Int, currentStats: List<Int>): List<Int> {
        var remaining = amount
        val drains = MutableList(currentStats.size) { 0 }
        for (i in currentStats.indices) {
            val drain = min(currentStats[i], remaining)
            drains[i] = drain
            remaining -= drain
            if (remaining == 0) {
                break
            }
        }
        return drains
    }
}

/**
 * Smash's defence reduction: floor(30% of current Defence). Wiki's own worked example on a
 * 75-Defence monster: 75 -> 53 -> 38 across two successful hits. That's `current - floor(current *
 * 0.3)` each time, *not* `floor(current * 0.7)` - the two differ whenever `current * 30` isn't a
 * clean multiple of 100 (75 -> floor(75*0.7)=52, but the wiki's real sequence is 53), since one
 * floors the drained amount and the other floors the remainder directly.
 */
internal object DragonWarhammerDefenceReduction {
    fun playerDrain(currentDefence: Int): Int = currentDefence * 30 / 100

    fun npcRemaining(currentDefence: Int): Int = currentDefence - playerDrain(currentDefence)
}
