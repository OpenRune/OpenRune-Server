package org.rsmod.content.other.special.attacks.melee

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.api.player.righthand
import org.rsmod.api.specials.combat.MagicSpecialAttack
import org.rsmod.game.inv.InvObj
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/**
 * Retainer traps a vampyre juvenile or juvinate below half health, preventing retaliation for
 * thirty seconds. It deals no damage.
 */
class VampyreFlailSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val retainer = Retainer(manager)
        val rodRetainer = RodOfIvandisRetainer(manager)
        registerMelee("obj.ivandis_flail", retainer)
        registerMelee("obj.blisterwood_flail", retainer)
        registerMelee("obj.hallowed_flail", retainer)
        (1..10).forEach { charges ->
            registerMagic("obj.burgh_rod_command_final_$charges", rodRetainer)
        }
    }

    private class Retainer(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            val heldType = target.retainerHeldType()
            if (heldType == null) {
                mes(NON_VAMPYRE_MESSAGE)
                manager.continueCombat(this, target)
                return false
            }
            if (target.hitpoints * 2 > target.baseHitpointsLvl) {
                mes(HEALTH_MESSAGE)
                manager.continueCombat(this, target)
                return false
            }

            playRetainerVisuals(manager, target, heldType, RETAINER_DURATION_CYCLES)
            target.clearQueue(RETALIATION_QUEUE)
            target.clearInteraction()
            manager.continueCombat(this, target)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            mes(NON_VAMPYRE_MESSAGE)
            manager.continueCombat(this, target)
            return false
        }

        private companion object {
            const val RETAINER_DURATION_CYCLES: Int = 50
            const val RETALIATION_QUEUE: String = "queue.com_retaliate_player"
            const val NON_VAMPYRE_MESSAGE: String =
                "This spell can only be used on vampyre juveniles or vampyre juvinates."
            const val HEALTH_MESSAGE: String =
                "This spell only works against vampyres with less than half of their health remaining."
        }
    }

    /**
     * Staff-form Retainer consumes one Rod of Ivandis charge only after a valid vampyre target
     * is trapped. The cache stores the ten charge states as separate equipped items.
     */
    private class RodOfIvandisRetainer(private val manager: SpecialAttackManager) : MagicSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean = retainer(target)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            mes(NON_VAMPYRE_MESSAGE)
            manager.continueCombat(this, target)
            return false
        }

        private suspend fun ProtectedAccess.retainer(target: Npc): Boolean {
            val heldType = target.retainerHeldType()
            if (heldType == null) {
                mes(NON_VAMPYRE_MESSAGE)
                manager.continueCombat(this, target)
                return false
            }
            if (target.hitpoints * 2 > target.baseHitpointsLvl) {
                mes(HEALTH_MESSAGE)
                manager.continueCombat(this, target)
                return false
            }

            playRetainerVisuals(manager, target, heldType, RETAINER_DURATION_CYCLES)
            target.clearQueue(RETALIATION_QUEUE)
            target.clearInteraction()
            consumeRodCharge()
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.consumeRodCharge() {
            val rod = player.righthand ?: return
            val nextId =
                when (rod.id) {
                    in FIRST_CHARGED_ROD_ID until LAST_CHARGED_ROD_ID -> rod.id + 1
                    LAST_CHARGED_ROD_ID -> ROD_DUST_ID
                    else -> return
                }
            player.righthand = InvObj(ItemServerType(nextId), rod.count, rod.vars)
        }

        private companion object {
            const val FIRST_CHARGED_ROD_ID: Int = 7639
            const val LAST_CHARGED_ROD_ID: Int = 7648
            const val ROD_DUST_ID: Int = 7636
            const val RETAINER_DURATION_CYCLES: Int = 50
            const val RETALIATION_QUEUE: String = "queue.com_retaliate_player"
            const val NON_VAMPYRE_MESSAGE: String =
                "This spell can only be used on vampyre juveniles or vampyre juvinates."
            const val HEALTH_MESSAGE: String =
                "This spell only works against vampyres with at most half of their health remaining."
        }
    }
}

