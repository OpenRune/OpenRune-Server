package org.rsmod.api.account

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.testing.persistence.PersistenceTestExtension
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType
import org.rsmod.map.CoordGrid

@ExtendWith(PersistenceTestExtension::class)
class EmptyAccountPersistenceTest {
    @Test
    fun `empty account saves and loads`(
        database: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ): Unit = runBlocking {
        // given
        val (accountId, characterId) =
            database.withSchemaTransaction(SCHEMA) { connection ->
                val accountId =
                    checkNotNull(repository.insertOrSelectAccountId(connection, NAME, "hash"))
                val characterId =
                    checkNotNull(repository.insertAndSelectCharacterId(connection, accountId))
                repository.ensureProgressRow(connection, characterId)
                accountId to characterId
            }
        val saved =
            Player().apply {
                coords = CoordGrid(3222, 3218, 0)
                displayName = NAME
                worldType = WorldType.MAIN
            }

        // when
        database.withSchemaTransaction(SCHEMA) { connection ->
            repository.save(connection, saved, accountId, characterId, gameWorldId = 1)
            for (pipeline in pipelines) {
                pipeline.save(connection, saved, characterId)
            }
        }
        val loaded = Player()
        database.withSchemaTransaction(SCHEMA) { connection ->
            val metadata = checkNotNull(repository.selectAndCreateMetadataList(connection, NAME))
            for (pipeline in pipelines) {
                pipeline.append(connection, metadata)
            }
            metadata.forEach { it.apply(loaded) }
        }

        // then
        assertEquals(accountId, loaded.accountId)
        assertEquals(characterId, loaded.characterId)
        assertEquals(saved.displayName, loaded.displayName)
        assertEquals(saved.coords, loaded.coords)
        assertEquals(saved.runEnergy, loaded.runEnergy)
        assertEquals(saved.xpRate, loaded.xpRate)
        assertEquals(saved.vars.backing, loaded.vars.backing)
        assertEquals(saved.statSnapshot(), loaded.statSnapshot())
    }

    private fun Player.statSnapshot(): List<Triple<Byte, Byte, Int>> =
        ServerCacheManager.getStats().values.map { type ->
            val stat = RSCM.getReverseMapping(RSCMType.STAT, type.id)
            val fineXp = statMap.getFineXP(stat)
            Triple(statMap.getCurrentLevel(stat), statMap.getBaseLevel(stat), fineXp)
        }

    private companion object {
        private const val NAME = "persistence_smoke"
        private val SCHEMA = WorldType.MAIN.key
    }
}
