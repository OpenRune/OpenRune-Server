package org.rsmod.api.account.character

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@ExtendWith(PersistenceTestExtension::class)
class CharacterSaveRoundTripTest {
    @Test
    fun `position run energy and xp rate load back unchanged`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "rt_position")
        val saved =
            testPlayer {
                coords = CoordGrid(3093, 3493, 1)
                runEnergy = 4321
                xpRate = 2.5
            }

        // when
        save(db, repository, pipelines, character, saved)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertEquals(CoordGrid(3093, 3493, 1), loaded.coords)
        assertEquals(4321, loaded.runEnergy)
        assertEquals(2.5, loaded.xpRate)
    }

    @Test
    fun `display name and members load back unchanged`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "rt_identity")
        val saved =
            testPlayer {
                displayName = "rt_renamed"
                members = true
            }

        // when
        save(db, repository, pipelines, character, saved)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertEquals(character.accountId, loaded.accountId)
        assertEquals(character.characterId, loaded.characterId)
        assertEquals("rt_renamed", loaded.displayName)
        assertEquals(true, loaded.members)
    }

    @Test
    fun `perm varps load back and temp varps are not saved`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "rt_varps")
        val saved =
            testPlayer {
                setVarp(QUEST_POINTS, 32)
                setVarp(COMBAT_MODE, 3)
                setVarp(TEMP_VARP, 1)
            }

        // when
        save(db, repository, pipelines, character, saved)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertEquals(32, loaded.vars[QUEST_POINTS])
        assertEquals(3, loaded.vars[COMBAT_MODE])
        assertFalse(TEMP_VARP in loaded.vars)
    }

    @Test
    fun `persistent attrs load back and temp attrs are not saved`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "rt_attrs")
        val saved =
            testPlayer {
                attr[COUNTER_ATTR] = 42
                attr[LABEL_ATTR] = "persisted"
                attr[TEMP_ATTR] = 7
            }

        // when
        save(db, repository, pipelines, character, saved)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertEquals(42, loaded.attr[COUNTER_ATTR])
        assertEquals("persisted", loaded.attr[LABEL_ATTR])
        assertNull(loaded.attr[TEMP_ATTR])
    }

    @Test
    fun `perm inventories load back slot for slot and temp inventories are not saved`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "rt_invs")
        val saved =
            testPlayer {
                invMap.getOrPut(INV)[0] = InvObj("obj.shark", 1)
                invMap.getOrPut(INV)[27] = InvObj("obj.bronze_arrow", 250)
                invMap.getOrPut(WORN)[3] = InvObj("obj.abyssal_whip", 1)
                invMap.getOrPut(BANK)[0] = InvObj("obj.coins", 1_000_000)
                invMap.getOrPut(BANK)[1] = InvObj("obj.lobster", 300)
                invMap.getOrPut(TEMP_INV)[0] = InvObj("obj.rune_platebody", 1)
            }

        // when
        save(db, repository, pipelines, character, saved)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertEquals(saved.objsIn(INV), loaded.objsIn(INV))
        assertEquals(saved.objsIn(WORN), loaded.objsIn(WORN))
        assertEquals(saved.objsIn(BANK), loaded.objsIn(BANK))
        assertFalse(TEMP_INV in loaded.invMap)
    }

    @Test
    fun `stats load back unchanged`(
        db: GameDatabase,
        repository: CharacterAccountRepository,
        pipelines: Set<CharacterDataStage.Pipeline>,
    ) {
        // given
        val character = createCharacter(db, repository, "rt_stats")
        val saved =
            testPlayer {
                statMap.setBaseLevel("stat.attack", 70)
                statMap.setCurrentLevel("stat.attack", 75)
                statMap.setXP("stat.attack", 737_627)
                statMap.setBaseLevel("stat.hitpoints", 99)
                statMap.setCurrentLevel("stat.hitpoints", 12)
                statMap.setXP("stat.hitpoints", 13_034_431)
            }

        // when
        save(db, repository, pipelines, character, saved)
        val loaded = load(db, repository, pipelines, character.name)

        // then
        assertEquals(saved.statSnapshot(), loaded.statSnapshot())
    }

    private fun Player.objsIn(inv: String): List<InvObj?> = invMap.getValue(inv).objs.toList()

    private fun Player.statSnapshot(): List<Triple<Byte, Byte, Int>> =
        ServerCacheManager.getStats().values.map { type ->
            val stat = RSCM.getReverseMapping(RSCMType.STAT, type.id)
            val fineXp = statMap.getFineXP(stat)
            Triple(statMap.getCurrentLevel(stat), statMap.getBaseLevel(stat), fineXp)
        }

    private companion object {
        const val QUEST_POINTS = "varp.qp"
        const val COMBAT_MODE = "varp.com_mode"
        const val TEMP_VARP = "varp.prayer0"

        const val INV = "inv.inv"
        const val WORN = "inv.worn"
        const val BANK = "inv.bank"
        const val TEMP_INV = "inv.looting_bag"

        val COUNTER_ATTR = AttributeKey<Int>(persistenceKey = "persistence_test_counter")
        val LABEL_ATTR = AttributeKey<String>(persistenceKey = "persistence_test_label")
        val TEMP_ATTR = AttributeKey<Int>(persistenceKey = "persistence_test_temp", temp = true)
    }
}
