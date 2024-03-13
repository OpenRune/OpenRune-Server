package gg.rsmod.cache.definition.data

import gg.rsmod.cache.Definition
import gg.rsmod.cache.definition.Parameterized
import gg.rsmod.cache.definition.Recolourable
import gg.rsmod.cache.definition.Transforms

data class NPCDefinition(
    override var id: Int = 0,
    var name: String = "null",
    var size : Int = 1,
    var category : Int = -1,
    var modelIds: IntArray? = null,
    var chatheadModels: IntArray? = null,
    var standingAnimation : Int = -1,
    var rotateLeftAnimation : Int = -1,
    var rotateRightAnimation : Int = -1,
    var walkingAnimation : Int = -1,
    var rotate180Animation : Int = -1,
    var rotate90RightAnimation : Int = -1,
    var rotate90LeftAnimation : Int = -1,
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NPCDefinition

        if (id != other.id) return false
        if (name != other.name) return false
        if (size != other.size) return false
        if (category != other.category) return false
        if (modelIds != null) {
            if (other.modelIds == null) return false
            if (!modelIds.contentEquals(other.modelIds)) return false
        } else if (other.modelIds != null) return false
        if (chatheadModels != null) {
            if (other.chatheadModels == null) return false
            if (!chatheadModels.contentEquals(other.chatheadModels)) return false
        } else if (other.chatheadModels != null) return false
        if (standingAnimation != other.standingAnimation) return false
        if (rotateLeftAnimation != other.rotateLeftAnimation) return false
        if (rotateRightAnimation != other.rotateRightAnimation) return false
        if (walkingAnimation != other.walkingAnimation) return false
        if (rotate180Animation != other.rotate180Animation) return false
        if (rotate90RightAnimation != other.rotate90RightAnimation) return false
        if (rotate90LeftAnimation != other.rotate90LeftAnimation) return false
        if (actions != other.actions) return false
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
        if (varbit != other.varbit) return false
        if (varp != other.varp) return false
        if (transforms != null) {
            if (other.transforms == null) return false
            if (!transforms.contentEquals(other.transforms)) return false
        } else if (other.transforms != null) return false
        if (isMinimapVisible != other.isMinimapVisible) return false
        if (combatLevel != other.combatLevel) return false
        if (widthScale != other.widthScale) return false
        if (heightScale != other.heightScale) return false
        if (hasRenderPriority != other.hasRenderPriority) return false
        if (ambient != other.ambient) return false
        if (contrast != other.contrast) return false
        if (headIconArchiveIds != null) {
            if (other.headIconArchiveIds == null) return false
            if (!headIconArchiveIds.contentEquals(other.headIconArchiveIds)) return false
        } else if (other.headIconArchiveIds != null) return false
        if (headIconSpriteIndex != null) {
            if (other.headIconSpriteIndex == null) return false
            if (!headIconSpriteIndex.contentEquals(other.headIconSpriteIndex)) return false
        } else if (other.headIconSpriteIndex != null) return false
        if (rotation != other.rotation) return false
        if (isInteractable != other.isInteractable) return false
        if (isClickable != other.isClickable) return false
        if (lowPriorityFollowerOps != other.lowPriorityFollowerOps) return false
        if (isFollower != other.isFollower) return false
        if (runSequence != other.runSequence) return false
        if (runBackSequence != other.runBackSequence) return false
        if (runRightSequence != other.runRightSequence) return false
        if (runLeftSequence != other.runLeftSequence) return false
        if (crawlSequence != other.crawlSequence) return false
        if (crawlBackSequence != other.crawlBackSequence) return false
        if (crawlRightSequence != other.crawlRightSequence) return false
        if (crawlLeftSequence != other.crawlLeftSequence) return false
        if (params != other.params) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + size
        result = 31 * result + category
        result = 31 * result + (modelIds?.contentHashCode() ?: 0)
        result = 31 * result + (chatheadModels?.contentHashCode() ?: 0)
        result = 31 * result + standingAnimation
        result = 31 * result + rotateLeftAnimation
        result = 31 * result + rotateRightAnimation
        result = 31 * result + walkingAnimation
        result = 31 * result + rotate180Animation
        result = 31 * result + rotate90RightAnimation
        result = 31 * result + rotate90LeftAnimation
        result = 31 * result + actions.hashCode()
        result = 31 * result + (originalColours?.contentHashCode() ?: 0)
        result = 31 * result + (modifiedColours?.contentHashCode() ?: 0)
        result = 31 * result + (originalTextureColours?.contentHashCode() ?: 0)
        result = 31 * result + (modifiedTextureColours?.contentHashCode() ?: 0)
        result = 31 * result + varbit
        result = 31 * result + varp
        result = 31 * result + (transforms?.contentHashCode() ?: 0)
        result = 31 * result + isMinimapVisible.hashCode()
        result = 31 * result + combatLevel
        result = 31 * result + widthScale
        result = 31 * result + heightScale
        result = 31 * result + hasRenderPriority.hashCode()
        result = 31 * result + ambient
        result = 31 * result + contrast
        result = 31 * result + (headIconArchiveIds?.contentHashCode() ?: 0)
        result = 31 * result + (headIconSpriteIndex?.contentHashCode() ?: 0)
        result = 31 * result + rotation
        result = 31 * result + isInteractable.hashCode()
        result = 31 * result + isClickable.hashCode()
        result = 31 * result + lowPriorityFollowerOps.hashCode()
        result = 31 * result + isFollower.hashCode()
        result = 31 * result + runSequence
        result = 31 * result + runBackSequence
        result = 31 * result + runRightSequence
        result = 31 * result + runLeftSequence
        result = 31 * result + crawlSequence
        result = 31 * result + crawlBackSequence
        result = 31 * result + crawlRightSequence
        result = 31 * result + crawlLeftSequence
        result = 31 * result + (params?.hashCode() ?: 0)
        return result
    }
}