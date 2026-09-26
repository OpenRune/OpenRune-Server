package org.rsmod.content.raids.toa.raid.encounter.crondis

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onPlayerHit
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Engine events for the Zebak room. All the logic is in [ZebakEncounter]; these handlers find the
 * npc's room and call into it, so nothing here affects npcs outside the raid.
 *
 * Why each binding:
 * - **onAiOpPlayer2** (Zebak, both types): binding it replaces the default npc combat for that type.
 *   Zebak's attacks come from the room's tick loop, so the handler just drops the interaction. That
 *   stops retaliation (and any aggression) from starting the engine's combat. Players can still
 *   attack him; only his side is disabled.
 * - **onNpcQueue(type, "queue.death")**: replaces the standard npc death for that type. The standard
 *   one would delete Zebak (and try to play a death anim from params); instead the room completes
 *   and plays the death itself. The clouds get their own too: they have no death params, and our
 *   handler removes them for good.
 * - **Both Zebak types.** At 25% Zebak is transmogged into toa_zebak_enraged. Queue events are looked
 *   up by the npc's *visible* type (NpcQueueProcessor uses `visType`), so the enraged type needs its
 *   own death handler. It's registered for hits and ai ops too, in case those also go by visible type;
 *   for a given npc only one of the two can fire, so it's never handled twice.
 */
class ZebakScript : PluginScript() {

    override fun ScriptContext.startup() {
        for (name in listOf(ZebakEncounter.ZEBAK, ZebakEncounter.ZEBAK_ENRAGED)) {
            val type = npcType(name)
            onAiOpPlayer2(type) { npc.noneMode() }
            onNpcHit(type) { ZebakEncounter.onZebakHit(npc, hit) }
            onNpcQueue(type, "queue.death") { ZebakEncounter.onZebakDeath(npc) }
        }

        for (name in listOf(ZebakEncounter.BLOOD_CLOUD, ZebakEncounter.BLOOD_CLOUD_SMALL)) {
            // Fires every tick: the clouds have `timer = 1` in toa_zebak.toml.
            onAiTimer(name) { ZebakEncounter.onCloudTick(npc) }
            onNpcQueue(npcType(name), "queue.death") { ZebakEncounter.onCloudDeath(npc) }
        }

        // Capture: the toa_damage_taken varbits (see ZebakEncounter.onPlayerDamaged).
        onPlayerHit {
            if (hit.damage > 0) ZebakEncounter.onPlayerDamaged(player, hit.damage)
        }
    }

    private fun npcType(name: String) = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!
}
