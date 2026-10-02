package org.rsmod.content.skills.magic.arceuus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal const val DEATH_CHARGE_OBJ = "obj.placeholder_vetion_pet2"

internal var Player.deathChargeActive by boolVarBit("varbit.arceuus_death_charge_active")

internal var Player.deathChargeCooldown by boolVarBit("varbit.arceuus_death_charge_cooldown")

internal var Player.deathChargeUpgraded by boolVarBit("varbit.death_charge_scroll_used")

internal var Player.deathChargeCharges by intVarBit("varbit.death_charge_charges")

class DeathChargeModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(DeathChargeKillHook::class.java)
    }
}

class DeathChargeScript
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueue(EXPIRE_QUEUE) { expire() }
        onIfOverlayButton("component.magic_spellbook:death_charge") { cast() }
        onOpHeld1(RITE_OBJ) { readRite() }
    }

    private fun ProtectedAccess.cast() {
        if (player.deathChargeCooldown) {
            mes("Death Charge is still on cooldown.")
            return
        }

        val spellObj = ServerCacheManager.getItem(DEATH_CHARGE_OBJ.asRSCM(RSCMType.OBJ)) ?: return
        val spell = spells.getObjSpell(spellObj) ?: return

        if (runes.attemptCast(player, spell).isFailure()) {
            return
        }

        val upgraded = player.deathChargeUpgraded
        statAdvance("stat.magic", spell.castXp)
        anim("seq.human_cast_selfimbue")
        spotanim(if (upgraded) UPGRADED_CAST_SPOTANIM else CAST_SPOTANIM)
        mes("Upon the death of your next foe, some of your special attack energy will be restored.")

        player.deathChargeCharges = if (upgraded) UPGRADED_CHARGES else BASE_CHARGES
        player.deathChargeActive = true
        player.deathChargeCooldown = true
        clearQueue(EXPIRE_QUEUE)
        queue(EXPIRE_QUEUE, DURATION_TICKS)
        runClientScript(BUFF_BAR_START_CLIENTSCRIPT, ACTIVE_BUFF_STRUCT, mapClock)
        runClientScript(BUFF_BAR_START_CLIENTSCRIPT, COOLDOWN_BUFF_STRUCT, mapClock)
    }

    private fun ProtectedAccess.expire() {
        player.deathChargeCooldown = false
        if (player.deathChargeCharges == 0) {
            return
        }
        player.deathChargeCharges = 0
        player.deathChargeActive = false
        spotanim(if (player.deathChargeUpgraded) UPGRADED_END_SPOTANIM else END_SPOTANIM)
        mes("Your Death Charge has faded away.")
    }

    private fun ProtectedAccess.readRite() {
        if (player.deathChargeUpgraded) {
            mes("You have already learned the rite of vile transference.")
            return
        }
        if (invDel(inv, RITE_OBJ).failure) {
            return
        }
        player.deathChargeUpgraded = true
        mes("You study the rite. Your Death Charge spell has been permanently upgraded.")
    }

    internal companion object {
        const val RITE_OBJ = "obj.death_charge_scroll"
        const val EXPIRE_QUEUE = "queue.death_charge_expire"
        const val CAST_SPOTANIM = "spotanim.death_charge_cast_spotanim"
        const val UPGRADED_CAST_SPOTANIM = "spotanim.death_charge_upgrade_cast_spotanim"
        const val END_SPOTANIM = "spotanim.death_charge_end_spotanim"
        const val UPGRADED_END_SPOTANIM = "spotanim.death_charge_end_upgrade_spotanim"
        const val BUFF_BAR_START_CLIENTSCRIPT = 5931
        const val ACTIVE_BUFF_STRUCT = 3124
        const val COOLDOWN_BUFF_STRUCT = 3123
        const val DURATION_TICKS = 100
        const val BASE_CHARGES = 1
        const val UPGRADED_CHARGES = 2
        const val RESTORED_ENERGY = 150
        const val MAX_DISTANCE = 16
        const val PVP_GRACE_TICKS = 10
    }
}

internal class DeathChargeKillHook @Inject constructor(private val energy: SpecialAttackEnergy) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val player = context.hero
        if (player.deathChargeCharges == 0) {
            return
        }
        if (player.vars["varp.lastcombat_pvp"] + DeathChargeScript.PVP_GRACE_TICKS >=
                player.currentMapClock
        ) {
            return
        }
        if (!player.isWithinRange(context.npc)) {
            return
        }

        energy.addSpecialEnergy(player, DeathChargeScript.RESTORED_ENERGY)
        player.mes("Some of your special attack energy has been restored.")

        val remaining = player.deathChargeCharges - 1
        player.deathChargeCharges = remaining
        if (remaining == 0) {
            player.deathChargeActive = false
            val end =
                if (player.deathChargeUpgraded) {
                    DeathChargeScript.UPGRADED_END_SPOTANIM
                } else {
                    DeathChargeScript.END_SPOTANIM
                }
            player.spotanim(end)
        }
    }

    private fun Player.isWithinRange(npc: Npc): Boolean {
        val distance = DeathChargeScript.MAX_DISTANCE
        return coords.level == npc.coords.level &&
            abs(coords.x - npc.coords.x) <= distance &&
            abs(coords.z - npc.coords.z) <= distance
    }
}
