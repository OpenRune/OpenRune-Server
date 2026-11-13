package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.scene.SceneMontage
import org.rsmod.api.player.scene.SceneWindow
import org.rsmod.game.region.zone.RegionZoneFlag
import org.rsmod.map.CoordGrid

/**
 * The workout montage packed into one large scene region. Window origins are chosen so the decoded
 * OSRS destination tables do not overlap; the build-area centre, table placement and content
 * transform are all shifted by the same origin, so the scene composition is unchanged.
 */
internal object BimScene {
    val Opening: SceneWindow =
        SceneWindow(
            name = "opening",
            flag = RegionZoneFlag.Flag1,
            buildCenterX = 6,
            buildCenterZ = 6,
            transformOffsetX = 0,
            transformOffsetZ = 0,
            sourceBase = CoordGrid(3200, 3344),
            rotation = 1,
            table = shift(BimCutsceneMaps.VARROCK_SQUARE, -6, -6),
        )

    val Pushup: SceneWindow =
        SceneWindow(
            name = "pushup",
            flag = RegionZoneFlag.Flag2,
            buildCenterX = 32,
            buildCenterZ = 6,
            transformOffsetX = 26,
            transformOffsetZ = 0,
            sourceBase = CoordGrid(3024, 3368),
            rotation = 0,
            table = shift(BimCutsceneMaps.BARBARIAN_PUSHUP, 20, -6),
        )

    val West: SceneWindow =
        SceneWindow(
            name = "west",
            flag = RegionZoneFlag.Flag3,
            buildCenterX = 6,
            buildCenterZ = 32,
            transformOffsetX = 0,
            transformOffsetZ = 26,
            sourceBase = CoordGrid(2984, 3368),
            rotation = 0,
            table = shift(BimCutsceneMaps.BARBARIAN_WEST, -6, 20),
        )

    val Palace: SceneWindow =
        SceneWindow(
            name = "palace",
            flag = RegionZoneFlag.Flag4,
            buildCenterX = 27,
            buildCenterZ = 32,
            transformOffsetX = 21,
            transformOffsetZ = 26,
            sourceBase = CoordGrid(3160, 3344),
            rotation = 1,
            table = shift(BimCutsceneMaps.VARROCK_PALACE, 15, 20),
        )

    val Montage: SceneMontage = SceneMontage.create(Opening, Pushup, West, Palace)

    const val PALACE_START_CENTER_X: Int = 27
    const val PALACE_START_CENTER_Z: Int = 32
    const val PALACE_MID_CENTER_X: Int = 32
    const val PALACE_MID_CENTER_Z: Int = 32

    private fun shift(entries: IntArray, x: Int, z: Int): IntArray {
        val shifted = entries.copyOf()
        var index = 0
        while (index < shifted.size) {
            shifted[index] += x
            shifted[index + 1] += z
            index += 5
        }
        return shifted
    }
}
