package org.rsmod.api.account.character

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.account.createCharacter
import org.rsmod.api.account.load
import org.rsmod.api.account.save
import org.rsmod.api.account.setVarp
import org.rsmod.api.account.testPlayer
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.testing.persistence.PersistenceTestExtension
import org.rsmod.game.inv.InvObj
import org.rsmod.game.world.WorldType
import org.rsmod.map.CoordGrid

@ExtendWith(PersistenceTestExtension::class)
class WorldTypeSchemaIsolationTest {
    @Test
    fun `the same character keeps a separate save per world type`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character =
            createCharacter(db, repository, "isolated", WorldType.MAIN, WorldType.GRIDMASTER)
        val mainSave =
            testPlayer(WorldType.MAIN) {
                coords = CoordGrid(3222, 3218, 0)
                runEnergy = 1000
                setVarp(QUEST_POINTS, 10)
                attr[COUNTER_ATTR] = 1
                invMap.getOrPut(INV)[0] = InvObj("obj.shark", 1)
                statMap.setBaseLevel("stat.attack", 40)
            }
        val gridmasterSave =
            testPlayer(WorldType.GRIDMASTER) {
                coords = CoordGrid(2964, 3378, 0)
                runEnergy = 250
                setVarp(QUEST_POINTS, 20)
                attr[COUNTER_ATTR] = 2
                invMap.getOrPut(INV)[0] = InvObj("obj.lobster", 3)
                statMap.setBaseLevel("stat.attack", 70)
            }

        // when
        save(db, repository, pipelines, character, mainSave)
        save(db, repository, pipelines, character, gridmasterSave)
        val main = load(db, repository, pipelines, character.name, WorldType.MAIN)
        val gridmaster = load(db, repository, pipelines, character.name, WorldType.GRIDMASTER)

        // then
        assertEquals(CoordGrid(3222, 3218, 0), main.coords)
        assertEquals(1000, main.runEnergy)
        assertEquals(10, main.vars[QUEST_POINTS])
        assertEquals(1, main.attr[COUNTER_ATTR])
        assertEquals(InvObj("obj.shark", 1), main.invMap.getValue(INV)[0])
        assertEquals(40.toByte(), main.statMap.getBaseLevel("stat.attack"))
        assertEquals(CoordGrid(2964, 3378, 0), gridmaster.coords)
        assertEquals(250, gridmaster.runEnergy)
        assertEquals(20, gridmaster.vars[QUEST_POINTS])
        assertEquals(2, gridmaster.attr[COUNTER_ATTR])
        assertEquals(InvObj("obj.lobster", 3), gridmaster.invMap.getValue(INV)[0])
        assertEquals(70.toByte(), gridmaster.statMap.getBaseLevel("stat.attack"))
    }

    @Test
    fun `a character never saved under a world type starts fresh there`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "first_visit")
        val mainSave =
            testPlayer(WorldType.MAIN) {
                coords = CoordGrid(3093, 3493, 1)
                setVarp(QUEST_POINTS, 10)
                attr[COUNTER_ATTR] = 1
                invMap.getOrPut(INV)[0] = InvObj("obj.shark", 1)
                statMap.setBaseLevel("stat.attack", 40)
            }
        save(db, repository, pipelines, character, mainSave)
        var firstVisit = false

        // when
        val gridmaster =
            load(db, repository, pipelines, character.name, WorldType.GRIDMASTER) {
                firstVisit = true
            }

        // then
        assertTrue(firstVisit)
        assertNotEquals(CoordGrid(3093, 3493, 1), gridmaster.coords)
        assertFalse(QUEST_POINTS in gridmaster.vars)
        assertNull(gridmaster.attr[COUNTER_ATTR])
        assertTrue(gridmaster.invMap.isEmpty())
        assertEquals(1.toByte(), gridmaster.statMap.getBaseLevel("stat.attack"))
    }

    private companion object {
        const val QUEST_POINTS = "varp.qp"
        const val INV = "inv.inv"

        val COUNTER_ATTR = AttributeKey<Int>(persistenceKey = "persistence_test_counter")
    }
}
