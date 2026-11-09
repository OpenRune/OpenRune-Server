package org.rsmod.api.player.output

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.outgoing.misc.player.ObjUnlockReset
import net.rsprot.protocol.game.outgoing.misc.player.ObjUnlockUpdate
import net.rsprot.protocol.game.outgoing.specific.ObjCustomiseSpecificV2
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import net.rsprot.protocol.internal.game.outgoing.info.CoordGrid as ProtocolCoordGrid

/**
 * Customises a ground item by its `obj.*` gameval for this client.
 * [coords] is the absolute tile, including its plane.
 * @see ObjCustomiseSpecificV2
 */
public fun Player.objCustomiseSpecific(
    coords: CoordGrid,
    obj: String,
    quantity: Int,
    model: Int = -1,
    recolours: Map<Int, Int> = emptyMap(),
    retextures: Map<Int, Int> = emptyMap(),
    colour: Int? = null,
): Unit = objCustomiseSpecific(
    coords,
    obj.asRSCM(RSCMType.OBJ),
    quantity,
    model,
    recolours,
    retextures,
    colour,
)

/**
 * Sends a visual override for the ground item matching [obj] and [quantity] at [coords].
 *
 * @param model Replacement model id, or `-1` to leave the model unspecified.
 * @param recolours Replacement colours keyed by the item's recolour indices.
 * @param retextures Replacement textures keyed by the item's retexture indices.
 * @param colour Colour to apply to the entire model, or `null` to omit it.
 */
public fun Player.objCustomiseSpecific(
    coords: CoordGrid,
    obj: Int,
    quantity: Int,
    model: Int = -1,
    recolours: Map<Int, Int> = emptyMap(),
    retextures: Map<Int, Int> = emptyMap(),
    colour: Int? = null,
) {
    client.write(
        ObjCustomiseSpecificV2(
            id = obj,
            quantity = quantity,
            model = model,
            recolours = recolours.toMap(),
            retextures = retextures.toMap(),
            colour = colour,
            coordGrid = ProtocolCoordGrid(coords.level, coords.x, coords.z),
        )
    )
}

/** Clears the client's item unlock flags. */
public fun Player.objUnlockReset() {
    client.write(ObjUnlockReset)
}

/**
 * Replaces the unlock flags for the supplied groups of 64 item ids.
 * Bit `b` in word `w` represents item id `w * 64 + b`. Set bits unlock items and
 * cleared bits lock them. Omitted words keep their current flags.
 */
public fun Player.objUnlockUpdate(words: Map<Int, Long>) {
    client.write(ObjUnlockUpdate(words.toMap()))
}
