package org.rsmod.content.events.leaguetasks.tutorial

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.events.leaguetasks.pacts.LeaguePactsSpentEvent
import org.rsmod.content.events.leaguetasks.relics.LeagueRelicUnlockedEvent
import org.rsmod.content.events.leaguetasks.tutorial.LeagueTutorial.Companion.WELCOME
import org.rsmod.content.interfaces.journal.tab.JournalStoneSelectedEvent
import org.rsmod.game.world.WorldType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeagueTutorialScript
@Inject
constructor(
    private val tutorial: LeagueTutorial,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (tutorial.isActive(player)) {
                player.softTimer("timer.league_tutorial_resume", cycles = 1)
            }
        }

        onPlayerSoftTimer("timer.league_tutorial_resume") {
            player.clearSoftTimer("timer.league_tutorial_resume")
            protectedAccess.launch(player) {
                if (player.coords != TUTORIAL_TILE) {
                    telejump(TUTORIAL_TILE, TeleportType.Exempt)
                }
            }
            tutorial.resume(player)
        }

        onIfModalButton("component.league_tutorial_main:button_2") { tutorial.getStarted(player) }

        onIfModalButton("component.league_tutorial_main:button_1") {
            if (tutorial.step(player) == WELCOME) {
                // UNVERIFIED: "Exit Leagues" was never clicked in a capture; logging out is a guess.
                logOut()
            } else {
                tutorial.finish(player)
            }
        }

        onEvent<JournalStoneSelectedEvent> { tutorial.onJournalStone(player) }
        onEvent<LeagueRelicUnlockedEvent> { tutorial.onRelicUnlocked(player) }
        onEvent<LeaguePactsSpentEvent> { tutorial.onPactsSpent(player) }
    }

    private companion object {
        val TUTORIAL_TILE = CoordGrid(1503, 5602, 0)
    }
}
