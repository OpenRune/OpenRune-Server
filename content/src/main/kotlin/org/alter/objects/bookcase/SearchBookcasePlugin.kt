package org.alter.objects.bookcase

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onObjectOption

class SearchBookcasePlugin() : PluginEvent() {

    val BOOKCASES = setOf("objects.bookcase", "objects.bookcase2")

    override fun init() {
        BOOKCASES.forEach { case ->
            onObjectOption(obj = case, "search") {
                player.queue {
                    search(this, player)
                }
            }
        }

    }

    suspend fun search(it: QueueTask, p: Player, ) {
        p.lock()
        p.message("You search the books...")
        it.wait(3)
        p.unlock()
        p.message("You don't find anything that you'd ever want to read.")
    }

}
