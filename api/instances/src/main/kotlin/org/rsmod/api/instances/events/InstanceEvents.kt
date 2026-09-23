package org.rsmod.api.instances.events

import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.events.KeyedEvent
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public fun instanceEventId(key: String): Long = key.hashCode().toLong()

public data class InstancePlayerJoinEvent(
    public val player: Player,
    public val key: String,
    public val instanceId: InstanceId,
    public val ownerId: Long,
    override val id: Long = instanceEventId(key),
) : KeyedEvent {
    public val isOwner: Boolean
        get() = player.uuid == ownerId
}

public data class InstancePlayerLeaveEvent(
    public val player: Player,
    public val key: String,
    public val instanceId: InstanceId,
    public val ownerId: Long,
    override val id: Long = instanceEventId(key),
) : KeyedEvent {
    public val isOwner: Boolean
        get() = player.uuid == ownerId
}

public data class InstancePlayerJoinUnboundEvent(
    public val player: Player,
    public val key: String,
    public val instanceId: InstanceId,
    public val ownerId: Long,
) : UnboundEvent

public data class InstancePlayerLeaveUnboundEvent(
    public val player: Player,
    public val key: String,
    public val instanceId: InstanceId,
    public val ownerId: Long,
) : UnboundEvent

public data class InstanceStartedEvent(
    public val key: String,
    public val instanceId: InstanceId,
    public val ownerId: Long,
    public val spec: InstanceSpec,
    public val startedAtTick: Int,
    override val id: Long = instanceEventId(key),
) : KeyedEvent

public data class InstanceEndedEvent(
    public val key: String,
    public val instanceId: InstanceId,
    public val ownerId: Long,
    public val spec: InstanceSpec,
    override val id: Long = instanceEventId(key),
) : KeyedEvent

public data class InstanceTimeTickEvent(
    public val key: String,
    public val instanceId: InstanceId,
    public val ownerId: Long,
    public val spec: InstanceSpec,
    public val currentTick: Int,
    public val startedAtTick: Int,
    public val elapsedTicks: Int,
    public val remainingTicks: Int,
    override val id: Long = instanceEventId(key),
) : KeyedEvent
