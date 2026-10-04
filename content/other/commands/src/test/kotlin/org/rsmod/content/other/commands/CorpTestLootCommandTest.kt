package org.rsmod.content.other.commands
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.central.account.Rights
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext
@ResourceLock("ServerCacheManager")
class CorpTestLootCommandTest {
 @Test fun `corp aliases use native death hooks and reject invalid counts`() {
  ServerCacheManager.init(240).close()
  val kills = mutableListOf<NpcDeathKillContext>()
  val hook = object : NpcDeathKillHook { override fun onKill(context: NpcDeathKillContext) { kills += context } }
  val ctor = AdminCommands::class.java.constructors.single()
  val deps = ctor.parameterTypes.map { if (it == Set::class.java) setOf(hook) else mock(it) }.toTypedArray()
  val script = ctor.newInstance(*deps) as AdminCommands
  val commands = CheatCommandMap(); with(script) { ScriptContext(EventBus(), commands, EngineQueueCache()).startup() }
  val player = Player().apply { modLevel = Rights.ADMINISTRATOR }
  for (alias in listOf("corp", "corporeal", "corporeal_beast")) commands.execute(player, "testloot", listOf(alias, "2"))
  assertEquals(6, kills.size); assertTrue(kills.all { it.npc.type.id == "npc.corp_beast".asRSCM() })
  for (count in listOf("0", "-1", "1001", "oops")) commands.execute(player, "testloot", listOf("corp", count))
  assertEquals(6, kills.size)
  commands.execute(player, "testloot", listOf("corp")); assertEquals(106, kills.size)
 }
}
