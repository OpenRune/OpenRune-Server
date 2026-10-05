package org.rsmod.content.bosses.doom

import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.realm.Realm
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.plugin.scripts.ScriptContext

class DoomInstance
@Inject
internal constructor(
    registry: BossInstanceRegistry,
    private val realm: Realm,
    private val delves: DoomDelves,
    private val acid: DoomAcid,
    private val loot: DoomLoot,
    private val stats: DoomStats,
) : InstanceScript(registry) {

    override fun settingsRow(): String = SETTINGS_ROW

    override fun area(): InstanceArea = INSTANCE

    override fun runsPreludeOnFreshRun(): Boolean = true

    override fun destroyWhenEmpty(): Boolean = true

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            val session =
                when (result) {
                    is InstanceManager.Result.Created -> result.session
                    is InstanceManager.Result.Joined -> result.session
                    else -> return@onEnterPrelude
                }
            val level = startingLevel()
            withInstanceEnterTransition(InstanceEnterTransition(message = JUMP_MESSAGE), enter)
            player.missedOrbs = 0
            acid.reset(player)
            loot.reset(player)
            stats.startRun(player)
            delves.placeExit(session)
            delves.startLevel(this, session, level)
        }
        onEnterObject { defaultInstanceEntry() }
        onExitObject { defaultLeaveFlow() }
        onInstancePlayerJoin { player.npcViewDistance = NPC_VIEW_DISTANCE }
        onInstancePlayerLeave {
            player.npcViewDistance = null
            player.missedOrbs = 0
            acid.reset(player)
            loot.stash(player)
            loot.reset(player)
            delves.resetLevel(player)
        }

        for (hole in listOf(HOLE_LOC, UNIQUE_HOLE_LOC)) {
            onOpLoc1(hole) { with(loot) { openEndLevel() } }
            onOpLoc2(hole) { descend() }
        }
        onOpLoc1(CHEST_LOC) { with(loot) { openChest() } }

        onIfModalButton("$END_LEVEL:btn_claim") { with(loot) { claim() } }
        onIfModalButton("$END_LEVEL:btn_descend") {
            if (player.lootClaimed) return@onIfModalButton
            with(loot) { closeEndLevel() }
            descend()
        }
        onIfModalButton("$END_LEVEL:btn_leave") {
            if (!player.lootClaimed) return@onIfModalButton
            with(loot) { closeEndLevel() }
            if (manager.sessionForPlayer(player) == null) return@onIfModalButton
            defaultLeaveFlow()
        }
        onIfModalButton("$END_LEVEL:btn_inv_all") { with(loot) { takeAll(inv) } }
        onIfModalButton("$END_LEVEL:btn_bank_all") { with(loot) { takeAll(bank) } }
        onIfModalButton("$END_LEVEL:loot_contents") {
            with(loot) {
                when (it.op) {
                    IfButtonOp.Op1 -> take(it.comsub, 1)
                    IfButtonOp.Op2 -> take(it.comsub, 5)
                    IfButtonOp.Op3 -> take(it.comsub, 10)
                    IfButtonOp.Op4 -> take(it.comsub, Int.MAX_VALUE)
                    IfButtonOp.Op5 -> take(it.comsub, countDialog())
                    IfButtonOp.Op10 -> examine(it.comsub)
                    else -> Unit
                }
            }
        }
    }

    private suspend fun ProtectedAccess.startingLevel(): Int {
        if (!realm.config.devMode) return 1
        val level =
            choice5(
                "Delve level 1", 1,
                "Delve level 3", 3,
                "Delve level 4", 4,
                "Delve level 5", 5,
                "Deeper...", 0,
                title = "Start on which delve level?",
            )
        if (level != 0) return level
        return choice3(
            "Delve level 6", 6,
            "Delve level 7", 7,
            "Delve level 8", 8,
            title = "Start on which delve level?",
        )
    }

    private suspend fun ProtectedAccess.descend() {
        if (player.lootClaimed) return
        val session = manager.sessionForPlayer(player) ?: return
        if (loot.hasUnique(player)) {
            val proceed =
                choice2(
                    "Descend anyway", true,
                    "Stay", false,
                    title = "You have a unique in your loot. Dying will lose it.",
                )
            if (!proceed) return
        }
        delves.removeHoles(session)
        val next = delves.currentLevel(player) + 1
        val landing = manager.resolveCoord(session, DoomArena.LANDING) ?: return
        withInstanceEnterTransition(InstanceEnterTransition(message = DESCEND_MESSAGE)) { telejump(landing) }
        delves.startLevel(this, session, next)
    }

    internal companion object {
        const val SETTINGS_ROW = "dbrow.instance_doom_of_mokhaiotl"

        private const val HOLE_LOC = "loc.dom_descend_hole"
        private const val UNIQUE_HOLE_LOC = "loc.dom_descend_hole_unique"
        private const val CHEST_LOC = "loc.dom_chest_loot"
        private const val END_LEVEL = "component.dom_end_level_ui"
        private const val JUMP_MESSAGE = "You jump the gap..."
        private const val DESCEND_MESSAGE = "You jump further into the burrow..."
        private const val NPC_VIEW_DISTANCE = 32

        private val INSTANCE = InstanceArea.copyRegions(centerRegionId = DoomArena.REGION)
    }
}
