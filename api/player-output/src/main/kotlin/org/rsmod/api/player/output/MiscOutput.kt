package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.logout.LogoutWithReason
import net.rsprot.protocol.game.outgoing.map.RebuildWorldEntityV4
import net.rsprot.protocol.game.outgoing.misc.client.HintArrow
import net.rsprot.protocol.game.outgoing.misc.client.PacketGroupStart
import net.rsprot.protocol.game.outgoing.misc.client.ResetInteractionMode
import net.rsprot.protocol.game.outgoing.misc.client.ServerTickEnd
import net.rsprot.protocol.game.outgoing.misc.client.SetInteractionMode
import net.rsprot.protocol.game.outgoing.misc.client.SetWebsiteToken
import net.rsprot.protocol.game.outgoing.misc.client.UpdateRebootTimerV2
import net.rsprot.protocol.game.outgoing.misc.player.AccountFlags
import net.rsprot.protocol.game.outgoing.misc.player.SetPlayerOp
import net.rsprot.protocol.message.OutgoingGameMessage
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public object MiscOutput {
    /**
     * Sends the account feature flags for this client session.
     *
     * [flags] is the complete 64-bit mask. Each set bit enables its corresponding client
     * feature, as defined by [AccountFlags].
     */
    public fun accountFlags(player: Player, flags: Long) {
        player.client.write(AccountFlags(flags))
    }

    /**
     * Sends [messages] as a group the client processes together in one cycle after it receives
     * the full group.
     *
     * The encoded size of all messages, including their packet headers, must fit in 32,767 bytes.
     */
    public fun packetGroup(player: Player, messages: List<OutgoingGameMessage>) {
        player.client.write(PacketGroupStart(messages.toList()))
    }

    /**
     * Changes which interactions the client offers in the specified world.
     *
     * @param worldId The world to override, or `-2` to change the default for all worlds.
     * @param tileInteractionMode `0` disables tile clicks, `1` walks, and `2` selects a heading.
     * @param entityInteractionMode `0` disables entity clicks, `1` enables them, and `2` allows examine only.
     */
    public fun setInteractionMode(
        player: Player,
        worldId: Int,
        tileInteractionMode: Int,
        entityInteractionMode: Int,
    ) {
        require(worldId == DEFAULT_WORLD_ID || worldId in 0..Short.MAX_VALUE)
        require(tileInteractionMode in 0..2 && entityInteractionMode in 0..2)
        player.client.write(SetInteractionMode(worldId, tileInteractionMode, entityInteractionMode))
    }

    /**
     * Clears the interaction-mode override for [worldId].
     *
     * The client can then use its default tile and entity interaction modes for that world.
     */
    public fun resetInteractionMode(player: Player, worldId: Int) {
        require(worldId in 0..Short.MAX_VALUE) { "Invalid world id: $worldId" }
        player.client.write(ResetInteractionMode(worldId))
    }

    /**
     * Sends the map zones that the client should render for a world entity.
     *
     * @param base Absolute base tile in instance land. The packet uses its X and Z coordinates.
     * @param sizeX Width in zones, from 0 through 12.
     * @param sizeZ Length in zones, from 0 through 12.
     * @param zones Supplies the map data by absolute zone coordinate and plane.
     */
    public fun rebuildWorldEntity(
        player: Player,
        base: CoordGrid,
        sizeX: Int,
        sizeZ: Int,
        zones: RebuildWorldEntityV4.RebuildWorldEntityZoneProvider,
    ) {
        player.client.write(RebuildWorldEntityV4(base.x, base.z, sizeX, sizeZ, zones))
    }

    /**
     * Sends an issued website session [token] to the client for its display-name change flow.
     *
     * The caller supplies the token for the current player's website session.
     */
    public fun setWebsiteToken(player: Player, token: String) {
        player.client.write(SetWebsiteToken(token))
    }

    /** @see [HintArrow] */
    public fun hintArrowNpc(player: Player, npcSlot: Int) {
        player.client.write(HintArrow(HintArrow.NpcHintArrow(npcSlot)))
    }

    public fun hintArrowTile(player: Player, coords: CoordGrid, height: Int) {
        val position = HintArrow.TileHintArrow.HintArrowTilePosition.CENTER
        player.client.write(HintArrow(HintArrow.TileHintArrow(coords.x, coords.z, height, position)))
    }

    public fun hintArrowReset(player: Player) {
        player.client.write(HintArrow(HintArrow.ResetHintArrow))
    }

    /** @see [SetPlayerOp] */
    public fun setPlayerOp(player: Player, slot: Int, op: String?, priority: Boolean = false) {
        player.options.add(slot, op)
        player.client.write(SetPlayerOp(slot, priority, op))
    }

    public fun findPlayerOption(player: Player, query: String): Int? {
        val index = player.options.indexOfFirst { it == query }
        return if (index >= 0) index else null
    }

    public fun clearPlayerOp(player: Player, slot: Int, query: String) {
        val optionIdx = findPlayerOption(player, query)
        if (optionIdx == slot) {
            setPlayerOp(player, slot, null)
        }
    }

    /** @see [ServerTickEnd] */
    public fun serverTickEnd(player: Player) {
        player.client.write(ServerTickEnd)
    }

    /** Calls [LogoutWithReason] with an arg of `0` (reason = `Requested`). */
    public fun logout(player: Player) {
        resetInteractionMode(player)
        player.client.write(LogoutWithReason(reason = 0))
    }

    /** Calls [LogoutWithReason] with an arg of `1` (reason = `Kicked`). */
    public fun logoutKicked(player: Player) {
        resetInteractionMode(player)
        player.client.write(LogoutWithReason(reason = 1))
    }

    /** Calls [LogoutWithReason] with an arg of `2` (reason = `Updating`). */
    public fun logoutUpdating(player: Player) {
        resetInteractionMode(player)
        player.client.write(LogoutWithReason(reason = 2))
    }

    private fun resetInteractionMode(player: Player) {
        setInteractionMode(
            player,
            worldId = DEFAULT_WORLD_ID,
            tileInteractionMode = TILE_INTERACTION_WALK,
            entityInteractionMode = ENTITY_INTERACTION_ALL,
        )
    }

    /**
     * Displays the client's update countdown with [message].
     *
     * [cycles] is measured in 600 ms game cycles. A value of zero clears the countdown.
     *
     * @see UpdateRebootTimerV2
     */
    public fun updateRebootTimer(player: Player, cycles: Int, message: String = "") {
        require(cycles in 0..65535) { "`cycles` must be within range [0..65535]. (cycles=$cycles)" }
        if (message.isEmpty()) {
            player.client.write(UpdateRebootTimerV2(cycles, UpdateRebootTimerV2.SetUpdateMessage("")))
        } else {
            player.client.write(UpdateRebootTimerV2(cycles, UpdateRebootTimerV2.SetUpdateMessage(message)))
        }
    }

    /**
     * Clears the client's update countdown by sending a timer of zero game cycles.
     *
     * Use this when a pending update is cancelled or the countdown should disappear.
     */
    public fun clearUpdateRebootTimer(player: Player) {
        updateRebootTimer(player, cycles = 0)
    }

    private const val DEFAULT_WORLD_ID = -2
    private const val TILE_INTERACTION_WALK = 1
    private const val ENTITY_INTERACTION_ALL = 1
}
