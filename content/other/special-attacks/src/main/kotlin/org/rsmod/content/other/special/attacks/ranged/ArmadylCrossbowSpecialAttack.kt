package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.DragonfireProtection
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.stat.rangedLvl
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

/**
 * Armadyl Eye doubles this shot's accuracy and doubles an equipped enchanted bolt's base proc
 * chance. The bolt effect itself is resolved through the normal ranged damage, ammo, projectile,
 * and impact paths rather than being replaced by a generic special hit.
 */
class ArmadylCrossbowSpecialAttack
@Inject
constructor(
    private val ammunition: RangedAmmoManager,
    private val random: GameRandom,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val armadylEye = ArmadylEye(manager, ammunition, random)
        registerRanged("obj.acb", armadylEye)
        registerRanged("obj.br_acb", armadylEye)
    }

    private class ArmadylEye(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val random: GameRandom,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = armadylEye(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = armadylEye(target, attack)

        private fun ProtectedAccess.armadylEye(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)
            if (!ammunition.attemptAmmoUsage(player, weaponType, quiverType)) {
                manager.stopCombat(this)
                return false
            }

            val projectileType = weaponType.paramOrNull(params.proj_type)
            if (quiverType == null || projectileType == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return false
            }

            anim("seq.xbows_human_fire_and_reload")
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }

            val activatedBolt =
                ArmadylEnchantedBolt
                    .from(quiverType)
                    ?.takeIf { it.rollsForArmadyl(random, target) }
                    ?.takeIf { it.isEffectiveAgainst(target) }
            val damage = rangedDamage(target, attack, activatedBolt)

            val projectile =
                manager.spawnProjectile(
                    this,
                    target,
                    ARMADYL_CROSSBOW_TRAVEL_SPOTANIM,
                    RSCM.getReverseMapping(RSCMType.PROJANIM, projectileType.id),
                )
            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = projectile.serverCycles,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = quiverType,
                damage = damage,
                clientDelay = projectile.clientCycles,
                hitDelay = projectile.serverCycles,
            )
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value for every bolt effect below - no impact callback needed.
            activatedBolt?.applyEffect(player, target, damage)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.rangedDamage(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            bolt: ArmadylEnchantedBolt?,
        ): Int {
            if (bolt == ArmadylEnchantedBolt.Ruby) {
                val cost = ArmadylCrossbowSpecialDamage.rubySelfDamage(player.hitpoints)
                if (cost > 0) {
                    // A real hit (not statSub) so a hitsplat actually shows on the shooter. The
                    // blood-sacrifice visual itself is played by applyEffect below, on the target.
                    takeInstantHit(type = HitType.Typeless, damage = cost)
                }
                return ArmadylCrossbowSpecialDamage.rubyDamage(target.currentHitpoints())
            }

            val bonusDamage = bolt?.bonusDamage(player, target) ?: 0
            val maxHitMultiplier = bolt?.maxHitMultiplier ?: 1.0
            return if (bolt?.bypassesAccuracy == true) {
                manager.rollRangedMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = maxHitMultiplier,
                    boltSpecDamage = bonusDamage,
                )
            } else {
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = ARMADYL_EYE_ACCURACY_MULTIPLIER,
                    maxHitMultiplier = maxHitMultiplier,
                    boltSpecDamage = bonusDamage,
                )
            }
        }
    }

    private companion object {
        const val ARMADYL_EYE_ACCURACY_MULTIPLIER: Double = 2.0
        const val ARMADYL_CROSSBOW_TRAVEL_SPOTANIM: String = "spotanim.acb_specialattack"
    }
}

/**
 * Cache-name resolver and real base proc data for enchanted crossbow bolts. Names are used here
 * deliberately because this cache contains both normal and dragon versions of every enchantment.
 */
