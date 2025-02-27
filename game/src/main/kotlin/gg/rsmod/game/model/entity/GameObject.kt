package gg.rsmod.game.model.entity

import com.google.common.base.MoreObjects
import dev.openrune.cache.CacheManager.objects
import dev.openrune.cache.CacheManager.varbit
import dev.openrune.cache.filestore.definition.data.ObjectDefinition
import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.World
import gg.rsmod.game.model.attr.AttributeMap
import gg.rsmod.game.model.timer.TimerMap

/**
 * A [GameObject] is any type of map object that can occupy a tile.
 *
 * @author Tom <rspsmods@gmail.com>
 */
abstract class GameObject : Entity {

    /**
     * The object id.
     */
    val id: Int

    /**
     * A bit-packed byte that holds the object "type" and "rotation".
     */
    val settings: Byte

    /**
     * @see [AttributeMap]
     */
    val attr = AttributeMap()

    /**
     * @see [TimerMap]
     */
    val timers = TimerMap()

    val type: Int get() = settings.toInt() shr 2

    val rot: Int get() = settings.toInt() and 3

    private constructor(id: Int, settings: Int, tile: Tile) {
        this.id = id
        this.settings = settings.toByte()
        this.tile = tile
    }

    constructor(id: Int, type: Int, rot: Int, tile: Tile) : this(id, (type shl 2) or rot, tile)

    fun getDef(): ObjectDefinition = objects(id)

    fun isSpawned(world: World): Boolean = world.isSpawned(this)

    /**
     * This method will get the "visually correct" object id for this npc from
     * [player]'s view point.
     *
     * Objects can change their appearance for each player depending on their
     * [ObjectDef.transforms] and [ObjectDef.varp]/[ObjectDef.varbit].
     */
    fun getTransform(player: Player): Int {
        val world = player.world
        val def = getDef()

        if (def.varbit != -1) {
            val varbitDef = varbit(def.varbit)
            val state = player.varps.getBit(varbitDef.varp, varbitDef.startBit, varbitDef.endBit)
            return def.transforms!![state]
        }

        if (def.varp != -1) {
            val state = player.varps.getState(def.varp)
            return def.transforms!![state]
        }

        return id
    }

    fun getRotatedWidth(): Int = when {
        (rot and 0x1) == 1 -> getDef().length
        else -> getDef().width
    }

    fun getRotatedLength(): Int = when {
        (rot and 0x1) == 1 -> getDef().width
        else -> getDef().length
    }

    override fun toString(): String = MoreObjects.toStringHelper(this).add("id", id).add("type", type).add("rot", rot).add("tile", tile.toString()).toString()
}