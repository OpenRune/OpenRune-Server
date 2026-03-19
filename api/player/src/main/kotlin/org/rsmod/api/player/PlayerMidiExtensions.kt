package org.rsmod.api.player

import dev.openrune.types.aconverted.MidiType
import net.rsprot.protocol.game.outgoing.sound.MidiJingle
import net.rsprot.protocol.game.outgoing.sound.MidiSongV2
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal var Player.musicClocks by intVarBit(varbits.music_curr_clocks)

/** @see [MidiJingle] */
public fun Player.midiJingle(jingle: Int) {
    musicClocks = 0 // Client restarts music when a jingle is played.
    client.write(MidiJingle(jingle))
}

/** @see [MidiSongV2] */
public fun Player.midiSong(
    midi: MidiType,
    fadeOutDelay: Int = 0,
    fadeOutSpeed: Int = 0,
    fadeInDelay: Int = 0,
    fadeInSpeed: Int = 0,
) {
    client.write(MidiSongV2(midi.id, fadeOutDelay, fadeOutSpeed, fadeInDelay, fadeInSpeed))
}
