package org.rsmod.api.player.output

import dev.openrune.types.aconverted.SynthType
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.rsmod.game.entity.Player

/** @see [SynthSound] */
public fun Player.soundSynth(synth: SynthType, loops: Int = 1, delay: Int = 0) {
    client.write(SynthSound(synth.id, loops, delay))
}
