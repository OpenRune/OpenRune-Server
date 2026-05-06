package org.rsmod.api.music

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.VarpServerType
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import jakarta.inject.Inject
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

    public fun getModernArea(area: String): List<Music>? {
        return modernAreas[area.asRSCM(RSCMType.AREA)]
    }

    public fun getClassicArea(area: String): Music? {
        return classicAreas[area.asRSCM(RSCMType.AREA)]
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

    private fun loadMusicRows(unlockVarps: List<String>): Map<Int, Music> {
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
            var unlockVarp: String? = null
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
            val area = "area.${it.area}".asRSCM(RSCMType.AREA)
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
            val mappedList = grouped.computeIfAbsent(area) { mutableListOf() }
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

    private fun unlockVarps(): List<String> =
        listOf(
            "varp.musicmulti_1",
            "varp.musicmulti_2",
            "varp.musicmulti_3",
            "varp.musicmulti_4",
            "varp.musicmulti_5",
            "varp.musicmulti_6",
            "varp.musicmulti_7",
            "varp.musicmulti_8",
            "varp.musicmulti_9",
            "varp.musicmulti_10",
            "varp.musicmulti_11",
            "varp.musicmulti_12",
            "varp.musicmulti_13",
            "varp.musicmulti_14",
            "varp.musicmulti_15",
            "varp.musicmulti_16",
            "varp.musicmulti_17",
            "varp.musicmulti_18",
            "varp.musicmulti_19",
            "varp.musicmulti_20",
            "varp.musicmulti_21",
            "varp.musicmulti_22",
            "varp.musicmulti_23",
            "varp.musicmulti_24",
            "varp.musicmulti_25",
            "varp.musicmulti_26",
            "varp.musicmulti_27",
        )
}
