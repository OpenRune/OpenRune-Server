package dev.openrune

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.OsrsCacheProvider.*
import dev.openrune.cache.CacheManager
import dev.openrune.cache.filestore.definition.ComponentDecoder
import dev.openrune.cache.filestore.definition.FontDecoder
import dev.openrune.cache.filestore.definition.InterfaceType
import dev.openrune.cache.getOrDefault
import dev.openrune.codec.osrs.BasTypeDecoder
import dev.openrune.codec.osrs.HealthBarDecoder
import dev.openrune.codec.osrs.HuntModeDecoder
import dev.openrune.codec.osrs.InventoryDecoder
import dev.openrune.codec.osrs.ItemDecoder
import dev.openrune.codec.osrs.MesAnimDecoder
import dev.openrune.codec.osrs.ModLevelDecoder
import dev.openrune.codec.osrs.NpcDecoder
import dev.openrune.codec.osrs.ObjectDecoder
import dev.openrune.codec.osrs.ProjectileTypeDecoder
import dev.openrune.codec.osrs.SequenceDecoder
import dev.openrune.codec.osrs.StatTypeDecoder
import dev.openrune.codec.osrs.VarConBitDecoder
import dev.openrune.codec.osrs.VarConDecoder
import dev.openrune.codec.osrs.VarObjBitDecoder
import dev.openrune.codec.osrs.VarnBitDecoder
import dev.openrune.codec.osrs.VarnDecoder
import dev.openrune.codec.osrs.VarpDecoder
import dev.openrune.codec.osrs.WalkTriggerDecoder
import dev.openrune.definition.type.*
import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.filesystem.Cache
import dev.openrune.gamevals.GameValProvider
import dev.openrune.net.CacheJs5GroupProvider
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.*
import dev.openrune.types.VarConType
import dev.openrune.types.aconverted.AreaType
import dev.openrune.types.aconverted.CategoryType
import dev.openrune.types.aconverted.ContentGroupType
import dev.openrune.types.aconverted.CurrencyType
import dev.openrune.types.aconverted.MidiType
import dev.openrune.types.aconverted.QueueType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.aconverted.SynthType
import dev.openrune.types.aconverted.TimerType
import dev.openrune.types.varp.VarpServerType
import java.nio.BufferUnderflowException
import java.nio.file.Path
import java.nio.file.Paths

object ServerCacheManager {

    private val items: MutableMap<Int, ItemServerType> = mutableMapOf()
    private val npcs: MutableMap<Int, NpcServerType> = mutableMapOf()
    private val objects: MutableMap<Int, ObjectServerType> = mutableMapOf()
    private val healthBars: MutableMap<Int, HealthBarServerType> = mutableMapOf()
    private val structs: MutableMap<Int, StructType> = mutableMapOf()
    private val dbrows: MutableMap<Int, DBRowType> = mutableMapOf()
    private val dbtables: MutableMap<Int, DBTableType> = mutableMapOf()
    private val enums: MutableMap<Int, EnumType> = mutableMapOf()
    private val varbits: MutableMap<Int, VarBitType> = mutableMapOf()
    private val varps: MutableMap<Int, VarpServerType> = mutableMapOf()
    private val sequences = mutableMapOf<Int, SequenceServerType>()
    private var fonts = mutableMapOf<Int, FontType>()
    private var interfaces = mutableMapOf<Int, InterfaceType>()
    private var inv = mutableMapOf<Int, InventoryServerType>()
    private var mesanim = mutableMapOf<Int, MesAnimType>()
    private var modLevels = mutableMapOf<Int, ModLevelType>()
    private var statTypes = mutableMapOf<Int, StatType>()
    private var projectiles = mutableMapOf<Int, ProjAnimType>()
    private var hitsplats = mutableMapOf<Int, HitSplatType>()
    private var bas = mutableMapOf<Int, BasType>()
    private val walkTriggers: MutableMap<Int, WalkTriggerType> = mutableMapOf()
    private val varns: MutableMap<Int, VarnType> = mutableMapOf()
    private val varnbit: MutableMap<Int, VarnBitType> = mutableMapOf()
    private val varcon: MutableMap<Int, VarConType> = mutableMapOf()
    private val varconbit: MutableMap<Int, VarConBitType> = mutableMapOf()
    private val varobjBit: MutableMap<Int, VarObjBitType> = mutableMapOf()
    private val params: MutableMap<Int, ParamType> = mutableMapOf()
    private val hunt: MutableMap<Int, HuntModeType> = mutableMapOf()

