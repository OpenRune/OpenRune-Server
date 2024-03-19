package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Parameterized
import dev.openrune.cache.filestore.definition.Recolourable
import it.unimi.dsi.fastutil.bytes.Byte2ByteOpenHashMap

data class ItemDefinition(
    override var id: Int = 0,
    var name: String = "null",
    override var originalColours: ShortArray? = null,
    override var modifiedColours: ShortArray? = null,
    override var originalTextureColours: ShortArray? = null,
    override var modifiedTextureColours: ShortArray? = null,
    override var params: Map<Int, Any>? = null,
    var resizeX: Int = 128,
    var resizeY: Int = 128,
    var resizeZ: Int = 128,
    var xan2d: Int = 0,
    var category : Int = -1,
    var yan2d: Int = 0,
    var zan2d: Int = 0,
    var wearPos1: Int = 0,
    var wearPos2: Int = 0,
    var wearPos3: Int = 0,
    var weight: Double = 0.0,
    var cost: Int = 1,
    var isTradeable: Boolean = false,
    var stacks: Int = 0,
    var model: Int = 0,
    var members: Boolean = false,
    var zoom2d: Int = 2000,
    var xOffset2d: Int = 0,
    var yOffset2d: Int = 0,
    var ambient: Int = 0,
    var contrast: Int = 0,
    var countCo: IntArray = IntArray(10),
    var countObj: IntArray = IntArray(10),
    var options : MutableList<String?> = mutableListOf(null, null, "Take", null, null),
    var interfaceOptions  : MutableList<String?> = mutableListOf(null, null, null, null, "Drop"),
    var maleModel0: Int = -1,
    var maleModel1: Int = -1,
    var maleModel2: Int = -1,
    var maleOffset: Int = 0,
    var maleHeadModel0: Int = -1,
    var maleHeadModel1: Int = -1,
    var femaleModel0: Int = -1,
    var femaleModel1: Int = -1,
    var femaleModel2: Int = -1,
    var femaleOffset: Int = -1,
    var femaleHeadModel0: Int = -1,
    var femaleHeadModel1: Int = -1,
    var noteLinkId: Int = -1,
    var noteTemplateId: Int = -1,
    var teamCape: Int = 0,
    var dropOptionIndex: Int = -2,
    var unnotedId: Int = -1,
    var notedId: Int = -1,
    var placeholderLink: Int = -1,
    var placeholderTemplate: Int = -1
) : Definition, Recolourable, Parameterized {

    lateinit var bonuses: IntArray

    var examine: String? = null
    var attackSpeed = -1
    var equipSlot = -1
    var equipType = 0
    var weaponType = -1
    var renderAnimations: IntArray? = null
    var skillReqs: Byte2ByteOpenHashMap? = null

    val stackable: Boolean
        get() = stacks == 1 || noteTemplateId > 0

    val noted: Boolean
        get() = noteTemplateId > 0

    /**
     * Whether or not the object is a placeholder.
     */
    val isPlaceholder
        get() = placeholderTemplate > 0 && placeholderLink > 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemDefinition

        if (id != other.id) return false
        if (name != other.name) return false
        if (originalColours != null) {
            if (other.originalColours == null) return false
            if (!originalColours.contentEquals(other.originalColours)) return false
        } else if (other.originalColours != null) return false
        if (modifiedColours != null) {
            if (other.modifiedColours == null) return false
            if (!modifiedColours.contentEquals(other.modifiedColours)) return false
        } else if (other.modifiedColours != null) return false
        if (originalTextureColours != null) {
            if (other.originalTextureColours == null) return false
            if (!originalTextureColours.contentEquals(other.originalTextureColours)) return false
        } else if (other.originalTextureColours != null) return false
        if (modifiedTextureColours != null) {
            if (other.modifiedTextureColours == null) return false
            if (!modifiedTextureColours.contentEquals(other.modifiedTextureColours)) return false
        } else if (other.modifiedTextureColours != null) return false
        if (params != other.params) return false
        if (resizeX != other.resizeX) return false
        if (resizeY != other.resizeY) return false
        if (resizeZ != other.resizeZ) return false
        if (xan2d != other.xan2d) return false
        if (category != other.category) return false
        if (yan2d != other.yan2d) return false
        if (zan2d != other.zan2d) return false
        if (wearPos1 != other.wearPos1) return false
        if (wearPos2 != other.wearPos2) return false
        if (wearPos3 != other.wearPos3) return false
        if (weight != other.weight) return false
        if (cost != other.cost) return false
        if (isTradeable != other.isTradeable) return false
        if (stackable != other.stackable) return false
        if (model != other.model) return false
        if (members != other.members) return false
        if (zoom2d != other.zoom2d) return false
        if (xOffset2d != other.xOffset2d) return false
        if (yOffset2d != other.yOffset2d) return false
        if (ambient != other.ambient) return false
        if (contrast != other.contrast) return false
        if (!countCo.contentEquals(other.countCo)) return false
        if (!countObj.contentEquals(other.countObj)) return false
        if (options != other.options) return false
        if (interfaceOptions != other.interfaceOptions) return false
        if (maleModel0 != other.maleModel0) return false
        if (maleModel1 != other.maleModel1) return false
        if (maleModel2 != other.maleModel2) return false
        if (maleOffset != other.maleOffset) return false
        if (maleHeadModel0 != other.maleHeadModel0) return false
        if (maleHeadModel1 != other.maleHeadModel1) return false
        if (femaleModel0 != other.femaleModel0) return false
        if (femaleModel1 != other.femaleModel1) return false
        if (femaleModel2 != other.femaleModel2) return false
        if (femaleOffset != other.femaleOffset) return false
        if (femaleHeadModel0 != other.femaleHeadModel0) return false
        if (femaleHeadModel1 != other.femaleHeadModel1) return false
        if (noteLinkId != other.noteLinkId) return false
        if (noteTemplateId != other.noteTemplateId) return false
        if (teamCape != other.teamCape) return false
        if (dropOptionIndex != other.dropOptionIndex) return false
        if (unnotedId != other.unnotedId) return false
        if (notedId != other.notedId) return false
        if (placeholderLink != other.placeholderLink) return false
        if (placeholderTemplate != other.placeholderTemplate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + (originalColours?.contentHashCode() ?: 0)
        result = 31 * result + (modifiedColours?.contentHashCode() ?: 0)
        result = 31 * result + (originalTextureColours?.contentHashCode() ?: 0)
        result = 31 * result + (modifiedTextureColours?.contentHashCode() ?: 0)
        result = 31 * result + (params?.hashCode() ?: 0)
        result = 31 * result + resizeX
        result = 31 * result + resizeY
        result = 31 * result + resizeZ
        result = 31 * result + xan2d
        result = 31 * result + category
        result = 31 * result + yan2d
        result = 31 * result + zan2d
        result = 31 * result + wearPos1
        result = 31 * result + wearPos2
        result = 31 * result + wearPos3
        result = (31 * result + weight).toInt()
        result = 31 * result + cost
        result = 31 * result + isTradeable.hashCode()
        result = 31 * result + stacks
        result = 31 * result + model
        result = 31 * result + members.hashCode()
        result = 31 * result + zoom2d
        result = 31 * result + xOffset2d
        result = 31 * result + yOffset2d
        result = 31 * result + ambient
        result = 31 * result + contrast
        result = 31 * result + countCo.contentHashCode()
        result = 31 * result + countObj.contentHashCode()
        result = 31 * result + options.hashCode()
        result = 31 * result + interfaceOptions.hashCode()
        result = 31 * result + maleModel0
        result = 31 * result + maleModel1
        result = 31 * result + maleModel2
        result = 31 * result + maleOffset
        result = 31 * result + maleHeadModel0
        result = 31 * result + maleHeadModel1
        result = 31 * result + femaleModel0
        result = 31 * result + femaleModel1
        result = 31 * result + femaleModel2
        result = 31 * result + femaleOffset
        result = 31 * result + femaleHeadModel0
        result = 31 * result + femaleHeadModel1
        result = 31 * result + noteLinkId
        result = 31 * result + noteTemplateId
        result = 31 * result + teamCape
        result = 31 * result + dropOptionIndex
        result = 31 * result + unnotedId
        result = 31 * result + notedId
        result = 31 * result + placeholderLink
        result = 31 * result + placeholderTemplate
        return result
    }

}