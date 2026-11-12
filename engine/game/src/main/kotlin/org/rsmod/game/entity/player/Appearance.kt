package org.rsmod.game.entity.player

import dev.openrune.types.BasType
import dev.openrune.types.NpcServerType
import dev.openrune.util.Wearpos

public data class WornObjCustomisation(
    public val objId: Int,
    public val recolours: Map<Int, Int>,
    public val retextures: Map<Int, Int>,
    public val colour: Int?,
)

public class Appearance {
    /**
     * Tracks whether the player's appearance needs to be updated. Initially set to `true` so that
     * the player's appearance can be synchronized in the next update block.
     */
    public var rebuild: Boolean = true
        internal set

    public var bas: BasType? = null
        set(value) {
            field = value
            rebuild = true
        }

    public var transmog: NpcServerType? = null
        set(value) {
            field = value
            rebuild = true
        }

    public var skullIcon: Int? = null
        set(value) {
            field = value
            rebuild = true
        }

    public var overheadIcon: Int? = null
        set(value) {
            field = value
            rebuild = true
        }

    public var bodyType: Int = 0
        set(value) {
            field = value
            rebuild = true
        }

    public var pronoun: Int = 0
        set(value) {
            field = value
            rebuild = true
        }

    public var namePrefix: String? = null
        set(value) {
            field = value
            rebuild = true
        }

    public var nameSuffix: String? = null
        set(value) {
            field = value
            rebuild = true
        }

    public var combatLvlSuffix: String? = null
        set(value) {
            field = value
            rebuild = true
        }

    public var softHidden: Boolean = false
        set(value) {
            field = value
            rebuild = true
        }

    public var combatLevel: Int = 3
        set(value) {
            field = value
            rebuild = true
        }

    private val colours: ByteArray = ByteArray(5)
    private val identKit: ShortArray = ShortArray(7) { -1 }
    private val wornObjCustomisations: MutableMap<Wearpos, WornObjCustomisation> = mutableMapOf()

    // TODO: Move default colours/identkit assignment to a relevant plugin and
    //  delete this init block.
    init {
        assignDefaultColours()
        assignDefaultIdentKit()
    }

    public fun setColour(index: Int, colour: Int) {
        require(colour in 0..255) { "colour must be in range [0..255]. ($colour)" }
        this.colours[index] = colour.toByte()
        this.rebuild = true
    }

    public fun setIdentKit(index: Int, identKit: Int) {
        require(identKit in 0..65535) { "identKit must be in range [0..65535]. ($identKit)" }
        this.identKit[index] = identKit.toShort()
        this.rebuild = true
    }

    /**
     * Replaces the visual override for [objId] in a visible equipment slot.
     * [recolours] and [retextures] map cache-defined indices to replacement values.
     * [colour] applies to the whole model when supplied. The override is discarded
     * when the next appearance update observes a different item in [wearpos].
     */
    public fun setWornObjCustomisation(
        wearpos: Wearpos,
        objId: Int,
        recolours: Map<Int, Int> = emptyMap(),
        retextures: Map<Int, Int> = emptyMap(),
        colour: Int? = null,
    ) {
        require(wearpos in Wearpos.visibleWearpos)
        require(recolours.size <= 255 && retextures.size <= 255)
        require(recolours.all { (index, value) -> index in 0..255 && value in 0..65535 })
        require(retextures.all { (index, value) -> index in 0..255 && value in -1..65535 })
        require(colour == null || colour in 0..65534)
        wornObjCustomisations[wearpos] =
            WornObjCustomisation(objId, recolours.toMap(), retextures.toMap(), colour)
        rebuild = true
    }

    /** Removes the override for [wearpos] and schedules an appearance update if it changed. */
    public fun clearWornObjCustomisation(wearpos: Wearpos) {
        if (wornObjCustomisations.remove(wearpos) != null) {
            rebuild = true
        }
    }

    /** Returns the override for the current item, discarding it if the item id changed. */
    public fun wornObjCustomisation(wearpos: Wearpos, wornObjId: Int?): WornObjCustomisation? {
        val customisation = wornObjCustomisations[wearpos] ?: return null
        if (customisation.objId != wornObjId) {
            wornObjCustomisations.remove(wearpos)
            return null
        }
        return customisation
    }

    public fun coloursSnapshot(): List<Byte> = colours.toList()

    public fun identKitSnapshot(): List<Short> = identKit.toList()

    public fun clearRebuildFlag() {
        rebuild = false
    }

    public fun subjectPronoun(): String =
        when (pronoun) {
            PRONOUN_HE -> "He"
            PRONOUN_SHE -> "She"
            PRONOUN_THEY -> "They"
            else -> if (bodyType == BODY_TYPE_A) "He" else "She"
        }

    private fun assignDefaultColours() {
        colours[0] = 0
        colours[1] = 3
        colours[2] = 2
        colours[3] = 0
        colours[4] = 0
    }

    private fun assignDefaultIdentKit() {
        identKit[0] = 9
        identKit[1] = 14
        identKit[2] = 109
        identKit[3] = 26
        identKit[4] = 33
        identKit[5] = 36
        identKit[6] = 42
    }

    public companion object {
        public const val PRONOUN_HE: Int = 0
        public const val PRONOUN_SHE: Int = 1
        public const val PRONOUN_THEY: Int = 2

        public const val BODY_TYPE_A: Int = 0
        public const val BODY_TYPE_B: Int = 1
    }
}
