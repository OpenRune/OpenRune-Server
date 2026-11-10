package org.rsmod.content.other.bots

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BotScript
@Inject
constructor(private val bots: BotWorld, private val hotspots: BotHotspots) : PluginScript() {
    private val perf = BotPerf()

    override fun ScriptContext.startup() {
        onPlayerLogin { bots.replaceNameCollisions(player) }
        onEvent<GameLifecycle.StartCycle> { perf.startCycle() }
        onEvent<GameLifecycle.LateCycle> {
            perf.timeBots {
                hotspots.tick()
                bots.tick()
            }
        }
        onEvent<GameLifecycle.EndCycle> {
            perf.timeBots { bots.postTick() }
            perf.endCycle()
        }
        onEvent<GameLifecycle.Shutdown> { bots.despawnAll() }

        onCommand("bots") {
            desc = "Manage cosmetic world bots"
            invalidArgs =
                "Usage: ::bots [on|off|count|perf|reset|here ACTIVITY [COUNT] [RADIUS] [STYLES]] " +
                    "(ex: ::bots here IDLING 3 4 wealthy,graceful)"
            cheat { manage() }
        }
    }

    private fun Cheat.manage() {
        when (args.getOrNull(0)?.lowercase()) {
            null,
            "count" -> {
                val state = if (hotspots.enabled) "on" else "off"
                player.mes(
                    "Bots are $state: ${bots.size} bots, " +
                        "${hotspots.awake}/${hotspots.total} areas awake."
                )
            }
            "perf" -> player.mes("${bots.size} bots. ${perf.summary()}")
            "on" -> {
                if (hotspots.enable()) {
                    player.mes("Bots are on; areas near players fill next tick.")
                } else {
                    player.mes("Bots are disabled in the game config.")
                }
            }
            "off" -> {
                hotspots.disable()
                player.mes("Bots are off and every bot has been removed.")
            }
            "clear",
            "reset" -> {
                hotspots.sleepAll()
                player.mes("Removed every bot; areas near players refill next tick.")
            }
            "here" -> spawnHere()
            else -> throw IllegalArgumentException()
        }
    }

    private fun Cheat.spawnHere() {
        if (!hotspots.enabled) {
            player.mes("Bots are off.")
            return
        }
        val activity = BotActivity.valueOf(args[1].uppercase())
        val count = args.getOrNull(2)?.toInt() ?: 1
        val radius = args.getOrNull(3)?.toInt() ?: DEFAULT_RADIUS
        val styles = args.getOrNull(4)?.split(',') ?: emptyList()
        val added = (1..count).count {
            bots.spawn(activity, player.coords, radius, styles, gamemode = player.gamemode)
        }
        player.mes("Spawned $added $activity bots around you.")
    }

    private companion object {
        private const val DEFAULT_RADIUS = 6
    }
}
