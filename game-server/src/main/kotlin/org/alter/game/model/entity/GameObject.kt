package org.alter.game.model.entity

import dev.openrune.ServerCacheManager
import dev.openrune.ServerCacheManager.getObject
import dev.openrune.definition.type.ObjectType
import dev.openrune.types.ObjectServerType
import gg.rsmod.util.toStringHelper
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeMap
import org.alter.game.model.entity.ObjectTimerMap
import org.alter.game.model.timer.TimerKey
import org.alter.game.model.timer.TimerMap
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType
import java.util.concurrent.ConcurrentHashMap

/**
 * A [GameObject] is any type of map object that can occupy a tile.
 *
 * @author Tom <rspsmods@gmail.com>
 */

object ObjectTimerMap {

    // Set of GameObjects that currently have active timers
    private val objectsWithTimers: MutableSet<GameObject> = ConcurrentHashMap.newKeySet()

    /**
     * Registers an object as having timers (called automatically when timers are added).
     */
    internal fun registerObject(obj: GameObject) {
        objectsWithTimers.add(obj)
    }

    /**
     * Unregisters an object if it no longer has timers.
     */
    internal fun unregisterIfEmpty(obj: GameObject) {
        if (!obj.hasTimers()) {
            objectsWithTimers.remove(obj)
        }
    }

    /**
     * Removes an object completely from the timer registry.
     */
    fun removeObject(obj: GameObject) {
        objectsWithTimers.remove(obj)
    }

    /**
     * Checks if an object has any active timers.
     */
    fun hasTimers(obj: GameObject): Boolean = obj.hasTimers()

    /**
     * Iterates over all objects that have timers.
     */
    fun forEach(block: (obj: GameObject) -> Unit) {
        objectsWithTimers.forEach { block(it) }
    }

    /**
     * Clears all tracked objects (but doesn't clear their timers).
     */
    fun clear() {
        objectsWithTimers.clear()
    }

    /**
     * Global tick update for all timers.
     * Call this every game cycle.
     */
    fun tick() {
        val iterator = objectsWithTimers.iterator()
        while (iterator.hasNext()) {
            val obj = iterator.next()
            val timers = obj.timers ?: continue

            val timerIterator = timers.getTimers().entries.iterator()
            while (timerIterator.hasNext()) {
                val entry = timerIterator.next()
                val remaining = entry.value - 1
                if (remaining <= 0) {
                    timerIterator.remove()
                } else {
                    timers[entry.key] = remaining
                }
            }

            // Remove from registry if no timers remain
            if (!timers.isNotEmpty) {
                iterator.remove()
            }
        }
    }
}
abstract class GameObject : Entity {

    /**
     * The object id.
     */
    val id: String

    /**
     * The object id.
     */
    internal val internalID: Int
        get() = RSCM.getRSCM(id)

    /**
     * A bit-packed byte that holds the object "type" and "rotation".
     */
    val settings: Byte

    /**
     * @see [AttributeMap]
     */
    val attr = AttributeMap()

    /**
     * The timer map for this object. Lazily initialized when first accessed.
     * Objects with timers are automatically registered in [ObjectTimerMap].
     */
    var timers: TimerMap? = null
        private set

    /**
     * Gets or creates the timer map for this object.
     */
    private fun getOrCreateTimers(): TimerMap {
        if (timers == null) {
            timers = TimerMap()
            ObjectTimerMap.registerObject(this)
        }
        return timers!!
    }

    /**
     * Checks if this object has any active timers.
     */
    fun hasTimers(): Boolean = timers?.isNotEmpty == true

    /**
     * Thanks to <a href="https://www.rune-server.ee/members/maxi/">Maxi</a> for this information:
     * <a href="https://www.rune-server.ee/runescape-development/rs2-client/configuration/462827-object-types-short-definitions.html">Object types short definitions</a>
     0	- straight walls, fences etc
     1	- diagonal walls corner, fences etc connectors
     2	- entire walls, fences etc corners
     3	- straight wall corners, fences etc connectors
     4	- straight inside wall decoration
     5	- straight outside wall decoration
     6	- diagonal outside wall decoration
     7	- diagonal inside wall decoration
     8	- diagonal in wall decoration
     9	- diagonal walls, fences etc
     10	- all kinds of objects, trees, statues, signs, fountains etc etc
     11	- ground objects like daisies etc
     12	- straight sloped roofs
     13	- diagonal sloped roofs
     14	- diagonal slope connecting roofs
     15	- straight sloped corner connecting roofs
     16	- straight sloped corner roof
     17	- straight flat top roofs
     18	- straight bottom egde roofs
     19	- diagonal bottom edge connecting roofs
     20	- straight bottom edge connecting roofs
     21	- straight bottom edge connecting corner roofs
     22	- ground decoration + map signs (quests, water fountains, shops etc)
     */
    val type: Int get() = settings.toInt() shr 2

