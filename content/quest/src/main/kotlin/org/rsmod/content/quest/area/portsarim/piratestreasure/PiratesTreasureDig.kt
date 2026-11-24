package org.rsmod.content.quest.area.portsarim.piratestreasure

import jakarta.inject.Inject
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.owner.assignSpawnOwner
import org.rsmod.api.npc.owner.isSpawnOwnedBy
import org.rsmod.api.npc.owner.isSpawnOwnedByOther
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.output.MiscOutput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiTimer
import org.rsmod.content.quest.area.portsarim.piratestreasure.PiratesTreasureQuest.Companion.STAGE_MESSAGE_READ
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PiratesTreasureDig
@Inject
constructor(
    private val quest: PiratesTreasureQuest,
    private val npcRepo: NpcRepository,
    private val playerList: PlayerList,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val mapClock: MapClock,
) : PluginScript(), SpadeDigHook {
    private var Player.gardenerAppeared by intVarBit("varbit.pirates_treasure_gardener")

    override fun ScriptContext.startup() {
        onAiTimer(GARDENER) { watchOwner(npc) }
    }

    override fun claims(player: Player): Boolean =
        quest.stage(player) == STAGE_MESSAGE_READ && player.coords in DIG_SPOTS

    override suspend fun ProtectedAccess.beforeDig(): Boolean {
        if (gardenerFor(player) != null) {
            mes("I can't dig up anything with him attacking me!")
            return false
        }
        return true
    }

    override suspend fun ProtectedAccess.dig() {
        if (player.gardenerAppeared == 0) {
            player.gardenerAppeared = 1
            spawnGardener(player)
            return
        }
        if (!quest.unearthTreasure(this)) {
            mes("You don't have enough inventory space to do that.")
        }
    }

    private fun spawnGardener(player: Player) {
        val gardener = Npc(GARDENER, player.coords.translateX(SPAWN_OFFSET_X))
        gardener.respawns = false
        npcRepo.add(gardener, LIFESPAN)
        gardener.assignSpawnOwner(player, mapClock.cycle)
        gardener.say(SHOUT)
        MiscOutput.hintArrowNpc(player, gardener.slotId)
        gardener.opPlayer2(player, aiPlayerInteractions)
    }

    private fun gardenerFor(player: Player): Npc? =
        npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius = 2).firstOrNull {
            it.isType(GARDENER) && it.isSpawnOwnedBy(player)
        }

    private fun watchOwner(gardener: Npc) {
        val owner = gardener.spawnOwner.resolve(playerList)
        val away =
            owner == null ||
                owner.coords.level != gardener.coords.level ||
                owner.coords.chebyshevDistance(gardener.coords) > LEASH
        if (away && gardener.isSlotAssigned) {
            owner?.let(MiscOutput::hintArrowReset)
            npcRepo.del(gardener, Int.MAX_VALUE)
        }
    }

    companion object {
        const val GARDENER = "npc.pirate_irate_gardener"
        const val SHOUT = "First moles, now this! Take this, vandal!"
        const val SPAWN_OFFSET_X = 2
        const val LIFESPAN = 500
        const val LEASH = 12

        val DIG_SPOTS = setOf(CoordGrid(2999, 3383, 0), CoordGrid(3000, 3383, 0))
    }
}

class PiratesTreasureGardenerAttackHook : NpcAttackValidateHook {
    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        if (!npc.isType(PiratesTreasureDig.GARDENER) || !npc.isSpawnOwnedByOther(player)) {
            return NpcAttackValidateResult.Pass
        }
        return NpcAttackValidateResult.Deny(NOT_YOUR_TARGET_MESSAGE)
    }

    private companion object {
        const val NOT_YOUR_TARGET_MESSAGE = "This is not your target."
    }
}

class PiratesTreasureGardenerKillHook : NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val npc = context.npc
        if (npc.isType(PiratesTreasureDig.GARDENER) && npc.isSpawnOwnedBy(context.hero)) {
            MiscOutput.hintArrowReset(context.hero)
        }
    }
}
