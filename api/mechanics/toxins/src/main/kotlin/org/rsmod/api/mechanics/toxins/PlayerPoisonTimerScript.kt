package org.rsmod.api.mechanics.toxins

import jakarta.inject.Inject
import org.rsmod.api.mechanics.toxins.impl.PlayerDisease
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.game.MapClock
import org.rsmod.game.entity.timerAt
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PlayerPoisonTimerScript
@Inject
constructor(
    private val worldClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            val clock =
                worldClock.cycle

            if (
                player.vars[
                    "varp.poison_severity"
                ] > 0
            ) {
                player.clearTimer(
                    "timer.player_poison",
                )

                player.timerAt(
                    timer = "timer.player_poison",
                    mapClock = clock,
                    cycles = PlayerPoison.TICK_INTERVAL,
                )
            }

            if (
                player.vars[
                    "varp.venom_strikes"
                ] > 0
            ) {
                player.clearTimer(
                    "timer.player_venom",
                )

                player.timerAt(
                    timer = "timer.player_venom",
                    mapClock = clock,
                    cycles = PlayerVenom.TICK_INTERVAL,
                )
            }

            PlayerDisease.rearmTimerAfterLogin(
                player = player,
                clock = clock,
            )

            ToxinImmunity.onLogin(
                player = player,
                clock = clock,
            )
        }

        onPlayerLogout {
            ToxinImmunity.onLogout(player)
        }

        onPlayerTimer(
            "timer.player_poison",
        ) {
            PlayerPoison.onPoisonTimerTick(
                player,
            )
        }

        onPlayerTimer(
            "timer.player_venom",
        ) {
            PlayerVenom.onVenomTimerTick(
                player,
            )
        }

        onPlayerTimer(
            "timer.player_disease",
        ) {
            PlayerDisease.onDiseaseTimerTick(
                player,
            )
        }

        onPlayerTimer(
            ToxinImmunity.TIMER,
        ) {
            ToxinImmunity.onTimer(player)
        }
    }
}