    val logger = InlineLogger()

    var PROJANIM: CacheVarLiteral = CacheVarLiteral.registerExternal(253, '[', name = "PROJANIM")
    var VARBIT: CacheVarLiteral = CacheVarLiteral.registerExternal(254, ']', name = "VARBIT")

    fun init(rev: Int): Cache {
        GameValProvider.load()

        val cacheDir =
            Paths.get(System.getProperty("user.dir"))
                .resolve(Paths.get(".data", "cache", "SERVER"))
                .normalize()
        val cache = Cache.load(cacheDir)
        return init(cache, rev)
    }

    fun init(cachePath: Path, rev: Int): Cache {
        val cache = Cache.load(cachePath)
        return init(cache, rev)
    }

    fun init(cache: Cache, rev: Int): Cache {
        val liveDir =
            Paths.get(System.getProperty("user.dir"))
                .resolve(Paths.get(".data", "cache", "LIVE"))
                .normalize()
        CacheJs5GroupProvider.load(liveDir)

        fonts = FontDecoder(cache).loadAllFonts()

        try {
            EnumDecoder().load(cache, enums)
            ObjectDecoder(rev).load(cache, objects)
            HealthBarDecoder().load(cache, healthBars)
            NpcDecoder(rev).load(cache, npcs)
            ItemDecoder(rev).load(cache, items)
            InventoryDecoder().load(cache, inv)
            SequenceDecoder().load(cache, sequences)
            VarBitDecoder().load(cache, varbits)
            VarpDecoder().load(cache, varps)
            StructDecoder().load(cache, structs)
            DBRowDecoder().load(cache, dbrows)
            DBTableDecoder().load(cache, dbtables)
            ComponentDecoder(cache).load(interfaces)
            MesAnimDecoder().load(cache, mesanim)
            ModLevelDecoder().load(cache, modLevels)
            StatTypeDecoder().load(cache, statTypes)
            ProjectileTypeDecoder().load(cache, projectiles)
            HitSplatDecoder().load(cache, hitsplats)
            BasTypeDecoder().load(cache, bas)
            WalkTriggerDecoder().load(cache, walkTriggers)
            VarnDecoder().load(cache, varns)
            VarConBitDecoder().load(cache, varconbit)
            VarConDecoder().load(cache, varcon)
            ParamDecoder(rev).load(cache, params)
            HuntModeDecoder().load(cache, hunt)
            VarnBitDecoder().load(cache, varnbit)
            VarObjBitDecoder().load(cache, varobjBit)
        } catch (e: BufferUnderflowException) {
            logger.error(e) { "Error reading definitions" }
            throw e
        }
        return cache
    }

    fun getNpc(id: Int) = npcs[id]

    fun getFont(id: Int) = fonts[id]

    fun getObject(id: Int) = objects[id]

    fun getItem(id: Int) = items[id]

    fun getVarbit(id: Int) = varbits[id]

    fun getVarp(id: Int) = varps[id]

    fun getAnim(id: Int) = sequences[id]

    fun getBas(id: Int) = bas[id]

    fun getHunt(id: Int) = hunt[id]

    fun getEnum(id: Int) = enums[id]

    fun getHealthBar(id: Int) = healthBars[id]

    fun getStruct(id: Int) = structs[id]

    fun getDbrow(id: Int) = dbrows[id]

    fun getWalkTrigger(id: Int) = walkTriggers[id]

