package org.rsmod.api.player.output

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SynthType
import net.rsprot.protocol.game.outgoing.sound.AmbientSoundStart
import net.rsprot.protocol.game.outgoing.sound.AmbientSoundStop
import net.rsprot.protocol.game.outgoing.sound.SynthSound
import org.rsmod.game.entity.Player

/**
 * Plays a sound effect for this player after resolving [synth] as a synth gameval.
 *
 * @param loops Number of times the sound plays.
 * @param delay Delay before playback in client cycles.
 * @see SynthSound
 */
public fun Player.soundSynth(synth: String, loops: Int = 1, delay: Int = 0) {
    client.write(SynthSound(synth.asRSCM(RSCMType.SYNTH), loops, delay))
}

public fun Player.soundSynth(synth: SynthType, loops: Int = 1, delay: Int = 0) {
    client.write(SynthSound(synth.id, loops, delay))
}

/**
 * Starts a looping background sound from the ambience archive. It plays until another
 * ambience replaces it or [stopAmbience] stops it.
 *
 * @param sound An ambience gameval such as `ambientsounds.castle_drakan`.
 * @param fade Whether to fade out the previous ambience when it is replaced.
 */
public fun Player.startAmbience(sound: String, fade: Boolean = true) {
    val id = sound.asRSCM(RSCMType.AMBIENTSOUNDS)
    require(id in 0..65535) { "Invalid ambience id for $sound: $id" }
    client.write(AmbientSoundStart(id, fade))
}

/**
 * Stops the background sound started by [startAmbience].
 *
 * @param fade Whether to fade the ambience out gradually before stopping playback.
 */
public fun Player.stopAmbience(fade: Boolean = true) {
    client.write(AmbientSoundStop(fade))
}
