package org.rsmod.content.interfaces.display.name

import jakarta.inject.Inject
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.db.gateway.model.fold
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.social.SocialNameRepository
import org.rsmod.api.social.pushFriends
import org.rsmod.api.social.pushIgnores
import org.rsmod.api.social.refreshCachedSocialNames
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SocialRefreshCommand
@Inject
constructor(
    private val db: GameDbManager,
    private val names: SocialNameRepository,
    private val playerList: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("refreshsocial") {
            internal = "modlevel.admin"
            desc = "Refresh cached friend/ignore display names from the account database."
            cheat(::refreshSocial)
        }
    }

    private fun refreshSocial(cheat: Cheat) {
        val player = cheat.player
        val uid = player.uid

        db.request(
            request = { connection ->
                val refreshed = player.refreshCachedSocialNames(connection, names)
                GameDbResult.Ok(refreshed)
            },
            response = { result ->
                val current = uid.resolve(playerList) ?: return@request

                result.fold(
                    onOk = { refreshed ->
                        current.pushFriends(playerList)
                        current.pushIgnores(playerList)
                        current.mes("Refreshed $refreshed social name record(s).")
                    },
                    onErr = {
                        current.mes("Unable to refresh social names right now.")
                    },
                )
            },
        )
    }
}
