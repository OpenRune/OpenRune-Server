package dev.openrune

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.OsrsCacheProvider.*
import dev.openrune.cache.CacheManager
import dev.openrune.cache.DBTABLEINDEX
import dev.openrune.cache.DenseIntMap
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
import dev.openrune.definition.codec.DBTableIndexCodec
import dev.openrune.definition.type.*
import dev.openrune.definition.type.DBTableIndexKey
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
import dev.openrune.types.aconverted.MidiType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.aconverted.SynthType
import dev.openrune.types.varp.VarpServerType
import java.nio.BufferUnderflowException
import java.nio.file.Path
import java.nio.file.Paths
object ServerCacheManager {

    /**
     * Tables are filled by the decoders through [DenseIntMap] and then adopted read-only, so a
     * loaded table cannot be mutated by a consumer even by casting. See [adopt].
     */
    private val itemsTable = DenseIntMap<ItemServerType>()
    private val npcsTable = DenseIntMap<NpcServerType>()
    private val objectsTable = DenseIntMap<ObjectServerType>()
    private val healthBarsTable = DenseIntMap<HealthBarServerType>()
    private val structsTable = DenseIntMap<StructType>()
    private val dbrowsTable = DenseIntMap<DBRowType>()
    private val dbtablesTable = DenseIntMap<DBTableType>()
    private val enumsTable = DenseIntMap<EnumType>()
    private val varbitsTable = DenseIntMap<VarBitType>()
    private val varpsTable = DenseIntMap<VarpServerType>()
    private val sequencesTable = DenseIntMap<SequenceServerType>()
    private val interfacesTable = DenseIntMap<InterfaceType>()
    private val invTable = DenseIntMap<InventoryServerType>()
    private val mesanimTable = DenseIntMap<MesAnimType>()
    private val statTypesTable = DenseIntMap<StatType>()
    private val projectilesTable = DenseIntMap<ProjAnimType>()
    private val hitsplatsTable = DenseIntMap<HitSplatType>()
    private val basTable = DenseIntMap<BasType>()
    private val walkTriggersTable = DenseIntMap<WalkTriggerType>()
    private val varnsTable = DenseIntMap<VarnType>()
    private val varnbitTable = DenseIntMap<VarnBitType>()
    private val varconTable = DenseIntMap<VarConType>()
    private val varconbitTable = DenseIntMap<VarConBitType>()
    private val varobjBitTable = DenseIntMap<VarObjBitType>()
    private val paramsTable = DenseIntMap<ParamType>()
    private val huntTable = DenseIntMap<HuntModeType>()

