package org.rsmod.content.events.leaguetasks

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.VarBitType
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.bits
import io.github.classgraph.ClassGraph
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.world.WorldType

/** One league's resolved task requirements. */
public class LeagueTaskSet(
    public val key: String,
    public val index: LeagueTaskIndex,
    /** Tasks defined in a task file. */
    public val defined: Set<Int>,
    /** Tasks with no task file entry, read from their `action` row instead. */
    public val fromAction: Set<Int>,
) {
    public val league: League
        get() = index.league
}

/**
 * Loads the simple league task definitions from the `league_tasks/` TOML on the classpath. Every
 * league folder is parsed and checked on every world, so a broken file fails any boot; only the
 * leagues this world serves are [active].
 */
@Singleton
public class LeagueTaskDefinitions
@Inject
constructor(private val tasks: LeagueTasks, config: ServerConfig) {
    private val served: Set<String> =
        WorldType.supportedFrom(config.worldTypes).mapTo(HashSet()) { it.key }

    private val actionTable = "dbtable.action".asRSCM(RSCMType.DBTABLE)

    private var sets: Map<String, LeagueTaskSet> = load()

    public val active: List<LeagueTaskSet>
        get() = sets.values.filter { it.key in served }

    public fun typeOf(worldType: WorldType): Int =
        checkNotNull(sets[worldType.key]?.league?.type) {
            "No league_tasks/${worldType.key}/${LeagueTaskFiles.LEAGUE_FILE} on the classpath."
        }

    /** Re-reads the task files; on any error the current definitions stay. */
    internal fun reload() {
        sets = load()
    }

    private fun load(): Map<String, LeagueTaskSet> {
        val files = LeagueTaskFiles.parse(scan())
        return files.leagues.mapValues { (key, folder) -> resolve(files, key, folder.type) }
    }

    private fun scan(): List<LeagueTaskSource> =
        ClassGraph().acceptPaths(LeagueTaskFiles.ROOT).scan().use { result ->
            result.allResources
                .filter { it.path.endsWith(".toml") }
                .map { LeagueTaskSource(it.path, it.contentAsString) }
        }

    private fun resolve(files: LeagueTaskFiles, key: String, type: Int): LeagueTaskSet {
        val league =
            checkNotNull(tasks.league(type)) {
                "league_tasks/$key/${LeagueTaskFiles.LEAGUE_FILE}: league type $type has no task " +
                    "list in the cache."
            }
        val requirements = mutableListOf<LeagueTaskRequirement>()
        val defined = HashSet<Int>()
        val errors = mutableListOf<String>()
        for (definition in files.definitions(key).values) {
            try {
                val task = actionRow(definition)
                if (task !in league) {
                    check(definition.file.startsWith("${LeagueTaskFiles.GENERIC}/")) {
                        "${definition.where()}: league $type's task list has no such task."
                    }
                    continue
                }
                definition.requirements.mapTo(requirements) { resolve(definition, task, it) }
                defined += task
            } catch (e: IllegalStateException) {
                errors += e.message.orEmpty()
            }
        }
        check(errors.isEmpty()) {
            "${errors.size} league task definition(s) for $key are invalid:\n" +
                errors.joinToString("\n")
        }
        val fromAction = HashSet<Int>()
        for (task in league.tasks - defined) {
            val requirement = LeagueTaskRequirement.fromAction(task) ?: continue
            requirements += requirement
            fromAction += task
        }
        return LeagueTaskSet(key, LeagueTaskIndex(league, requirements), defined, fromAction)
    }

    private fun actionRow(definition: TaskDefinition): Int {
        val row =
            RSCM.getRSCMOrNull("dbrow.${definition.task}", RSCMType.DBROW)
                ?: error("${definition.where()}: there is no dbrow.${definition.task}.")
        check(ServerCacheManager.getDbrow(row)?.tableId == actionTable) {
            "${definition.where()}: dbrow.${definition.task} is not a dbtable.action row."
        }
        return row
    }

    private fun resolve(
        definition: TaskDefinition,
        task: Int,
        toml: RequirementToml,
    ): LeagueTaskRequirement {
        val where = definition.where()
        fun id(symbol: String, type: RSCMType): Int =
            RSCM.getRSCMOrNull(symbol, type)
                ?: error("$where: '$symbol' is not a known ${type.prefix} symbol.")
        fun ids(symbols: List<String>, type: RSCMType): List<Int> = symbols.map { id(it, type) }
        fun varbit(symbol: String): VarBitType =
            checkNotNull(ServerCacheManager.getVarbit(id(symbol, RSCMType.VARBIT))) {
                "$where: $symbol is not in the cache."
            }

        val trigger =
            LeagueTaskTrigger.named(toml.trigger)
                ?: error("$where: unknown trigger '${toml.trigger}'.")
        val counter = toml.counter?.let(::varbit)
        val requirement =
            LeagueTaskRequirement(
                task = task,
                trigger = trigger,
                source = where,
                stats = toml.stat.onEach { id(it, RSCMType.STAT) },
                value = toml.value,
                count = toml.times,
                objs = ids(toml.obj, RSCMType.OBJ),
                npcs = ids(toml.npc, RSCMType.NPC),
                categories = ids(toml.category, RSCMType.CATEGORY),
                slayerTask = toml.slayerTask,
                counter = counter,
                areas = ids(toml.area, RSCMType.AREA),
                tools = ids(toml.tool, RSCMType.OBJ),
                varp =
                    toml.varp?.let {
                        checkNotNull(ServerCacheManager.getVarp(id(it, RSCMType.VARP))) {
                            "$where: $it is not in the cache."
                        }
                    },
                varbit = toml.varbit?.let(::varbit),
                interf = toml.interf?.let { id(it, RSCMType.INTERFACE) },
                locs = ids(toml.loc, RSCMType.LOC),
                option = toml.option,
                emote = toml.emote,
                collections = ids(toml.collection, RSCMType.DBROW),
            )
        check(toml.times >= 1) { "$where: times must be at least 1." }
        check(trigger != LeagueTaskTrigger.Emote || toml.emote != null) {
            "$where: an emote requirement needs an emote."
        }
        check(trigger != LeagueTaskTrigger.Interface || toml.interf != null) {
            "$where: an interface requirement needs an interface."
        }
        if (trigger.isState) {
            check(requirement.areas.isEmpty()) {
                "$where: a ${toml.trigger} requirement cannot be limited to an area."
            }
            check(trigger != LeagueTaskTrigger.Var || requirement.baseVarp != null) {
                "$where: a var requirement needs a varp or varbit."
            }
            return requirement
        }
        val remembers = toml.times > 1 || definition.requirements.size > 1
        check(!remembers || counter != null) {
            "$where needs a counter varbit for its ${toml.trigger} requirement."
        }
        if (counter != null) {
            check(toml.times <= counter.maxValue) {
                "$where: counter ${toml.counter} holds ${counter.maxValue}, below times = " +
                    "${toml.times}."
            }
        }
        return requirement
    }
}

internal val VarBitType.maxValue: Int
    get() = (1 shl (bits.last - bits.first + 1)) - 1
