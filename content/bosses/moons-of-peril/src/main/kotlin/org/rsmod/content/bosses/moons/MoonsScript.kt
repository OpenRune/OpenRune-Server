package org.rsmod.content.bosses.moons

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.hook.NeypotzliHook
import org.rsmod.api.player.hook.NeypotzliMoon
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.bosses.moons.MoonsFights.Companion.isSubdued
import org.rsmod.content.bosses.moons.MoonsFights.Companion.set
import org.rsmod.content.bosses.moons.eclipse.EclipseFights
import org.rsmod.content.bosses.moons.eclipse.Side
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

object MoonsExternals {
    const val FOCUS = "moons.focus"
    const val STANDARD_ATTACK = "moons.standard_attack"
    const val BURN = "moons.burn"
    const val SUBDUED = "moons.subdued"
    const val OVERLAYS = "moons.overlays"
    const val CIRCLE_SPAWN = "moons.circle_spawn"
    const val JAGUAR_SPAWN = "moons.jaguar_spawn"
    const val JAGUAR_ATTACK = "moons.jaguar_attack"
    const val SAP = "moons.sap"
    const val POOL_HIT = "moons.pool_hit"
    const val RAIN_THROW = "moons.rain_throw"
    const val THROW_TO = "moons.throw_to"
    const val SHIELD_SPAWN = "moons.shield_spawn"
    const val SHIELD_STEP = "moons.shield_step"
    const val SHIELD_SINK = "moons.shield_sink"
    const val RAY_FIRE = "moons.ray_fire"
    const val BIND = "moons.bind"
    const val UNBIND = "moons.unbind"
    const val MOON_HIDE = "moons.moon_hide"
    const val MOON_SHOW = "moons.moon_show"
    const val OPS_HIDE = "moons.ops_hide"
    const val OPS_SHOW = "moons.ops_show"
    const val CLONE_SPAWN = "moons.clone_spawn"
    const val CLONE_STRIKE = "moons.clone_strike"
    const val CLONE_LEAVE = "moons.clone_leave"
}