    private var items: Map<Int, ItemServerType> = emptyMap()
    private var npcs: Map<Int, NpcServerType> = emptyMap()
    private var objects: Map<Int, ObjectServerType> = emptyMap()
    private var healthBars: Map<Int, HealthBarServerType> = emptyMap()
    private var structs: Map<Int, StructType> = emptyMap()
    private var dbrows: Map<Int, DBRowType> = emptyMap()
    private var dbtables: Map<Int, DBTableType> = emptyMap()
    private var enums: Map<Int, EnumType> = emptyMap()
    private var varbits: Map<Int, VarBitType> = emptyMap()
    private var varps: Map<Int, VarpServerType> = emptyMap()
    private var transmitVarps: List<VarpServerType> = emptyList()
    private var sequences: Map<Int, SequenceServerType> = emptyMap()
    private var fonts: Map<Int, FontType> = emptyMap()
    private var interfaces: Map<Int, InterfaceType> = emptyMap()
    private var inv: Map<Int, InventoryServerType> = emptyMap()
    private var mesanim: Map<Int, MesAnimType> = emptyMap()
    private var statTypes: Map<Int, StatType> = emptyMap()
    private var projectiles: Map<Int, ProjAnimType> = emptyMap()
    private var hitsplats: Map<Int, HitSplatType> = emptyMap()
    private var bas: Map<Int, BasType> = emptyMap()
    private var walkTriggers: Map<Int, WalkTriggerType> = emptyMap()
    private var varns: Map<Int, VarnType> = emptyMap()
    private var varnbit: Map<Int, VarnBitType> = emptyMap()
    private var varcon: Map<Int, VarConType> = emptyMap()
    private var varconbit: Map<Int, VarConBitType> = emptyMap()
    private var varobjBit: Map<Int, VarObjBitType> = emptyMap()
    private var params: Map<Int, ParamType> = emptyMap()
    private var hunt: Map<Int, HuntModeType> = emptyMap()
    private var masterRowIdsByTable: Map<Int, List<Int>> = emptyMap()

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
            EnumDecoder().load(cache, enumsTable)
            ObjectDecoder(rev).load(cache, objectsTable)
            HealthBarDecoder().load(cache, healthBarsTable)
            NpcDecoder(rev).load(cache, npcsTable)
            ItemDecoder(rev).load(cache, itemsTable)
            InventoryDecoder().load(cache, invTable)
            SequenceDecoder().load(cache, sequencesTable)
            VarBitDecoder().load(cache, varbitsTable)
            VarpDecoder().load(cache, varpsTable)
            StructDecoder().load(cache, structsTable)
            DBRowDecoder().load(cache, dbrowsTable)
            DBTableDecoder().load(cache, dbtablesTable)
            ComponentDecoder(cache,rev).load(interfacesTable)
            MesAnimDecoder().load(cache, mesanimTable)
            StatTypeDecoder().load(cache, statTypesTable)
            ProjectileTypeDecoder().load(cache, projectilesTable)
            HitSplatDecoder().load(cache, hitsplatsTable)
            BasTypeDecoder().load(cache, basTable)
            WalkTriggerDecoder().load(cache, walkTriggersTable)
            VarnDecoder().load(cache, varnsTable)
            VarConBitDecoder().load(cache, varconbitTable)
            VarConDecoder().load(cache, varconTable)
            ParamDecoder(rev).load(cache, paramsTable)
            HuntModeDecoder().load(cache, huntTable)
            VarnBitDecoder().load(cache, varnbitTable)
            VarObjBitDecoder().load(cache, varobjBitTable)
            adoptTables()
            transmitVarps = varps.values.filter { !it.transmit.never }.sortedBy { it.id }
            loadMasterRowIndexes(cache)
        } catch (e: BufferUnderflowException) {
            logger.error(e) { "Error reading definitions" }
            throw e
        }
        return cache
    }

    /**
     * Publishes each decoded table as a read-only view. Re-running [init] over a second cache
     * merges into the existing view rather than replacing it.
     */
    private fun adoptTables() {
        items = adopt(itemsTable)
        npcs = adopt(npcsTable)
        objects = adopt(objectsTable)
        healthBars = adopt(healthBarsTable)
        structs = adopt(structsTable)
        dbrows = adopt(dbrowsTable)
        dbtables = adopt(dbtablesTable)
        enums = adopt(enumsTable)
        varbits = adopt(varbitsTable)
        varps = adopt(varpsTable)
        sequences = adopt(sequencesTable)
        interfaces = adopt(interfacesTable)
        inv = adopt(invTable)
        mesanim = adopt(mesanimTable)
        statTypes = adopt(statTypesTable)
        projectiles = adopt(projectilesTable)
        hitsplats = adopt(hitsplatsTable)
        bas = adopt(basTable)
        walkTriggers = adopt(walkTriggersTable)
        varns = adopt(varnsTable)
        varnbit = adopt(varnbitTable)
        varcon = adopt(varconTable)
        varconbit = adopt(varconbitTable)
        varobjBit = adopt(varobjBitTable)
        params = adopt(paramsTable)
        hunt = adopt(huntTable)
        rowsByTableId = null
    }

    /**
     * The decoders fill the same table instances on every [init], so a second cache merges into
     * the table itself and only the published view has to be re-wrapped.
     */
    private fun <T : Any> adopt(source: DenseIntMap<T>): Map<Int, T> = source.readOnly()

    private fun loadMasterRowIndexes(cache: Cache) {
        val codec = DBTableIndexCodec()
        val mapped = HashMap<Int, List<Int>>()
        for (tableId in cache.archives(DBTABLEINDEX)) {
            val data = cache.data(DBTABLEINDEX, tableId, 0) ?: continue
            try {
                val def = codec.loadData(tableId, data)
                val column = def.columns.firstOrNull() ?: continue
                val rowIds =
                    column.valueToRowIds[DBTableIndexKey.IntKey(0)]
                        ?: column.valueToRowIds[DBTableIndexKey.IntKey(-1)]
                        ?: continue
                mapped[tableId] = rowIds
            } catch (e: BufferUnderflowException) {
                logger.warn(e) { "Failed to decode DB table master index for tableId=$tableId" }
            }
        }
        masterRowIdsByTable = mapped
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

    /** The fallback is only constructed on a miss, not on every lookup. */
    private inline fun <T> lookupOrDefault(map: Map<Int, T>, id: Int, typeName: String, default: () -> T): T {
        if (id == -1) println("$typeName with id $id is missing.")
        return map[id] ?: default()
    }

    fun getNpcOrDefault(id: Int) = lookupOrDefault(npcs, id, "Npc") { NpcServerType() }

    fun getObjectOrDefault(id: Int) = lookupOrDefault(objects, id, "Object") { ObjectServerType() }

    fun getItemOrDefault(id: Int) = lookupOrDefault(items, id, "Item") { ItemServerType() }

    fun getVarbitOrDefault(id: Int) = lookupOrDefault(varbits, id, "Varbit") { VarBitType() }

    fun getVarpOrDefault(id: Int) = lookupOrDefault(varps, id, "Varp") { VarpType() }

    fun getEnumOrDefault(id: Int) = lookupOrDefault(enums, id, "Enum") { EnumType() }

    fun getHealthBarOrDefault(id: Int) =
        lookupOrDefault(healthBars, id, "HealthBar") { HealthBarServerType() }

    fun getStructOrDefault(id: Int) = lookupOrDefault(structs, id, "Struct") { StructType() }

    fun getDbrowOrDefault(id: Int) = lookupOrDefault(dbrows, id, "DBRow") { DBRowType() }

    fun getDbtableOrDefault(id: Int) = lookupOrDefault(dbtables, id, "DBTable") { DBTableType() }

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

    // Bulk getters. These return the read-only table itself rather than copying it per call.
    fun getNpcs(): Map<Int, NpcServerType> = npcs

    fun getObjects(): Map<Int, ObjectServerType> = objects

    fun getItems(): Map<Int, ItemServerType> = items

    fun getItemTypes(): Collection<ItemServerType> = items.values

    fun getVarbits(): Map<Int, VarBitType> = varbits

    fun getVarps(): Map<Int, VarpServerType> = varps

    fun getTransmitVarps(): List<VarpServerType> = transmitVarps

    fun getVarns(): Map<Int, VarnType> = varns

    fun getStats(): Map<Int, StatType> = statTypes

    fun getProjectiles(): Map<Int, ProjAnimType> = projectiles

    fun getEnums(): Map<Int, EnumType> = enums

    fun getHealthBars(): Map<Int, HealthBarServerType> = healthBars

    fun getStructs(): Map<Int, StructType> = structs

    fun getAnims(): Map<Int, SequenceServerType> = sequences

    fun getRows(): Map<Int, DBRowType> = dbrows

    fun getRowsForTable(tableId: Int): List<DBRowType> {
        val indexed = masterRowIdsByTable[tableId]
        if (indexed != null) {
            val out = ArrayList<DBRowType>(indexed.size)
            for (rowId in indexed) {
                val row = dbrows[rowId] ?: continue
                out += row
            }
            return out
        }
        val fallback = rowsByTableId ?: synchronized(this) {
            rowsByTableId ?: buildRowsByTableId().also { rowsByTableId = it }
        }
        return fallback[tableId].orEmpty()
    }

    private fun buildRowsByTableId(): Map<Int, List<DBRowType>> {
        val grouped = HashMap<Int, MutableList<DBRowType>>(dbtables.size.coerceAtLeast(16))
        for (row in dbrows.values) {
            grouped.computeIfAbsent(row.tableId) { ArrayList() }.add(row)
        }
        return grouped
    }

    @Volatile private var rowsByTableId: Map<Int, List<DBRowType>>? = null

    fun getParams(): Map<Int, ParamType> = params

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
