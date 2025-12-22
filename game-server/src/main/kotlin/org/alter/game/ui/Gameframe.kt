package org.alter.game.ui

import dev.openrune.definition.type.widget.Component

data class Gameframe(
    val topLevel: String,
    val overlays: List<GameframeOverlay>,
    val mappings: Map<Component, Component>,
    val clientMode: Int,
    val resizable: Boolean,
    val isDefault: Boolean,
    val stoneArrangement: Boolean,
)

data class GameframeOverlay(val interf: String, val target: String)

data class GameframeMove(val from: Gameframe, val dest: Gameframe, val intermediate: Gameframe?)
