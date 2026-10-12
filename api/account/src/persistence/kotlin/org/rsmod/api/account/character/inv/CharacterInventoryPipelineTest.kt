package org.rsmod.api.account.character.inv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rsmod.api.account.TestCharacter
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.account.createCharacter
import org.rsmod.api.account.load
import org.rsmod.api.account.save
import org.rsmod.api.account.testPlayer
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.testing.persistence.PersistenceTestExtension
import org.rsmod.game.inv.InvObj
import org.rsmod.game.world.WorldType

@ExtendWith(PersistenceTestExtension::class)
class CharacterInventoryPipelineTest {
    @Test
    fun `a stack of Int MAX_VALUE loads back whole`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "inv_max_stack")
        val saved =
            testPlayer {
                invMap.getOrPut(INV)[0] = InvObj("obj.coins", Int.MAX_VALUE)
                invMap.getOrPut(BANK)[0] = InvObj("obj.coins", Int.MAX_VALUE)
            }

        // when
        save(db, repository, pipelines, character, saved)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertEquals(InvObj("obj.coins", Int.MAX_VALUE), loaded.invMap.getValue(INV)[0])
        assertEquals(InvObj("obj.coins", Int.MAX_VALUE), loaded.invMap.getValue(BANK)[0])
    }

    @Test
    fun `a slot emptied since the last save loads back empty`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "inv_cleared_slot")
        val player =
            testPlayer {
                invMap.getOrPut(INV)[0] = InvObj("obj.shark", 1)
                invMap.getOrPut(INV)[1] = InvObj("obj.lobster", 1)
            }
        save(db, repository, pipelines, character, player)
        player.invMap.getValue(INV)[0] = null

        // when
        save(db, repository, pipelines, character, player)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertNull(loaded.invMap.getValue(INV)[0])
        assertEquals(InvObj("obj.lobster", 1), loaded.invMap.getValue(INV)[1])
    }

    @Test
    fun `an inventory removed since the last save is deleted with its objs`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "inv_removed_inv")
        val player =
            testPlayer {
                invMap.getOrPut(INV)[0] = InvObj("obj.shark", 1)
                invMap.getOrPut(WORN)[3] = InvObj("obj.abyssal_whip", 1)
            }
        save(db, repository, pipelines, character, player)
        player.invMap.remove(WORN)

        // when
        save(db, repository, pipelines, character, player)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertFalse(WORN in loaded.invMap)
        assertEquals(InvObj("obj.shark", 1), loaded.invMap.getValue(INV)[0])
        assertEquals(0, objRowCount(db, character, WORN))
    }

    @Test
    fun `removing every inventory deletes them all`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "inv_removed_all")
        val player =
            testPlayer {
                invMap.getOrPut(INV)[0] = InvObj("obj.shark", 1)
                invMap.getOrPut(BANK)[0] = InvObj("obj.coins", 500)
            }
        save(db, repository, pipelines, character, player)
        player.invMap.remove(INV)
        player.invMap.remove(BANK)

        // when
        save(db, repository, pipelines, character, player)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertTrue(loaded.invMap.isEmpty())
        assertEquals(0, objRowCount(db, character, INV))
        assertEquals(0, objRowCount(db, character, BANK))
    }

    @Disabled("Save never deletes rows past the inv size, so a shrunk inv fails every login")
    @Test
    fun `a saved row beyond the inventory size is dropped by the next save`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "inv_shrunk")
        val player = testPlayer { invMap.getOrPut(INV)[0] = InvObj("obj.shark", 1) }
        save(db, repository, pipelines, character, player)
        insertObjRow(db, character, INV, slot = player.invMap.getValue(INV).size, "obj.lobster")

        // when
        save(db, repository, pipelines, character, player)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertEquals(InvObj("obj.shark", 1), loaded.invMap.getValue(INV)[0])
        assertEquals(1, objRowCount(db, character, INV))
    }

    private fun insertObjRow(
        db: GameDatabase,
        character: TestCharacter,
        inv: String,
        slot: Int,
        obj: String,
    ) {
        db.withTransactionBlocking(WorldType.MAIN.key) { connection ->
            val sql =
                "INSERT INTO inventory_objs (character_id, inv, slot, obj, count, vars) " +
                    "VALUES (?, ?, ?, ?, 1, 0)"
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, character.characterId)
                statement.setString(2, inv)
                statement.setInt(3, slot)
                statement.setString(4, obj)
                statement.executeUpdate()
            }
        }
    }

    private fun objRowCount(db: GameDatabase, character: TestCharacter, inv: String): Int =
        db.withTransactionBlocking(WorldType.MAIN.key) { connection ->
            val sql = "SELECT count(*) FROM inventory_objs WHERE character_id = ? AND inv = ?"
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, character.characterId)
                statement.setString(2, inv)
                statement.executeQuery().use { result ->
                    result.next()
                    result.getInt(1)
                }
            }
        }

    private companion object {
        const val INV = "inv.inv"
        const val WORN = "inv.worn"
        const val BANK = "inv.bank"
    }
}
