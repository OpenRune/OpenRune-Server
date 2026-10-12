package org.rsmod.api.testing.persistence

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterModule
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.parsers.jackson.JacksonModule
import org.rsmod.api.parsers.json.JsonModule
import org.rsmod.api.testing.TestServerCache

/**
 * Runs each test class against its own copy of a migrated Postgres database, without a game
 * server. One embedded Postgres and one Flyway-migrated `seed` database serve the whole JVM; every
 * test class gets a fresh `CREATE DATABASE … TEMPLATE seed` clone, dropped once it finishes.
 * Cloning and connecting cost several hundred milliseconds, too much to pay per test method, so
 * methods of one class share the clone and run sequentially; tests keep apart by account name.
 *
 * Resolves these test method parameters:
 * - [GameDatabase] connected to the test's clone;
 * - [CharacterAccountRepository] and `Set<CharacterDataStage.Pipeline>`, wired as in production.
 */
public class PersistenceTestExtension : BeforeAllCallback, ParameterResolver {
    override fun beforeAll(context: ExtensionContext) {
        context.getStore(NAMESPACE).put(CLONE_KEY, context.shared().postgres.cloneSeed())
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean = parameterContext.parameter.parameterizedType.injectionKey() != null

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any =
        when (val key = checkNotNull(parameterContext.parameter.parameterizedType.injectionKey())) {
            GAME_DATABASE_KEY -> extensionContext.cloned().database
            else -> extensionContext.shared().injector.getInstance(key)
        }

    private fun ExtensionContext.cloned(): PersistencePostgres.ClonedDatabase =
        getStore(NAMESPACE).get(CLONE_KEY, PersistencePostgres.ClonedDatabase::class.java)

    private fun ExtensionContext.shared(): SharedState =
        root.getStore(NAMESPACE).getOrComputeIfAbsent(
            SharedState::class.java,
            { SharedState.create() },
            SharedState::class.java,
        )

    private class SharedState(val postgres: PersistencePostgres, val injector: Injector) :
        AutoCloseable {
        override fun close() {
            postgres.close()
        }

        companion object {
            fun create(): SharedState {
                TestServerCache.load()
                val injector = Guice.createInjector(JacksonModule, JsonModule, CharacterModule)
                return SharedState(PersistencePostgres.start(), injector)
            }
        }
    }

    private companion object {
        private val NAMESPACE = ExtensionContext.Namespace.create("persistence-tests")

        private const val CLONE_KEY = "cloned-database"

        private val GAME_DATABASE_KEY: Key<*> = Key.get(GameDatabase::class.java)

        private val REPOSITORY_KEY: Key<*> = Key.get(CharacterAccountRepository::class.java)

        private val PIPELINES_KEY: Key<*> =
            Key.get(
                object : TypeLiteral<Set<@JvmSuppressWildcards CharacterDataStage.Pipeline>>() {}
            )

        private fun Type.injectionKey(): Key<*>? =
            when {
                this == GameDatabase::class.java -> GAME_DATABASE_KEY
                this == CharacterAccountRepository::class.java -> REPOSITORY_KEY
                isSetOf(CharacterDataStage.Pipeline::class.java) -> PIPELINES_KEY
                else -> null
            }

        private fun Type.isSetOf(element: Class<*>): Boolean {
            if (this !is ParameterizedType || rawType != Set::class.java) {
                return false
            }
            val argument = actualTypeArguments.single()
            val bound = if (argument is WildcardType) argument.upperBounds.single() else argument
            return bound == element
        }
    }
}
