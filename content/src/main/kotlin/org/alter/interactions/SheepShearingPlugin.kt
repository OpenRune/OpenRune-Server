package org.alter.interactions

import org.alter.api.ext.message
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ItemOnNpcEvent
import org.alter.game.pluginnew.event.impl.onNpcOption
import org.alter.rscm.RSCM.getRSCM

class SheepShearingPlugin : PluginEvent() {

    companion object {
        /**
         * Mapping of unshorn sheep RSCM names to their shorn variants.
         * All standard unshorn sheep transform to sheepsheeredshaggy (2691),
         * and the shaggy variant transforms to sheepsheeredshaggy2 (2692).
         */
        private val UNSHORN_TO_SHORN = mapOf(
            "npcs.sheepunsheered" to "npcs.sheepsheeredshaggy",
            "npcs.sheepunsheeredg" to "npcs.sheepsheeredshaggy",
            "npcs.sheepunsheeredw" to "npcs.sheepsheeredshaggy",
            "npcs.sheepunsheered2" to "npcs.sheepsheeredshaggy",
            "npcs.sheepunsheered3" to "npcs.sheepsheeredshaggy",
            "npcs.sheepunsheered3g" to "npcs.sheepsheeredshaggy",
            "npcs.sheepunsheered3w" to "npcs.sheepsheeredshaggy",
            "npcs.sheepunsheeredshaggy" to "npcs.sheepsheeredshaggy2"
        )

        private const val REGROWTH_TICKS = 100
    }

    override fun init() {
        val shears = getRSCM("items.shears")
        val wool = getRSCM("items.wool")

        // Build ID-based lookup maps
        val unshornIdToShornId = UNSHORN_TO_SHORN.map { (unshorn, shorn) ->
            getRSCM(unshorn) to getRSCM(shorn)
        }.toMap()

        val unshornIdToRscm = UNSHORN_TO_SHORN.keys.associateBy { getRSCM(it) }

        // Register "Shear" option for each unshorn sheep
        for (unshornRscm in UNSHORN_TO_SHORN.keys) {
            onNpcOption(unshornRscm, "Shear") {
                doShear(player, npc, unshornIdToShornId, unshornIdToRscm, wool)
            }
        }

        // Register item-on-npc for using shears on sheep
        on<ItemOnNpcEvent> {
            where {
                item.id == shears && unshornIdToShornId.containsKey(npc.id)
            }
            then {
                doShear(player, npc, unshornIdToShornId, unshornIdToRscm, wool)
            }
        }
    }

    private fun doShear(
        player: Player,
        npc: Npc,
        unshornIdToShornId: Map<Int, Int>,
        unshornIdToRscm: Map<Int, String>,
        wool: Int
    ) {
        val shornId = unshornIdToShornId[npc.id] ?: return
        val unshornRscm = unshornIdToRscm[npc.id] ?: return

        player.queue {
            player.facePawn(npc)
            player.animate("sequences.human_shearing")
            wait(2)

            if (!player.inventory.hasFreeSpace()) {
                player.message("You don't have enough inventory space.")
                return@queue
            }

            // Give wool
            player.inventory.add(wool, 1)
            player.message("You get some wool.")

            // Transform sheep: despawn unshorn, spawn shorn
            val tile = npc.tile
            val spawnTile = npc.spawnTile
            val walkRadius = npc.walkRadius
            world.remove(npc)

            val shornNpc = Npc(shornId, tile, world)
            shornNpc.walkRadius = walkRadius
            shornNpc.setActive(true)
            world.spawn(shornNpc)

            // Schedule regrowth
            world.queue {
                wait(REGROWTH_TICKS)
                world.remove(shornNpc)
                val regrown = Npc(getRSCM(unshornRscm), spawnTile, world)
                regrown.respawns = true
                regrown.walkRadius = walkRadius
                regrown.setActive(true)
                world.spawn(regrown)
            }
        }
    }
}
