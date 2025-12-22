package org.alter.interfaces.gameframe

import dev.openrune.definition.type.widget.Component
import dev.openrune.definition.type.widget.IfEvent
import org.alter.api.CommonClientScripts
import org.alter.api.ext.runClientScript
import org.alter.api.ext.syncVarp
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ClientModeSwapped
import org.alter.game.pluginnew.event.impl.PlayerTickEvent
import org.alter.game.pluginnew.event.impl.onIfMoveSub
import org.alter.game.pluginnew.event.impl.onIfMoveTop
import org.alter.game.pluginnew.event.impl.onLogin
import org.alter.game.ui.Gameframe
import org.alter.game.ui.GameframeMove
import org.alter.game.ui.IfSubType
import org.alter.game.ui.InternalApi
import org.alter.game.ui.UserInterfaceMap
import org.alter.interfaces.closeSubs
import org.alter.interfaces.gameframe.GameframeLoader.move
import org.alter.interfaces.ifClose
import org.alter.interfaces.ifMoveSub
import org.alter.interfaces.ifOpenOverlay
import org.alter.interfaces.ifOpenSub
import org.alter.interfaces.ifOpenTop
import org.alter.interfaces.ifSetEvents
import org.alter.rscm.RSCM.asRSCM

class GameFrameEvents : PluginEvent() {

    override fun init() {
        loadAll()

        onLogin { openLoginGameframe(player) }

        gameframes.forEach { (topLevel, gameframe) ->
            onIfMoveTop(topLevel) { player.queueGameframeMove(gameframe) }
        }

        on<ClientModeSwapped> {
            then { player.changeGameframe(gameframeMove) }
        }

        moveEvents.forEach { event ->
            onIfMoveSub(event.target) { player.moveSetEvents(event.event) }
        }

        on<PlayerTickEvent> {
            then {
                processIfCloseQueue(player)
                processIfCloseModal(player)
                if (player.getPendingLogout()) {
                    player.ifClose()
                }
            }
        }

    }

    @OptIn(InternalApi::class)
    private fun processIfCloseQueue(player: Player) {
        for (target in player.ui.closeQueue.iterator()) {
            val component = Component(target)
            player.closeSubs(component)
        }
        player.ui.closeQueue.clear()
    }

    @OptIn(InternalApi::class)
    private fun processIfCloseModal(player: Player) {
        if (player.ui.closeModal) {
            player.ui.closeModal = false
            player.ifClose()
        }
    }

    private fun Player.changeGameframe(move: GameframeMove) {
        val (from, dest, intermediate) = move

        if (dest.resizable) {
            stoneArrangements = dest.stoneArrangement
        }

        syncVarp("varp.chat_filter_assist")
        syncVarp("varp.settings_tracking")

        if (from.topLevel != dest.topLevel) {
            if (intermediate != null) {
                moveGameframe(from, intermediate)
                moveGameframe(intermediate, dest)
            } else {
                moveGameframe(from, dest)
            }
        }

        ifOpenOverlay("interfaces.orbs", "components.toplevel_osrs_stretch:orbs")
    }

    private fun Player.moveGameframe(from: Gameframe, dest: Gameframe) {
        ifOpenTop(dest.topLevel.asRSCM())
        ui.setGameframe(dest.mappings)

        val moveComponents = move
        for (moveComponent in moveComponents) {
            val target = Component(moveComponent.packed)
            val sourceComponent =
                from.mappings[target]
                    ?: error("Expected move target in source mapping: '${moveComponent.internalName}'")
            val destComponent =
                dest.mappings[target]
                    ?: error("Expected move target in dest mapping: '${moveComponent.internalName}'")
            ifMoveSub(sourceComponent, destComponent, target)
        }
    }

