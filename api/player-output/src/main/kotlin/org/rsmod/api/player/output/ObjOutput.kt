package org.rsmod.api.player.output

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import net.rsprot.protocol.game.outgoing.misc.player.ObjUnlockReset
import net.rsprot.protocol.game.outgoing.misc.player.ObjUnlockUpdate
import net.rsprot.protocol.game.outgoing.specific.ObjAddSpecific
import net.rsprot.protocol.game.outgoing.specific.ObjCountSpecific
import net.rsprot.protocol.game.outgoing.specific.ObjCustomiseSpecificV2
import net.rsprot.protocol.game.outgoing.specific.ObjDelSpecific
import net.rsprot.protocol.game.outgoing.specific.ObjEnabledOpsSpecific
import net.rsprot.protocol.game.outgoing.specific.ObjUncustomiseSpecific
import net.rsprot.protocol.game.outgoing.util.OpFlags
import net.rsprot.protocol.internal.game.outgoing.info.CoordGrid as ProtocolCoordGrid
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * Displays a ground-item stack at [coords] for this client. Use the object registry for items
 * that players should be able to pick up.
 *
 * @param coords Absolute tile, including its plane.
 * @param quantity Number of items to display in the stack.
 * @param despawnCycles Lifetime in game cycles.
 * @param opFlags The right-click options to show. Build the mask with [OpFlags].
 */
public fun Player.objAddSpecific(
    coords: CoordGrid,
    obj: Int,
    quantity: Int,
    despawnCycles: Int,
    opFlags: Byte = OpFlags.ALL_SHOWN,
) {
    require(obj in 0..65535 && quantity > 0 && despawnCycles in 0..65535)
    client.write(
        ObjAddSpecific(
            id = obj,
            quantity = quantity,
            coordGrid = coords.toProtocolCoord(),
            opFlags = opFlags,
            timeUntilPublic = 0,
            timeUntilDespawn = despawnCycles,
            ownershipType = 0,
            neverBecomesPublic = true,
        ),
    )
}

/**
 * Removes a displayed ground-item stack from this client's view.
 *
 * The client matches [obj] and [quantity] at the absolute tile and plane in [coords].
 * Use the object registry when removing the actual gameplay item.
 */
public fun Player.objDelSpecific(coords: CoordGrid, obj: Int, quantity: Int) {
    require(obj in 0..65535 && quantity > 0)
    client.write(ObjDelSpecific(obj, quantity, coords.toProtocolCoord()))
}

/**
 * Updates a ground-item stack in this client's view from [oldQuantity] to [newQuantity].
 *
 * The old quantity identifies the existing stack together with [obj] and [coords]. Both
 * quantities must be positive. Use [objDelSpecific] to remove the displayed stack.
 */
public fun Player.objCountSpecific(coords: CoordGrid, obj: Int, oldQuantity: Int, newQuantity: Int) {
    require(obj in 0..65535 && oldQuantity > 0 && newQuantity > 0)
    client.write(ObjCountSpecific(obj, oldQuantity, newQuantity, coords.toProtocolCoord()))
}

/**
 * Updates the right-click options on the first displayed ground item matching [obj] at [coords].
 *
 * [opFlags] controls client visibility only. Gameplay interaction permissions still come from
 * the server's item handlers.
 */
public fun Player.objEnabledOpsSpecific(coords: CoordGrid, obj: Int, opFlags: Byte) {
    require(obj in 0..65535)
    client.write(ObjEnabledOpsSpecific(obj, opFlags, coords.toProtocolCoord()))
}

/**
 * Restores the cache-defined model, colours, and textures for this client's ground-item view.
 *
 * Pass the same [coords], [obj], and [quantity] used to identify the customised stack.
 */
public fun Player.objUncustomiseSpecific(coords: CoordGrid, obj: Int, quantity: Int) {
    require(obj in 0..65535 && quantity > 0)
    client.write(ObjUncustomiseSpecific(obj, quantity, coords.toProtocolCoord()))
}

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
            coordGrid = coords.toProtocolCoord(),
        )
    )
}

private fun CoordGrid.toProtocolCoord(): ProtocolCoordGrid = ProtocolCoordGrid(level, x, z)

/**
 * Clears every item unlock flag currently tracked by the client.
 *
 * Use [objUnlockUpdate] after clearing to send a fresh set of unlocked items.
 */
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
