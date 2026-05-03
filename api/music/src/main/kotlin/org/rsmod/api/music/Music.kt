package org.rsmod.api.music

import dev.openrune.types.aconverted.MidiType
import dev.openrune.types.varp.VarpServerType
import org.rsmod.api.table.MusicRow

public data class Music(
    val id: Int,
    val displayName: String,
    val unlockHint: String,
    val duration: Int,
    val midi: MidiType,
    val unlockVarp: String?,
    val unlockBitpos: Int,
    val hidden: Boolean,
    val secondary: MusicRow?,
) {
    val unlockBitflag: Int
        get() = 1 shl unlockBitpos

    val canUnlock: Boolean
        get() = unlockVarp != null
}

public data class MusicVariable(val varpIndex: Int, val bitpos: Int)
