package org.rsmod.api.net.rsprot.handlers

import net.rsprot.protocol.game.incoming.misc.user.SetChatFilterSettings
import org.rsmod.api.social.SocialData
import org.rsmod.api.social.persistSocial
import org.rsmod.api.social.pushChatModes
import org.rsmod.api.social.social
import org.rsmod.game.entity.Player

class SetChatFilterSettingsHandler : MessageHandler<SetChatFilterSettings> {
    override fun handle(player: Player, message: SetChatFilterSettings) {
        player.social.publicChatMode = SocialData.ChatFilterMode.fromId(message.publicChatFilter)
        player.social.privateChatMode = SocialData.PrivateChatMode.fromId(message.privateChatFilter)
        player.social.tradeChatMode = SocialData.ChatFilterMode.fromId(message.tradeChatFilter)

        player.persistSocial()
        player.pushChatModes()
    }
}
