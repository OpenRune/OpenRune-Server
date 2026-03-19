package org.rsmod.api.music

import dev.openrune.area
import dev.openrune.types.aconverted.AreaType
import dev.openrune.types.varp.VarpServerType
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import jakarta.inject.Inject
import org.rsmod.api.music.configs.music_varps
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.MusicClassicRow
import org.rsmod.api.table.MusicModernRow
import org.rsmod.api.table.MusicRow

public class MusicRepository @Inject constructor(private val random: GameRandom) {
    private lateinit var musicRows: Int2ObjectMap<Music>
    private lateinit var musicIds: Int2ObjectMap<Music>

    private lateinit var modernAreas: Int2ObjectMap<List<Music>>
    private lateinit var classicAreas: Int2ObjectMap<Music>

    public fun forRow(row: MusicRow): Music? = musicRows[row.rowId]

    public fun forId(id: Int): Music? = musicIds[id]

    public fun getModernArea(area: AreaType): List<Music>? {
        return modernAreas[area.id]
    }

    public fun getClassicArea(area: AreaType): Music? {
        return classicAreas[area.id]
    }

    public fun getAll(): Collection<Music> {
        return musicRows.values
    }

    public fun load() {
        val unlockVarps = unlockVarps()

        val musicRows = loadMusicRows(unlockVarps)
        this.musicRows = Int2ObjectOpenHashMap(musicRows)

        val musicSlots = mapMusicById(musicRows)
        this.musicIds = Int2ObjectOpenHashMap(musicSlots)

        val modernAreas = loadModernAreas(musicRows)
        this.modernAreas = Int2ObjectOpenHashMap(modernAreas)

        val classicAreas = loadClassicAreas(musicRows)
        this.classicAreas = Int2ObjectOpenHashMap(classicAreas)
    }

    private fun loadMusicRows(unlockVarps: List<VarpServerType>): Map<Int, Music> {
        val rows = MusicRow.all()
        val mapped = mutableMapOf<Int, Music>()
        var currId = 1
        for (row in rows) {
            val displayName = row.displayname
            val unlockHint = row.unlockhint
            val midi = row.midi
            val variable = row.variable
            val duration = row.duration
            val hidden = row.hidden
            val secondary = row.secondaryTrack
            var unlockVarp: VarpServerType? = null
            var unlockBitpos = -1
            if (variable.isNotEmpty()) {
                unlockVarp = unlockVarps.getOrNull(variable[0] - 1)
                unlockBitpos = variable[1]
            }
            val music =
                Music(
                    id = currId++,
                    displayName = displayName,
                    unlockHint = unlockHint,
                    duration = duration,
                    midi = midi,
                    unlockVarp = unlockVarp,
                    unlockBitpos = unlockBitpos,
                    hidden = hidden ?: false,
                    secondary = secondary,
                )
            mapped[row.rowId] = music
        }
        return mapped
    }

    private fun mapMusicById(musicRows: Map<Int, Music>): Map<Int, Music> {
        return musicRows.values.associateBy(Music::id)
    }

    private fun loadModernAreas(musicRows: Map<Int, Music>): Map<Int, List<Music>> {
        val grouped = mutableMapOf<Int, MutableList<Music>>()

        MusicModernRow.all().forEach {
            val area = area(it.area)
            val trackRows = it.tracks
            val musicList = ArrayList<Music>(trackRows.size)
            for (trackRow in trackRows) {
                val musicRow = MusicRow.getRow(trackRow.rowId)
                val music = musicRows[musicRow.rowId]
                if (music == null) {
                    throw IllegalStateException("Music row not found: '${musicRow.displayname}'")
                }
                musicList += music
            }
            val mappedList = grouped.computeIfAbsent(area.id) { mutableListOf() }
            mappedList += musicList
        }

        return grouped
    }

    private fun loadClassicAreas(musicRows: Map<Int, Music>): Map<Int, Music> {
        val areas = mutableMapOf<Int, Music>()

        MusicClassicRow.all().forEach {
            error("Add Classic Music")

            //            val area = it.area
            //            if (area.id in areas) {
            //                val message =
            //                    "Classic music area can only be mapped to a " +
            //                        "single track: '${area}' (row=${it.id})"
            //                throw IllegalStateException(message)
            //            }
            //
            //            val music = musicRows[it.track]
            //            if (music == null) {
            //                throw IllegalStateException("Music row not found: '${it.id}'")
            //            }
            // areas[area.id] = music
        }

        return areas
    }

    private fun unlockVarps(): List<VarpServerType> =
        listOf(
            music_varps.multi_1,
            music_varps.multi_2,
            music_varps.multi_3,
            music_varps.multi_4,
            music_varps.multi_5,
            music_varps.multi_6,
            music_varps.multi_7,
            music_varps.multi_8,
            music_varps.multi_9,
            music_varps.multi_10,
            music_varps.multi_11,
            music_varps.multi_12,
            music_varps.multi_13,
            music_varps.multi_14,
            music_varps.multi_15,
            music_varps.multi_16,
            music_varps.multi_17,
            music_varps.multi_18,
            music_varps.multi_19,
            music_varps.multi_20,
            music_varps.multi_21,
            music_varps.multi_22,
            music_varps.multi_23,
            music_varps.multi_24,
            music_varps.multi_25,
        )
}
