package org.rsmod.api.account

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.game.entity.Player
import org.rsmod.game.world.WorldType

internal data class TestCharacter(val name: String, val accountId: Int, val characterId: Int)

internal fun createCharacter(
    db: GameDatabase,
    repository: CharacterAccountRepository,
    name: String,
    vararg worldTypes: WorldType = arrayOf(WorldType.MAIN),
): TestCharacter {
    val character =
        db.withTransactionBlocking(worldTypes.first().key) { connection ->
            val accountId =
                checkNotNull(repository.insertOrSelectAccountId(connection, name, PASSWORD_HASH))
            val characterId =
                checkNotNull(repository.insertAndSelectCharacterId(connection, accountId))
            TestCharacter(name, accountId, characterId)
        }
    for (worldType in worldTypes) {
        db.withTransactionBlocking(worldType.key) { connection ->
            repository.ensureProgressRow(connection, character.characterId)
        }
    }
    return character
}

internal fun testPlayer(
    worldType: WorldType = WorldType.MAIN,
    init: Player.() -> Unit = {},
): Player =
    Player().apply {
        this.worldType = worldType
        init()
    }

internal fun Player.setVarp(varp: String, value: Int) {
    vars.backing.put(varp.asRSCM(RSCMType.VARP), value)
}

internal fun save(
    db: GameDatabase,
    repository: CharacterAccountRepository,
    pipelines: Iterable<CharacterDataStage.Pipeline>,
    character: TestCharacter,
    player: Player,
) {
    db.withTransactionBlocking(player.worldType.key) { connection ->
        repository.save(
            connection,
            player,
            character.accountId,
            character.characterId,
            GAME_WORLD_ID,
        )
        for (pipeline in pipelines) {
            pipeline.save(connection, player, character.characterId)
        }
    }
}

internal fun load(
    db: GameDatabase,
    repository: CharacterAccountRepository,
    pipelines: Iterable<CharacterDataStage.Pipeline>,
    name: String,
    worldType: WorldType = WorldType.MAIN,
    onFirstVisitToWorldType: () -> Unit = {},
): Player {
    val player = testPlayer(worldType)
    db.withTransactionBlocking(worldType.key) { connection ->
        val metadata =
            checkNotNull(
                repository.selectAndCreateMetadataList(connection, name, onFirstVisitToWorldType)
            )
        for (pipeline in pipelines) {
            pipeline.append(connection, metadata)
        }
        metadata.forEach { it.apply(player) }
    }
    return player
}

private const val PASSWORD_HASH = "hash"
private const val GAME_WORLD_ID = 1
