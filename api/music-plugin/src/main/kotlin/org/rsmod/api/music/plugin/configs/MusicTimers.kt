package org.rsmod.api.music.plugin.configs

import dev.openrune.timer
import dev.openrune.types.aconverted.TimerType

public typealias music_timers = MusicTimers

public object MusicTimers {
    public val sync: TimerType = timer("music_sync")
}
