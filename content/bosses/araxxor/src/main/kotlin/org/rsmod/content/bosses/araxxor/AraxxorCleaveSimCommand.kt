package org.rsmod.content.bosses.araxxor

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val ENRAGED_PHASE = "enraged"
private const val CLEAVE_ABILITY = "cleave"
private const val SEARCH_RADIUS = 40
private const val WAIT_FOR_ENRAGE_TICKS = 15
private const val WATCH_TICKS = 16

/**
 * `::araxxorcleave [dx dz]` replays the sequence that makes the enraged cleave hit Araxxor: drop
 * him under the enrage threshold, wait for the enrage phase, stand the player beside his body,
 * step them inside it (offset from his south-west tile, default his middle) the tick the cleave
 * is cast, and trace every tick until it lands. Each stage is reported in chat so the failing stage is visible.
 */
class AraxxorCleaveSimCommand
@Inject
constructor(private val deps: BossDeps, private val npcList: NpcList) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("araxxorcleave") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Stage the enraged cleave on a player standing inside Araxxor and trace it"
            invalidArgs = "Use as ::araxxorcleave [dx dz] (offsets from his SW tile, default 3 3)"
            cheat { simulate() }
        }
    }

    private fun Cheat.simulate() {
        val dx = args.getOrNull(0)?.toIntOrNull() ?: BOSS_CENTRE_OFFSET
        val dz = args.getOrNull(1)?.toIntOrNull() ?: BOSS_CENTRE_OFFSET
        val boss = nearestBoss(player)
        if (boss == null) {
            player.mes("[cleave sim] No living Araxxor within $SEARCH_RADIUS tiles.")
            return
        }
        val size = boss.visType.size
        if (dx !in 0 until size || dz !in 0 until size) {
            player.mes("[cleave sim] Offsets must be inside his body: 0..${size - 1}.")
            return
        }
        val encounter = deps.encounterRegistry.of(boss)
        if (encounter.currentPhaseName == ENRAGED_PHASE) {
            player.mes("[cleave sim] Already enraged; staging the cleave now.")
            watch(player, boss, dx, dz)
            return
        }
        val enrageHp = (boss.baseHitpointsLvl * ENRAGE_FRACTION).toInt() - 1
        boss.hitpoints = minOf(boss.hitpoints, enrageHp.coerceAtLeast(1))
        player.mes("[cleave sim] Araxxor set to ${boss.hitpoints} hp; waiting for the enrage phase.")
        deps.repeatTick(
            ticks = WAIT_FOR_ENRAGE_TICKS,
            onTick = tick@{
                if (!canContinue(player, boss)) return@tick false
                if (encounter.currentPhaseName != ENRAGED_PHASE) return@tick true
                watch(player, boss, dx, dz)
                false
            },
            onStop = {
                if (encounter.currentPhaseName != ENRAGED_PHASE && canContinue(player, boss)) {
                    player.mes(
                        "[cleave sim] Enrage phase never started; he is in '${encounter.currentPhaseName}'."
                    )
                }
            },
        )
    }

    private fun watch(player: Player, boss: Npc, dx: Int, dz: Int) {
        val encounter = deps.encounterRegistry.of(boss)
        val inside = boss.coords.translate(dx, dz)
        val outside = boss.coords.translate(boss.visType.size, dz)
        val startTick = deps.mapClock.cycle
        PathingEntityCommon.telejump(player, deps.collision, outside)
        player.mes("[cleave sim] T0: you are at $outside, boss at ${boss.coords}; waiting for the cast.")

        var castTick = -1
        var lastHp = boss.hitpoints
        var selfHits = 0
        deps.repeatTick(
            ticks = WATCH_TICKS,
            onTick = tick@{ remaining ->
                if (!canContinue(player, boss)) return@tick false
                val tick = deps.mapClock.cycle - startTick
                val cast =
                    encounter.lastAbilityName == CLEAVE_ABILITY &&
                        encounter.lastAbilityTick >= startTick
                if (cast && castTick < 0) {
                    castTick = tick
                    PathingEntityCommon.telejump(player, deps.collision, inside)
                    player.mes("[cleave sim] T$tick: cleave cast; you stepped under him at $inside.")
                }
                val hp = boss.hitpoints
                if (hp < lastHp) {
                    selfHits += lastHp - hp
                    player.mes("[cleave sim] T$tick: Araxxor lost ${lastHp - hp} hp (cleave self-hit).")
                }
                lastHp = hp
                player.mes(
                    "[cleave sim] T$tick: boss=${boss.coords} you=${player.coords} " +
                        "locked=${boss.movementLocked} nextAttack=${encounter.nextAttackTick}"
                )
                remaining > 1
            },
            onStop = { report(player, castTick, selfHits) },
        )
    }

    private fun report(player: Player, castTick: Int, selfHits: Int) {
        val verdict =
            when {
                castTick < 0 -> "FAIL: the boss never cast the cleave."
                selfHits == 0 -> "FAIL: cleave cast at T$castTick but Araxxor took no damage."
                else -> "PASS: cleave cast at T$castTick and Araxxor took $selfHits damage."
            }
        player.mes("[cleave sim] $verdict")
    }

    private fun canContinue(player: Player, boss: Npc): Boolean =
        boss.isSlotAssigned && boss.hitpoints > 0 && player.isValidTarget()

    private fun nearestBoss(player: Player): Npc? {
        val araxxorId = ARAXXOR.asRSCM(RSCMType.NPC)
        return npcList
            .filter {
                it.visType.id == araxxorId &&
                    it.isSlotAssigned &&
                    it.hitpoints > 0 &&
                    it.coords.level == player.coords.level &&
                    it.edgeDistanceTo(player.coords) <= SEARCH_RADIUS
            }
            .minByOrNull { it.edgeDistanceTo(player.coords) }
    }

    private companion object {
        private const val ENRAGE_FRACTION = 0.25
    }
}