private suspend fun ProtectedAccess.playRetainerVisuals(
    manager: SpecialAttackManager,
    target: Npc,
    heldType: dev.openrune.types.NpcServerType,
    duration: Int,
) {
    val hallowed = player.righthand?.id == "obj.hallowed_flail".asRSCM(RSCMType.OBJ)
    anim(if (hallowed) HALLOWED_RETAINER_CAST else RETAINER_CAST)
    spotanim(
        spot = if (hallowed) HALLOWED_RETAINER_CAST_SPOT else RETAINER_CAST_SPOT,
        slot = constants.spotanim_slot_combat,
        height = 96,
    )
    val projectile =
        manager.spawnProjectile(
            source = this,
            target = target,
            spotanim = RETAINER_TRAVEL,
            projanim = RETAINER_PROJANIM,
        )
    delay(projectile.serverCycles)
    target.spotanim(
        spot = RETAINER_IMPACT,
        slot = constants.spotanim_slot_combat,
        height = 0,
    )
    npcChangeType(target, heldType, duration)
    target.anim(RETAINER_CAPTURE_START)
}

private fun Npc.retainerHeldType(): dev.openrune.types.NpcServerType? {
    val heldId = RETAINER_HELD_TYPE_BY_BASE[type.id] ?: return null
    return ServerCacheManager.getNpc(heldId)
}

private val RETAINER_HELD_TYPE_BY_BASE: Map<Int, Int> =
    buildMap {
        fun pair(base: String, held: String) {
            put(base.asRSCM(RSCMType.NPC), held.asRSCM(RSCMType.NPC))
        }

        pair("npc.sang_myq3_male_juvenile", "npc.sang_myq3_male_juvenile_held")
        pair("npc.sang_myq3_female_juvenile", "npc.sang_myq3_female_juvenile_held")
        pair("npc.sang_myq3_male_juvinate", "npc.sang_myq3_male_juvinate_held")
        pair("npc.sang_myq3_female_juvinate", "npc.sang_myq3_female_juvinate_held")
        for (variant in 1..4) {
            pair("npc.burgh_vampire_juve_$variant", "npc.burgh_vampire_juve_held")
        }
        pair("npc.burgh_vampire_juve_1_attackable", "npc.burgh_vampire_juve_held")
        pair("npc.burgh_vampire_juve_2_attackable", "npc.burgh_vampire_juve_held")
        for (variant in 1..3) {
            pair("npc.burgh_vampire_juvenile_$variant", "npc.burgh_vampire_juvenile_held")
            pair("npc.trek_vampire_juve_angry_$variant", "npc.trek_vampire_juve_held_$variant")
        }
        for (variant in 1..2) {
            pair("npc.myq5_trek_juvinate_$variant", "npc.myq5_trek_juvinate_${variant}_held")
            pair("npc.darkm_juvinate_0$variant", "npc.darkm_juvinate_0${variant}_held")
            pair("npc.darkm_juvenile_0$variant", "npc.darkm_juvenile_0${variant}_held")
        }
    }

private const val RETAINER_CAST = "seq.burgh_human_holding_cast"
private const val HALLOWED_RETAINER_CAST = "seq.burgh_human_holding_cast_hallowed_flail"
private const val RETAINER_CAPTURE_START = "seq.burgh_human_holding_capture_start"
private const val RETAINER_CAST_SPOT = "spotanim.burgh_holding_cast_player_spot"
private const val HALLOWED_RETAINER_CAST_SPOT = "spotanim.burgh_holding_cast_player_spot_red"
private const val RETAINER_TRAVEL = "spotanim.burgh_hold_travel"
private const val RETAINER_IMPACT = "spotanim.burgh_hold_hit"
private const val RETAINER_PROJANIM = "projanim.magic_spell_low"
