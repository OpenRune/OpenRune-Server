package org.rsmod.content.bosses.hueycoatl

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

internal class HueycoatlInstance @Inject constructor(
    private val registry: BossInstanceRegistry,
    private val fights: HueycoatlFightManager,
    private val locRepo: LocRepository,
) : InstanceScript(registry) {

    override fun settingsRow(): String = SETTINGS_ROW

    override fun area(): InstanceArea = INSTANCE

    override fun runsPreludeOnFreshRun(): Boolean = true

    override fun ScriptContext.configure() {
        onEnterPrelude { result, enter ->
            val session =
                when (result) {
                    is InstanceManager.Result.Created -> result.session
                    is InstanceManager.Result.Joined -> result.session
                    else -> return@onEnterPrelude
                }
            withInstanceEnterTransition(InstanceEnterTransition(), enter)
            if (result is InstanceManager.Result.Joined && result.isFreshRun()) {
                fights.removeInstance(session.id)
            }
            fights.ensureInstance(session)
            placeInstanceLocs(session)
        }
        onEnterObject { openInstanceMenu() }
        onExitObject { defaultLeaveFlow() }
        onInstancePlayerLeave {
            fights.fightForInstance(instanceId)?.removeParticipant(player)
            VarPlayerIntMapSetter.set(player, "varbit.huey_in_area", 0)
        }
        onInstanceEnded { fights.removeInstance(instanceId) }
    }

    private fun placeInstanceLocs(session: InstanceSession) {
        val flag = manager.resolveCoord(session, CoordGrid(1525, 3295)) ?: return
        val entrance = manager.resolveCoord(session, CoordGrid(1522, 3295)) ?: return
        locRepo.add(flag, HueycoatlLocs.MEETING_FLAG_INSTANCED, Int.MAX_VALUE, LocAngle[0], LocShape[10])
        locRepo.add(entrance, HueycoatlLocs.HOLDING_ENTRANCE, Int.MAX_VALUE, LocAngle[0], LocShape[10])
    }

    private suspend fun ProtectedAccess.openInstanceMenu() {
        if (!QuestRequirements.hasCompleted(player, QUEST)) {
            mes("You must complete Children of the Sun before you can fight the Hueycoatl.")
            return
        }
        val spec = registry.get(key) ?: return mes("No Hueycoatl instance is available.")
        val owned = player.uuid?.let { manager.sessionOwnedBy(key, it) }
        if (owned != null) {
            if (manager.sessionForPlayer(player)?.id == owned.id) {
                val pick =
                    choice2(
                        "Edit access.", 1,
                        "Cancel.", 2,
                        title = spec.bossName,
                    )
                if (pick == 1) editAccess(owned)
            } else {
                val pick =
                    choice3(
                        "Rejoin instance.", 1,
                        "Edit access.", 2,
                        "Cancel.", 3,
                        title = spec.bossName,
                    )
                when (pick) {
                    1 ->
                        completeInstanceEntry(
                            manager.join(player, owned, worldClock.cycle, code = null, forceAccess = true),
                        )
                    2 -> editAccess(owned)
                }
            }
            return
        }
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }
        val pick =
            choice4(
                "Start a public/friends fight (based on privacy setting).", 1,
                "Start a solo fight.", 2,
                "Join a public/friends fight.", 3,
                "Cancel.", 4,
                title = "Select an option",
            )
        when (pick) {
            1 -> {
                val result =
                    manager.create(
                        player,
                        key,
                        spec.copy(maxPlayers = 20),
                        InstanceAccess.Public,
                        worldClock.cycle,
                    )
                completeInstanceEntry(result)
            }
            2 -> {
                val result =
                    manager.create(
                        player,
                        key,
                        spec.copy(maxPlayers = 1),
                        InstanceAccess.Private,
                        worldClock.cycle,
                    )
                completeInstanceEntry(result)
            }
            3 -> joinByName()
        }
    }

    private suspend fun ProtectedAccess.joinByName() {
        val name = stringDialog("Enter the player's name:")
        if (name.isBlank()) {
            mes("No player name entered.")
            return
        }
        val session = manager.sessionForKeyAndMemberName(key, name.trim())
        if (
            session == null ||
                (session.access !is InstanceAccess.Public && session.access !is InstanceAccess.Friends)
        ) {
            mes("There is no public or friends Hueycoatl fight to join for that player.")
            return
        }
        completeInstanceEntry(
            manager.join(player, session, worldClock.cycle, code = null, forceAccess = false),
        )
    }

    private suspend fun ProtectedAccess.editAccess(session: InstanceSession) {
        val current =
            when (val access = session.access) {
                InstanceAccess.Private -> "Private"
                InstanceAccess.Friends -> "Friends"
                InstanceAccess.Public -> "Public"
                is InstanceAccess.Code -> "Code (${access.value})"
            }
        mes("Current access: $current")
        val pick =
            choice4(
                "Private - only you.", 1,
                "Friends - anyone.", 2,
                "Public - anyone on the world.", 3,
                "Cancel.", 4,
                title = "Change instance access?",
            )
        val access =
            when (pick) {
                1 -> InstanceAccess.Private
                2 -> InstanceAccess.Friends
                3 -> InstanceAccess.Public
                else -> return mes("No changes made.")
            }
        session.access = access
        mes("Instance access updated.")
    }

    private companion object {
        private const val SETTINGS_ROW = "dbrow.instance_hueycoatl"
        private const val QUEST = "quest_childrenofthesun"

        private val INSTANCE =
            InstanceArea.copyRegions(
                centerRegionId = 5939,
                npcSpawns =
                    listOf(
                        InstanceNpc("npc.huey_lobby_leader", CoordGrid(1528, 3293)),
                        InstanceNpc("npc.huey_lobby_seer", CoordGrid(1531, 3292)),
                        InstanceNpc("npc.huey_lobby_chef_multibase", CoordGrid(1534, 3289)),
                        InstanceNpc("npc.huey_lobby_guard", CoordGrid(1531, 3287)),
                        InstanceNpc("npc.huey_lobby_guard_viero_multibase", CoordGrid(1523, 3293)),
                        InstanceNpc("npc.huey_lobby_worker_1", CoordGrid(1532, 3290)),
                        InstanceNpc("npc.huey_lobby_worker_2", CoordGrid(1528, 3290)),
                        InstanceNpc("npc.huey_nelta", CoordGrid(1525, 3290)),
                    ),
            )
    }
}
