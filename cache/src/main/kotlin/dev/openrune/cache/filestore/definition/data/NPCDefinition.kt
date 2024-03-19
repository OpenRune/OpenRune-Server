package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition
import dev.openrune.cache.filestore.definition.Parameterized
import dev.openrune.cache.filestore.definition.Recolourable
import dev.openrune.cache.filestore.definition.Transforms

data class NPCDefinition(
    override var id: Int = 0,
    var name: String = "null",
    var size : Int = 1,
    var category : Int = -1,
    var modelIds: IntArray? = null,
    var chatheadModels: IntArray? = null,
    var standAnim : Int = -1,
    var render3 : Int = -1,
    var render4 : Int = -1,
    var walkAnim : Int = -1,
    var render5 : Int = -1,
    var render6 : Int = -1,
    var render7 : Int = -1,
    var actions : MutableList<String?> = mutableListOf(null, null, null, null, null),
    override var originalColours: ShortArray? = null,
    override var modifiedColours: ShortArray? = null,
    override var originalTextureColours: ShortArray? = null,
    override var modifiedTextureColours: ShortArray? = null,
    override var varbit: Int = -1,
    override var varp: Int = -1,
    override var transforms: IntArray? = null,
    var isMinimapVisible : Boolean = true,
    var combatLevel : Int = -1,
    var widthScale : Int = 128,
    var heightScale : Int = 128,
    var hasRenderPriority : Boolean = false,
    var ambient : Int = 0,
    var contrast : Int = 0,
    var headIconArchiveIds: IntArray? = null,
    var headIconSpriteIndex: IntArray? = null,
    var rotation : Int = 32,
    var isInteractable : Boolean = true,
    var isClickable : Boolean = true,
    var lowPriorityFollowerOps : Boolean = false,
    var isFollower : Boolean = false,
    var runSequence : Int = -1,
    var runBackSequence : Int = -1,
    var runRightSequence : Int = -1,
    var runLeftSequence : Int = -1,
    var crawlSequence : Int = -1,
    var crawlBackSequence : Int = -1,
    var crawlRightSequence : Int = -1,
    var crawlLeftSequence : Int = -1,
    override var params: Map<Int, Any>? = null,
) : Definition, Transforms, Recolourable, Parameterized {
    var examine : String = ""

    fun isAttackable(): Boolean = combatLevel > 0 && actions.any { it == "Attack" }

}