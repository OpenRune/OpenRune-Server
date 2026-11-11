package org.rsmod.content.bosses.moons

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.bosses.moons.MoonsFights.Companion.isSubdued
import org.rsmod.content.bosses.moons.MoonsFights.Companion.set
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
}

class MoonsScript
@Inject
constructor(
    private val deps: BossDeps,
    private val fights: MoonsFights,
    private val protectedAccess: ProtectedAccessLauncher,
) : PluginScript() {

    override fun ScriptContext.startup() {
        val moonsByNpc = Moon.entries.associateBy { it.npc.asRSCM(RSCMType.NPC) }
        onEvent<NpcStateEvents.Create> { moonsByNpc[npc.type.id]?.let { init(npc, it) } }

        with(deps.extensionRegistry) {
            register(MoonsExternals.FOCUS) { _, npc, _, params ->
                fights.moonOf(npc)?.let { fights.setFocus(it, params as Int) }
            }
            register(MoonsExternals.STANDARD_ATTACK) { _, npc, target, _ -> fights.standardAttack(npc, target) }
            register(MoonsExternals.BURN) { _, npc, target, _ -> fights.burnOffGlyph(npc, target) }
            register(MoonsExternals.SUBDUED) { _, _, target, _ -> fights.subdue(target) }
        }

        for (moon in Moon.entries) {
            onOpLoc1(moon.statue) { enter(moon) }
            if (moon !in ACTIVE_MOONS) continue
            onArea(moon.area) { if (!player.isSubdued(moon)) fights.begin(player, moon) }
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
        soundSynth(MoonsFights.PORTAL_SYNTH)
        delay(ENTRY_DELAY)
        telejump(moon.glyph(fights.focus(moon)), TeleportType.Exempt)
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

    private enum class WarningAnswer {
        Enter,
        EnterAndRemember,
        Stay,
    }

    private companion object {
        private const val QUEST_FIGHT_STAGE = 28
        private const val QUEST_COMPLETE = 36
        private const val ATTACK_RANGE = 15
        private const val ENTRY_DELAY = 2
        private val ACTIVE_MOONS = setOf(Moon.Blood)
        private val EXIT_STAIRS = listOf("loc.pmoon_boss_exit_stairs", "loc.pmoon_boss_exit_stairs_2")
        private val STATUE_APPROACH =
            mapOf(
                Moon.Blood to CoordGrid(1413, 9634),
                Moon.Blue to CoordGrid(1441, 9658),
                Moon.Eclipse to CoordGrid(1466, 9632),
            )
    }
}
