package org.rsmod.api.db

public interface Database {
    /** Runs [block] against the shared tables in `public`. */
    public suspend fun <T> withTransaction(block: (DatabaseConnection) -> T): T

    /**
     * Runs [block] with the search path set to [schema], falling back to `public` for shared tables,
     * so a per-world-type query resolves to one mode's save without being rewritten.
     *
     * A `null` [schema] behaves as [withTransaction].
     */
    public suspend fun <T> withSchemaTransaction(
        schema: String?,
        block: (DatabaseConnection) -> T,
    ): T
}
