package org.rsmod.content.events.demonicpacts

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionNpc
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@JsonIgnoreProperties(ignoreUnknown = true)
data class ButlerJob(
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("item") val item: String,
    @param:JsonProperty("stat") val stat: String,
    @param:JsonProperty("level") val level: Int,
    @param:JsonProperty("per_minute") val perMinute: Double,
    @param:JsonProperty("xp_stat") val xpStat: String? = null,
    @param:JsonProperty("xp") val xp: Double = 0.0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ButlerFile(
    @param:JsonProperty("max_items") val maxItems: Int = 2000,
    @param:JsonProperty("job") val jobs: List<ButlerJob> = emptyList(),
)

/** Relics that give you a helper: the Minion's whistle and the demon butler's bell. */
class RelicCompanionsScript
@Inject
constructor(private val random: GameRandom, private val npcHitModifier: NpcHitModifier) :
    PluginScript() {
    private val logger = InlineLogger()

    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    private val butler: ButlerFile by lazy {
        val stream =
            RelicCompanionsScript::class.java.classLoader.getResourceAsStream(BUTLER_FILE)
                ?: error("Missing Demonic Pacts resource: $BUTLER_FILE")
        stream.use { ObjectMapper(TomlFactory()).registerKotlinModule().readValue<ButlerFile>(it) }
    }

    override fun ScriptContext.startup() {
        onRelicItem(MINION_ITEM, MINION_OPS) { summonMinion() }
        onRelicItem(MINION_ITEM, setOf("Banish")) { banishMinion() }
        onRelicItem(MINION_ITEM, setOf("Check")) { checkMinion() }
        onRelicItem(MINION_ITEM, setOf("Rules")) { mes(MINION_RULES) }
        onRelicItem(BUTLER_ITEM, BUTLER_OPS) { ringBell() }
        onRelicItem(BUTLER_ITEM, setOf("Check")) { checkButler() }
        onPlayerSoftTimer(MINION_TIMER) { player.minionAttack() }
    }

    private fun ProtectedAccess.summonMinion() {
        if (!player.hasRelic(RelicNames.MINION)) {
            mes("You need the Minion relic to use this.")
            return
        }
        VarPlayerIntMapSetter.set(player, MINION_UNTIL, nowSeconds() + MINION_SECONDS)
        player.softTimer(MINION_TIMER, MINION_ATTACK_TICKS)
        mes("<col=ef1020>Your minion answers the whistle. It will fight and loot for you for 30 minutes.")
    }

    private fun ProtectedAccess.banishMinion() {
        if (!isMinionActive(player)) {
            mes("You don't have a minion with you.")
            return
        }
        VarPlayerIntMapSetter.set(player, MINION_UNTIL, 0)
        player.clearSoftTimer(MINION_TIMER)
        mes("Your minion returns to the Abyss.")
    }

    private fun ProtectedAccess.checkMinion() {
        if (!isMinionActive(player)) {
            mes("You don't have a minion with you.")
            return
        }
        val minutes = (player.vars[MINION_UNTIL] - nowSeconds() + 59) / 60
        mes("Your minion will stay for about $minutes more minute${if (minutes == 1) "" else "s"}.")
    }

    private fun ProtectedAccess.checkButler() {
        val job = currentJob(player)
        if (job == null) {
            mes("The butler has no job. Ring the bell to give him one.")
            return
        }
        mes("The butler is set to ${job.name.lowercase()} and has ${readyItems(player)} ${itemName(job.item)} ready.")
    }

    private fun Player.minionAttack() {
        if (!isMinionActive(this)) {
            clearSoftTimer(MINION_TIMER)
            mes("Your minion returns to the Abyss.")
            return
        }
        val target = (interaction as? InteractionNpc)?.target ?: return
        if (target.hitpoints <= 0 || target.name.equals(YAMA, ignoreCase = true)) {
            return
        }
        if (coords.chebyshevDistance(target.coords) > MINION_RANGE) {
            return
        }
        val damage = MINION_MIN_HIT + random.of(MINION_MAX_HIT - MINION_MIN_HIT + 1)
        target.queueHit(this, 1, HitType.Typeless, damage, npcHitModifier)
    }

    private suspend fun ProtectedAccess.ringBell() {
        if (!player.hasRelic(RelicNames.BUTLERS_BELL)) {
            mes("You need the Butler's Bell relic to use this.")
            return
        }
        val current = currentJob(player)
        val ready = current?.let { readyItems(player) } ?: 0
        val options = mutableListOf<String>()
        if (current != null) {
            options += "Collect $ready ${itemName(current.item)}"
        }
        options += "Give the butler a new job"
        if (current != null) {
            options += "Dismiss the butler"
        }
        when (options.getOrNull(menu("Demon butler", hotkeys = true, choices = options))) {
            null -> return
            "Give the butler a new job" -> chooseJob()
            "Dismiss the butler" -> {
                collect(player, current)
                VarPlayerIntMapSetter.set(player, BUTLER_JOB, 0)
                mes("The butler bows and vanishes.")
            }
            else -> collect(player, current)
        }
    }

    private suspend fun ProtectedAccess.chooseJob() {
        val jobs = butler.jobs.filter { player.statBase(it.stat) >= it.level }
        if (jobs.isEmpty()) {
            mes("The butler has nothing you are skilled enough to direct.")
            return
        }
        val job = jobs.getOrNull(menu("Butler jobs", hotkeys = false, choices = jobs.map { it.name })) ?: return
        collect(player, currentJob(player))
        VarPlayerIntMapSetter.set(player, BUTLER_JOB, butler.jobs.indexOf(job) + 1)
        VarPlayerIntMapSetter.set(player, BUTLER_SINCE, nowSeconds())
        mes("<col=ef1020>The butler sets off to ${job.name.lowercase()}. Ring the bell to collect, even after logging out.")
    }

    private fun collect(player: Player, job: ButlerJob?) {
        job ?: return
        val count = readyItems(player)
        if (count <= 0) {
            player.mes("The butler hasn't gathered anything yet.")
            return
        }
        val id = job.item.asRSCM(RSCMType.OBJ)
        if (player.invAdd(player.inv, id, count, cert = true).failure) {
            player.mes("You need a free inventory slot to take the butler's haul.")
            return
        }
        if (job.xpStat != null && job.xp > 0) {
            player.statAdvance(job.xpStat, job.xp * count)
        }
        VarPlayerIntMapSetter.set(player, BUTLER_SINCE, nowSeconds())
        player.mes("The butler hands you $count ${itemName(job.item)}.")
    }

    private fun currentJob(player: Player): ButlerJob? = butler.jobs.getOrNull(player.vars[BUTLER_JOB] - 1)

    private fun readyItems(player: Player): Int {
        val job = currentJob(player) ?: return 0
        val since = player.vars[BUTLER_SINCE]
        if (since <= 0) {
            return 0
        }
        val minutes = (nowSeconds() - since).coerceAtLeast(0) / SECONDS_PER_MINUTE
        return (minutes * job.perMinute).toInt().coerceAtMost(butler.maxItems)
    }

    private fun itemName(item: String): String =
        ServerCacheManager.getItem(item.asRSCM(RSCMType.OBJ))?.name?.lowercase() ?: "items"

    private fun ScriptContext.onRelicItem(
        item: String,
        names: Set<String>,
        action: suspend ProtectedAccess.() -> Unit,
    ) {
        val id = runCatching { item.asRSCM(RSCMType.OBJ) }.getOrNull()
        val type = id?.let { ServerCacheManager.getItem(it) }
        if (type == null) {
            logger.warn { "Demonic Pacts League: relic item $item is not in the cache." }
            return
        }
        val ops = type.interfaceOptions
        val index = ops.indexOfFirst { op -> op != null && names.any { it.equals(op, ignoreCase = true) } }
        when (index) {
            0 -> onOpHeld1(item) { action() }
            1 -> onOpHeld2(item) { action() }
            2 -> onOpHeld3(item) { action() }
            3 -> onOpHeld4(item) { action() }
            else -> logger.warn { "Demonic Pacts League: no usable op on $item (ops=$ops)." }
        }
    }

    companion object {
        private const val BUTLER_FILE = "demonic_pacts_butler.toml"
        private const val MINION_ITEM = "obj.league_guardian_summon"
        private const val BUTLER_ITEM = "obj.league_butlers_bell"
        private const val MINION_TIMER = "timer.dp_minion"
        private const val MINION_UNTIL = "varp.dp_minion_until"
        private const val BUTLER_JOB = "varp.dp_butler_job"
        private const val BUTLER_SINCE = "varp.dp_butler_since"
        private const val YAMA = "Yama"

        private const val SECONDS_PER_MINUTE = 60.0
        private const val MINION_SECONDS = 30 * 60
        private const val MINION_ATTACK_TICKS = 3
        private const val MINION_MIN_HIT = 3
        private const val MINION_MAX_HIT = 10
        private const val MINION_RANGE = 10

        private val MINION_OPS = setOf("Summon", "Blow", "Whistle", "Call", "Activate", "Toggle")
        private val BUTLER_OPS = setOf("Ring", "Summon", "Collect", "Activate", "Call")
        private const val MINION_RULES =
            "Blow the whistle to summon your minion for 30 minutes. It hits whatever you're fighting " +
                "and puts your kills' loot in your inventory, noted."

        fun nowSeconds(): Int = (System.currentTimeMillis() / 1000).toInt()

        fun isMinionActive(player: Player): Boolean =
            player.hasRelic(RelicNames.MINION) && player.vars[MINION_UNTIL] > nowSeconds()
    }
}