    fun getDbtable(id: Int) = dbtables[id]

    fun getStats(id: Int) = statTypes[id]

    fun getInterface(id: Int) = interfaces[id]

    fun getHitSplats(id: Int) = hitsplats[id]

    fun getProjectile(id: Int) = projectiles[id]

    fun getVarn(id: Int) = varns[id]

    fun getVarnBit(id: Int) = varnbit[id]

    fun getVarCon(id: Int) = varcon[id]

    fun getVarObj(id: Int) = varobjBit[id]

    fun getParam(id: Int) = params[id]

    fun getInventory(id: Int) = inv[id]

    fun getMesAnim(id: Int) = mesanim[id]

    fun getModLevel(id: Int) = modLevels[id]

    fun getNpcOrDefault(id: Int) = getOrDefault(npcs, id, NpcServerType(), "Npc")

    fun getObjectOrDefault(id: Int) = getOrDefault(objects, id, ObjectServerType(), "Object")

    fun getItemOrDefault(id: Int) = getOrDefault(items, id, ItemServerType(), "Item")

    fun getVarbitOrDefault(id: Int) = getOrDefault(varbits, id, VarBitType(), "Varbit")

    fun getVarpOrDefault(id: Int) = getOrDefault(varps, id, VarpType(), "Varp")

    fun getEnumOrDefault(id: Int) = getOrDefault(enums, id, EnumType(), "Enum")

    fun getHealthBarOrDefault(id: Int) =
        getOrDefault(healthBars, id, HealthBarServerType(), "HealthBar")

    fun getStructOrDefault(id: Int) = getOrDefault(structs, id, StructType(), "Struct")

    fun getDbrowOrDefault(id: Int) = getOrDefault(dbrows, id, DBRowType(), "DBRow")

    fun getDbtableOrDefault(id: Int) = getOrDefault(dbtables, id, DBTableType(), "DBTable")

    // Size methods
    fun npcSize() = npcs.size

    fun objectSize() = objects.size

    fun itemSize() = items.size

    fun varbitSize() = varbits.size

    fun varpSize() = varps.size

    fun enumSize() = enums.size

    fun healthBarSize() = healthBars.size

    fun structSize() = structs.size

    fun animSize() = sequences.size

    // Bulk getters
    fun getNpcs() = npcs.toMap()

    fun getObjects() = objects.toMap()

    fun getItems() = items.toMap()

    fun getVarbits() = varbits.toMap()

    fun getVarps() = varps.toMap()

    fun getVarns() = varns.toMap()

    fun getStats() = statTypes.toMap()

    fun getProjectiles() = projectiles.toMap()

    fun getEnums() = enums.toMap()

    fun getHealthBars() = healthBars.toMap()

    fun getStructs() = structs.toMap()

    fun getAnims() = sequences.toMap()

    fun getRows() = dbrows.toMap()

    fun getParams() = params.toMap()

    fun getModelLevels() = modLevels.toMap()

    fun fromInterface(packed: Int): InterfaceType {
        val interfaceId = packed ushr 16
        return getInterface(interfaceId) ?: error("Interface $interfaceId could not be not found")
    }

    fun fromInterface(id: String): InterfaceType = fromInterface(id.asRSCM())

    fun fromComponent(packed: Int): ComponentType {
        val interfaceId = packed ushr 16
        val childId = packed and 0xFFFF

        return getInterface(interfaceId)?.components[childId]
            ?: error("Component $childId not found in interface $interfaceId")
    }

    fun fromComponent(id: String) = fromComponent(id.asRSCM())
}

fun rscmKey(prefix: String, internal: String): Int {
    val key = "$prefix.$internal"
    val type = RSCMType.entries.find { it.prefix == prefix }

    if (type != null) {
        RSCM.requireRSCM(type, key)
    }

    return key.asRSCM()
}

inline fun <T> resolve(
    prefix: String,
    internal: String,
    crossinline resolver: (Int) -> T?,
    crossinline errorMessage: (String) -> String = { "Missing $prefix: $it" },
): T {
    val key = rscmKey(prefix, internal)
    return resolver(key) ?: error(errorMessage(internal))
}

