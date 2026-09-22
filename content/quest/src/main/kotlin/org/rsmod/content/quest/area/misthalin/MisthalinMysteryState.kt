package org.rsmod.content.quest.area.misthalin

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid

internal object MisthalinStage {
    const val Accepted = 5

    const val Briefed = 10

    const val SeenShadyFigure = 15

    const val SidDead = 20

    const val BucketFilled = 25

    const val InsideManor = 30

    const val TaytenDead = 35

    const val Note1Read = 40

    const val PaintingSlashed = 45

    const val RubyRoomOpen = 50

    const val CandlesLit = 55

    const val FuseLit = 60

    const val WallBlown = 65

    const val LaceyDead = 70

    const val Note2Read = 75

    const val PianoSolved = 80

    const val EmeraldRoomOpen = 85

    const val MandyDead = 90

    const val Note3Read = 95

    const val PanelRevealed = 100

    const val GemPanelSolved = 105

    const val SapphireRoomOpen = 110

    const val BossArmed = 111

    const val BossBeaten = 115

    const val HeweyDead = 120

    const val AbigaleKilled = 125

    const val MandyWaiting = 130

    const val Complete = 135
}

internal val MisthalinCandleVarbits =
    listOf(
        "varbit.mistmyst_candle1",
        "varbit.mistmyst_candle2",
        "varbit.mistmyst_candle3",
        "varbit.mistmyst_candle4",
    )

internal var ProtectedAccess.mistmystCandles by intVarBit("varbit.mistmyst_candles")

internal var ProtectedAccess.mistmystPianoMask by intVarBit("varbit.mistmyst_piano_dead")

internal var ProtectedAccess.mistmystPianoAttempts by intVarBit("varbit.mistmyst_piano_attempts")

internal var ProtectedAccess.mistmystGemMask by intVarBit("varbit.mistmyst_gems_switched")

internal var ProtectedAccess.mistmystGemAttempts by intVarBit("varbit.mistmyst_switch_attempts")

internal var ProtectedAccess.mistmystXpAwarded by boolVarBit("varbit.mistmyst_xpreward")

internal val MisthalinPianoNoteVarbits =
    listOf(
        "varbit.mistmyst_piano_d1",
        "varbit.mistmyst_piano_e",
        "varbit.mistmyst_piano_a",
        "varbit.mistmyst_piano_d2",
    )

internal val MisthalinGemNoteVarbits =
    listOf(
        "varbit.mistmyst_sapphire_switched",
        "varbit.mistmyst_diamond_switched",
        "varbit.mistmyst_zenyte_switched",
        "varbit.mistmyst_emerald_switched",
        "varbit.mistmyst_onyx_switched",
        "varbit.mistmyst_ruby_switched",
    )

internal object MisthalinCoords {
    val LumbridgeShore = CoordGrid(3240, 3142, 0)

    val IslandLanding = CoordGrid(1637, 4802, 0)

    const val FrontDoorZ = 4824

    val FrontDoorLeft = CoordGrid(1636, FrontDoorZ, 0)
    val FrontDoorRight = CoordGrid(1637, FrontDoorZ, 0)

    val BrokenWallWest = CoordGrid(1647, 4829, 0)
    val BrokenWallEast = CoordGrid(1648, 4829, 0)

    val BossProxy = CoordGrid(1623, 4829, 0)

    val MirrorStart = CoordGrid(1622, 4828, 0)

    val BossWardrobes =
        listOf(
            BossWardrobe(CoordGrid(1627, 4831, 0), LocAngle.East, CoordGrid(1619, 4831, 0)),
            BossWardrobe(CoordGrid(1624, 4825, 0), LocAngle.South, CoordGrid(1624, 4834, 0)),
            BossWardrobe(CoordGrid(1619, 4828, 0), LocAngle.West, CoordGrid(1627, 4828, 0)),
            BossWardrobe(CoordGrid(1622, 4834, 0), LocAngle.North, CoordGrid(1622, 4825, 0)),
        )

    class BossWardrobe(val coords: CoordGrid, val angle: LocAngle, val wallEnd: CoordGrid)

    val StrayReflection = CoordGrid(1619, 4832, 0)

    val MirrorBoxX = 1622..1624
    val MirrorBoxZ = 4828..4831

    val MirrorBlockerSentinel = CoordGrid(1622, 4823, 0)
}

internal var Player.misthalinReturnTile by intVarp("varp.misthalin_return_tile")
internal var Player.misthalinMirrorBlocker by intVarp("varp.misthalin_mirror_blocker")
internal var Player.misthalinReflections by intVarBit("varbit.misthalin_reflections")
internal var Player.misthalinGemRowSlot by intVarBit("varbit.misthalin_gem_row_slot")

internal fun restoreMisthalinLogin(player: Player) {
    val saved = player.misthalinReturnTile
    if (saved == 0) return
    val destination = CoordGrid(saved)
    player.coords = if (destination.isOnMisthalinIsland()) destination else MisthalinCoords.IslandLanding
    player.misthalinReturnTile = 0
    val stage = player.vars["varbit.mistmyst_progress"]
    if (stage in MisthalinStage.BossArmed until MisthalinStage.AbigaleKilled) {
        VarPlayerIntMapSetter.set(player, "varbit.mistmyst_progress", MisthalinStage.SapphireRoomOpen)
    }
    val knife = "obj.mistmyst_cutscene_knife".asRSCM()
    for (inventory in listOf(player.inv, player.worn)) {
        for (slot in inventory.objs.indices) if (inventory[slot]?.id == knife) inventory[slot] = null
    }
}
