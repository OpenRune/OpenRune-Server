package org.rsmod.content.areas.misc.motherlode.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.motherlode.MotherlodeMine
import org.rsmod.content.areas.misc.motherlode.PayDirtOre
import org.rsmod.content.areas.misc.motherlode.addCleaningPayDirt
import org.rsmod.content.areas.misc.motherlode.circuit.MotherlodeWaterCircuit
import org.rsmod.content.areas.misc.motherlode.cleaningPayDirtTotal
import org.rsmod.content.areas.misc.motherlode.clearHeldPayDirt
import org.rsmod.content.areas.misc.motherlode.hasUpperHopper
import org.rsmod.content.areas.misc.motherlode.movePayDirtToMachine
import org.rsmod.content.areas.misc.motherlode.sackCapacity
import org.rsmod.content.areas.misc.motherlode.sackCount
import org.rsmod.content.areas.misc.motherlode.sackTotal
import org.rsmod.content.areas.misc.motherlode.scripts.MercyScript.Companion.mercyWarnsAboutHopper
import org.rsmod.content.areas.misc.motherlode.setSackCount
import org.rsmod.content.areas.misc.motherlode.syncMotherlodeVars
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeMachineScript
@Inject
constructor(private val circuit: MotherlodeWaterCircuit) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.motherlode_hopper") { deposit(it.loc) }
        onApLoc1("loc.motherlode_hopper") { apDeposit(it.loc) }
        onOpLoc1("loc.motherlode_sack") { searchSack() }
        onOpLoc1("loc.motherlode_wheel_strut_broken") { hammerStrut(it.loc) }
    }

    /** The upper hopper is fenced in by railings, so it can only be used across them. */
    private suspend fun ProtectedAccess.apDeposit(hopper: BoundLocInfo) {
        if (isWithinApRange(hopper, distance = 1)) {
            deposit(hopper)
        }
    }

    private suspend fun ProtectedAccess.deposit(hopper: BoundLocInfo) {
        if (hopper.coords == MotherlodeMine.UPPER_HOPPER && !player.hasUpperHopper) {
            mercyWarnsAboutHopper()
            return
        }
        val held = inv.count(MotherlodeMine.PAYDIRT)
        if (held == 0) {
            mes("You don't have any pay-dirt to deposit.")
            return
        }
        val space = player.sackCapacity - player.sackTotal - player.cleaningPayDirtTotal
        if (space <= 0) {
            mes("The sack is too full to hold any more pay-dirt. You should collect your ore first.")
            return
        }
        val count = minOf(held, space)
        if (invDel(inv, MotherlodeMine.PAYDIRT, count).failure) {
            return
        }

        val moved = player.movePayDirtToMachine(count)
        repeat(count - moved) { player.addCleaningPayDirt(PayDirtOre.roll(player.miningLvl, random)) }
        if (inv.count(MotherlodeMine.PAYDIRT) == 0) {
            player.clearHeldPayDirt()
        }

        anim("seq.human_pickuptable")
        soundSynth(HOPPER_SOUND)
        circuit.deposit(player, count)
        if (count < held) {
            mes("The sack can't hold all of your pay-dirt, so you keep the rest.")
        }
    }

    private suspend fun ProtectedAccess.searchSack() {
        if (player.sackTotal == 0) {
            if (player.cleaningPayDirtTotal > 0) {
                mes("Your pay-dirt is still being cleaned.")
            } else {
                mes("The sack is empty.")
            }
            return
        }

        var collected = false
        for (ore in PayDirtOre.entries) {
            val stored = player.sackCount(ore)
            if (stored == 0) {
                continue
            }
            val take = if (ore == PayDirtOre.Nugget) stored else minOf(stored, inv.freeSpace())
            if (take == 0 || invAdd(inv, ore.obj, take).failure) {
                continue
            }
            player.setSackCount(ore, stored - take)
            collected = true
        }
        player.syncMotherlodeVars()

        if (!collected) {
            mes("Your inventory is too full to hold any more ore.")
            return
        }
        if (player.sackTotal == 0) {
            objbox(MotherlodeMine.PAYDIRT, "You collect your ore from the sack.<br>The sack is now empty.")
        } else {
            objbox(MotherlodeMine.PAYDIRT, "You collect some of your ore from the sack.")
        }
    }

    private suspend fun ProtectedAccess.hammerStrut(strut: BoundLocInfo) {
        if (!hasHammer()) {
            mes("You need a hammer to repair the strut.")
            return
        }
        var cycles = 0
        while (circuit.isBrokenStrut(strut.coords)) {
            if (cycles % HAMMER_ANIM_CYCLES == 0) {
                anim("seq.swan_hammer")
                soundSynth(HAMMER_SOUND, delay = 10)
            }
            delay(1)
            cycles++
            if (random.randomDouble() >= repairChance()) {
                continue
            }
            if (circuit.repairStrut(strut.coords)) {
                statAdvance("stat.smithing", player.smithingLvl * STRUT_XP_MULTIPLIER)
            }
        }
        resetAnim()
    }

    private fun ProtectedAccess.hasHammer(): Boolean =
        HAMMERS.any { inv.count(it) > 0 || worn.count(it) > 0 }

    private fun ProtectedAccess.repairChance(): Double {
        val level = player.smithingLvl.coerceIn(1, MAX_LEVEL)
        return MIN_REPAIR_CHANCE + (MAX_REPAIR_CHANCE - MIN_REPAIR_CHANCE) * (level - 1) / (MAX_LEVEL - 1)
    }

    private companion object {
        const val HOPPER_SOUND = 2496
        const val HAMMER_SOUND = 1786
        const val HAMMER_ANIM_CYCLES = 3
        const val STRUT_XP_MULTIPLIER = 1.5
        const val MIN_REPAIR_CHANCE = 0.12
        const val MAX_REPAIR_CHANCE = 0.27
        const val MAX_LEVEL = 99

        val HAMMERS = listOf("obj.hammer", "obj.imcando_hammer", "obj.imcando_hammer_offhand")
    }
}
