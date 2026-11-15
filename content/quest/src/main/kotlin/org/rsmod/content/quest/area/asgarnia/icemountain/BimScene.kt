package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.scene.SceneBlock
import org.rsmod.api.player.scene.SceneMontage
import org.rsmod.api.player.scene.SceneWindow
import org.rsmod.api.player.scene.SceneWindowView
import org.rsmod.game.region.zone.RegionZoneFlag
import org.rsmod.map.CoordGrid

/**
 * The workout montage packed into one large scene region. The opening has one window; the
 * Barbarian Village pushups and west run share one union table with two build-area vantages; the
 * palace start and mid-run do the same.
 *
 * The blocks are the same rotated chunks decoded from the OSRS rebuild packets; overlapping blocks
 * contain identical source zones, so the west block covers the shared part of the pushup table and
 * only the two pushup-only columns are copied separately.
 */
internal object BimScene {
    val Opening: SceneWindow =
        SceneWindow(
            name = "opening",
            flag = RegionZoneFlag.Flag1,
            blocks =
                listOf(
                    SceneBlock(
                        sourceX = 372,
                        sourceZ = 429,
                        zoneWidth = 7,
                        zoneLength = 5,
                        destX = 6,
                        destZ = 0,
                        rotation = 0,
                    ),
                    SceneBlock(
                        sourceX = 400,
                        sourceZ = 424,
                        zoneWidth = 8,
                        zoneLength = 7,
                        destX = 6,
                        destZ = 5,
                        rotation = 1,
                    ),
                ),
            views =
                mapOf(
                    "default" to
                        SceneWindowView(
                            name = "default",
                            buildCenterX = 6,
                            buildCenterZ = 6,
                            transformOffsetX = 0,
                            transformOffsetZ = 0,
                            sourceBase = CoordGrid(3200, 3344),
                            rotation = 1,
                        ),
                ),
            defaultView = "default",
        )

    val Barbarian: SceneWindow =
        SceneWindow(
            name = "barbarian",
            flag = RegionZoneFlag.Flag2,
            blocks =
                listOf(
                    SceneBlock(
                        sourceX = 373,
                        sourceZ = 426,
                        zoneWidth = 13,
                        zoneLength = 8,
                        destX = 6,
                        destZ = 24,
                        rotation = 0,
                    ),
                    SceneBlock(
                        sourceX = 386,
                        sourceZ = 426,
                        zoneWidth = 2,
                        zoneLength = 8,
                        destX = 19,
                        destZ = 24,
                        rotation = 0,
                    ),
                ),
            views =
                mapOf(
                    "pushup" to
                        SceneWindowView(
                            name = "pushup",
                            buildCenterX = 17,
                            buildCenterZ = 25,
                            transformOffsetX = 11,
                            transformOffsetZ = 19,
                            sourceBase = CoordGrid(3024, 3368),
                            rotation = 0,
                        ),
                    "west" to
                        SceneWindowView(
                            name = "west",
                            buildCenterX = 12,
                            buildCenterZ = 25,
                            transformOffsetX = 6,
                            transformOffsetZ = 19,
                            sourceBase = CoordGrid(2984, 3368),
                            rotation = 0,
                        ),
                ),
            defaultView = "pushup",
        )

    val Palace: SceneWindow =
        SceneWindow(
            name = "palace",
            flag = RegionZoneFlag.Flag4,
            blocks =
                listOf(
                    SceneBlock(
                        sourceX = 400,
                        sourceZ = 424,
                        zoneWidth = 8,
                        zoneLength = 12,
                        destX = 27,
                        destZ = 26,
                        rotation = 1,
                    ),
                ),
            views =
                mapOf(
                    "start" to
                        SceneWindowView(
                            name = "start",
                            buildCenterX = 27,
                            buildCenterZ = 32,
                            transformOffsetX = 21,
                            transformOffsetZ = 26,
                            sourceBase = CoordGrid(3160, 3344),
                            rotation = 1,
                        ),
                    "mid" to
                        SceneWindowView(
                            name = "mid",
                            buildCenterX = 32,
                            buildCenterZ = 32,
                            transformOffsetX = 21,
                            transformOffsetZ = 26,
                            sourceBase = CoordGrid(3160, 3344),
                            rotation = 1,
                        ),
                ),
            defaultView = "start",
        )

    val Montage: SceneMontage = SceneMontage.create(Opening, Barbarian, Palace)
}