internal enum class ArmadylEnchantedBolt(
    private val playerBaseChance: Double,
    private val npcBaseChance: Double?,
    val bypassesAccuracy: Boolean,
    /** Wiki's own name for the effect, and the activation spotanim it plays on the target. */
    val spotanim: String,
    val maxHitMultiplier: Double = 1.0,
) {
    // "Lucky Lightning".
    Opal(playerBaseChance = 0.05, npcBaseChance = 0.05, bypassesAccuracy = true, spotanim = "spotanim.xbows_lucky_lightening_strike_spot_anim"),
    // "Earth's Fury".
    Jade(playerBaseChance = 0.06, npcBaseChance = 0.06, bypassesAccuracy = true, spotanim = "spotanim.xbows_earths_fury_spot_anim"),
    // "Sea Curse".
    Pearl(playerBaseChance = 0.06, npcBaseChance = 0.06, bypassesAccuracy = true, spotanim = "spotanim.xbows_sea_curse_waterfall_spot_anim"),
    // "Down to Earth".
    Topaz(playerBaseChance = 0.04, npcBaseChance = null, bypassesAccuracy = true, spotanim = "spotanim.xbows_down_to_earth_spot_anim"),
    // "Clear Mind".
    Sapphire(playerBaseChance = 0.05, npcBaseChance = 0.25, bypassesAccuracy = true, spotanim = "spotanim.xbows_clear_mind_glowing_spot_anim"),
    // "Magical Poison".
    Emerald(playerBaseChance = 0.54, npcBaseChance = 0.55, bypassesAccuracy = false, spotanim = "spotanim.xbows_magical_poison_spot_anim"),
    // "Blood Forfeit".
    Ruby(playerBaseChance = 0.11, npcBaseChance = 0.06, bypassesAccuracy = true, spotanim = "spotanim.xbows_blood_sacrifice_spot_anim"),
    // "Armour Piercing".
    Diamond(
        playerBaseChance = 0.05,
        npcBaseChance = 0.10,
        bypassesAccuracy = true,
        spotanim = "spotanim.xbows_diamond_tips_spotanim",
        maxHitMultiplier = 1.15,
    ),
    // "Dragon's Breath".
    Dragonstone(playerBaseChance = 0.06, npcBaseChance = 0.06, bypassesAccuracy = false, spotanim = "spotanim.xbows_dragons_breath_spot_anim"),
    // "Life Leech".
    Onyx(
        playerBaseChance = 0.10,
        npcBaseChance = 0.11,
        bypassesAccuracy = false,
        spotanim = "spotanim.xbows_life_leach_spot_anim",
        maxHitMultiplier = 1.20,
    ),
    ;

    fun armadylActivationChance(againstPlayer: Boolean): Double =
        2.0 * if (againstPlayer) playerBaseChance else npcBaseChance.orEmpty()

    fun rollsForArmadyl(
        random: GameRandom,
        target: PathingEntity,
    ): Boolean = random.randomDouble() < armadylActivationChance(target is Player)

    fun isEffectiveAgainst(target: PathingEntity): Boolean =
        when (this) {
            Topaz, Sapphire -> target is Player
            Dragonstone ->
                target !is Npc ||
                    (
                        (target.visType.paramOrNull(params.draconic) ?: 0) == 0 &&
                            (target.visType.paramOrNull(params.elemental_weakness_type) ?: -1) !=
                                constants.elemental_weakness_water
                    )

            Onyx ->
                target !is Npc || (target.visType.paramOrNull(params.undead) ?: 0) == 0

            else -> true
        }

    fun bonusDamage(
        source: Player,
        target: PathingEntity,
    ): Int =
        when (this) {
            Opal -> ArmadylCrossbowSpecialDamage.opalBonus(source.rangedLvl)
            Pearl ->
                ArmadylCrossbowSpecialDamage.pearlBonus(
                    source.rangedLvl,
                    fieryTarget = target.isFiery(),
                )

            Dragonstone ->
                ArmadylCrossbowSpecialDamage.dragonstoneBonus(
                    source.rangedLvl,
                    target = target,
                )

            else -> 0
        }

    /**
     * Real OSRS doesn't clamp damage after the roll, so [damage] (the already-known, pre-clamp
     * value) is the authentic figure for every bolt effect below - no impact callback needed.
     */
    fun applyEffect(source: Player, target: PathingEntity, damage: Int) {
        if (damage > 0) {
            target.spotanim(spot = spotanim, height = 96)
        }
        when (this) {
            Jade -> {
                val player = target as? Player ?: return
                if (damage > 0) {
                    CombatEffects.freeze(player, JADE_FREEZE_TICKS)
                }
            }

            Topaz -> {
                val player = target as? Player ?: return
                if (damage > 0) {
                    player.statSub("stat.magic", constant = TOPAZ_MAGIC_DRAIN, percent = 0)
                }
            }

            Sapphire -> {
                val player = target as? Player ?: return
                if (damage <= 0) {
                    return
                }
                val requested = ArmadylCrossbowSpecialDamage.sapphirePrayerDrain(source.rangedLvl)
                val drained = min(requested, player.prayerLvl)
                if (drained <= 0) {
                    return
                }
                player.statSub("stat.prayer", constant = drained, percent = 0)
                source.statHeal(
                    "stat.prayer",
                    constant = ArmadylCrossbowSpecialDamage.sapphirePrayerRestore(drained),
                    percent = 0,
                )
            }

            Emerald -> {
                val player = target as? Player ?: return
                if (damage > 0) {
                    CombatEffects.poison(player, EMERALD_POISON_DAMAGE)
                }
            }

            Onyx -> {
                if (damage > 0 && source.hitpoints > 0) {
                    source.statHeal(
                        "stat.hitpoints",
                        constant = ArmadylCrossbowSpecialDamage.onyxHeal(damage),
                        percent = 0,
                    )
                }
            }

            else -> {}
        }
    }

    companion object {
        const val JADE_FREEZE_TICKS: Int = 8
        const val TOPAZ_MAGIC_DRAIN: Int = 1
        const val EMERALD_POISON_DAMAGE: Int = 5

        fun from(ammo: ItemServerType): ArmadylEnchantedBolt? = fromName(ammo.lowercaseName)

        fun fromName(name: String): ArmadylEnchantedBolt? {
            val lower = name.lowercase()
            if (!lower.contains("bolts (e)")) {
                return null
            }
            return when {
                lower.startsWith("opal ") -> Opal
                lower.startsWith("jade ") -> Jade
                lower.startsWith("pearl ") -> Pearl
                lower.startsWith("topaz ") -> Topaz
                lower.startsWith("sapphire ") -> Sapphire
                lower.startsWith("emerald ") -> Emerald
                lower.startsWith("ruby ") -> Ruby
                lower.startsWith("diamond ") -> Diamond
                lower.startsWith("dragonstone ") -> Dragonstone
                lower.startsWith("onyx ") -> Onyx
                else -> null
            }
        }

        private fun Double?.orEmpty(): Double = this ?: 0.0
    }
}

