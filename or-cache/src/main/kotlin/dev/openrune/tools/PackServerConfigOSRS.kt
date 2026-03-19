package dev.openrune.tools

import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.*
import dev.openrune.cache.filestore.definition.ConfigDefinitionDecoder
import dev.openrune.cache.tools.TaskPriority
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.codec.osrs.*
import dev.openrune.codec.osrs.impl.*
import dev.openrune.definition.Definition
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.type.HealthBarType
import dev.openrune.definition.type.InventoryType
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.NpcType
import dev.openrune.definition.type.ObjectType
import dev.openrune.definition.type.SequenceType
import dev.openrune.definition.type.VarpType
import dev.openrune.filesystem.Cache
import dev.openrune.getCacheLocation
import dev.openrune.server.impl.item.ItemRenderDataManager
import dev.openrune.toml.TomlMapper
import dev.openrune.toml.model.TomlValue
import dev.openrune.toml.rsconfig.decodeRuneScape
import dev.openrune.toml.rsconfig.decodeRuneScapeBlocks
import dev.openrune.toml.rsconfig.rsconfig
import dev.openrune.toml.tomlMapper
import dev.openrune.toml.util.InternalAPI
import dev.openrune.types.*
import dev.openrune.types.InventoryServerType.Companion.pack
import dev.openrune.types.varp.VarpServerType
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KType
import kotlin.reflect.typeOf

data class PackType(
    /** Toml section key `[[table]]`, key in [parsedDefinitions], and [packTypes] map key. */
    val table: String,
    val tomlMapper: TomlMapper,
    val kType: KType,
    val pack: PackServerConfig.(Cache, Map<String, List<Definition>>, String) -> Unit,
)

