package org.rsmod.content.bosses.bryophyta

import jakarta.inject.Inject
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.exitLocObjects
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class BryophytaInstance
@Inject
constructor(
    registry: BossInstanceRegistry,
    private val deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : InstanceScript(registry) {

    private var Player.lairUnlocked by boolVarBit("varbit.bryophyta_lair_unlocked")
    private var Player.chestsOpened by intVarp("varp.bryophyta_chests_opened")

    override fun settingsRow(): String = "dbrow.instance_bryophyta"

    override fun area(): InstanceArea = INSTANCE

    override fun destroyWhenEmpty(): Boolean = true

    override fun recordsKillTime(player: Player): Boolean = false

    override fun ScriptContext.configure() {
        onEnterObject { openGate() }
        onExitObject { leaveLair() }
        onExitObjectApproach2 { leaveLair() }
        settingsRowData().exitLocObjects().forEach { loc ->
            onOpLoc3(loc) { quickExitLair() }
        }
        onInstancePlayerJoin {
            val boss = manager.npcsForInstance(instanceId).firstOrNull { npc ->
                npc.isType(BOSS_NPC) && npc.hitpoints > 0
            } ?: return@onInstancePlayerJoin
            boss.respawns = false
            boss.movementLocked = true
            boss.apRangeOverride = MAGIC_RANGE
            boss.apRequiresLineOfSight = false
            boss.apPlayer2(player, aiPlayerInteractions)
        }
        onOpLoc2(GATE) {
            mesbox("You have opened Bryophyta's chest a total of ${player.chestsOpened} times.")
        }
    }

    private suspend fun ProtectedAccess.openGate() {
        val hasKey = invTotal(player.inv, MOSSY_KEY) > 0
        if (!player.lairUnlocked && !hasKey) {
            mes("It's locked!")
            return
        }
        mesbox(INSTANCE_WARNING)
        val enter =
            choice2(
                "Yes, let's go!",
                true,
                "I don't think I'm quite ready yet.",
                false,
                title = "Are you sure you wish to open it?",
            )
        if (!enter) {
            mes("You decide not to open the door.")
            return
        }
        if (!player.lairUnlocked) {
            player.lairUnlocked = true
        }
        mes("Your key fits the gate, causing it to swing open.", ChatType.Spam)
        defaultInstanceEntry()
    }

    private suspend fun ProtectedAccess.leaveLair() {
        mes("Cautiously, you climb out of the damp cave.", ChatType.Spam)
        defaultLeaveFlow()
    }

    private suspend fun ProtectedAccess.quickExitLair() {
        defaultLeaveFlow()
    }

    private companion object {
        private const val BOSS_NPC = "npc.gb_mossgiant"
        private const val GATE = "loc.gb_moss_door_in"
        private const val MOSSY_KEY = "obj.mossy_key"
        private const val MAGIC_RANGE = 10

        private const val INSTANCE_WARNING =
            "<col=7f0000>Warning!</col> You're about to enter an instanced area. Anything left " +
                "on the ground when you leave will be lost. Would you like to continue?"

        private val INSTANCE =
            InstanceArea.copyRegions(
                centerRegionId = 12955,
                npcSpawns = listOf(InstanceNpc(BOSS_NPC, CoordGrid(3220, 9933))),
            )
    }
}
