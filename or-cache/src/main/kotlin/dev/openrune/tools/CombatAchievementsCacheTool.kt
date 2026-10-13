package dev.openrune.tools

import com.google.gson.GsonBuilder
import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.gameval.GameValHandler
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
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.varp.VarpLifetime
import dev.openrune.types.varp.VarpServerType
import dev.openrune.types.varp.VarpTransmitLevel
import java.io.File
import java.security.MessageDigest

/** Adds only the two server-side CA counters and preserves every native definition. */
fun main() {
    val root = File("..").canonicalFile
    check(File(".").canonicalFile == File(root, "or-cache"))
    GameValProvider.load(root.path + "/")
    val path = File(root, ".data/cache/SERVER").toPath()
    val before = Cache.load(path).use(::snapshot)
    val owned = listOf("varp.ca_legitimate_barrows_chests", "varp.ca_legitimate_kraken_kills")
        .map { it.asRSCM() }.toSet()
    check(owned == setOf(65270, 65271))
    val offset = BoundedConfigReference.varpFileOffset(path)
    CacheDelegate(path.toString()).use { cache ->
        val codec = VarpServerTypeCodec()
        for (id in owned) {
            cache.write(
                2, 67, id + offset,
                codec.encodeToBuffer(VarpServerType(id, scope = VarpLifetime.Perm, transmit = VarpTransmitLevel.Never)),
            )
        }
        cache.update()
    }
    BoundedConfigReference.restoreSignedVarpSentinel(path, offset)
    val after = Cache.load(path).use(::snapshot)
    val changes = (before.keys + after.keys).filter { before[it] != after[it] }
    check(changes.all { key -> key.startsWith("2/67/") && key.substringAfterLast('/').toInt() in owned }) {
        "CA packing changed an unrelated cache definition: $changes"
    }
    check(before["2/67/-1"] == after["2/67/-1"])
    Cache.load(path).use { cache ->
        GamevalDumper.dumpCols(cache, 240)
        GamevalDumper.dumpComponents(cache, 240, verifySourceContracts = false)
        val tableTypes = GameValHandler.readGameVal(GameValGroupTypes.TABLETYPES, cache, 240)
        val rows = mutableMapOf<Int, DBRowType>().also { OsrsCacheProvider.DBRowDecoder().load(cache, it) }
        val enums = mutableMapOf<Int, EnumType>().also { OsrsCacheProvider.EnumDecoder().load(cache, it) }
        val tables = mutableMapOf<Int, DBTableType>().also { OsrsCacheProvider.DBTableDecoder().load(cache, it) }
        startGeneration(tableTypes, rows, enums, tables, true)
        startEnumGeneration(enums, true)
    }
    val proof = mapOf(
        "revision" to 240,
        "changedDefinitions" to changes.sorted(),
        "preservedDefinitions" to before.size - changes.count { it in before },
        "signedVarpPreserved" to true,
        "passed" to true,
    )
    File(root, ".data/combat-achievements-pack-proof.json").writeText(GsonBuilder().setPrettyPrinting().create().toJson(proof))
    println("PASS_COMBAT_ACHIEVEMENTS_BOUNDED_CACHE")
}

private fun snapshot(cache: Cache): Map<String, String> = buildMap {
    for (group in cache.archives(2)) for (id in cache.files(2, group)) {
        val bytes = cache.data(2, group, id)
        put("2/$group/$id", bytes?.let { java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(it)) } ?: "absent")
    }
}

private inline fun <T> Cache.use(block: (Cache) -> T): T = try { block(this) } finally { close() }
