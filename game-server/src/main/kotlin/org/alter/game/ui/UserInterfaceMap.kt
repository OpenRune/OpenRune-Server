package org.alter.game.ui

import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.IntArraySet
import net.rsprot.protocol.util.CombinedId
import org.alter.game.ui.collection.ComponentEventMap
import org.alter.game.ui.collection.ComponentTargetMap
import org.alter.game.ui.collection.ComponentTranslationMap
import org.alter.game.ui.type.IfEvent
import org.alter.rscm.RSCM.asRSCM

public class UserInterfaceMap(
    public var topLevel: UserInterface = UserInterface.NULL,
    public val overlays: ComponentTargetMap = ComponentTargetMap(),
    public val modals: ComponentTargetMap = ComponentTargetMap(),
    public val events: ComponentEventMap = ComponentEventMap(),
    public val gameframe: ComponentTranslationMap = ComponentTranslationMap(),
    public val closeQueue: MutableSet<String> = emptySet<String>().toMutableSet()
) {
    @InternalApi public var closeModal: Boolean = false

    public var frameResizable: Boolean = false

    public var frameWidth: Int = 0
        private set

    public var frameHeight: Int = 0
        private set

    public fun queueClose(target: String) {
        closeQueue.add(target)
    }

    public fun removeQueuedCloseSub(target: String) {
        closeQueue.remove(target)
    }

    public operator fun contains(type: String): Boolean {
        return containsModal(type) ||
                containsOverlay(type) ||
                containsTopLevel(type) ||
                containsGameframe(type)
    }

    public fun containsTopLevel(topLevel: String): Boolean = this.topLevel.id == topLevel

    public fun containsOverlay(overlay: String): Boolean =
        overlays.backing.containsValue(overlay)

    public fun containsModal(modal: String): Boolean = modals.backing.containsValue(modal)

    public fun containsGameframe(type: String): Boolean =
        gameframe.backing.containsValue(type)

    public fun getOverlay(key: String): String? = overlays.backing[key]

    public fun getOverlayOrNull(key: String): String? =
        runCatching { getOverlay(key) }.getOrNull()

    public fun getModal(key: String): String? = modals.backing[key]

    public fun getModalOrNull(key: String): String? =
        runCatching { getModal(key) }.getOrNull()

    public fun getGameframe(key: String): String? = gameframe.backing.get(key)

    public fun getGameframeOrNull(key: String): Component? =
        getGameframe(key)?.toIntOrNull()?.let { Component(it) }

    public fun hasEvent(component: CombinedId, slot: Int, event: IfEvent): Boolean {
        val events = events[component, slot]
        return (events and event.bitmask) != 0L
    }

    private fun Component.orNull(): Component? = if (this == Component.NULL) null else this

    private fun Int2IntMap.get(key: String): Component {
        val packed = getOrDefault(key.asRSCM(), null) ?: return Component.NULL
        return Component(packed)
    }

    public fun setWindowStatus(width: Int, height: Int, resizable: Boolean) {
        this.frameWidth = width
        this.frameHeight = height
        this.frameResizable = resizable
    }
}
