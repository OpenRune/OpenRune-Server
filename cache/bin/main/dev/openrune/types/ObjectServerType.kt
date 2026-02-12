package dev.openrune.types

import dev.openrune.definition.Definition

data class ObjectServerType(
    override var id: Int = -1,
    var name : String = "",
    var examine : String = "",
    var sizeX: Int = 1,
    var sizeY: Int = 1,
    var offsetX: Int = 0,
    var interactive: Int = -1,
    var solid: Int = 2,
    var actions: MutableList<String?> = mutableListOf(null, null, null, null, null),
    var modelSizeX: Int = 128,
    var modelSizeZ: Int = 128,
    var modelSizeY: Int = 128,
    var offsetZ: Int = 0,
    var offsetY: Int = 0,
    var clipMask: Int = 0,
    var obstructive: Boolean = false,
    var category: Int = -1,
    var supportsItems: Int = -1,
    var isRotated: Boolean = false,
    var impenetrable: Boolean = true,
    var replacementId: Int = -1,
    var varbit: Int = -1,
    var varp: Int = -1,
    var transforms: MutableList<Int>? = null,
    var params: MutableMap<String, Any>? = null
) : Definition
