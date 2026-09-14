package org.rsmod.api.player.protect

import net.rsprot.protocol.game.outgoing.sound.MidiJingle
import org.rsmod.api.player.musicClocks

private const val QUEST_COMPLETE_JINGLE = 153

public fun ProtectedAccess.questCompleteJingle() {
    player.musicClocks = 0
    player.client.write(MidiJingle(QUEST_COMPLETE_JINGLE))
}