inline fun <T> wrap(prefix: String, internal: String, crossinline constructor: (Int) -> T): T {
    return constructor(rscmKey(prefix, internal))
}

fun npc(internal: String): NpcServerType = resolve("npc", internal, ServerCacheManager::getNpc)

fun obj(internal: String): ItemServerType = resolve("obj", internal, ServerCacheManager::getItem)

fun loc(internal: String): ObjectServerType =
    resolve("loc", internal, ServerCacheManager::getObject)

fun seq(internal: String): SequenceServerType =
    resolve("seq", internal, ServerCacheManager::getAnim)

fun projAnim(internal: String): ProjAnimType =
    resolve("projanim", internal, ServerCacheManager::getProjectile) {
        "Unable to find proj Anim: $it"
    }

fun varp(internal: String): VarpServerType =
    resolve("varp", internal, ServerCacheManager::getVarp) { "Varp Missing: $it" }

fun varBit(internal: String): VarBitType =
    resolve("varbit", internal, ServerCacheManager::getVarbit)

fun varcon(internal: String): VarConType =
    resolve("varcon", internal, ServerCacheManager::getVarCon)

fun varn(internal: String): VarnType = resolve("varn", internal, ServerCacheManager::getVarn)

fun varobjs(internal: String): VarObjBitType =
    resolve("varobj", internal, ServerCacheManager::getVarObj)

fun stat(internal: String): StatType =
    resolve("stat", internal, ServerCacheManager::getStats) { "Unable to get StatReference: $it" }

fun inv(internal: String): InventoryServerType =
    ServerCacheManager.getInventory(internal.asRSCM()) ?: error("Error Loading Inv")

fun headbar(internal: String): HealthBarServerType =
    resolve("headbar", internal, ServerCacheManager::getHealthBar) {
        "Unable to get HealthBar: $it"
    }

fun hitmark(internal: String): HitSplatType =
    resolve("hitmark", internal, ServerCacheManager::getHitSplats) { "Could not find hitmark: $it" }

fun hunt(internal: String): HuntModeType =
    resolve("stalk", internal, ServerCacheManager::getHunt) { "Missing hunt: $it" }

fun walkTrigger(internal: String): WalkTriggerType =
    resolve("walktrigger", internal, ServerCacheManager::getWalkTrigger) {
        "Missing walking Trigger: $it"
    }

fun mod(internal: String): ModLevelType =
    resolve("modlevel", internal, ServerCacheManager::getModLevel) { "Unable to find Mod Level" }

fun category(internal: String): CategoryType = wrap("category", internal, ::CategoryType)

fun area(internal: String): AreaType = wrap("area", internal, ::AreaType)

fun content(internal: String): ContentGroupType = wrap("content", internal, ::ContentGroupType)

fun currency(internal: String): CurrencyType = wrap("currency", internal, ::CurrencyType)

fun queue(internal: String): QueueType = wrap("queue", internal, ::QueueType)

fun spotAnim(internal: String): SpotanimType = wrap("spotanim", internal, ::SpotanimType)

fun synth(internal: String): SynthType = wrap("synth", internal, ::SynthType)

fun timer(internal: String): TimerType = wrap("timer", internal, ::TimerType)

fun fontMetrics(internal: String): FontType =
    resolve("font", internal, ServerCacheManager::getFont) { "Missing Font: $it" }

fun component(internal: String): ComponentType =
    resolve("component", internal, ServerCacheManager::fromComponent)

fun inter(internal: String): InterfaceType {
    val formatted = internal.removePrefix("interface.")
    return resolve("interface", formatted, ServerCacheManager::getInterface) {
        "Unable to find Interface: $it"
    }
}

fun jingle(internal: String): Int = rscmKey("synth", internal)

fun midi(internal: String): MidiType = MidiType(rscmKey("midi", internal))
