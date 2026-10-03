package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.lockOverheads
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit

class MeleeWeaponSpecialAttacks @Inject constructor(private val rng: GameRandom, private val specialDamage: MeleeSpecialDamage) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (spec in MeleeWeaponSpec.entries) {
            val handler = WeaponAttack(manager, spec)
            for (weapon in spec.weapons) registerMelee(weapon, handler)
        }
    }

    private inner class WeaponAttack(
        private val manager: SpecialAttackManager,
        private val spec: MeleeWeaponSpec,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean =
            perform(target, attack)

        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean =
            perform(target, attack)

        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim(spec.animation)
            spotanim(spec.spot, height = spec.effectHeight, slot = constants.spotanim_slot_combat)
            val firstAccurate = accuracy(target, attack)
            val hits = if (spec.effect == MeleeEffect.Dagger || spec.effect == MeleeEffect.AbyssalDagger) 2 else 1
            for (index in 0 until hits) {
                val accurate = when {
                    index == 0 -> firstAccurate
                    spec.effect == MeleeEffect.AbyssalDagger -> firstAccurate
                    else -> accuracy(target, attack)
                }
                val multiplier = if (spec.effect == MeleeEffect.Bludgeon) {
                    1.0 + (player.statBase("stat.prayer") - player.stat("stat.prayer")).coerceAtLeast(0) * 0.005
                } else spec.damageMultiplier
                var damage = if (!accurate) 0 else if (spec == MeleeWeaponSpec.ArmadylGodsword || spec == MeleeWeaponSpec.BandosGodsword) {
                    val extra = if (spec == MeleeWeaponSpec.ArmadylGodsword) 125 else 110
                    val maximum = specialDamage.maximum(player, target, attack, 110, extra).coerceAtLeast(1)
                    if (player.adminMaxHit) maximum else rng.of(1, maximum)
                } else if (spec.effect == MeleeEffect.Fang) {
                    val maximum = manager.calculateMeleeMaxHit(this, target, attack.type, attack.style, 1.0).coerceAtLeast(1)
                    if (player.adminMaxHit) maximum else rng.of((maximum * 15 / 100).coerceAtLeast(1), maximum)
                } else manager.rollMeleeMaxHit(this, target, attack.type, attack.style, multiplier).coerceAtLeast(1)
                if (spec.effect == MeleeEffect.GraniteHammer) damage += 5
                val delay = if (spec.effect == MeleeEffect.ElderMaul) 2 else 1
                val hit = if (spec.ignoresPrayer) manager.queueMeleeHitIgnoringPrayer(this, target, damage, delay)
                    else manager.queueMeleeHit(this, target, damage, delay)
                manager.giveCombatXp(this, target, attack, hit.damage)
                if (hit.damage > 0 || (accurate && spec.effect == MeleeEffect.AncientMace)) {
                    attachEffect(hit, player, target, spec.effect, rng, damage)
                }
            }
            if (spec == MeleeWeaponSpec.AnchorImbued && firstAccurate) manager.setNextAttackDelay(this, 4)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.accuracy(target: PathingEntity, attack: CombatAttack.Melee): Boolean =
            manager.rollMeleeAccuracy(this, target, attack.type, attack.style, spec.blockType, spec.accuracyMultiplier)
    }

    internal companion object {
        fun attachEffect(hit: Hit, source: Player, target: PathingEntity, effect: MeleeEffect, rng: GameRandom,
            rolledDamage: Int = hit.damage) {
            if (effect !in setOf(MeleeEffect.Warhammer, MeleeEffect.ElderMaul, MeleeEffect.Bandos,
                MeleeEffect.Saradomin, MeleeEffect.Zamorak, MeleeEffect.Whip, MeleeEffect.Anchor, MeleeEffect.Scimitar, MeleeEffect.AncientMace)) return
            val sourceUid = source.uid.packed
            val targetUid = when (target) {
                is Npc -> target.uid.packed
                is Player -> target.uid.packed
            }
            hit.impactEffects.add { actualDamage ->
                val valid = when (target) {
                    is Npc -> target.isSlotAssigned && target.uid.packed == targetUid
                    is Player -> target.isSlotAssigned && target.uid.packed == targetUid
                }
                if (valid && source.isSlotAssigned && source.uid.packed == sourceUid &&
                    (actualDamage > 0 || effect == MeleeEffect.Saradomin || (effect == MeleeEffect.AncientMace && target is Npc))) {
                    val roll = if (effect == MeleeEffect.Zamorak) rng.of(100) else 99
                    // Healing Blade retains its pre-overkill heal basis; drains use applied damage.
                    val damage = when {
                        effect == MeleeEffect.Saradomin -> hit.damage
                        effect == MeleeEffect.AncientMace && target is Npc -> rolledDamage
                        else -> actualDamage
                    }
                    applyEffect(source, target, damage, effect, roll)
                }
            }
        }
        fun applyEffect(source: Player, target: PathingEntity, damage: Int, effect: MeleeEffect, roll: Int = 99) {
            when (effect) {
                MeleeEffect.Warhammer -> drainDefence(target, 30)
                MeleeEffect.ElderMaul -> drainDefence(target, 35)
                MeleeEffect.Bandos -> drainStats(target, damage)
                MeleeEffect.Saradomin -> {
                    source.statHeal("stat.hitpoints", maxOf(10, damage / 2), 0)
                    source.statHeal("stat.prayer", maxOf(5, damage / 4), 0)
                }
                MeleeEffect.Zamorak -> when (target) {
                    is Npc -> if (roll >= (target.visType.paramOrNull(params.freeze_resistance) ?: 0)) CombatEffects.freeze(target, 32)
                    is Player -> CombatEffects.freeze(target, 32)
                }
                MeleeEffect.Whip -> if (target is Player) {
                    val drained = target.runEnergy / 10
                    target.runEnergy -= drained
                    source.runEnergy = (source.runEnergy + drained).coerceAtMost(constants.run_max_energy)
                }
                MeleeEffect.Anchor -> drainAnchor(target, damage / 10)
                MeleeEffect.Scimitar -> if (target is Player) target.lockOverheads(8)
                MeleeEffect.AncientMace -> {
                    source.statBoost("stat.prayer", damage, 0)
                    if (target is Player) target.statSub("stat.prayer", damage, 0)
                }
                else -> Unit
            }
        }

        private fun drainAnchor(target: PathingEntity, amount: Int) {
            // Sunder drains the first stat above one; overflow does not spill into another stat.
            when (target) {
                is Npc -> when {
                    target.defenceLvl > 1 -> target.defenceLvl = maxOf(1, target.defenceLvl - amount)
                    target.attackLvl > 1 -> target.attackLvl = maxOf(1, target.attackLvl - amount)
                    target.rangedLvl > 1 -> target.rangedLvl = maxOf(1, target.rangedLvl - amount)
                    target.magicLvl > 1 -> target.magicLvl = maxOf(1, target.magicLvl - amount)
                }
                is Player -> listOf("stat.defence", "stat.attack", "stat.ranged", "stat.magic")
                    .firstOrNull { target.stat(it) > 1 }?.let { target.statSub(it, minOf(amount, target.stat(it) - 1), 0) }
            }
        }

        private fun drainDefence(target: PathingEntity, percent: Int) {
            when (target) {
                is Npc -> target.defenceLvl -= MeleeSpecialRules.percentDrain(target.defenceLvl, percent)
                is Player -> target.statSub("stat.defence", MeleeSpecialRules.percentDrain(target.stat("stat.defence"), percent), 0)
            }
        }

        private fun drainStats(target: PathingEntity, damage: Int) {
            when (target) {
                is Npc -> {
                    val levels = listOf(target.defenceLvl, target.strengthLvl, target.attackLvl, target.magicLvl, target.rangedLvl)
                    val after = MeleeSpecialRules.drainInOrder(levels, damage)
                    target.defenceLvl = after[0]
                    target.strengthLvl = after[1]
                    target.attackLvl = after[2]
                    target.magicLvl = after[3]
                    target.rangedLvl = after[4]
                }
                is Player -> {
                    val stats = listOf("stat.defence", "stat.strength", "stat.prayer", "stat.attack", "stat.magic", "stat.ranged")
                    val levels = stats.map(target::stat)
                    val after = MeleeSpecialRules.drainInOrder(levels, damage)
                    stats.forEachIndexed { i, stat -> target.statSub(stat, levels[i] - after[i], 0) }
                }
            }
        }
    }
}

