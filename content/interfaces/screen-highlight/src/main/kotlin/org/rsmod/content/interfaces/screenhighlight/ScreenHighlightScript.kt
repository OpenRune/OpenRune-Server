package org.rsmod.content.interfaces.screenhighlight

import jakarta.inject.Inject
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ScreenHighlightScript @Inject constructor(private val tours: ScreenHighlightTours) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton("component.screenhighlight:continue") { player.turnPage(forward = true) }
        onIfOverlayButton("component.screenhighlight:previous") { player.turnPage(forward = false) }
        onIfClose("interface.screenhighlight") { tours.tourAt(from.packed)?.onClose?.invoke(player) }
    }

    private fun Player.turnPage(forward: Boolean) {
        val tour = tours.active(this) ?: return
        val current = vars["varbit.hnt_hint_step"]
        val page = if (forward) current + 1 else current - 1
        val finish = tour.onFinish
        when {
            page in tour.pages.indices -> tours.show(this, tour, page)
            forward && finish != null -> {
                VarPlayerIntMapSetter.set(this, "varbit.hnt_hint_step", tour.pages.size)
                finish(this)
            }
            else -> tours.show(this, tour, current.coerceIn(tour.pages.indices))
        }
    }
}
