package org.rsmod.content.skills.woodcutting.configs

import dev.openrune.ParamReferences.param
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum

object WoodcuttingParams {
    val success_rates = param<EnumTypeMap<ItemServerType, Int>>("woodcutting_axe_success_rates")
}

internal object WoodcuttingEnums {
    val regular_tree_axes = enum<ItemServerType, Int>("regular_tree_axes")
    val oak_tree_axes = enum<ItemServerType, Int>("oak_tree_axes")
    val willow_tree_axes = enum<ItemServerType, Int>("willow_tree_axes")
    val teak_tree_axes = enum<ItemServerType, Int>("teak_tree_axes")
    val maple_tree_axes = enum<ItemServerType, Int>("maple_tree_axes")
    val arctic_tree_axes = enum<ItemServerType, Int>("arctic_tree_axes")
    val mahogany_tree_axes = enum<ItemServerType, Int>("mahogany_tree_axes")
    val yew_tree_axes = enum<ItemServerType, Int>("yew_tree_axes")
    val magic_tree_axes = enum<ItemServerType, Int>("magic_tree_axes")
    val redwood_tree_axes = enum<ItemServerType, Int>("redwood_tree_axes")
    val hollow_tree_axes = enum<ItemServerType, Int>("hollow_tree_axes")
}
