package org.rsmod.content.other.moonsets

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.MoonSet
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.specials.SpecialAttack
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.specials.SpecialAttackType
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.spells.attack.SpellCastHook
import org.rsmod.game.entity.PathingEntity
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Frostweaver
@Inject
constructor(
    private val manager: PlayerAttackManager,
    private val random: GameRandom,
    private val specials: SpecialAttackRegistry,
    private val energy: SpecialAttackEnergy,
) : SpellCastHook {
    override fun onCast(access: ProtectedAccess, attack: CombatAttack.Spell) {
        val player = access.player
        val chance = chanceFor(RSCM.getReverseMapping(RSCMType.OBJ, attack.spell.obj.id))
        val triggered =
            chance > 0 &&
                player.vars[TOGGLE_VARBIT] == 1 &&
                MoonSet.Blue.wearsSetEffect(player) &&
                random.of(100) < chance
        VarPlayerIntMapSetter.set(player, PENDING_VARP, if (triggered) 1 else 0)
    }

    override suspend fun afterSpell(
        access: ProtectedAccess,
        target: PathingEntity,
        attack: CombatAttack.Spell,
    ) {
        val player = access.player
        if (player.vars[PENDING_VARP] == 0) return
        VarPlayerIntMapSetter.set(player, PENDING_VARP, 0)

        val stillValid = target.validityCheck()
        access.delay(1)
        if (!stillValid()) return

        // UNVERIFIED: the wiki does not say what Frostweaver does outside melee range; it is skipped.
        if (!player.isWithinDistance(target, MELEE_REACH)) return

        access.spotanim(
            "spotanim.special_spear_spotanim",
            slot = constants.spotanim_slot_combat,
        )

        // Jab animation, but the damage trains Strength.
        val jab =
            CombatAttack.Melee(
                attack.weapon,
                MeleeAttackType.Stab,
                MeleeAttackStyle.Accurate,
                CombatStance.Stance1,
            )

        // A special attack left armed while casting is spent on the first Frostweaver swing.
        val special = attack.weapon?.let { specials[it] }
        val armed = player.vars[SPECIAL_VARP] == SpecialAttackType.Weapon.varValue
        if (armed && special is SpecialAttack.Melee) {
            val required = special.energyInHundreds
            if (energy.hasSpecialEnergy(player, required)) {
                VarPlayerIntMapSetter.set(player, SPECIAL_VARP, SpecialAttackType.None.varValue)
                if (special.attack(access, target, jab)) {
                    energy.takeSpecialEnergy(player, required)
                }
                return
            }
        }

        val training = jab.copy(style = MeleeAttackStyle.Aggressive)
        val damage = manager.rollMeleeDamage(player, target, jab, 1.0, 1.0)
        manager.giveCombatXp(player, target, training, damage)
        manager.playWeaponFx(player, jab)
        manager.queueMeleeHit(player, target, damage, delay = 1)
    }

    private fun chanceFor(spell: String?): Int =
        when (spell) {
            in BIND_SPELLS, in ICE_SPELLS -> 20
            in GRASP_SPELLS -> 50
            else -> 0
        }

    private companion object {
        const val MELEE_REACH = 1
        const val PENDING_VARP = "varp.frostweaver_pending"
        const val SPECIAL_VARP = "varp.sa_attack"
        const val TOGGLE_VARBIT = "varbit.pmoon_blue_moon_effect_toggle"
        val BIND_SPELLS = setOf("obj.20_bind", "obj.50_snare", "obj.79_entangle")
        val ICE_SPELLS =
            setOf("obj.58_ice_rush", "obj.70_ice_burst", "obj.82_ice_blitz", "obj.94_ice_barrage")
        val GRASP_SPELLS = setOf("obj.dmm_ref_hat", "obj.dmm_ref_torso", "obj.dmm_ref_legs")
    }
}

class SetEffectToggleScript : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton("component.combat_interface:set_effect") {
            val enabled = player.vars[TOGGLE_VARBIT] == 1
            VarPlayerIntMapSetter.set(player, TOGGLE_VARBIT, if (enabled) 0 else 1)
        }
    }

    private companion object {
        const val TOGGLE_VARBIT = "varbit.pmoon_blue_moon_effect_toggle"
    }
}
