package org.alter.game.model.attr

/**
 * Attribute keys used for quest progress tracking.
 *
 * Quest stages and states are persisted so that they survive player logout.
 * Journals are cached per session and can be recomputed on demand, so they are kept temporary.
 */
val QUEST_STAGE_MAP_ATTR = AttributeKey<MutableMap<String, Int>>("quest_stages")

