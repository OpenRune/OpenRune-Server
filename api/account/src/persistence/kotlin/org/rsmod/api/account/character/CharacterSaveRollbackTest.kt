package org.rsmod.api.account.character

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.account.createCharacter
import org.rsmod.api.account.load
import org.rsmod.api.account.save
import org.rsmod.api.account.setVarp
import org.rsmod.api.account.testPlayer
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.testing.persistence.PersistenceTestExtension
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@ExtendWith(PersistenceTestExtension::class)
class CharacterSaveRollbackTest {
    @Test
    fun `a pipeline throwing after every table is written rolls the whole save back`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "rollback")
        val previous =
            testPlayer {
                coords = CoordGrid(3222, 3218, 0)
                displayName = "rollback"
                runEnergy = 900
                setVarp(QUEST_POINTS, 10)
                attr[COUNTER_ATTR] = 1
                invMap.getOrPut(INV)[0] = InvObj("obj.shark", 1)
                statMap.setBaseLevel("stat.attack", 50)
            }
        save(db, repository, pipelines, character, previous)
        val failing =
            testPlayer {
                coords = CoordGrid(2964, 3378, 0)
                displayName = "rollback_new"
                members = true
                runEnergy = 100
                setVarp(QUEST_POINTS, 20)
                attr[COUNTER_ATTR] = 2
                invMap.getOrPut(INV)[0] = InvObj("obj.lobster", 5)
                invMap.getOrPut(WORN)[3] = InvObj("obj.abyssal_whip", 1)
                statMap.setBaseLevel("stat.attack", 60)
            }

        // when
        val failure =
            runCatching {
                    save(db, repository, pipelines + FailingPipeline, character, failing)
                }
                .exceptionOrNull()
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertSame(FailingPipeline.FAILURE, failure)
        assertEquals(CoordGrid(3222, 3218, 0), loaded.coords)
        assertEquals("rollback", loaded.displayName)
        assertFalse(loaded.members)
        assertEquals(900, loaded.runEnergy)
        assertEquals(10, loaded.vars[QUEST_POINTS])
        assertEquals(1, loaded.attr[COUNTER_ATTR])
        assertEquals(InvObj("obj.shark", 1), loaded.invMap.getValue(INV)[0])
        assertFalse(WORN in loaded.invMap)
        assertEquals(50.toByte(), loaded.statMap.getBaseLevel("stat.attack"))
    }

    private object FailingPipeline : CharacterDataStage.Pipeline {
        val FAILURE = IllegalStateException("pipeline failed mid-save")

        override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {}

        override fun save(connection: DatabaseConnection, player: Player, characterId: Int) {
            throw FAILURE
        }
    }

    private companion object {
        const val INV = "inv.inv"
        const val WORN = "inv.worn"

        const val QUEST_POINTS = "varp.qp"

        val COUNTER_ATTR = AttributeKey<Int>(persistenceKey = "persistence_test_counter")
    }
}