internal enum class MeleeEffect { None, Dagger, AbyssalDagger, Warhammer, ElderMaul, Bandos, Saradomin, Zamorak, Whip, Fang, Bludgeon, GraniteHammer, Anchor, Scimitar, AncientMace }

internal enum class MeleeWeaponSpec(
    val weapons: List<String>,
    val animation: String,
    val spot: String,
    val accuracyMultiplier: Double,
    val damageMultiplier: Double,
    val blockType: MeleeAttackType,
    val effect: MeleeEffect = MeleeEffect.None,
    val ignoresPrayer: Boolean = false,
    val effectHeight: Int = 96,
) {
    DragonSword(
        listOf("obj.dragon_shortsword", "obj.br_dragon_sword", "obj.bh_dragon_shortsword_corrupted"),
        "seq.human_dragon_sword_spec", "spotanim.dragon_sword_spec_spotanim", 1.25, 1.25, MeleeAttackType.Stab,
        ignoresPrayer = true, effectHeight = 0,
    ),
    AncientMace(
        listOf("obj.ancient_goblin_mace"),
        "seq.slice_player_mace_special_attack", "spotanim.slice_player_mace_special_attack_spotanim", 1.0, 1.0, MeleeAttackType.Crush,
        MeleeEffect.AncientMace, ignoresPrayer = true, effectHeight = 0,
    ),
    DragonScimitar(
        listOf("obj.dragon_scimitar", "obj.dragon_scimitar_ornament", "obj.br_dragon_scimitar", "obj.bh_dragon_scimitar_corrupted"),
        "seq.sp_attack_dragon_scimitar", "spotanim.sp_attack_dragon_scimitar_trail_spotanim", 1.25, 1.0, MeleeAttackType.Slash, MeleeEffect.Scimitar,
    ),
    DragonDagger(
        listOf("obj.dragon_dagger", "obj.dragon_dagger_p", "obj.dragon_dagger_p+", "obj.dragon_dagger_p++", "obj.br_dragon_dagger", "obj.bh_dragon_dagger_corrupted", "obj.bh_dragon_dagger_p_corrupted", "obj.bh_dragon_dagger_p+_corrupted", "obj.bh_dragon_dagger_p++_corrupted"),
        "seq.puncture", "spotanim.sp_attack_puncture_spotanim", 1.15, 1.15, MeleeAttackType.Slash, MeleeEffect.Dagger,
    ),
    AbyssalDagger(
        listOf("obj.abyssal_dagger", "obj.abyssal_dagger_p", "obj.abyssal_dagger_p+", "obj.abyssal_dagger_p++"),
        "seq.abyssal_dagger_special", "spotanim.abyssal_dagger_special_spotanim", 1.25, 0.85, MeleeAttackType.Stab, MeleeEffect.AbyssalDagger,
    ),
    AbyssalDaggerImbued(
        listOf("obj.bh_abyssal_dagger_imbue", "obj.bh_abyssal_dagger_p_imbue", "obj.bh_abyssal_dagger_p+_imbue", "obj.bh_abyssal_dagger_p++_imbue"),
        "seq.abyssal_dagger_special", "spotanim.abyssal_dagger_special_spotanim", 1.25, 0.95, MeleeAttackType.Stab, MeleeEffect.AbyssalDagger,
    ),
    DragonMace(
        listOf("obj.dragon_mace", "obj.bh_dragon_mace_corrupted"),
        "seq.shatter", "spotanim.sp_attack_shatter_spotanim", 1.25, 1.5, MeleeAttackType.Crush,
    ),
    DragonWarhammer(
        listOf("obj.dragon_warhammer", "obj.dragon_warhammer_ornament", "obj.br_dragon_warhammer", "obj.bh_dragon_warhammer_corrupted"),
        "seq.dragon_warhammer_sa_player", "spotanim.dragon_warhammer_sa_spotanim", 1.0, 1.5, MeleeAttackType.Crush, MeleeEffect.Warhammer,
    ),
    ElderMaul(
        listOf("obj.elder_maul", "obj.elder_maul_ornament", "obj.br_elder_maul"),
        "seq.human_elder_maul_spec", "spotanim.spotanim_elder_maul_special", 1.25, 1.0, MeleeAttackType.Crush, MeleeEffect.ElderMaul,
    ),
    ArmadylGodsword(
        listOf("obj.ags", "obj.agsg", "obj.br_ags", "obj.deadman_ags", "obj.deadman_blighted_ags"),
        "seq.ags_special_player", "spotanim.godwars_godsword_armadyl_spot", 2.0, 1.375, MeleeAttackType.Slash,
    ),
    BandosGodsword(
        listOf("obj.bgs", "obj.bgsg"),
        "seq.bgs_special_player", "spotanim.godwars_godsword_bandos_spot", 2.0, 1.21, MeleeAttackType.Slash, MeleeEffect.Bandos,
    ),
    SaradominGodsword(
        listOf("obj.sgs", "obj.sgsg"),
        "seq.sgs_special_player", "spotanim.godwars_godsword_saradomin_spot", 2.0, 1.1, MeleeAttackType.Slash, MeleeEffect.Saradomin,
    ),
    ZamorakGodsword(
        listOf("obj.zgs", "obj.zgsg"),
        "seq.zgs_special_player", "spotanim.godwars_godsword_zamorak_spot", 2.0, 1.1, MeleeAttackType.Slash, MeleeEffect.Zamorak,
    ),
    AbyssalWhip(
        listOf("obj.abyssal_whip", "obj.abyssal_whip_lava", "obj.abyssal_whip_ice", "obj.league_3_whip", "obj.br_abyssal_whip"),
        "seq.slayer_whip_sp_attack", "spotanim.sp_attack_abyssal_whip", 1.25, 1.0, MeleeAttackType.Slash, MeleeEffect.Whip,
    ),
    Fang(
        listOf("obj.osmumtens_fang", "obj.osmumtens_fang_ornament"),
        "seq.weapon_sword_osmumten03_special", "spotanim.spotanim_weapon_sword_osmumten_special", 1.5, 1.0, MeleeAttackType.Stab, MeleeEffect.Fang,
    ),
    Bludgeon(
        listOf("obj.abyssal_bludgeon"),
        "seq.abyssal_bludgeon_special_attack", "spotanim.abyssal_miasma_spotanim_bludgeon", 1.0, 1.0, MeleeAttackType.Crush, MeleeEffect.Bludgeon,
    ),
    GraniteHammer(
        listOf("obj.granite_hammer"),
        "seq.dragon_warhammer_sa_player", "spotanim.granite_hammer_sa_spotanim", 1.5, 1.0, MeleeAttackType.Crush, MeleeEffect.GraniteHammer,
    ),
    Anchor(
        listOf("obj.brain_anchor"),
        "seq.brain_player_anchor_special_attack", "spotanim.brain_anchor_special_attack_spot", 2.0, 1.1, MeleeAttackType.Crush, MeleeEffect.Anchor,
    ),
    AnchorImbued(
        listOf("obj.bh_brain_anchor_imbue"),
        "seq.brain_player_anchor_special_attack", "spotanim.brain_anchor_special_attack_spot", 2.0, 1.25, MeleeAttackType.Crush, MeleeEffect.Anchor,
    ),
}
