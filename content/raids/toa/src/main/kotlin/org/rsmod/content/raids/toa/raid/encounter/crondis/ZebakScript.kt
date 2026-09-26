package org.rsmod.content.raids.toa.raid.encounter.crondis

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpNpc4
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
 *   for a given npc only one of the two can fire, so it's never handled twice. The same goes for
 *   the jug, which becomes the rolling jug npc once pushed or pulled.
 * - **Jug ops.** Push/Pull (op1/op3) roll it; "Hit" (op4) breaks it. Push and Pull are ap
 *   (approach) handlers, not op ones: an op handler only runs once the player stands on a tile
 *   *cardinally* next to the npc, so a player standing diagonally was first walked round to a
 *   side (a delay, and never a diagonal roll). Offline_Scape JugPushAction acts as soon as the
 *   player is within 1 tile, diagonals included; isWithinApRange(npc, 1) does the same
 *   (Chebyshev distance), and otherwise keeps the player approaching until it is.
 *   The standing jug has no
 *   Attack op, and the engine's player-vs-npc combat only accepts npcs with one (PvNCombat checks
 *   op2), so "Hit" can't start real combat: it breaks the jug from melee range. The rolling jug's
 *   Attack (op2) is normal combat, and any hit landing on either jug breaks it (onNpcHit).
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

        // Great Roar jugs.
        onApNpc1(ZebakEncounter.JUG) {
            if (isWithinApRange(it.npc, 1)) ZebakEncounter.onJugMoved(player, it.npc, push = true)
        }
        onApNpc3(ZebakEncounter.JUG) {
            if (isWithinApRange(it.npc, 1)) ZebakEncounter.onJugMoved(player, it.npc, push = false)
        }
        onOpNpc4(ZebakEncounter.JUG) { ZebakEncounter.onJugBroken(it.npc) }
        for (name in listOf(ZebakEncounter.JUG, ZebakEncounter.JUG_ROLLING)) {
            val type = npcType(name)
            // Fires every tick: the jugs have `timer = 1` in toa_zebak.toml; only rolling ones move.
            onAiTimer(name) { ZebakEncounter.onJugTick(npc) }
            onNpcHit(type) { if (hit.isFromPlayer) ZebakEncounter.onJugBroken(npc) }
            onNpcQueue(type, "queue.death") { ZebakEncounter.onJugBroken(npc) }
        }

        // Great Roar boulders: the third roar wave takes their 150 hitpoints.
        onNpcQueue(npcType(ZebakEncounter.BOULDER), "queue.death") { ZebakEncounter.onBoulderDeath(npc) }

        // Capture: the toa_damage_taken varbits (see ZebakEncounter.onPlayerDamaged).
        onPlayerHit {
            if (hit.damage > 0) ZebakEncounter.onPlayerDamaged(player, hit.damage)
        }
    }

    private fun npcType(name: String) = ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC))!!
}