class MoonsScript
@Inject
constructor(
    private val deps: BossDeps,
    private val fights: MoonsFights,
    private val eclipse: EclipseFights,
    private val overlays: MoonsOverlays,
    private val protectedAccess: ProtectedAccessLauncher,
    private val hooks: Set<@JvmSuppressWildcards NeypotzliHook>,
) : PluginScript() {

    override fun ScriptContext.startup() {
        val moonsByNpc = Moon.entries.associateBy { it.npc.asRSCM(RSCMType.NPC) }
        onEvent<NpcStateEvents.Create> { moonsByNpc[npc.type.id]?.let { init(npc, it) } }

        ServerCacheManager.getNpc(CLONE_NPC.asRSCM(RSCMType.NPC))?.let { cloneType ->
            onModifyNpcHit(cloneType) {
                val uid = hit.sourceUid
                val attacker = if (hit.isFromPlayer && uid != null) PlayerUid(uid).resolve(deps.playerList) else null
                hit.damage = if (attacker == null) 0 else eclipse.cloneHit(attacker, hit.damage)
            }
        }
        ServerCacheManager.getNpc(MoonsFights.JAGUAR_NPC.asRSCM(RSCMType.NPC))?.let { jaguarType ->
            onModifyNpcHit(jaguarType) {
                val uid = hit.sourceUid
                val attacker = if (hit.isFromPlayer && uid != null) PlayerUid(uid).resolve(deps.playerList) else null
                hit.damage = if (attacker == null) 0 else fights.jaguarHit(npc, attacker, hit.damage)
            }
        }

        with(deps.extensionRegistry) {
            register(MoonsExternals.FOCUS) { ctx ->
                fights.moonOf(ctx.npc)?.let { fights.setFocus(it, ctx.params as Int) }
            }
            register(MoonsExternals.STANDARD_ATTACK) { _, npc, target, _ -> fights.standardAttack(npc, target) }
            register(MoonsExternals.BURN) { _, npc, target, _ -> fights.burnOffGlyph(npc, target) }
            register(MoonsExternals.SUBDUED) { _, _, target, _ ->
                fights.subdue(target)
                eclipse.subdued(target)
            }
            register(MoonsExternals.OVERLAYS) { ctx -> fights.moonOf(ctx.npc)?.let(overlays::show) }
            register(MoonsExternals.CIRCLE_SPAWN) { ctx ->
                ctx.npc.anim("seq.vfx_djinn_ueyatlalli_protection_spawn_02")
            }
            register(MoonsExternals.JAGUAR_SPAWN) { ctx -> fights.jaguarSpawned(ctx.params as Moon, ctx.npc) }
            register(MoonsExternals.JAGUAR_ATTACK) { ctx -> fights.jaguarAttack(ctx.npc) }
            register(MoonsExternals.SAP) { ctx -> fights.sap(ctx.npc) }
            register(MoonsExternals.POOL_HIT) { ctx -> fights.poolHit(ctx.npc, ctx.target, ctx.params as Int) }
            register(MoonsExternals.RAIN_THROW) { ctx -> fights.rainThrow(ctx.npc) }
            register(MoonsExternals.THROW_TO) { ctx ->
                @Suppress("UNCHECKED_CAST")
                fights.moonOf(ctx.npc)?.let { fights.throwToOffset(it, ctx.params as Pair<Int, Int>) }
            }
            register(MoonsExternals.SHIELD_SPAWN) { ctx -> eclipse.shieldSpawned(ctx.npc) }
            register(MoonsExternals.SHIELD_STEP) { eclipse.shieldStep() }
            register(MoonsExternals.SHIELD_SINK) { eclipse.shieldSink() }
            register(MoonsExternals.RAY_FIRE) { ctx -> eclipse.rayFire(ctx.target) }
            register(MoonsExternals.BIND) { eclipse.bindAll() }
            register(MoonsExternals.UNBIND) { eclipse.unbindAll() }
            register(MoonsExternals.MOON_HIDE) { ctx -> eclipse.hideMoon(ctx.npc) }
            register(MoonsExternals.MOON_SHOW) { ctx -> ctx.npc.resetTransmog() }
            register(MoonsExternals.OPS_HIDE) { ctx -> eclipse.setMoonOps(ctx.npc, false) }
            register(MoonsExternals.OPS_SHOW) { ctx -> eclipse.setMoonOps(ctx.npc, true) }
            register(MoonsExternals.CLONE_SPAWN) { ctx -> eclipse.cloneSpawned(ctx.npc, ctx.params as Side) }
            register(MoonsExternals.CLONE_STRIKE) { ctx -> eclipse.cloneStrike(ctx.params as Side) }
            register(MoonsExternals.CLONE_LEAVE) { ctx -> eclipse.cloneLeave(ctx.params as Side) }
        }

        for (moon in Moon.entries) {
            onOpLoc1(moon.statue) { enter(moon) }
            if (moon !in ACTIVE_MOONS) continue
            onArea(moon.area) {
                val privateCopy = RegionRegistry.inWorkingArea(player.coords)
                if (!privateCopy && !player.isSubdued(moon)) {
                    fights.begin(player, moon)
                    if (moon == Moon.Eclipse) eclipse.joined(player)
                }
            }
            onAreaExit(moon.area) { fights.end(player) }
        }
        for (stairs in EXIT_STAIRS) {
            onOpLoc1(stairs) { escape() }
            onOpLoc2(stairs) { escape() }
        }

        onPlayerLogin {
            if (player.vars["varp.pmoon_inside"] == 0) return@onPlayerLogin
            player.set("varp.pmoon_inside", 0)
            PathingEntityCommon.telejump(player, deps.collision, Moon.ANTECHAMBER)
        }
        onPlayerLogout { fights.end(player, quiet = true) }

        onCommand("moon") {
            desc = "Teleport outside a Moon's statue"
            invalidArgs = "Use as ::moon blood|blue|eclipse"
            cheat { teleToStatue() }
        }
        onCommand("moonquest") {
            desc = "Set the Perilous Moons quest stage (default: complete)"
            cheat { player.set("varbit.pmoon_quest", args.firstOrNull()?.toInt() ?: QUEST_COMPLETE) }
        }
        onCommand("moonhp") {
            desc = "Set your pool of the Moon you are fighting"
            invalidArgs = "Use as ::moonhp amount"
            cheat { setPool() }
        }
        onCommand("moondebug") {
            desc = "Toggle Moon hit debug messages"
            cheat { player.spam("Moon debug: ${if (fights.toggleDebug(player)) "on" else "off"}") }
        }
        onCommand("bindtest") {
            desc = "Bind yourself in place; walk clicks only turn you"
            invalidArgs = "Use as ::bindtest [ticks]"
            cheat { bindTest() }
        }
        onCommand("moonreset") {
            desc = "Clear your subdued Moons"
            cheat { Moon.entries.forEach { player.set(it.deadVarbit, 0) } }
        }
    }

    private fun init(npc: Npc, moon: Moon) {
        npc.vars["varn.flat_armour"] = moon.flatArmour
        npc.apRangeOverride = ATTACK_RANGE
        npc.apRequiresLineOfSight = false
        npc.movementLocked = true
        fights.register(moon, npc)
    }

    private suspend fun ProtectedAccess.enter(moon: Moon) {
        for (hook in hooks) {
            if (!with(hook) { mayFight(moon.hookKey) }) return
        }
        if (moon !in ACTIVE_MOONS || vars["varbit.pmoon_quest"] < QUEST_FIGHT_STAGE) {
            mes("The statue does not respond.")
            return
        }
        if (player.isSubdued(moon)) {
            mes("You have already subdued the ${moon.label}. Claim your reward from the Lunar Chest first.")
            return
        }
        if (vars["varbit.pmoon_drop_item_warning"] == 0) {
            mesbox(
                "You are about to enter a dangerous area. " +
                    "<col=7f0000>Items dropped within the arena may be irretrievable.</col>"
            )
            val answer =
                choice3(
                    "Yes. I know that dropped items may be irretrievable.",
                    WarningAnswer.Enter,
                    "Yes, and don't ask me again.",
                    WarningAnswer.EnterAndRemember,
                    "No. Don't enter.",
                    WarningAnswer.Stay,
                    title = "Continue anyway?",
                )
            if (answer == WarningAnswer.Stay) return
            if (answer == WarningAnswer.EnterAndRemember) player.set("varbit.pmoon_drop_item_warning", 1)
        }
        for (hook in hooks) {
            with(hook) { beforeFight(moon.hookKey) }
        }
        soundSynth(MoonsFights.PORTAL_SYNTH)
        delay(ENTRY_DELAY)
        telejump(fights.arrival(moon), TeleportType.Exempt)
    }

    private suspend fun ProtectedAccess.escape() {
        soundSynth(MoonsFights.PORTAL_SYNTH)
        delay(ENTRY_DELAY)
        telejump(Moon.ANTECHAMBER, TeleportType.Exempt)
    }

    private fun Cheat.teleToStatue() {
        val moon = Moon.entries.firstOrNull { it.name.equals(args.firstOrNull(), ignoreCase = true) }
        if (moon == null) {
            player.mes("Use as ::moon blood|blue|eclipse")
            return
        }
        val outside = STATUE_APPROACH.getValue(moon)
        protectedAccess.launch(player) { telejump(outside, TeleportType.Exempt) }
    }

    private fun Cheat.setPool() {
        val amount = args.firstOrNull()?.toIntOrNull() ?: return
        val fight = fights.of(player) ?: return player.mes("You are not fighting a Moon.")
        val npc = fights.npc(fight.moon) ?: return
        deps.encounterRegistry.of(npc).personalHp?.set(player, amount.coerceAtLeast(1))
    }

    private fun Cheat.bindTest() {
        val ticks = args.firstOrNull()?.toIntOrNull() ?: BIND_TEST_TICKS
        val anchor = player.coords
        player.abortRoute()
        player.bound = true
        player.spam("Bound at ${anchor.x},${anchor.z} for $ticks ticks.")
        deps.worldQueues.add(ticks) {
            if (!player.isSlotAssigned) return@add
            player.bound = false
            val moved = if (player.coords == anchor) "never moved" else "MOVED to ${player.coords.x},${player.coords.z}"
            player.spam("Released; $moved.")
        }
    }

    private val Moon.hookKey: NeypotzliMoon
        get() =
            when (this) {
                Moon.Blood -> NeypotzliMoon.Blood
                Moon.Blue -> NeypotzliMoon.Blue
                Moon.Eclipse -> NeypotzliMoon.Eclipse
            }

    private enum class WarningAnswer {
        Enter,
        EnterAndRemember,
        Stay,
    }

    private companion object {
        private const val CLONE_NPC = "npc.pmoon_boss_eclipse_clone"
        private const val QUEST_FIGHT_STAGE = 28
        private const val QUEST_COMPLETE = 36
        private const val ATTACK_RANGE = 15
        private const val ENTRY_DELAY = 2
        private const val BIND_TEST_TICKS = 30
        private val ACTIVE_MOONS = setOf(Moon.Blood, Moon.Eclipse)
        private val EXIT_STAIRS = listOf("loc.pmoon_boss_exit_stairs", "loc.pmoon_boss_exit_stairs_2")
        private val STATUE_APPROACH =
            mapOf(
                Moon.Blood to CoordGrid(1413, 9634),
                Moon.Blue to CoordGrid(1441, 9658),
                Moon.Eclipse to CoordGrid(1466, 9632),
            )
    }
}
