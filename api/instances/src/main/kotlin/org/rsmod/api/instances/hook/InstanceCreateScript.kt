package org.rsmod.api.instances.hook

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.random.Random
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceAccess
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.InstanceSpec
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.table.InstanceSettingsRow
import org.rsmod.game.MapClock
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
internal class InstanceCreateScript @Inject constructor(
    private val manager: InstanceManager,
    private val worldClock: MapClock,
    private val registry: BossInstanceRegistry,
    private val objectHooks: InstanceObjectHookRegistry,
) : PluginScript() {

    override fun ScriptContext.startup() {
        InstanceSettingsRow.all().forEach { row ->
            onOpLoc1(row.enterObject) {
                val custom = objectHooks.getEnter(row.key)
                if (custom != null) custom() else instanceEntry(row.key)
            }
            onOpLoc1(row.exitObject) {
                val custom = objectHooks.getExit(row.key)
                if (custom != null) custom() else leaveFlow()
            }
        }
    }

    private suspend fun ProtectedAccess.instanceEntry(key: String) {
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }
        val spec = registry.get(key) ?: run { mes("No instance available."); return }
        val ownedSession = player.uuid?.let { id -> manager.sessionOwnedBy(key, id) }
        if (ownedSession != null) {
            showMenuWithExistingInstance(key, spec, ownedSession)
        } else {
            showMenuNoInstance(key, spec)
        }
    }

    private suspend fun ProtectedAccess.showMenuNoInstance(key: String, spec: InstanceSpec) {
        val pick = choice4(
            "Create Private Instance.", 1,
            "Join Friend's Instance.", 2,
            "Join by Code.", 3,
            "Cancel.", 4,
            title = spec.bossName,
        )
        when (pick) {
            1 -> showCreateDialogue(key, spec)
            2 -> joinByName(key)
            3 -> joinByCode(key)
        }
    }

    private suspend fun ProtectedAccess.showMenuWithExistingInstance(
        key: String,
        spec: InstanceSpec,
        session: InstanceSession,
    ) {
        val pick = choice5(
            "Rejoin Private Instance.", 1,
            "Edit Instance Settings.", 2,
            "Join Friend's Instance.", 3,
            "Join by Code.", 4,
            "Cancel.", 5,
            title = spec.bossName,
        )
        when (pick) {
            1 -> applyResult(
                manager.join(player, session, worldClock.cycle, code = null, forceAccess = true),
                "Rejoining your instance...",
            )
            2 -> editInstanceSettings(session)
            3 -> joinByName(key)
            4 -> joinByCode(key)
        }
    }

    private suspend fun ProtectedAccess.joinByName(key: String) {
        val name = stringDialog("Enter the player's name:")
        if (name.isBlank()) return mes("No player name entered.")
        val session = manager.sessionForKeyAndMemberName(key, name.trim())
        if (session == null) {
            mes("No $key instance found for '$name'.")
            return
        }
        applyResult(manager.join(player, session, worldClock.cycle, code = null, forceAccess = false), "Joining instance...")
    }

    private suspend fun ProtectedAccess.joinByCode(key: String) {
        val code = stringDialog("Enter the join code:")
        if (code.isBlank()) return mes("No code entered.")
        val trimmed = code.trim().uppercase()
        val session = manager.sessionsForKey(key).firstOrNull { s ->
            (s.access as? InstanceAccess.Code)?.value?.equals(trimmed, ignoreCase = true) == true
        }
        if (session == null) {
            mes("No $key instance found with that code.")
            return
        }
        applyResult(manager.join(player, session, worldClock.cycle, trimmed, forceAccess = false), "Joining instance...")
    }

    private suspend fun ProtectedAccess.editInstanceSettings(session: InstanceSession) {
        val currentLabel = when (val a = session.access) {
            InstanceAccess.Private -> "Private"
            InstanceAccess.Friends -> "Friends"
            is InstanceAccess.Code -> "Code (${a.value})"
        }
        mes("Current access: $currentLabel")
        val pick = choice4(
            "Private - only you.", 1,
            "Friends - anyone.", 2,
            "New Code - generate a new join code.", 3,
            "Cancel.", 4,
            title = "Change instance access?",
        )
        val newAccess: InstanceAccess = when (pick) {
            1 -> InstanceAccess.Private
            2 -> InstanceAccess.Friends
            3 -> InstanceAccess.Code(generateCode())
            else -> return mes("No changes made.")
        }
        session.access = newAccess
        if (newAccess is InstanceAccess.Code) {
            mes("New join code: <col=8f0000>${newAccess.value}</col>")
        } else {
            mes("Instance access updated.")
        }
    }

    private suspend fun ProtectedAccess.showCreateDialogue(key: String, spec: InstanceSpec) {
        val fee = "%,d".format(spec.fee)
        mes("<col=8f0000>Instance Setup:</col> ${spec.bossName}")
        mes("Recommended Combat: ${spec.recommendedCombat ?: "-"}+ | Team Size: ${spec.teamSize}")

        val pick = choice4(
            "Private - only you.", 1,
            "Friends - anyone.", 2,
            "Code - share a join code.", 3,
            "Cancel.", 4,
            title = "Who can join your instance?",
        )
        val access: InstanceAccess = when (pick) {
            1 -> InstanceAccess.Private
            2 -> InstanceAccess.Friends
            3 -> InstanceAccess.Code(generateCode())
            else -> return mes("Instance creation cancelled.")
        }
        if (access is InstanceAccess.Code) {
            mes("Join code: <col=8f0000>${access.value}</col> - share it to let others join.")
        }

        val confirm = choice2(
            "Yes - pay $fee coins and create.", true,
            "No.", false,
            title = "Create a ${spec.bossName} instance?",
        )
        if (!confirm) return mes("Instance creation cancelled.")

        applyResult(
            manager.create(player, key, spec, access, worldClock.cycle),
            "Your ${spec.bossName} instance is ready. Entering...",
        )
    }

    private suspend fun ProtectedAccess.leaveFlow() {
        val session = manager.sessionForPlayer(player) ?: run {
            mes("You are not inside an instance.")
            return
        }
        val exit = manager.leave(player, session, worldClock.cycle)
        telejump(exit)
    }

    private fun ProtectedAccess.applyResult(result: InstanceManager.Result, success: String) {
        when (result) {
            is InstanceManager.Result.Created -> { mes(success); telejump(result.enter) }
            is InstanceManager.Result.Joined -> { mes(success); telejump(result.enter) }
            is InstanceManager.Result.Failed -> mes(result.reason)
        }
    }

    private fun generateCode(): String =
        buildString { repeat(CODE_LENGTH) { append(CODE_CHARS[Random.nextInt(CODE_CHARS.length)]) } }

    private companion object {
        private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val CODE_LENGTH = 4
    }
}