    val rot: Int get() = settings.toInt() and 3

    private constructor(id: String, settings: Int, tile: Tile) {
        RSCM.requireRSCM(RSCMType.LOCTYPES,id)
        this.id = id
        this.settings = settings.toByte()
        this.tile = tile
    }

    constructor(id: String, type: Int, rot: Int, tile: Tile) : this(id, (type shl 2) or rot, tile)

    fun getDef(): ObjectServerType = getObject(internalID) ?: run {
        println("Object $internalID not found, using default NPC 0")
        getObject(0)!!
    }

    fun isSpawned(world: World): Boolean = world.isSpawned(this)

    /**
     * This method will get the "visually correct" object id for this npc from
     * [player]'s view point.
     *
     * Objects can change their appearance for each player depending on their
     * [ObjectType.transforms] and [ObjectType.varp]/[ObjectType.varbit].
     */
    fun getTransform(player: Player): Int {
        val world = player.world
        val def = getDef()

        if (def.varbit != -1) {
            val varbitDef = ServerCacheManager.getVarbit(def.varbit)?: return internalID
            val state = player.varps.getBit(varbitDef.varp, varbitDef.startBit, varbitDef.endBit)
            return def.transforms!![state]
        }

        if (def.varp != -1) {
            val state = player.varps.getState(def.varp)
            return def.transforms!![state]
        }

        return internalID
    }


    /**
     * Replaces this object in the world with another game object.
     *
     * If this object is dynamic, the replacement will also be dynamic.
     * Otherwise, a static replacement is spawned.
     *
     * @param world The [World] instance the object exists in.
     * @param obj The name or identifier of the replacement object.
     * @param removeAfter Optional: number of game ticks after which the replacement
     * should be removed. Default is 0 (permanent).
     * @param restoreOriginal Whether to restore the original object after [removeAfter] ticks. Default is true.
     * @return The newly created [GameObject] that replaced the original.
     */
    fun replaceWith(
        world: World,
        obj: String,
        removeAfter: Int = 0,
        restoreOriginal: Boolean = false
    ): GameObject {
        world.remove(this)

        val replacement: GameObject = when (this) {
            is DynamicObject -> DynamicObject(this, obj)
            else -> StaticObject(obj, type, rot, tile)
        }

        world.spawn(replacement)

        if (removeAfter > 0) {
            world.queue {
                wait(removeAfter)
                world.remove(replacement)
                if (restoreOriginal) {
                    world.spawn(this@GameObject)
                }
            }
        }

        return replacement
    }

    /**
     * Replaces this object in the world with another game object.
     *
     * The replacement will persist until the specified [TimerKey] expires.
     *
     * @param world The [World] instance the object exists in.
     * @param obj The name or identifier of the replacement object.
     * @param key The [TimerKey] controlling how long the replacement lasts.
     * @param restoreOriginal Whether to restore the original object after the timer expires. Default is true.
     * @return The newly created [GameObject] that replaced the original.
     */
    fun replaceWith(
        world: World,
        obj: String,
        key: TimerKey,
        restoreOriginal: Boolean = false
    ): GameObject {
        val replacement: GameObject = when (this) {
            is DynamicObject -> DynamicObject(obj, type, rot, tile)
            else -> StaticObject(obj, type, rot, tile)
        }

        world.remove(this)
        world.spawn(replacement)

        world.queue {
            repeatUntil(1,true,{false}) {
                if (replacement.getTimeLeft(key) == 0) {
                    world.remove(replacement)
                    if (restoreOriginal) {
                        world.spawn(this@GameObject)
                    }
                }
            }
        }

        return replacement
    }

    fun setTimer(key: TimerKey, value: Int) {
        val timerMap = getOrCreateTimers()
        timerMap[key] = value
    }

    fun getTimeLeft(key: TimerKey): Int {
        return timers?.getTimers()?.get(key) ?: 0
    }

    fun removeTimer(key: TimerKey) {
        timers?.remove(key)
        ObjectTimerMap.unregisterIfEmpty(this)
    }

    fun addTimer(key: TimerKey, amount: Int = 1) {
        val timerMap = getOrCreateTimers()
        val current = if (timerMap.exists(key)) timerMap[key] else 0

        timerMap[key] = current + amount
    }

    fun reduceTimer(key: TimerKey, amount: Int = 1) {
        val timerMap = timers ?: return
        if (!timerMap.exists(key)) return

        val newValue = timerMap[key] - amount
        if (newValue <= 0) {
            timerMap.remove(key)
            ObjectTimerMap.unregisterIfEmpty(this)
        } else {
            timerMap[key] = newValue
        }
    }

    override fun toString(): String =
        toStringHelper().add("id", internalID).add("type", type).add("rot", rot).add("tile", tile.toString()).toString()
}
