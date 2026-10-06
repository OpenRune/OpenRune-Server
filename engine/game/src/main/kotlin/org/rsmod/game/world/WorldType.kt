package org.rsmod.game.world

/**
 * The gameplay modes a character's save can belong to. Independent of the `realms` table, which
 * describes a world rather than a save - see `docs/world-types.md`.
 *
 * [key] is the identifier everywhere: the `world-types` entry in `game.yml`, the schema holding that
 * mode's saves, and the value in `accounts.active_world_type`.
 */
public enum class WorldType(public val key: String, public val label: String) {
    MAIN("main", "Main"),
    TWISTED_LEAGUE("twisted_league", "Twisted League"),
    TRAILBLAZER_LEAGUE("trailblazer_league", "Trailblazer League"),
    SHATTERED_RELICS_LEAGUE("shattered_relics_league", "Shattered Relics League"),
    TRAILBLAZER_RELOADED_LEAGUE("trailblazer_reloaded_league", "Trailblazer Reloaded League"),
    RAGING_ECHOES_LEAGUE("raging_echoes_league", "Raging Echoes League"),
    GRIDMASTER("gridmaster", "Gridmaster");

    public val isLeague: Boolean
        get() = this != MAIN && this != GRIDMASTER

    public val isSeasonalEvent: Boolean
        get() = this != MAIN

    public companion object {
        /** What a world serves when `world-types` is not configured. */
        public val DEFAULT: WorldType = MAIN

        public val SEASONAL_EVENTS: List<WorldType> = entries.filter { it.isSeasonalEvent }

        private val byKey: Map<String, WorldType> = entries.associateBy { it.key }

        public fun byKeyOrNull(key: String): WorldType? = byKey[key.trim().lowercase()]

        /** [label] for [key], falling back to the raw key for anything unrecognised. */
        public fun labelOf(key: String): String = byKeyOrNull(key)?.label ?: key

        /**
         * The complete set of modes a world serves, in `world-types` order. Nothing is implied; an
         * empty config means `main` only. Order matters only as a fallback of last resort, when
         * this world does not serve [MAIN] - see `AccountLoaderService.resolveWorldType`.
         *
         * @throws IllegalStateException if an entry is not a known world type, so a typo fails
         *   startup rather than silently dropping a mode.
         */
        public fun supportedFrom(configured: List<String>): List<WorldType> {
            val served =
                configured.map(String::trim).filter(String::isNotEmpty).map { key ->
                    val resolved = byKeyOrNull(key)
                    checkNotNull(resolved) {
                        "Unknown `world-types` entry in game.yml: '$key'. " +
                            "Known world types: ${entries.joinToString { it.key }}"
                    }
                    resolved
                }
            return served.distinct().ifEmpty { listOf(MAIN) }
        }
    }
}
