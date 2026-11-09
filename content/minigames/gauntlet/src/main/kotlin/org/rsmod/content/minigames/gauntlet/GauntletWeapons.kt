package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.weapons.MagicWeapon
import org.rsmod.api.weapons.RangedWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class GauntletWeaponsModule : PluginModule() {
    override fun bind() {
        addSetBinding<WeaponMap>(GauntletWeapons::class.java)
    }
}

class GauntletWeapons @Inject constructor() : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for (corrupted in listOf(false, true)) {
            for (tier in 1..3) {
                register(gauntletObj("ranged_t$tier", corrupted), GauntletBow(manager))
                register(
                    gauntletObj("magic_t$tier", corrupted),
                    GauntletStaff(manager, corrupted, STAFF_MAX_HITS[tier - 1]),
                )
            }
        }
    }

    private class GauntletBow(private val manager: WeaponAttackManager) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack)
            return true
        }

        private fun ProtectedAccess.shoot(target: PathingEntity, attack: CombatAttack.Ranged) {
            manager.playWeaponFx(this, attack)
            val proj = manager.spawnProjectile(this, target, BOW_TRAVEL, "projanim.arrow")
            val (serverDelay, clientDelay) = proj.durations
            val damage = manager.rollRangedDamage(this, target, attack)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(this, target, null, damage, clientDelay, serverDelay)
            manager.continueCombat(this, target)
        }
    }

    private class GauntletStaff(
        private val manager: WeaponAttackManager,
        private val corrupted: Boolean,
        private val baseMaxHit: Int,
    ) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            cast(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            cast(target, attack)
            return true
        }

        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Staff) {
            val suffix = if (corrupted) "_hm" else ""
            anim("seq.human_castwave_staff")
            spotanim("spotanim.crystal_staff_casting$suffix", height = CAST_HEIGHT)
            val proj =
                manager.spawnProjectile(
                    this,
                    target,
                    "spotanim.crystal_staff_projectile$suffix",
                    "projanim.crystal_staff",
                )
            val (serverDelay, clientDelay) = proj.durations

            if (manager.rollStaffSplash(this, target, attack)) {
                manager.playSplashFx(this, target, clientDelay, CAST_SOUND, soundRadius = 10)
                manager.queueSplashHit(this, target, clientDelay, serverDelay)
            } else {
                val damage = manager.rollStaffMaxHit(this, target, baseMaxHit)
                manager.playMagicHitFx(
                    source = this,
                    target = target,
                    clientDelay = clientDelay,
                    castSound = CAST_SOUND,
                    soundRadius = 10,
                    hitSpot = "spotanim.crystal_staff_impact$suffix",
                    hitSpotHeight = IMPACT_HEIGHT,
                    hitSound = HIT_SOUND,
                )
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, damage, clientDelay, serverDelay)
            }
            manager.continueCombat(this, target)
        }
    }

    private companion object {
        val STAFF_MAX_HITS = listOf(23, 31, 39)
        const val CAST_HEIGHT = 92
        const val IMPACT_HEIGHT = 60
        const val CAST_SOUND = "synth.shadow_cast"
        const val HIT_SOUND = "synth.contact_darkness_impact"
        const val BOW_TRAVEL = "spotanim.sp_attack_glow_arrow_travel"
    }
}
