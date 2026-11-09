package org.rsmod.api.player.music

import dev.openrune.ServerCacheManager
import net.rsprot.protocol.game.outgoing.sound.MidiSongV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.music.MusicRepository
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.table.MusicModernRow
import org.rsmod.api.table.MusicRow
import org.rsmod.api.testing.capture.CaptureClient
import org.rsmod.api.testing.random.FixedRandom
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class MusicPlayerTest {
    @Test
    fun `skip in shuffle mode moves to another unlocked track and never a locked one`() {
        val f = Fixture(MusicPlayMode.Random)
        val (current, first, second) = f.unlockable.take(3)
        f.unlock(current, first, second)
        val others = listOf(first, second).map(f::idOf)

        for (index in others.indices) {
            f.musicPlayer.play(f.player, current)
            f.client.clearOutgoing()
            f.random.set(index)

            f.musicPlayer.skip(f.player)

            assertEquals(others[index], f.player.currMusicId)
            assertTrue(f.client.hasAny<MidiSongV2>())
        }
    }

    @Test
    fun `skip keeps the current track when no other track is unlocked`() {
        val f = Fixture(MusicPlayMode.Random)
        val current = f.unlockable.first()
        f.unlock(current)
        f.musicPlayer.play(f.player, current)
        f.client.clearOutgoing()

        f.musicPlayer.skip(f.player)

        assertEquals(f.idOf(current), f.player.currMusicId)
        assertTrue(f.client.hasNone<MidiSongV2>())
    }

    @Test
    fun `skip does nothing outside shuffle mode or while music is muted`() {
        for (mode in listOf(MusicPlayMode.Area, MusicPlayMode.Manual, MusicPlayMode.Random)) {
            val f = Fixture(mode)
            val (current, other) = f.unlockable.take(2)
            f.unlock(current, other)
            f.musicPlayer.play(f.player, current)
            if (mode == MusicPlayMode.Random) {
                f.set("varbit.option_music_desktop", 0)
            }
            f.client.clearOutgoing()

            f.musicPlayer.skip(f.player)

            assertEquals(f.idOf(current), f.player.currMusicId, "$mode")
            assertTrue(f.client.hasNone<MidiSongV2>(), "$mode")
        }
    }

    @Test
    fun `area mode leaves the selected playlist unless playlist changes keep the mode`() {
        for (mode in MusicPlayMode.entries) {
            for (keepPlaylist in listOf(false, true)) {
                val f = Fixture(MusicPlayMode.Random)
                f.set("varbit.music_current_playlist", 2)
                f.set("varbit.dont_update_music_on_playlist_change", if (keepPlaylist) 1 else 0)

                f.musicPlayer.selectPlayMode(f.player, mode)

                val expected = if (mode == MusicPlayMode.Area && !keepPlaylist) 0 else 2
                assertEquals(mode.varValue, f.player.vars["varp.musicplay"])
                assertEquals(expected, f.player.selectedPlaylist, "$mode keep=$keepPlaylist")
            }
        }
    }

    @Test
    fun `selecting area mode while idle starts the current area's music`() {
        val f = Fixture(MusicPlayMode.Manual)
        val area = f.musicArea()
        f.musicPlayer.enterArea(f.player, area.first)
        assertEquals(0, f.player.currMusicId)

        f.musicPlayer.selectPlayMode(f.player, MusicPlayMode.Area)
        f.musicPlayer.playNextIfIdle(f.player)

        assertTrue(f.player.currMusicId in area.second.map(f::idOf))
    }

    @Test
    fun `idle start keeps a playing track and respects mute`() {
        val f = Fixture(MusicPlayMode.Area)
        val playing = f.unlockable.first()
        f.unlock(playing)
        f.musicPlayer.play(f.player, playing)
        f.musicPlayer.playNextIfIdle(f.player)
        assertEquals(f.idOf(playing), f.player.currMusicId)

        val muted = Fixture(MusicPlayMode.Manual)
        val area = muted.musicArea()
        muted.musicPlayer.enterArea(muted.player, area.first)
        muted.set("varbit.option_master_volume_desktop", 0)
        muted.musicPlayer.selectPlayMode(muted.player, MusicPlayMode.Area)
        muted.client.clearOutgoing()

        muted.musicPlayer.playNextIfIdle(muted.player)

        assertEquals(0, muted.player.currMusicId)
        assertTrue(muted.client.hasNone<MidiSongV2>())
    }

    private class Fixture(mode: MusicPlayMode) {
        val random = FixedRandom(0)
        val client = CaptureClient()
        val player = Player(client = client)
        val musicPlayer = MusicPlayer(random, repo)
        val unlockable = MusicRow.all().filter { repo.forRow(it)?.canUnlock == true }

        init {
            set("varbit.option_master_volume_desktop", 100)
            set("varbit.option_music_desktop", 100)
            set("varbit.music_unlock_text_toggle", 1)
            set("varp.musicplay", mode.varValue)
        }

        fun set(varName: String, value: Int) = VarPlayerIntMapSetter.set(player, varName, value)

        fun unlock(vararg rows: MusicRow) = rows.forEach { musicPlayer.unlock(player, it) }

        fun idOf(row: MusicRow): Int = checkNotNull(repo.forRow(row)).id

        fun musicArea(): Pair<String, List<MusicRow>> {
            val row =
                MusicModernRow.all().first { row ->
                    row.autoScript &&
                        row.tracks.isNotEmpty() &&
                        row.tracks.all { repo.forRow(it)?.canUnlock == true }
                }
            return "area.${row.area}" to row.tracks
        }
    }

    private companion object {
        private val Player.currMusicId by intVarBit("varbit.music_curr_id")
        private val Player.selectedPlaylist by intVarBit("varbit.music_current_playlist")

        private lateinit var repo: MusicRepository

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            repo = MusicRepository(FixedRandom(0)).also(MusicRepository::load)
        }
    }
}