class PackServerConfig(
    private val rev : Int,
    private val directory: File,
    private val tokenizedReplacements: Map<String, String> = emptyMap(),
    private val tokenizedFile: Path? = null,
) : CacheTask(serverTaskOnly = true) {

    val mapper = tomlMapper {
        rsconfig {
            enableConstantProvider()
            enabledTokenizedReplacement(tokenizedReplacements, tokenizedFile)
        }
    }

    override val priority: TaskPriority
        get() = TaskPriority.END

    fun Map<String, Any?>.bool(key: String, default: Boolean = true): Boolean {
        return (this[key] as? TomlValue.Bool)?.value ?: default
    }

    fun Map<String, Any?>.getTomlIntList(key: String): List<Int> {
        val value = this[key] ?: return emptyList()
        return when (value) {
            is TomlValue.List ->
                value.elements.mapNotNull { (it as? TomlValue.Integer)?.value?.toInt() }
            is TomlValue.Integer -> listOf(value.value.toInt())
            else -> emptyList()
        }
    }

    init {

        var examinesObject: Map<Int, String> =
            File("../.data/raw-cache/examines/locs.csv")
                .readText()
                .lineSequence()
                .filter { it.isNotBlank() }
                .associate { line ->
                    val (idStr, text) = line.split(",", limit = 2)
                    idStr.trim().toInt() to text.trim().removeSurrounding("\"")
                }

        registerMergedBaseAndTomlPackType<ObjectServerType, ObjectType>(
            table = "object",
            decoder = ObjectDecoder(rev),
            loadBaseInto = { c, dest -> OsrsCacheProvider.ObjectDecoder(dev.openrune.revision.first).load(c, dest) },
            codec = { base, overlay -> ObjectServerCodec(rev,base, overlay, examinesObject) },
            create = { ObjectServerType(it) },
        )

        var examinesNpc: Map<Int, String> =
            File("../.data/raw-cache/examines/npcs.csv")
                .readText()
                .lineSequence()
                .filter { it.isNotBlank() }
                .associate { line ->
                    val (idStr, text) = line.split(",", limit = 2)
                    idStr.trim().toInt() to text.trim().removeSurrounding("\"")
                }

        registerMergedBaseAndTomlPackType<NpcServerType, NpcType>(
            table = "npc",
            decoder = NpcDecoder(rev),
            loadBaseInto = { c, dest -> OsrsCacheProvider.NPCDecoder(dev.openrune.revision.first).load(c, dest) },
            codec = { base, overlay -> NpcServerCodec(rev,base, overlay, examinesNpc) },
            create = { NpcServerType(it) },
        )

        registerMergedBaseAndTomlPackType<ItemServerType, ItemType>(
            table = "item",
            decoder = ItemDecoder(rev),
            loadBaseInto = { c, dest -> OsrsCacheProvider.ItemDecoder(dev.openrune.revision.first).load(c, dest) },
            codec = { base, overlay -> ItemServerCodec(rev,base, overlay) },
            create = { ItemServerType(it) },
        )

        registerMergedBaseAndTomlPackType<VarpServerType, VarpType>(
            table = "varp",
            decoder = VarpDecoder(),
            loadBaseInto = { c, dest -> OsrsCacheProvider.VarDecoder().load(c, dest) },
            codec = { base, overlay -> VarpServerTypeCodec(base, overlay) },
            create = { VarpServerType(it) },
        )

        registerCacheBackedPackType<HealthBarServerType, HealthBarType>(
            table = "health",
            decoder = HealthBarDecoder(),
            baseDefinitions = { CacheManager.getHealthBars() },
            codec = { HealthBarServerCodec(it) },
            create = { HealthBarServerType(it) },
        )

        registerCacheBackedPackType<SequenceServerType, SequenceType>(
            table = "anims",
            decoder = SequenceDecoder(),
            baseDefinitions = { CacheManager.getAnims() },
            codec = { SequenceServerCodec(it) },
            create = { SequenceServerType(it) },
        )

        registerParsedOverlayPackType<MesAnimType>(
            table = "mesanim",
            decoder = MesAnimDecoder(),
            codec = { MesAnimServerCodec(it) },
            create = { MesAnimType(it) },
        )

        registerParsedOverlayPackType<WalkTriggerType>(
            table = "walktrigger",
            decoder = WalkTriggerDecoder(),
            codec = { WalkTriggerTypeCodec(it) },
            create = { WalkTriggerType(it) },
        )

        registerParsedOverlayPackType<VarnType>(
            table = "varn",
            decoder = VarnDecoder(),
            codec = { VarnTypeCodec(it) },
            create = { VarnType(it) },
        )

        registerParsedOverlayPackType<VarnBitType>(
            table = "varnbit",
            decoder = VarnBitDecoder(),
            codec = { VarnBitTypeCodec(it) },
            create = { VarnBitType(it) },
        )

        registerParsedOverlayPackType<VarConType>(
            table = "varcon",
            decoder = VarConDecoder(),
            codec = { VarConCodec(it) },
            create = { VarConType(it) },
        )

        registerParsedOverlayPackType<VarObjBitType>(
            table = "varobj",
            decoder = VarObjBitDecoder(),
            codec = { VarObjCodec(it) },
            create = { VarObjBitType(it) },
        )

        registerParsedOverlayPackType<VarConBitType>(
            table = "varconbit",
            decoder = VarConBitDecoder(),
            codec = { VarConBitTypeCodec(it) },
            create = { VarConBitType(it) },
        )

        registerParsedOverlayPackType<HuntModeType>(
            table = "hunt",
            decoder = HuntModeDecoder(),
            codec = { HuntCodec(it) },
            create = { HuntModeType(it) },
        )

        registerParsedOverlayPackType<StatType>(
            table = "stat",
            decoder = StatTypeDecoder(),
            codec = { StatTypeServerCodec(it) },
            create = { StatType(it) },
        )

        registerParsedOverlayPackType<ProjAnimType>(
            table = "projectile",
            decoder = ProjectileTypeDecoder(),
            codec = { ProjectileTypeServerCodec(it) },
            create = { ProjAnimType(it) },
        )

        registerParsedOverlayPackType<BasType>(
            table = "bas",
            decoder = BasTypeDecoder(),
            codec = { BasCodec(it) },
            create = { BasType(it) },
        )

        registerParsedOverlayPackType<ModLevelType>(
            table = "modlevel",
            decoder = ModLevelDecoder(),
            tomlMapper =
                tomlMapper {
                    addDecoder<ModLevelType> { content, def: ModLevelType ->
                        val permissions = content.getTomlIntList("permissions")
                        def.apply {
                            var flags = 0L
                            for (permId in permissions) {
                                flags = flags or (1L shl permId)
                            }
                            this.accessflags = flags
                        }
                    }
                },
            codec = { ModLevelServerCodec(it) },
            create = { ModLevelType(it) },
        )

        registerMergedBaseAndTomlPackType<InventoryServerType, InventoryType>(
            table = "inventory",
            decoder = InventoryDecoder(),
            tomlMapper =
                tomlMapper {
                    addDecoder<InventoryServerType> { content, def: InventoryServerType ->
                        def.apply {
                            flags =
                                pack(
                                    protect = content.bool("protect", true),
                                    allStock = content.bool("allStock", false),
                                    restock = content.bool("restock", false),
                                    runWeight = content.bool("runWeight", false),
                                    dummyInv = content.bool("dummyInv", false),
                                    placeholders = content.bool("placeholders", false),
                                )
                        }
                    }
                },
            loadBaseInto = { c, dest -> OsrsCacheProvider.InventoryDecoder().load(c, dest) },
            codec = { base, overlay -> InventoryServerCodec(base, overlay) },
            create = { InventoryServerType(it) },
        )
    }

    private fun <T : Definition> packDefs(
        cache: Cache,
        archive: Int,
        ids: Iterable<Int>,
        create: (Int) -> T,
        codec: OpcodeDefinitionCodec<T>,
    ) {
        for (id in ids) {
            cache.write(CONFIGS, archive, id, codec.encodeToBuffer(create(id)))
        }
    }

    private inline fun <reified T : Definition> parsedById(
        parsedDefinitions: Map<String, List<Definition>>,
        key: String,
    ): Map<Int, T> =
        parsedDefinitions[key]?.filterIsInstance<T>()?.associateBy { it.id } ?: emptyMap()

    @OptIn(InternalAPI::class)
    override fun init(cache: Cache) {
        val parsedDefinitions = mutableMapOf<String, MutableList<Definition>>()
        CacheManager.init(OsrsCacheProvider(Cache.load(Path.of(getCacheLocation())), revision))

        ItemRenderDataManager.init()

        val files = getFiles(directory, "toml")

        for (file in files) {
            val blocks = mapper.decodeRuneScapeBlocks(file.toPath())
            for (block in blocks) {
                val packType = packTypes[block.name] ?: continue
                val def =
                    packType.tomlMapper.decodeRuneScape(packType.kType, block.map.properties)
                        as Definition
                parsedDefinitions.getOrPut(packType.table) { mutableListOf() }.add(def)
            }
        }

        val parsed: Map<String, List<Definition>> = parsedDefinitions
        val progress = progress("Packing Server Configs", packTypes.size)
        for (packType in packTypes.values) {
            progress.extraMessage = packType.table
            packType.pack(this, cache, parsed, packType.table)
            progress.step()
        }
        progress.close()
    }

    companion object {

        private val tomlMapperDefault = tomlMapper {}
        private val packTypes = LinkedHashMap<String, PackType>()

        /**
         * Pack every id from [baseDefinitions]; Toml section `[[table]]` (for any overlay files);
         * archive from [decoder]. [B] is the client/base definition from cache (e.g. [NpcType]);
         * [T] is the server pack row ([NpcServerType]).
         */
        private inline fun <reified T : Definition, B : Definition> registerCacheBackedPackType(
            table: String,
            decoder: ConfigDefinitionDecoder<*>,
            tomlMapper: TomlMapper = tomlMapperDefault,
            crossinline baseDefinitions: () -> Map<Int, B>,
            crossinline codec: (Map<Int, B>) -> OpcodeDefinitionCodec<T>,
            noinline create: (Int) -> T,
        ) {
            registerPackType<T>(table, tomlMapper) { cache, _, _ ->
                val defs = baseDefinitions()
                packDefs(cache, decoder.getArchive(0), defs.keys.sorted(), create, codec(defs))
            }
        }

        /**
         * Client/base defs loaded via [loadBaseInto], Toml overlay under `[[table]]`, pack union of
         * ids. Archive from [decoder] (config archive id for this definition group).
         */
        private inline fun <
            reified T : Definition,
            B : Definition,
        > registerMergedBaseAndTomlPackType(
            table: String,
            decoder: ConfigDefinitionDecoder<*>,
            tomlMapper: TomlMapper = tomlMapperDefault,
            crossinline loadBaseInto: (Cache, MutableMap<Int, B>) -> Unit,
            crossinline codec: (Map<Int, B>, Map<Int, T>) -> OpcodeDefinitionCodec<T>,
            noinline create: (Int) -> T,
        ) {
            registerPackType<T>(table, tomlMapper) { cache, parsed, _ ->
                val base = mutableMapOf<Int, B>().apply { loadBaseInto(cache, this) }
                val overlay = parsedById<T>(parsed, table)
                val ids = (base.keys + overlay.keys).distinct().sorted()
                packDefs(cache, decoder.getArchive(0), ids, create, codec(base, overlay))
            }
        }

        /** Pack ids from Toml only under `[[table]]`; [codec] receives the parsed overlay map. */
        private inline fun <reified T : Definition> registerParsedOverlayPackType(
            table: String,
            decoder: ConfigDefinitionDecoder<*>,
            tomlMapper: TomlMapper = tomlMapperDefault,
            crossinline codec: (Map<Int, T>) -> OpcodeDefinitionCodec<T>,
            noinline create: (Int) -> T,
        ) {
            registerPackType<T>(table, tomlMapper) { cache, parsed, _ ->
                val overlay = parsedById<T>(parsed, table)
                packDefs(
                    cache,
                    decoder.getArchive(0),
                    overlay.keys.sorted(),
                    create,
                    codec(overlay),
                )
            }
        }

        /**
         * [table] is the Toml section (`[[table]]`), parsed map key, and progress label. [T] is the
         * list element type for Toml decode (`typeOf<List<T>>()`).
         */
        private inline fun <reified T : Definition> registerPackType(
            table: String,
            tomlMapper: TomlMapper = tomlMapperDefault,
            noinline pack: PackServerConfig.(Cache, Map<String, List<Definition>>, String) -> Unit,
        ) {
            packTypes[table] = PackType(table, tomlMapper, typeOf<List<T>>(), pack)
        }
    }
}
