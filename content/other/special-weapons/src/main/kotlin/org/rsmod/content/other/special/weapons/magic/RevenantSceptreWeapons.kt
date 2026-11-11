package org.rsmod.content.other.special.weapons.magic

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.weapons.MagicWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class RevenantSceptre(
    val name: String,
    val uncharged: String,
    val charged: String,
    val unchargedAutocast: String,
    val chargedAutocast: String,
    val maxHit: (Int) -> Int,
) {
    val all: List<String>
        get() = listOf(uncharged, charged, unchargedAutocast, chargedAutocast)

    val chargedForms: List<String>
        get() = listOf(charged, chargedAutocast)

    val unchargedForms: List<String>
        get() = listOf(uncharged, unchargedAutocast)

    fun swapTarget(obj: String): String =
        when (obj) {
            uncharged -> unchargedAutocast
            unchargedAutocast -> uncharged
            charged -> chargedAutocast
            chargedAutocast -> charged
            else -> error("$obj does not belong to $name")
        }

    fun chargedFor(obj: String): String = if (obj == unchargedAutocast) chargedAutocast else charged

    fun unchargedFor(obj: String): String = if (obj == chargedAutocast) unchargedAutocast else uncharged

    companion object {
        const val ETHER_VAROBJ = PoweredStaffTypes.CHARGES_15_BIT
        const val ETHER = "obj.wild_cave_shard"
        const val ACTIVATION_ETHER = 1_000
        const val MAX_ETHER = 17_000

        val THAMMARON =
            RevenantSceptre(
                name = "Thammaron's sceptre",
                uncharged = "obj.wild_cave_sceptre_uncharged",
                charged = "obj.wild_cave_sceptre_charged",
                unchargedAutocast = "obj.wild_cave_sceptre_uncharged_recol",
                chargedAutocast = "obj.wild_cave_sceptre_charged_recol",
                maxHit = { it / 3 - 8 },
            )

        val ACCURSED =
            RevenantSceptre(
                name = "accursed sceptre",
                uncharged = "obj.wild_cave_accursed_uncharged",
                charged = "obj.wild_cave_accursed_charged",
                unchargedAutocast = "obj.wild_cave_accursed_uncharged_recol",
                chargedAutocast = "obj.wild_cave_accursed_charged_recol",
                maxHit = { it / 3 - 6 },
            )

        val ALL = listOf(THAMMARON, ACCURSED)
    }
}

class RevenantSceptreWeapons
@Inject
constructor(private val objCharges: ObjChargeManager) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for (sceptre in RevenantSceptre.ALL) {
            register(sceptre.charged, Sceptre(manager, objCharges, sceptre))
            register(sceptre.uncharged, UnchargedSceptre(manager))
        }
    }

    private class Sceptre(
        private val manager: WeaponAttackManager,
        private val objCharges: ObjChargeManager,
        private val sceptre: RevenantSceptre,
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
            val ether = objCharges.getCharges(player.worn[Wearpos.RightHand.slot], ETHER_VAROBJ)
            if (ether <= ACTIVATION_ETHER) {
                mes("There is not enough revenant ether left powering your sceptre.")
                manager.stopCombat(this)
                return
            }
            objCharges.setValue(player.worn, Wearpos.RightHand.slot, ETHER_VAROBJ, ether - 1)

            anim("seq.human_castwave_staff_walkmerge")
            val proj =
                manager.spawnProjectile(
                    this,
                    target,
                    "spotanim.spells_thammaron01_travel01",
                    "projanim.powered_staff",
                )
            val (serverDelay, clientDelay) = proj.durations

            if (manager.rollStaffSplash(this, target, attack)) {
                manager.playSplashFx(this, target, clientDelay, CAST_SOUND, SOUND_RADIUS)
                manager.queueSplashHit(this, target, clientDelay, serverDelay)
            } else {
                val baseMaxHit = maxOf(0, sceptre.maxHit(player.magicLvl))
                val damage = manager.rollStaffMaxHit(this, target, baseMaxHit)
                manager.playMagicHitFx(
                    source = this,
                    target = target,
                    clientDelay = clientDelay,
                    castSound = CAST_SOUND,
                    soundRadius = SOUND_RADIUS,
                    hitSpot = null,
                    hitSpotHeight = 0,
                    hitSound = null,
                )
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, damage, clientDelay, serverDelay)
            }
            manager.continueCombat(this, target)
        }
    }

    private class UnchargedSceptre(private val manager: WeaponAttackManager) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            terminateAttack()
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            terminateAttack()
            return true
        }

        private fun ProtectedAccess.terminateAttack() {
            mes("Your sceptre has no revenant ether powering it. You need to charge it first.")
            manager.stopCombat(this)
        }
    }

    private companion object {
        const val ETHER_VAROBJ = RevenantSceptre.ETHER_VAROBJ
        const val ACTIVATION_ETHER = RevenantSceptre.ACTIVATION_ETHER
        const val CAST_SOUND = "synth.shadow_cast"
        const val SOUND_RADIUS = 10
    }
}
