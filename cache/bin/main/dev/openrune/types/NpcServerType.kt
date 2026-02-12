package dev.openrune.types

import dev.openrune.definition.Definition

data class NpcServerType(
    override var id: Int = -1,
    var name : String = "",
    var size: Int = 1,
    var category: Int = -1,
    var standAnim: Int = -1,
    var rotateLeftAnim: Int = -1,
    var rotateRightAnim: Int = -1,
    var walkAnim: Int = -1,
    var rotateBackAnim: Int = -1,
    var walkLeftAnim: Int = -1,
    var walkRightAnim: Int = -1,
    var actions: MutableList<String?> = mutableListOf(null, null, null, null, null),
    var varbit: Int = -1,
    var varp: Int = -1,
    var transforms: MutableList<Int>? = null,
    var combatLevel: Int = -1,
    var renderPriority: Int = 0,
    var lowPriorityFollowerOps: Boolean = false,
    var isFollower: Boolean = false,
    var runSequence: Int = -1,
    var isInteractable : Boolean = true,
    var runBackSequence: Int = -1,
    var runRightSequence: Int = -1,
    var runLeftSequence: Int = -1,
    var crawlSequence: Int = -1,
    var crawlBackSequence: Int = -1,
    var crawlRightSequence: Int = -1,
    var crawlLeftSequence: Int = -1,
    var height: Int = -1,
    var attack : Int = 1,
    var defence : Int = 1,
    var strength : Int = 1,
    var hitpoints : Int = 1,
    var ranged : Int = 1,
    var magic : Int = 1,
    var params: MutableMap<String, Any>? = null,
) : Definition {
    fun isAttackable(): Boolean = combatLevel > 0 && actions.any { it == "Attack" }
}