    companion object {

        lateinit var gameframes: Map<String, Gameframe>
        private lateinit var moveEvents: List<MoveEvent>
        private lateinit var defaultGameframe: Gameframe


        fun Player.queueGameframeMove(dest: Gameframe) {
            gameframeTopLevelLastKnown = gameframeTopLevel
            val clientModeChanged = ui.frameResizable != dest.resizable
            if (clientModeChanged) {
                runClientScript(CommonClientScripts.CLIENT_MODE, dest.clientMode)
            }

            val isFullScreen = dest.topLevel == "interfaces.toplevel_display"

            val previous = gameframes.getValue(gameframeTopLevel)
            gameframeTopLevel = dest.topLevel
            if (!isFullScreen) {
                ui.frameResizable = dest.resizable
            }
            val gameframeMove = resolveGameframeMove(previous, dest)
            val delay = if (clientModeChanged) 2 else 1

            queue {
                wait(delay)
                ClientModeSwapped(gameframeMove, this@queueGameframeMove).post()
            }
        }

        private fun Player.resolveGameframeMove(from: Gameframe, dest: Gameframe): GameframeMove =
            GameframeMove(from, dest, resolveIntermediate(from, dest))

        private fun Player.resolveIntermediate(from: Gameframe, dest: Gameframe): Gameframe? {
            val mismatch = dest.resizable && !from.resizable &&
                    stoneArrangements != dest.stoneArrangement

            return if (mismatch)
                gameframes.values.first { it.hasFlags(resizable = true, stoneArrangements) }
            else
                null
        }


        private fun Gameframe.hasFlags(resizable: Boolean, stoneArrangements: Boolean): Boolean =
            this.resizable == resizable && this.stoneArrangement == stoneArrangements


    }


    private fun Player.moveSetEvents(component: String) {
        ifSetEvents(component, -1..-1, IfEvent.Op1)
    }

    private fun openLoginGameframe(player: Player) {
        val gf = gameframes[player.gameframeTopLevel]
        val correct =
            gf != null && gf.resizable == player.ui.frameResizable

        if (correct) {
            player.ifOpenTop(gf.topLevel.asRSCM())
            openGameframe(player, gf)
            return
        }

        val fallback =
            selectFallback(player.ui.frameResizable, player.stoneArrangements)
                ?: defaultGameframe

        player.ui.frameResizable = fallback.resizable
        player.gameframeTopLevel = fallback.topLevel
        player.stoneArrangements = fallback.stoneArrangement

        player.ifOpenTop(fallback.topLevel.asRSCM())
        openGameframe(player, fallback)
    }

    private fun openGameframe(player: Player, gameframe: Gameframe) {
        player.ui.setGameframe(gameframe.mappings)
        gameframe.overlays.forEach { overlay ->
            player.ifOpenSub(overlay.interf, overlay.target, IfSubType.Overlay)
        }
    }

    private fun UserInterfaceMap.setGameframe(mappings: Map<Component, Component>) {
        gameframe.clear()
        for ((original, translated) in mappings) {
            gameframe[original] = translated
        }
    }

    private fun loadAll() {
        gameframes = GameframeLoader.loadGameframe()
        moveEvents = GameframeLoader.loadMoveEvents().mapMoveEvents()
        defaultGameframe = selectDefault(gameframes.values)
    }

    private fun Map<String, String>.mapMoveEvents(): List<MoveEvent> =
        map { MoveEvent(it.key, it.value) }

    private fun selectDefault(values: Iterable<Gameframe>): Gameframe =
        values.single(Gameframe::isDefault)

    private fun selectFallback(resizable: Boolean, stoneArrangements: Boolean): Gameframe? =
        gameframes.values.firstOrNull { it.hasFlags(resizable, stoneArrangements) }
            ?: gameframes.values.firstOrNull { it.resizable == resizable }

    private fun Gameframe.hasFlags(resizable: Boolean, stoneArrangements: Boolean): Boolean =
        this.resizable == resizable && this.stoneArrangement == stoneArrangements

    private data class MoveEvent(val target: String, val event: String)
}