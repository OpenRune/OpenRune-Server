package dev.openrune.tools

import com.google.gson.GsonBuilder
import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.gameval.GameValHandler
import dev.openrune.cache.tools.CacheTool
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.tools.tasks.impl.PackDBTables
import dev.openrune.cache.tools.tasks.impl.defs.PackConfig
import dev.openrune.codec.osrs.InventoryDecoder
import dev.openrune.codec.osrs.impl.InventoryServerCodec
import dev.openrune.codec.osrs.impl.VarpServerTypeCodec
import dev.openrune.codegen.startEnumGeneration
import dev.openrune.codegen.startGeneration
import dev.openrune.definition.GameValGroupTypes
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBTableType
import dev.openrune.definition.type.EnumType
import dev.openrune.filesystem.Cache
import dev.openrune.gamevals.GameValProvider
import dev.openrune.gamevals.GamevalDumper
import dev.openrune.pack.PluginPacks
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.tables.consumables.food.FoodTable
import dev.openrune.tables.consumables.potion.PotionEffectTable
import dev.openrune.tables.consumables.potion.PotionTable
import dev.openrune.tables.skills.Cooking
import dev.openrune.tables.skills.Mining
import dev.openrune.types.InvScope
import dev.openrune.types.varp.VarpLifetime
import dev.openrune.types.varp.VarpServerType
import dev.openrune.types.varp.VarpTransmitLevel
import java.io.File
import java.security.MessageDigest

/** Bounded rebuild over an accepted rev-240 cache; no download, maps, CS2 or other packs. */
fun main() {
    val root = File("..").canonicalFile
    check(File(".").canonicalFile == File(root, "or-cache"))
    GameValProvider.load(root.path + "/")
    val pack = PluginPacks.discover(root).all.single { it.javaClass.simpleName == "GauntletPluginPack" }
    val configs = checkNotNull(pack.configDirectory())
    val idLine = Regex("(?m)^id\\s*=\\s*\"([^\"]+)\"")
    val headers = Regex("(?m)^\\[\\[([a-z_]+)\\]\\]")
    val owned = configs.walkTopDown().filter { it.extension == "toml" }.associate { file ->
        val text = file.readText()
        headers.find(text)!!.groupValues[1] to idLine.findAll(text).map { it.groupValues[1].asRSCM() }.toSet()
    }
    val ownedIds = owned.values.flatten().toSet() + setOf(65300, 65301, 65302, "inv.gauntlet_holding".asRSCM())
    val newRows = setOf(63680, 63681, 63682, 63683, 55515, 55516, 55517) + (56001..56012)
    val changedTables = setOf("dbtable.cooking_foods", "dbtable.food", "dbtable.potion", "dbtable.potion_effect", "dbtable.mining_rocks").map { it.asRSCM() }.toSet() + 56000
    val tables = pack.dbTables() + listOf(FoodTable.table(), PotionEffectTable.table(), PotionTable.table(), Cooking.foods(), Mining.rocks())
    val report = linkedMapOf<String, Any>()
    for (server in listOf(false, true)) {
        val pass = if (server) "SERVER" else "LIVE"
        val path = File(root, ".data/cache/$pass").toPath()
        val before = Cache.load(path).use(::snapshot)
        val offset = if (server) BoundedConfigReference.varpFileOffset(path) else 0
        CacheDelegate(path.toString()).use { cache ->
            fun run(task: CacheTask) {
                task.revision = 240
                CacheTask::class.java.getMethod("setServerPass\$tools", Boolean::class.javaPrimitiveType).invoke(task, server)
                task.init(cache)
            }
            run(PackConfig(configs))
            if (server) {
                run(PackServerConfig(240, File(root, ".data/raw-cache/server"), listOf(configs), targetIdsByTable = owned.filterKeys { it != "varp" }))
                val codec = VarpServerTypeCodec()
                for (id in listOf(65300, 65301, 65302)) {
                    cache.write(2, 67, id + offset, codec.encodeToBuffer(VarpServerType(id, scope = if (id == 65301) VarpLifetime.Temp else VarpLifetime.Perm, transmit = VarpTransmitLevel.Never)))
                }
                val invId = "inv.gauntlet_holding".asRSCM()
                val group = InventoryDecoder().getArchive(0)
                val invCodec = InventoryServerCodec()
                val inventory = invCodec.loadData(invId, checkNotNull(cache.data(2, group, invId)))
                check(inventory.size >= 42) { "Gauntlet escrow must hold inventory and equipment" }
                inventory.scope = InvScope.Perm
                cache.write(2, group, invId, invCodec.encodeToBuffer(inventory))
            }
            run(PackDBTables(tables))
            for ((group, additions) in CacheTool.gameValMappings) {
                if (group != GameValGroupTypes.TABLETYPES && group != GameValGroupTypes.ROWTYPES) continue
                val existing = GameValHandler.readGameVal(group, cache, 240)
                val replace = additions.associateBy { it.id }
                GameValHandler.encodeGameVals(group, existing.filter { it.id !in replace } + replace.values, cache, 240)
            }
            cache.update()
        }
        BoundedConfigReference.restoreSignedVarpSentinel(path, offset)
        val after = Cache.load(path).use(::snapshot)
        val changed = (before.keys + after.keys).filter { before[it] != after[it] }
        for (key in changed) {
            val id = key.substringAfterLast('/').toInt()
            check(id in ownedIds || id in newRows || id in changedTables) { "Unrelated cache definition changed: $pass/$key" }
        }
        check(before["2/67/-1"] == after["2/67/-1"]) { "Native signed varp sentinel changed" }
        report[pass] = mapOf("preservedDefinitions" to before.size - changed.count { it in before }, "changedDefinitions" to changed.sorted(), "signedVarpPreserved" to true)
    }
    Cache.load(File(root, ".data/cache/SERVER").toPath()).use { cache ->
        GamevalDumper.dumpCols(cache, 240)
        val tableTypes = GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache, 240)
        val rows = mutableMapOf<Int, DBRowType>().also { OsrsCacheProvider.DBRowDecoder().load(cache, it) }
        val enums = mutableMapOf<Int, EnumType>().also { OsrsCacheProvider.EnumDecoder().load(cache, it) }
        val definitions = mutableMapOf<Int, DBTableType>().also { OsrsCacheProvider.DBTableDecoder().load(cache, it) }
        startGeneration(tableTypes, rows, enums, definitions, true)
        startEnumGeneration(enums, true)
    }
    report["revision"] = 240
    report["passed"] = true
    File(root, ".data/gauntlet-pack-proof.json").writeText(GsonBuilder().setPrettyPrinting().create().toJson(report))
    println("PASS_GAUNTLET_BOUNDED_CACHE")
}

private fun snapshot(cache: Cache): Map<String, String> = buildMap {
    for (group in cache.archives(2)) for (id in cache.files(2, group)) {
        val bytes = cache.data(2, group, id)
        put("2/$group/$id", bytes?.let { java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(it)) } ?: "absent")
    }
}

private inline fun <T> Cache.use(block: (Cache) -> T): T = try { block(this) } finally { close() }
