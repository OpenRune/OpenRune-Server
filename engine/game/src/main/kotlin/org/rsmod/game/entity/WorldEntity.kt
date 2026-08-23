package org.rsmod.game.entity

import org.rsmod.game.entity.worldentity.NoopWorldEntityInfo
import org.rsmod.game.entity.worldentity.WorldEntityInfoProtocol
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * A world entity is a movable instance rendered in the root world - e.g. a sailing boat. Its
 * "body" is a slab of instance land (the deck), anchored at [southWestZoneX]/[southWestZoneZ]
 * and spanning [sizeX] x [sizeZ] zones; entities standing on those instance-land coords are
 * automatically attributed to this world entity by the client protocol.
 *
 * The entity renders in the root world at fine-coordinate precision ([fineX]/[fineZ], 128 units
 * per tile) with a smooth [angle] (0..2047). All position and angle mutations must go through
 * [updateCoord]/[teleport]/[updateAngle] so the client info protocol stays in sync.
 */
public class WorldEntity(
    public val id: Int,
    public val sizeX: Int,
    public val sizeZ: Int,
    public val southWestZoneX: Int,
    public val southWestZoneZ: Int,
    fineX: Int,
    fineZ: Int,
    public val minLevel: Int = 0,
    public val maxLevel: Int = MAX_LEVEL,
    public val activeLevel: Int = 0,
    projectedLevel: Int = 0,
    angle: Int = 0,
    public val ownerIndex: Int = NPC_OWNER,
) {
    public var slotId: Int = INVALID_SLOT

    public var fineX: Int = fineX
        private set

    public var fineZ: Int = fineZ
        private set

    public var projectedLevel: Int = projectedLevel
        private set

    public var angle: Int = angle and MAX_ANGLE
        private set

    /**
     * The instanced region backing this world entity's deck, if one has been allocated. Used to
     * build the `REBUILD_WORLDENTITY` zone data; when `null`, the entity's sub-scene is empty.
     */
    public var region: Region? = null

    public var infoProtocol: WorldEntityInfoProtocol = NoopWorldEntityInfo

    /** The root-world coord grid this entity currently renders at. */
    public val coords: CoordGrid
        get() = CoordGrid(fineX shr FINE_BITS, fineZ shr FINE_BITS, projectedLevel)

    /**
     * Returns whether the given instance-land [coords] fall within this entity's deck area
     * (the zone rectangle anchored at [southWestZoneX]/[southWestZoneZ], spanning [minLevel]
     * to [maxLevel]). An entity standing on these coords is aboard this world entity.
     */
    public fun contains(coords: CoordGrid): Boolean {
        val zoneX = coords.x shr 3
        val zoneZ = coords.z shr 3
        return zoneX >= southWestZoneX &&
            zoneX < southWestZoneX + sizeX &&
            zoneZ >= southWestZoneZ &&
            zoneZ < southWestZoneZ + sizeZ &&
            coords.level in minLevel..maxLevel
    }

    /**
     * Updates the root-world render position of this entity. [teleport] jumps the entity to the
     * coordinate; otherwise the client interpolates the movement smoothly over the cycle.
     */
    public fun updateCoord(level: Int, fineX: Int, fineZ: Int, teleport: Boolean = false) {
        this.projectedLevel = level
        this.fineX = fineX
        this.fineZ = fineZ
        infoProtocol.updateCoord(level, fineX, fineZ, teleport)
    }

    public fun teleport(level: Int, fineX: Int, fineZ: Int): Unit =
        updateCoord(level, fineX, fineZ, teleport = true)

    /**
     * Updates the render angle of this entity. Note the client turns at most 128/2048 units per
     * game cycle, so large turns animate over multiple cycles.
     */
    public fun updateAngle(angle: Int) {
        this.angle = angle and MAX_ANGLE
        infoProtocol.updateAngle(this.angle)
    }

    override fun toString(): String =
        "WorldEntity(slot=$slotId, id=$id, coords=$coords, angle=$angle, " +
            "swZone=$southWestZoneX/$southWestZoneZ, size=${sizeX}x$sizeZ)"

    public companion object {
        public const val INVALID_SLOT: Int = -1

        /** Owner index for npc-owned entities (rsprot: `< 0` = npc-owned render priority). */
        public const val NPC_OWNER: Int = -1

        public const val MAX_LEVEL: Int = 3

        public const val MAX_ANGLE: Int = 2047

        /** Fine-coordinate units per tile. */
        public const val FINE_UNITS_PER_TILE: Int = 128

        private const val FINE_BITS: Int = 7

        /** Converts an absolute tile coordinate to the fine coordinate of the tile's center. */
        public fun tileToFine(tile: Int): Int =
            (tile * FINE_UNITS_PER_TILE) + (FINE_UNITS_PER_TILE / 2)
    }
}
