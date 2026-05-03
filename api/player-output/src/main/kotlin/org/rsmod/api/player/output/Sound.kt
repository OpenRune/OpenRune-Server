package org.rsmod.api.player.output

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.rsmod.game.entity.Player

/** @see [SynthSound] */
public fun Player.soundSynth(synth: String, loops: Int = 1, delay: Int = 0) {
    client.write(SynthSound(synth.asRSCM(RSCMType.SYNTH), loops, delay))
}

public fun Player.soundSynth(synth: Int, loops: Int = 1, delay: Int = 0) {
    client.write(SynthSound(synth, loops, delay))
}
