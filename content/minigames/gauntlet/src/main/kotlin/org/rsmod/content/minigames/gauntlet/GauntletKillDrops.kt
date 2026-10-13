package org.rsmod.content.minigames.gauntlet

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.random.Random
import org.rsmod.api.config.constants
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.death.NpcDropReceiveHook
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.minigames.gauntlet.layout.MonsterKind
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.module.PluginModule

class GauntletKillDropsModule : PluginModule() {
    override fun bind() {
        addSetBinding<NpcDeathKillHook>(GauntletKillDropsHook::class.java)
        addSetBinding<NpcDropReceiveHook>(GauntletShardReceiveHook::class.java)
    }
}

@Singleton
class GauntletShardReceiveHook @Inject constructor(private val runs: GauntletRuns) :
    NpcDropReceiveHook {
    override fun tryReceive(receiver: Player, obj: String, count: Int): Boolean {
        if (obj != SHARD && obj != CORRUPTED_SHARD) return false
        if (runs.runFor(receiver) == null) return false
        return receiver.invAdd(receiver.inv, obj, count).success
    }

    private companion object {
        const val SHARD = "obj.gauntlet_crystal_shard"
        const val CORRUPTED_SHARD = "obj.gauntlet_crystal_shard_hm"
    }
}

@Singleton
class GauntletKillDropsHook
@Inject
constructor(private val runs: GauntletRuns, private val objRepo: ObjRepository) :
    NpcDeathKillHook {
    override fun onKill(context: NpcDeathKillContext) {
        val kind = kindOf(context.npc) ?: return
        val run = runs.runFor(context.hero) ?: return
        val corrupted = run.mode.corrupted
        context.hero.addGauntletPoints(
            when (tierOf(kind)) {
                Tier.WEAK -> GauntletPoints.WEAK_KILL
                Tier.STRONG -> GauntletPoints.STRONG_KILL
                Tier.DEMI -> GauntletPoints.DEMI_KILL
            }
        )
        if (frameDrops(run, kind)) give(context, gauntletObj("generic_component", corrupted))
        if (tierOf(kind) == Tier.DEMI) {
            componentFor(run, kind)?.let { give(context, gauntletObj(it, corrupted)) }
        }
    }

    private fun frameDrops(run: GauntletRun, kind: MonsterKind): Boolean =
        when (tierOf(kind)) {
            Tier.WEAK -> {
                val drop = !run.weakFrameGiven || Random.nextInt(WEAK_FRAME_ODDS) == 0
                run.weakFrameGiven = true
                drop
            }
            Tier.STRONG -> {
                val drop =
                    run.strongKillsWithoutFrame >= STRONG_GUARANTEE_AFTER ||
                        Random.nextInt(STRONG_FRAME_DENOMINATOR) < STRONG_FRAME_NUMERATOR
                run.strongKillsWithoutFrame = if (drop) 0 else run.strongKillsWithoutFrame + 1
                drop
            }
            Tier.DEMI -> true
        }

    private fun componentFor(run: GauntletRun, kind: MonsterKind): String? {
        val own = OWN_COMPONENT[kind] ?: return null
        val index = COMPONENTS.indexOf(own)
        val pick =
            if (run.componentsObtained and (1 shl index) == 0) {
                index
            } else {
                COMPONENTS.indices.filter { run.componentsObtained and (1 shl it) == 0 }
                    .randomOrNull() ?: return null
            }
        run.componentsObtained = run.componentsObtained or (1 shl pick)
        return COMPONENTS[pick]
    }

    private fun give(context: NpcDeathKillContext, obj: String) {
        val duration = context.hero.lootDropDuration ?: constants.lootdrop_duration
        objRepo.add(obj, context.dropCoords, duration, context.hero, 1)
    }

    private fun tierOf(kind: MonsterKind): Tier =
        when (kind) {
            in MonsterKind.WEAK -> Tier.WEAK
            in MonsterKind.STRONG -> Tier.STRONG
            else -> Tier.DEMI
        }

    private fun kindOf(npc: Npc): MonsterKind? =
        MonsterKind.entries.firstOrNull {
            npc.isType("npc.crystal_${it.locName}") || npc.isType("npc.crystal_${it.locName}_hm")
        }

    private companion object {
        const val WEAK_FRAME_ODDS = 4
        const val STRONG_FRAME_NUMERATOR = 2
        const val STRONG_FRAME_DENOMINATOR = 7
        const val STRONG_GUARANTEE_AFTER = 2
        val COMPONENTS = listOf("melee_component", "ranged_component", "magic_component")
        val OWN_COMPONENT =
            mapOf(
                MonsterKind.BEAR to "melee_component",
                MonsterKind.DARK_BEAST to "ranged_component",
                MonsterKind.DRAGON to "magic_component",
            )
    }
}

private enum class Tier {
    WEAK,
    STRONG,
    DEMI,
}
