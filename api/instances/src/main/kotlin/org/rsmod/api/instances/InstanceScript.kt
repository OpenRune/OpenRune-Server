package org.rsmod.api.instances

import jakarta.inject.Inject
import kotlin.random.Random
import org.rsmod.api.instances.events.InstanceEndedEvent
import org.rsmod.api.instances.hook.InstanceObjectHookRegistry
import org.rsmod.api.instances.events.InstancePlayerJoinEvent
import org.rsmod.api.instances.events.InstancePlayerLeaveEvent
import org.rsmod.api.instances.events.InstanceStartedEvent
import org.rsmod.api.instances.events.InstanceTimeTickEvent
import org.rsmod.api.instances.events.instanceEventId
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.table.InstanceSettingsRow
import org.rsmod.game.MapClock
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public abstract class InstanceScript(
    private val registry: BossInstanceRegistry,
) : PluginScript() {

    @field:Inject private lateinit var objectHooks: InstanceObjectHookRegistry
    @field:Inject protected lateinit var manager: InstanceManager
    @field:Inject protected lateinit var worldClock: MapClock

    private val rowData: InstanceSettingsRow by lazy { InstanceSettingsRow.getRow(settingsRow()) }

    protected val key: String get() = rowData.key

    protected abstract fun area(): InstanceArea

    protected abstract fun settingsRow(): String

    protected open fun ScriptContext.configure() {}

    override fun ScriptContext.startup() {
        configure()
        registry.register(key, buildSpec())
    }

    protected fun settingsRowData(): InstanceSettingsRow = rowData

    protected fun settingsRowId(): Int = rowData.rowId

    protected open fun spawnOnFirstJoin(): Boolean = false

    protected fun buildSpec(area: InstanceArea = area()): InstanceSpec {
        val settings = rowData.toInstanceSettings()
        val resolved = if (spawnOnFirstJoin()) settings.copy(spawnOnFirstJoin = true) else settings
        return resolved.withArea(area.withDbCoords(rowData), rowData.rowId)
    }

    protected fun onEnterObject(action: suspend ProtectedAccess.() -> Unit) {
        objectHooks.registerEnter(key, action)
    }

    protected fun onExitObject(action: suspend ProtectedAccess.() -> Unit) {
        objectHooks.registerExit(key, action)
    }

    protected suspend fun ProtectedAccess.defaultInstanceEntry() {
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }
        val spec = registry.get(key) ?: run { mes("No instance available."); return }
        val ownedSession = player.uuid?.let { id -> manager.sessionOwnedBy(key, id) }
        if (ownedSession != null) {
            showMenuWithExistingInstance(spec, ownedSession)
        } else {
            showMenuNoInstance(spec)
        }
    }

    private suspend fun ProtectedAccess.showMenuNoInstance(spec: InstanceSpec) {
        val pick = choice4(
            "Create Private Instance.", 1,
            "Join Friend's Instance.", 2,
            "Join by Code.", 3,
            "Cancel.", 4,
            title = spec.bossName,
        )
        when (pick) {
            1 -> showCreateDialogue(spec)
            2 -> joinByName()
            3 -> joinByCode()
        }
    }

    private suspend fun ProtectedAccess.showMenuWithExistingInstance(
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
                manager.join(player, session, worldClock.cycle, code = null, forceAccess = true)
            )
            2 -> editInstanceSettings(session)
            3 -> joinByName()
            4 -> joinByCode()
        }
    }

    private suspend fun ProtectedAccess.joinByName() {
        val name = stringDialog("Enter the player's name:")
        if (name.isBlank()) return mes("No player name entered.")
        val session = manager.sessionForKeyAndMemberName(key, name.trim())
        if (session == null) {
            mes("No $key instance found for '$name'.")
            return
        }
        applyResult(manager.join(player, session, worldClock.cycle, code = null, forceAccess = false))
    }

    private suspend fun ProtectedAccess.joinByCode() {
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
        applyResult(manager.join(player, session, worldClock.cycle, trimmed, forceAccess = false))
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

    protected suspend fun ProtectedAccess.defaultLeaveFlow() {
        val session = manager.sessionForPlayer(player) ?: run {
            mes("You are not inside an instance.")
            return
        }
        val exit = manager.leave(player, session, worldClock.cycle)
        telejump(exit)
    }

    protected suspend fun ProtectedAccess.enterPublicRoom(area: InstanceArea) {
        enterPublicRoom(buildSpec(area))
    }

    protected suspend fun ProtectedAccess.enterPublicRoom(spec: InstanceSpec) {
        if (manager.sessionForPlayer(player) != null) {
            mes("You are already inside an instance.")
            return
        }
        val existing = manager.sessionsForKey(key).firstOrNull { session ->
            session.isServerOwned &&
                !session.isFull() &&
                session.state !is SessionState.Grace
        }
        val session = existing ?: manager.createServerOwned(key, spec, InstanceAccess.Friends, worldClock.cycle)
        if (session == null) {
            mes("No public room available, try again shortly.")
            return
        }
        applyResult(manager.join(player, session, worldClock.cycle, code = null, forceAccess = true))
    }

    private suspend fun ProtectedAccess.showCreateDialogue(spec: InstanceSpec) {
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

        applyResult(manager.create(player, key, spec, access, worldClock.cycle))
    }

    private fun ProtectedAccess.applyResult(result: InstanceManager.Result) {
        when (result) {
            is InstanceManager.Result.Created -> { telejump(result.enter) }
            is InstanceManager.Result.Joined -> { telejump(result.enter) }
            is InstanceManager.Result.Failed -> mes(result.reason)
        }
    }

    private fun generateCode(): String =
        buildString { repeat(CODE_LENGTH) { append(CODE_CHARS[Random.nextInt(CODE_CHARS.length)]) } }

    protected fun ScriptContext.onInstancePlayerJoin(action: InstancePlayerJoinEvent.() -> Unit) {
        onEvent(instanceEventId(key), action)
    }

    protected fun ScriptContext.onInstancePlayerLeave(action: InstancePlayerLeaveEvent.() -> Unit) {
        onEvent(instanceEventId(key), action)
    }

    protected fun ScriptContext.onInstanceStarted(action: InstanceStartedEvent.() -> Unit) {
        onEvent(instanceEventId(key), action)
    }

    protected fun ScriptContext.onInstanceEnded(action: InstanceEndedEvent.() -> Unit) {
        onEvent(instanceEventId(key), action)
    }

    protected fun ScriptContext.onInstanceTimeTick(action: InstanceTimeTickEvent.() -> Unit) {
        onEvent(instanceEventId(key), action)
    }

    private companion object {
        private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val CODE_LENGTH = 4
    }
}