/** Small pure helpers used by Armadyl Eye's bolt effects and their focused tests. */
internal object ArmadylCrossbowSpecialDamage {
    fun rubyDamage(targetHitpoints: Int): Int = min(targetHitpoints / 5, RUBY_DAMAGE_CAP)

    fun rubySelfDamage(sourceHitpoints: Int): Int = sourceHitpoints / 10

    fun opalBonus(visibleRangedLevel: Int): Int = visibleRangedLevel / 10

    fun pearlBonus(
        visibleRangedLevel: Int,
        fieryTarget: Boolean,
    ): Int = visibleRangedLevel / if (fieryTarget) 15 else 20

    fun dragonstoneBonus(
        visibleRangedLevel: Int,
        target: PathingEntity,
    ): Int {
        val base = visibleRangedLevel / 5
        return if (target is Player) {
            DragonfireProtection.resolveMaxHit(
                player = target,
                type = DragonfireProtection.DragonfireType.Chromatic,
                baseMax = base,
            )
        } else {
            base
        }
    }

    fun sapphirePrayerDrain(visibleRangedLevel: Int): Int = visibleRangedLevel / 20

    fun sapphirePrayerRestore(drainedPrayer: Int): Int = drainedPrayer / 2

    fun onyxHeal(damage: Int): Int = damage / 4

    private const val RUBY_DAMAGE_CAP: Int = 100
}

private fun PathingEntity.currentHitpoints(): Int =
    when (this) {
        is Npc -> hitpoints
        is Player -> hitpoints
    }

private fun PathingEntity.isFiery(): Boolean =
    this is Npc &&
        (
            (visType.paramOrNull(params.draconic) ?: 0) != 0 ||
                (visType.paramOrNull(params.elemental_weakness_type) ?: -1) ==
                    constants.elemental_weakness_water
        )
