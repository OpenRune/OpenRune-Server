package org.rsmod.api.net.rsprot.handlers

import dev.openrune.ServerCacheManager
import net.rsprot.protocol.game.incoming.resumed.ResumePObjDialog
import org.rsmod.api.player.input.ResumePObjDialogInput
import org.rsmod.game.entity.Player

class ResumePObjDialogHandler : MessageHandler<ResumePObjDialog> {
    override fun handle(player: Player, message: ResumePObjDialog) {
        val objType = ServerCacheManager.getItem(message.obj) ?: return
        val input = ResumePObjDialogInput(objType)
        player.resumeActiveCoroutine(input)
    }
}
