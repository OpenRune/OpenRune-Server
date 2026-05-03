package org.rsmod.content.skills.magic.spell.attacks.standard

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.SequenceServerType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.aconverted.SynthType
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull

class ElementalSpells : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        registerStrikes(manager)
        registerBolts(manager)
        registerBlasts(manager)
        registerWaves(manager)
        registerSurges(manager)
    }

    private fun SpellAttackRepository.registerStrikes(manager: SpellAttackManager) {
        fun getMaxHit(magicLvl: Int): Int =
            when {
                magicLvl >= 13 -> 8
                magicLvl >= 9 -> 6
                magicLvl >= 5 -> 4
                else -> 2
            }

        register(
            spell = "obj.01_wind_strike",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.windstrike_casting",
                    travel = "spotanim.windstrike_travel",
                    impact = "spotanim.windstrike_impact",
                    castSound = "synth.windstrike_cast_and_fire",
                    hitSound = "synth.windstrike_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.05_water_strike",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.waterstrike_casting",
                    travel = "spotanim.waterstrike_travel",
                    impact = "spotanim.waterstrike_impact",
                    castSound = "synth.waterstrike_cast_and_fire",
                    hitSound = "synth.waterstrike_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.09_earth_strike",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.earthstrike_casting",
                    travel = "spotanim.earthstrike_travel",
                    impact = "spotanim.earthstrike_impact",
                    castSound = "synth.earthstrike_cast_and_fire",
                    hitSound = "synth.earthstrike_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.13_fire_strike",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.firestrike_casting",
                    travel = "spotanim.firestrike_travel",
                    impact = "spotanim.firestrike_impact",
                    castSound = "synth.firestrike_cast_and_fire",
                    hitSound = "synth.firestrike_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )
    }

    private fun SpellAttackRepository.registerBolts(manager: SpellAttackManager) {
        fun getMaxHit(magicLvl: Int): Int =
            when {
                magicLvl >= 35 -> 12
                magicLvl >= 29 -> 11
                magicLvl >= 23 -> 10
                else -> 9
            }

        register(
            spell = "obj.17_wind_bolt",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.windbolt_casting",
                    travel = "spotanim.windbolt_travel",
                    impact = "spotanim.windbolt_impact",
                    castSound = "synth.windbolt_cast_and_fire",
                    hitSound = "synth.windbolt_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.23_water_bolt",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.waterbolt_casting",
                    travel = "spotanim.waterbolt_travel",
                    impact = "spotanim.waterbolt_impact",
                    castSound = "synth.waterbolt_cast_and_fire",
                    hitSound = "synth.waterbolt_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.29_earth_bolt",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.earthbolt_casting",
                    travel = "spotanim.earthbolt_travel",
                    impact = "spotanim.earthbolt_impact",
                    castSound = "synth.earthbolt_cast_and_fire",
                    hitSound = "synth.earthbolt_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.35_fire_bolt",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.firebolt_casting",
                    travel = "spotanim.firebolt_travel",
                    impact = "spotanim.firebolt_impact",
                    castSound = "synth.firebolt_cast_and_fire",
                    hitSound = "synth.firebolt_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )
    }

    private fun SpellAttackRepository.registerBlasts(manager: SpellAttackManager) {
        fun getMaxHit(magicLvl: Int): Int =
            when {
                magicLvl >= 59 -> 16
                magicLvl >= 53 -> 15
                magicLvl >= 47 -> 14
                else -> 13
            }

        register(
            spell = "obj.41_wind_blast",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.windblast_casting",
                    travel = "spotanim.windblast_travel",
                    impact = "spotanim.windblast_impact",
                    castSound = "synth.windblast_cast_and_fire",
                    hitSound = "synth.windblast_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.47_water_blast",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.waterblast_casting",
                    travel = "spotanim.waterblast_travel",
                    impact = "spotanim.waterblast_impact",
                    castSound = "synth.waterblast_cast_and_fire",
                    hitSound = "synth.waterblast_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.53_earth_blast",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.earthblast_casting",
                    travel = "spotanim.earthblast_travel",
                    impact = "spotanim.earthblast_impact",
                    castSound = "synth.earthblast_cast_and_fire",
                    hitSound = "synth.earthblast_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.59_fire_blast",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_caststrike_staff",
                    unarmedAnim = "seq.human_caststrike",
                    launch = "spotanim.fireblast_casting",
                    travel = "spotanim.fireblast_travel",
                    impact = "spotanim.fireblast_impact",
                    castSound = "synth.fireblast_cast_and_fire",
                    hitSound = "synth.fireblast_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )
    }

    private fun SpellAttackRepository.registerWaves(manager: SpellAttackManager) {
        fun getMaxHit(magicLvl: Int): Int =
            when {
                magicLvl >= 75 -> 20
                magicLvl >= 70 -> 19
                magicLvl >= 65 -> 18
                else -> 17
            }

        register(
            spell = "obj.62_wind_wave",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_castwave_staff",
                    unarmedAnim = "seq.human_castwave",
                    launch = "spotanim.windwave_casting",
                    travel = "spotanim.windwave_travel",
                    impact = "spotanim.windwave_impact",
                    castSound = "synth.windwave_cast_and_fire",
                    hitSound = "synth.windwave_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.65_water_wave",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_castwave_staff",
                    unarmedAnim = "seq.human_castwave",
                    launch = "spotanim.waterwave_casting",
                    travel = "spotanim.waterwave_travel",
                    impact = "spotanim.waterwave_impact",
                    castSound = "synth.waterwave_cast_and_fire",
                    hitSound = "synth.waterwave_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.70_earth_wave",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_castwave_staff",
                    unarmedAnim = "seq.human_castwave",
                    launch = "spotanim.earthwave_casting",
                    travel = "spotanim.earthwave_travel",
                    impact = "spotanim.earthwave_impact",
                    castSound = "synth.earthwave_cast_and_fire",
                    hitSound = "synth.earthwave_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.75_fire_wave",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_castwave_staff",
                    unarmedAnim = "seq.human_castwave",
                    launch = "spotanim.firewave_casting",
                    travel = "spotanim.firewave_travel",
                    impact = "spotanim.firewave_impact",
                    castSound = "synth.firewave_cast_and_fire",
                    hitSound = "synth.firewave_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )
    }

    private fun SpellAttackRepository.registerSurges(manager: SpellAttackManager) {
        fun getMaxHit(magicLvl: Int): Int =
            when {
                magicLvl >= 95 -> 24
                magicLvl >= 90 -> 23
                magicLvl >= 85 -> 22
                else -> 21
            }

        register(
            spell = "obj.81_wind_surge",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_cast_surge",
                    unarmedAnim = "seq.human_cast_surge",
                    launch = "spotanim.windsurge_casting",
                    travel = "spotanim.windsurge_travel",
                    impact = "spotanim.windsurge_impact",
                    castSound = "synth.windsurge_cast_and_fire",
                    hitSound = "synth.windsurge_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.85_water_surge",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_cast_surge",
                    unarmedAnim = "seq.human_cast_surge",
                    launch = "spotanim.watersurge_casting",
                    travel = "spotanim.watersurge_travel",
                    impact = "spotanim.watersurge_impact",
                    castSound = "synth.watersurge_cast_and_fire",
                    hitSound = "synth.watersurge_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.90_earth_surge",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_cast_surge",
                    unarmedAnim = "seq.human_cast_surge",
                    launch = "spotanim.earthsurge_casting",
                    travel = "spotanim.earthsurge_travel",
                    impact = "spotanim.earthsurge_impact",
                    castSound = "synth.earthsurge_cast_and_fire",
                    hitSound = "synth.earthsurge_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )

        register(
            spell = "obj.95_fire_surge",
            attack =
                ElementalSpellAttack(
                    manager = manager,
                    staffAnim = "seq.human_cast_surge",
                    unarmedAnim = "seq.human_cast_surge",
                    launch = "spotanim.firesurge_casting",
                    travel = "spotanim.firesurge_travel",
                    impact = "spotanim.firesurge_impact",
                    castSound = "synth.firesurge_cast_and_fire",
                    hitSound = "synth.firesurge_hit",
                    getMaxHit = ::getMaxHit,
                ),
        )
    }

    private class ElementalSpellAttack(
        private val manager: SpellAttackManager,
        private val staffAnim: String,
        private val unarmedAnim: String,
        private val launch: String,
        private val travel: String,
        private val impact: String,
        private val castSound: String,
        private val hitSound: String,
        private val getMaxHit: (Int) -> Int,
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
            val weaponType = getOrNull(attack.weapon)
            val castAnim = weaponType.castStrikeAnim()

            anim(castAnim)
            spotanim(launch, height = 92)

            val proj = manager.spawnProjectile(this, target, travel, "projanim.magic_spell")
            val (serverDelay, clientDelay) = proj.durations
            val spell = attack.spell.obj

            val splash = manager.rollSplash(this, target, attack, castResult)
            if (splash) {
                manager.playSplashFx(this, target, clientDelay, castSound, soundRadius = 8)
                manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
                manager.continueCombatIfAutocast(this, target)
                return
            }

            val baseMaxHit = getMaxHit(player.magicLvl)
            val damage = manager.rollMaxHit(this, target, attack, castResult, baseMaxHit)
            manager.playHitFx(
                source = this,
                target = target,
                clientDelay = clientDelay,
                castSound = castSound,
                soundRadius = 8,
                hitSpot = impact,
                hitSpotHeight = 124,
                hitSound = hitSound,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
            manager.continueCombatIfAutocast(this, target)
        }

        private fun ItemServerType?.castStrikeAnim(): String =
            if (this != null && isCategoryType("category.staff")) {
                staffAnim
            } else {
                unarmedAnim
            }
    }
}
