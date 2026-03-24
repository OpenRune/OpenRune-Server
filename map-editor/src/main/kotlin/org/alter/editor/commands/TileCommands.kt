package org.alter.editor.commands

import org.alter.editor.model.RegionData

class PaintOverlayCommand(
    private val region: RegionData,
    private val heightLevel: Int,
    private val tiles: List<Triple<Int, Int, Short>>,  // x, z, oldOverlay
    private val newOverlay: Short,
) : Command {
    override val description = "Paint overlay"
    override fun execute() {
        tiles.forEach { (x, z, _) -> region.getTile(heightLevel, x, z).overlayId = newOverlay }
    }
    override fun undo() {
        tiles.forEach { (x, z, old) -> region.getTile(heightLevel, x, z).overlayId = old }
    }
}

class HeightCommand(
    private val region: RegionData,
    private val heightLevel: Int,
    private val tiles: List<Triple<Int, Int, Int>>,  // x, z, oldHeight
    private val delta: Int,
) : Command {
    override val description = "Change height"
    override fun execute() {
        tiles.forEach { (x, z, _) ->
            val tile = region.getTile(heightLevel, x, z)
            tile.height = (tile.height + delta).coerceIn(0, 255)
        }
    }
    override fun undo() {
        tiles.forEach { (x, z, old) -> region.getTile(heightLevel, x, z).height = old }
    }
}

class CollisionCommand(
    private val region: RegionData,
    private val heightLevel: Int,
    private val tiles: List<Triple<Int, Int, Boolean>>,  // x, z, wasBlocked
) : Command {
    override val description = "Toggle collision"
    override fun execute() {
        tiles.forEach { (x, z, was) -> region.getTile(heightLevel, x, z).setBlocked(!was) }
    }
    override fun undo() {
        tiles.forEach { (x, z, was) -> region.getTile(heightLevel, x, z).setBlocked(was) }
    }
}
