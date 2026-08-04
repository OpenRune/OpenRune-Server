package org.rsmod.content.skills.shootingstars.scripts

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.table.ShootingStarLocationsRow
import org.rsmod.content.skills.shootingstars.ShootingStarManager
import org.rsmod.content.skills.shootingstars.ShootingStarStages
import org.rsmod.content.skills.shootingstars.ShootingstarsSettings
import org.rsmod.content.skills.shootingstars.byKey
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ShootingStarScript
@Inject
constructor(
    private val stars: ShootingStarManager,
    private val mining: StarMiningScript,
) : PluginScript() {
    private val settings: ShootingstarsSettings
        get() = ShootingstarsSettings.load()

    override fun ScriptContext.startup() {
        val settings = settings
        if (!settings.isEnabled) {
            return
        }
        stars.bindSettings(settings)

        onEvent<GameLifecycle.LateCycle> { stars.tick() }

        for (loc in ShootingStarStages.LOC_NAMES) {
            onOpLoc1(loc) { with(mining) { attempt(it.loc) } }
            onOpLoc2(loc) { showProgress() }
            onOpLoc3(loc) { with(mining) { mine(it.loc) } }
        }

        onCommand("star") {
            desc = "Force-spawn a shooting star"
            invalidArgs = "Usage: ::star [LOCATION|ANY] (ex: ::star MINING_GUILD)"
            cheat { forceStar() }
        }
    }

    private fun ProtectedAccess.showProgress() {
        if (!stars.active) {
            mes("There is no active shooting star.")
            return
        }
        val stage = stars.currentStage()
        mes(
            "This is a size-${stage.size} star. " +
                "It has been mined ${stars.percentageToNextLevel()}% of the way to the next layer.",
        )
    }

    private fun Cheat.forceStar() {
        val arg = args.getOrNull(0)?.uppercase()
        val location =
            when {
                arg == null || arg == "ANY" -> null
                else ->
                    ShootingStarLocationsRow.byKey(arg)
                        ?: run {
                            player.mes(
                                "Unknown location '$arg'. Use ::star ANY or a site key " +
                                    "(ex: MINING_GUILD).",
                            )
                            return
                        }
            }
        stars.spawnStar(location)
        player.mes("Shooting star crash sequence started.")
    }
}
