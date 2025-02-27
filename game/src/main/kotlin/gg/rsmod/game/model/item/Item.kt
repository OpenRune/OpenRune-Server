package gg.rsmod.game.model.item

import com.google.common.base.MoreObjects
import dev.openrune.cache.CacheManager.item

/**
 * @author Tom <rspsmods@gmail.com>
 */
class Item(val id: Int, var amount: Int = 1) {

    constructor(other: Item) : this(other.id, other.amount) {
        copyAttr(other)
    }

    constructor(other: Item, amount: Int) : this(other.id, amount) {
        copyAttr(other)
    }

    val attr = mutableMapOf<ItemAttribute, Int>()

    /**
     * Returns a <strong>new</strong> [Item] with the noted link as the item id.
     * If this item does not have a noted link item id, it will return a new [Item]
     * with the same [Item.id].
     */
    fun toNoted(): Item {
        val def = getDef()
        return if (def.noteTemplateId == 0 && def.noteLinkId > 0) Item(def.noteLinkId, amount).copyAttr(this) else Item(this).copyAttr(this)
    }

    /**
     * Returns a <strong>new</strong> [Item] with the unnoted link as the item id.
     * If this item does not have a unnoted link item id, it will return a new [Item]
     * with the same [Item.id].
     */
    fun toUnnoted(): Item {
        val def = getDef()
        return if (def.noteTemplateId > 0) Item(def.noteLinkId, amount).copyAttr(this) else Item(this).copyAttr(this)
    }

    /**
     * Get the name of this item. If this item is noted this method will use
     * its un-noted template and get the name for said template.
     */
    fun getName(): String = toUnnoted().getDef().name

    fun getDef() = item(id)

    /**
     * Returns true if [attr] contains any value.
     */
    fun hasAnyAttr(): Boolean = attr.isNotEmpty()

    fun getAttr(attrib: ItemAttribute): Int? = attr[attrib]

    fun putAttr(attrib: ItemAttribute, value: Int): Item {
        attr[attrib] = value
        return this
    }

    /**
     * Copies the [Item.attr] map from [other] to this.
     */
    fun copyAttr(other: Item): Item {
        if (other.hasAnyAttr()) {
            attr.putAll(other.attr)
        }
        return this
    }

    override fun toString(): String = MoreObjects.toStringHelper(this).add("id", id).add("amount", amount).toString()
}