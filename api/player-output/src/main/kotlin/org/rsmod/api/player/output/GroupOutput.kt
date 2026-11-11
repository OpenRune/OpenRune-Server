package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.group.GroupFull
import net.rsprot.protocol.game.outgoing.group.GroupVar
import net.rsprot.protocol.game.outgoing.group.GroupVarInt
import net.rsprot.protocol.game.outgoing.group.GroupVarLong
import net.rsprot.protocol.game.outgoing.group.util.GroupVarUpdate
import org.rsmod.game.entity.Player

/**
 * Adds, replaces, or deletes groups in this client's group table.
 *
 * Group and member variables in [updates] must match the schema in the client cache.
 * The client uses that schema to read the values in the packet.
 */
public fun Player.updateGroups(updates: List<GroupFull.GroupUpdate>) {
    client.write(GroupFull(updates.toList()))
}

/**
 * Sends a batch of variable updates for groups or group members already known to the client.
 *
 * Use [updateGroups] to send full group data. Each entry in [updates] must use the value
 * type declared by the client's cache.
 */
public fun Player.updateGroupVars(updates: List<GroupVarUpdate<*>>) {
    client.write(GroupVar(updates.toList()))
}

/**
 * Sends one 32-bit integer update for a group or group member known to the client.
 *
 * [update] must identify a variable declared as an integer in the client cache.
 */
public fun Player.updateGroupVarInt(update: GroupVarUpdate<Int>) {
    client.write(GroupVarInt(update))
}

/**
 * Sends one 64-bit integer update for a group or group member known to the client.
 *
 * Use this packet for a cache-defined long variable even when its value fits in 32 bits.
 */
public fun Player.updateGroupVarLong(update: GroupVarUpdate<Long>) {
    client.write(GroupVarLong(update))
}
