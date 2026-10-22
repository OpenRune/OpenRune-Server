package org.rsmod.api.account

import com.fasterxml.jackson.databind.ObjectMapper
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.sql.OpenRuneSql
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.account.autosave.PlayerAutosaveOrchestrator
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.inv.CharacterInventoryApplier
import org.rsmod.api.account.character.inv.CharacterInventoryPipeline
import org.rsmod.api.account.character.main.CharacterAccountApplier
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.account.character.stats.CharacterStatApplier
import org.rsmod.api.account.character.stats.CharacterStatPipeline
import org.rsmod.api.account.saver.AccountSavingService
import org.rsmod.api.account.saver.request.AccountSaveRequest
import org.rsmod.api.account.saver.request.AccountSaveResponse
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.attr.AttributeMap
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerPersistenceHints
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.world.WorldType

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AccountSaveCoherenceTest {
    private val repository = CharacterAccountRepository(ObjectMapper(), mockk<CharacterAccountApplier>())
    private val pipelines: Set<CharacterDataStage.Pipeline> = setOf(
        CharacterInventoryPipeline(CharacterInventoryApplier()),
        CharacterStatPipeline(CharacterStatApplier()),
    )
    private val requests = mutableListOf<AccountSaveRequest>()
    private val saver = mockk<AccountSavingService> {
        every { queue(any()) } answers { requests.add(firstArg()); Unit }
    }
    private val manager = AccountManager(mockk(), saver, repository, pipelines)
    private val swordId = "obj.bronze_sword".asRSCM()
    private val varpId = "varp.giants_foundry_preform_1".asRSCM()

    @AfterEach
    fun releaseHooks() {
        PlayerPersistenceHints.bind {}
        AttributeMap.persistenceMutationSink = null
    }

    @Test
    fun `save requested during mutation captures only the completed tick state`() {
        val player = player()
        val nested = mutableListOf(1)
        player.attr[AttributeKey<MutableList<Int>>(persistenceKey = "save_test")] = nested
        manager.save(player) {}
        assertTrue(requests.isEmpty())
        player.vars.backing.put(varpId, 1 shl 30)
        player.worn[0] = null
        manager.capturePendingSaves()
        nested.add(2)
        player.vars.backing.put(varpId, 0)
        player.worn[0] = InvObj(checkNotNull(ServerCacheManager.getItem(swordId)), 1)
        player.worldType = WorldType.GRIDMASTER

        val request = requests.single()
        val writes = SqlWrites()
        persist(request, writes)
        assertEquals(WorldType.MAIN, request.worldType)
        assertEquals(1 shl 30, writes.varpValue())
        assertTrue(writes.rows("game/inventory/upsert_inventory_obj.sql").isEmpty())
        assertTrue(writes.rows("game/inventory/delete_obj_by_inventory_slot.sql").any { it[3] == 0 })
        assertEquals("[1]", request.account.attrsJson.getValue("save_test"))
    }

    @Test
    fun `consumer cannot combine captured varps with later inventory or stats`() {
        val player = player()
        player.statMap.setFineXP("stat.attack", 123)
        manager.save(player) {}
        manager.capturePendingSaves()
        val request = requests.single()
        val writes = SqlWrites {
            player.vars.backing.put(varpId, 1 shl 30)
            player.worn[0] = null
            player.statMap.setFineXP("stat.attack", 456)
        }
        persist(request, writes)
        assertEquals(0, writes.varpValue())
        assertEquals("obj.bronze_sword", writes.rows("game/inventory/upsert_inventory_obj.sql").single()[4])
        val attack = writes.rows("game/stats/upsert_stat.sql").single { it[2] == "stat.attack".asRSCM() }
        assertEquals(123, attack[5])
    }

    @Test
    fun `mutations during an in flight autosave are saved after its callback`() {
        val player = player()
        val autosave = PlayerAutosaveOrchestrator(manager)
        PlayerPersistenceHints.notify(player)
        autosave.processEndOfTick(listOf(player))
        assertEquals(1, requests.size)
        player.worn.clearModifiedSlots()
        player.vars.backing.put(varpId, 1 shl 30)
        player.worn[0] = null
        PlayerPersistenceHints.notify(player)
        autosave.processEndOfTick(listOf(player))
        player.worn.clearModifiedSlots()
        assertEquals(1, requests.size)
        requests.first().callback(AccountSaveResponse.Success(player))
        autosave.processEndOfTick(listOf(player))
        assertEquals(2, requests.size)
        val writes = SqlWrites()
        persist(requests.last(), writes)
        assertEquals(1 shl 30, writes.varpValue())
        assertTrue(writes.rows("game/inventory/upsert_inventory_obj.sql").isEmpty())
    }

    @Test
    fun `retry consumes the same snapshot before the newer final logout save`() = runBlocking {
        val player = player()
        manager.save(player) {}
        manager.capturePendingSaves()
        player.vars.backing.put(varpId, 1 shl 30)
        player.worn[0] = null
        player.pendingLogout = true
        manager.save(player) {}
        manager.capturePendingSaves()
        player.worldType = WorldType.GRIDMASTER
        val database = mockk<GameDatabase>()
        val config = mockk<ServerConfig> { every { world } returns 1 }
        val attempts = mutableListOf<SqlWrites>()
        val schemas = mutableListOf<String?>()
        coEvery { database.withSchemaTransaction<Unit>(any(), any()) } coAnswers {
            schemas += firstArg<String?>()
            val writes = SqlWrites()
            attempts += writes
            secondArg<(DatabaseConnection) -> Unit>()(writes.connection)
            if (attempts.size == 1) error("retry this transaction")
        }
        val service = AccountSavingService(database, repository, config)
        requests.forEach(service::queue)
        service.run()
        assertEquals(listOf("main", "main", "main"), schemas)
        assertEquals(listOf(0, 0, 1 shl 30), attempts.map { it.varpValue() })
        assertEquals(listOf(1, 1, 0), attempts.map { it.rows("game/inventory/upsert_inventory_obj.sql").size })
        assertEquals(1, attempts.last().rows("game/character/character_progress_update_clear_presence.sql").size)
    }

    @Test
    fun `shutdown retains the interrupted batch ahead of newer snapshots`() = runBlocking {
        val player = player()
        val callbacks = mutableListOf<Int>()
        repeat(3) { index ->
            player.vars.backing.put(varpId, index)
            manager.save(player) { response ->
                assertTrue(response is AccountSaveResponse.Success)
                callbacks += index
            }
            manager.capturePendingSaves()
        }
        val database = mockk<GameDatabase>()
        val config = mockk<ServerConfig> { every { world } returns 1 }
        val entered = CompletableDeferred<Unit>()
        val saved = mutableListOf<Int>()
        var interrupt = true
        coEvery { database.withSchemaTransaction<Unit>(any(), any()) } coAnswers {
            if (interrupt) {
                interrupt = false
                entered.complete(Unit)
                awaitCancellation()
            }
            val writes = SqlWrites()
            secondArg<(DatabaseConnection) -> Unit>()(writes.connection)
            saved += writes.varpValue()
        }
        val service = AccountSavingService(database, repository, config)
        requests.take(2).forEach(service::queue)
        val worker = launch { service.run() }
        entered.await()
        service.queue(requests.last())
        worker.cancelAndJoin()
        service.shutdown()
        assertEquals(listOf(0, 1, 2), saved)
        assertEquals(listOf(0, 1, 2), callbacks)
    }

    private fun persist(request: AccountSaveRequest, writes: SqlWrites) {
        repository.save(writes.connection, request.account, request.characterId, 1)
        request.segments.forEach { it.save(writes.connection, request.characterId) }
    }

    private fun player(): Player = Player().apply {
        accountId = 1
        characterId = 2
        processedMapClock = 1
        worldType = WorldType.MAIN
        worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        worn[0] = InvObj(checkNotNull(ServerCacheManager.getItem(swordId)), 1)
        invMap["inv.worn"] = worn
        vars.backing.put(varpId, 0)
    }

    private inner class SqlWrites(private val onFirstWrite: () -> Unit = {}) {
        private val batches = mutableMapOf<String, MutableList<Map<Int, Any?>>>()
        private var firstWrite = true
        val connection = mockk<DatabaseConnection> {
            every { prepareStatement(any()) } answers { statement(firstArg()) }
        }

        fun rows(path: String): List<Map<Int, Any?>> = batches[OpenRuneSql.text(path)].orEmpty()

        fun varpValue(): Int = rows("game/character/character_varps_insert.sql")
            .single { it[2] == "varp.giants_foundry_preform_1" }[3] as Int

        private fun statement(sql: String): PreparedStatement {
            val params = mutableMapOf<Int, Any?>()
            fun record() {
                batches.getOrPut(sql) { mutableListOf() }.add(params.toMap())
                if (firstWrite) {
                    firstWrite = false
                    onFirstWrite()
                }
            }
            return mockk(relaxed = true) {
                every { setInt(any(), any()) } answers { params[firstArg()] = secondArg<Int>() }
                every { setString(any(), any()) } answers { params[firstArg()] = secondArg<String?>() }
                every { addBatch() } answers { record() }
                every { executeUpdate() } answers { record(); 1 }
                every { executeQuery() } returns mockk<ResultSet> {
                    every { next() } returns true
                    every { getInt("character_id") } returns 2
                    every { getString("inv") } returns "inv.worn"
                    every { close() } returns Unit
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
