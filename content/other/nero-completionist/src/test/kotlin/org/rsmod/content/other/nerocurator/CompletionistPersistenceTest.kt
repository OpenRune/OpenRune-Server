package org.rsmod.content.other.nerocurator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.openrune.ServerCacheManager
import dev.or2.central.db.FlywayMigrator
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import java.nio.file.Path
import java.time.Duration
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.account.character.main.CharacterAccountApplier
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
class CompletionistPersistenceTest {
    @TempDir lateinit var directory: Path

    @Test fun `native PostgreSQL repository saves styles presets Ava and extra ammo then loads a new player without temporary editor state`() {
        EmbeddedPostgres.builder().setDataDirectory(directory.resolve("postgres").toFile()).setCleanDataDirectory(false).setPGStartupWait(Duration.ofSeconds(30)).start().use { database ->
            val jdbc = database.getJdbcUrl("postgres", "postgres")
            FlywayMigrator.migrate(jdbc, "postgres", "")
            database.postgresDatabase.connection.use { connection ->
                connection.autoCommit = false
                val wrapped = DatabaseConnection(connection)
                val repo = CharacterAccountRepository(ObjectMapper().registerKotlinModule(), CharacterAccountApplier())
                val accountId = checkNotNull(repo.insertOrSelectAccountId(wrapped, "Cape Persistence Test", "test-only-unused-password"))
                val characterId = checkNotNull(repo.insertAndSelectCharacterId(wrapped, accountId))
                val player = Player().apply { displayName = "Cape Persistence Test"; lastLogin = LocalDateTime.now(); coords = CoordGrid(3087, 3493); pendingLogout = true }
                for (n in 0..41) VarPlayerIntMapSetter.set(player, "varp.nero_style_$n", if (n == 6) 2 else if (n < 28) 100 + n else 1)
                DizanasQuiver.store(player, InvObj("obj.rune_arrow", 100))
                player.attr[CompletionistVariantScript.JUNK_DISABLED] = true
                player.attr[CompletionistVariantScript.NEXT_COLLECTION] = 999
                repo.save(wrapped, player, accountId, characterId, 255); connection.commit()
                val metadata = checkNotNull(repo.selectAndCreateMetadataList(wrapped, "Cape Persistence Test"))
                val restored = Player(); metadata.transformers.forEach { it.apply(restored) }
                for (n in 0..27) assertEquals(player.vars["varp.nero_style_$n"], restored.vars["varp.nero_style_$n"], "saved style $n")
                for (n in 28..41) assertEquals(0, restored.vars["varp.nero_style_$n"], "temporary style $n")
                assertEquals(InvObj("obj.rune_arrow", 100), DizanasQuiver.stored(restored)); assertEquals(true, restored.attr[CompletionistVariantScript.JUNK_DISABLED]); assertNull(restored.attr[CompletionistVariantScript.NEXT_COLLECTION])
                connection.rollback()
            }
        }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
