package org.rsmod.content.bosses.corp
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.or2.central.account.Rights
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.bosses.runtime.*
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.respawn.BossRespawnTimers
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext
@ResourceLock("ServerCacheManager")
class CorpRegistrationTest {
 @Test fun `cache assets and native passage registrations exist`() {
  ServerCacheManager.init(240).close()
  val npc = ServerCacheManager.getNpc(CorpRules.BOSS.asRSCM())!!
  assertEquals(230, npc.param(BaseParams.defence_light))
  assertEquals(230, npc.param(BaseParams.defence_standard))
  assertEquals(100, npc.param(BaseParams.defence_heavy))
  assertEquals(0, npc.param(BaseParams.elemental_weakness_percent))
  val registry = EncounterRegistry(); val extensions = BossExtensionRegistry()
  val deps = mock(BossDeps::class.java) { call -> when (call.method.name) {
   "getEncounterRegistry" -> registry; "getExtensionRegistry" -> extensions; else -> RETURNS_DEFAULTS.answer(call)
  } }
  val bus = EventBus(); val commands = CheatCommandMap(); val context = ScriptContext(bus, commands, EngineQueueCache())
  with(CorpScript(deps, mock(CorpController::class.java), mock(NpcDeath::class.java), mock(BossRespawnTimers::class.java), mock(ProtectedAccessLauncher::class.java))) { context.startup() }
  assertEquals(Rights.ADMINISTRATOR, commands.commands.getValue("testcorp").requiredRights)
  for (symbol in listOf("loc.corp_beast_entrance", "loc.corp_cave_entrance", "loc.corp_cave_exit")) assertTrue(bus.contains(LocEvents.Op1::class.java, symbol.asRSCM()))
  assertTrue(bus.contains(LocEvents.Op2::class.java, "loc.corp_beast_entrance".asRSCM()))
  for (seq in listOf("seq.corpbeast_death", "seq.corpbeast_stomp_attack", "seq.corpbeast_swiping_attack", "seq.corpbeast_sprite_shoot_1", "seq.corpbeast_sprite_shoot_2", "seq.corpbeast_sprite_shoot_3")) assertTrue(ServerCacheManager.getAnim(seq.asRSCM())!!.tickDuration > 0)
 }
}
