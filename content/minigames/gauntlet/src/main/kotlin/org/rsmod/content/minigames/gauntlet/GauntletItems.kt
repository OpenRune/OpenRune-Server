package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GauntletItems
@Inject
constructor(private val manager: InstanceManager, private val runs: GauntletRuns) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (corrupted in listOf(false, true)) {
            val teleport = gauntletObj("teleport_crystal", corrupted)
            onOpHeld1(teleport) { useTeleportCrystal(teleport, corrupted) }
            onOpHeld1(gauntletObj("escape_crystal", corrupted)) {
                with(runs) { leave(loot = player.gauntletBossStarted) }
            }

            val shard = gauntletObj("crystal_shard", corrupted)
            val dust = gauntletObj("crystal_shard_crushed", corrupted)
            onOpHeldU("obj.gauntlet_pestle", shard) { crushShards(shard, dust) }
            onOpHeldU(gauntletObj("herb", corrupted), "obj.gauntlet_vial_water") {
                addGrym(corrupted)
            }
            onOpHeldU(dust, "obj.gauntlet_potion_unfinished") { mixEgniol(dust) }
            for (component in listOf("melee_component", "magic_component", "ranged_component")) {
                val obj = gauntletObj(component, corrupted)
                onOpHeldU("obj.gauntlet_pestle", obj) { grindComponent(obj, shard) }
            }
        }
    }

    private suspend fun ProtectedAccess.useTeleportCrystal(crystal: String, corrupted: Boolean) {
        val run = runs.runFor(player) ?: return
        if (player.gauntletBossStarted || run.mode.corrupted != corrupted) {
            mes("You cannot use this crystal here.")
            return
        }
        val mode = if (corrupted) GauntletMode.CORRUPTED else GauntletMode.NORMAL
        val start =
            manager.sessionForPlayer(player)?.let { manager.resolveCoord(it, mode.enterSource()) }
        if (start == null) {
            mes("Nothing interesting happens.")
            return
        }
        if (!player.invDel(inv, crystal, 1).success) return
        anim("seq.human_castteleport")
        spotanim("spotanim.teleport_casting", height = TELEPORT_SPOT_HEIGHT)
        soundSynth("synth.teleport_all")
        delay(3)
        telejump(start, TeleportType.Exempt)
        resetAnim()
    }

    private fun ProtectedAccess.crushShards(shard: String, dust: String) {
        if (inv.count(shard) < CRUSH_COUNT) {
            mes("You need at least $CRUSH_COUNT crystal shards to do that.")
            return
        }
        if (!player.exchangeGauntlet(listOf(shard to CRUSH_COUNT), dust, CRUSH_COUNT)) return
        spam("You crush some of your shards down to a powdery form.")
    }

    private fun ProtectedAccess.grindComponent(component: String, shard: String) {
        if (!player.exchangeGauntlet(listOf(component to 1), shard, COMPONENT_SHARDS)) return
        spam("You grind the component down into crystal shards.")
    }

    private fun ProtectedAccess.addGrym(corrupted: Boolean) {
        if (!player.exchangeGauntlet(listOf(gauntletObj("herb", corrupted) to 1, "obj.gauntlet_vial_water" to 1), "obj.gauntlet_potion_unfinished")) return
        spam("You put the grym leaf into the vial of water.")
    }

    private fun ProtectedAccess.mixEgniol(dust: String) {
        if (inv.count(dust) < CRUSH_COUNT) {
            mes("You need $CRUSH_COUNT crystal dust to finish the potion.")
            return
        }
        if (!player.exchangeGauntlet(listOf(dust to CRUSH_COUNT, "obj.gauntlet_potion_unfinished" to 1), "obj.gauntlet_potion_3")) return
        statAdvance("stat.herblore", EGNIOL_XP)
        spam("You mix the crystal dust into your potion.")
    }

    private companion object {
        const val TELEPORT_SPOT_HEIGHT = 92
        const val CRUSH_COUNT = 10
        const val COMPONENT_SHARDS = 80
        const val EGNIOL_XP = 10.0
    }
}
