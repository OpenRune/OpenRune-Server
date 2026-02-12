package dev.openrune.tools

import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.model.TomlValue
import cc.ekblad.toml.serialization.from
import cc.ekblad.toml.tomlMapper
import cc.ekblad.toml.util.InternalAPI
import dev.openrune.OsrsCacheProvider
import dev.openrune.cache.*
import dev.openrune.cache.tools.TaskPriority
import org.alter.getCacheLocation
import dev.openrune.cache.tools.tasks.CacheTask
import dev.openrune.cache.util.getFiles
import dev.openrune.cache.util.progress
import dev.openrune.codec.osrs.impl.*
import dev.openrune.definition.Definition
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.type.InventoryType
import dev.openrune.definition.type.ItemType
import dev.openrune.filesystem.Cache
import dev.openrune.server.impl.item.ItemRenderDataManager
import dev.openrune.server.infobox.InfoBoxItem
import dev.openrune.server.infobox.InfoBoxObject
import dev.openrune.server.infobox.Load
import dev.openrune.types.*
import dev.openrune.types.InventoryServerType.Companion.pack
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class PackType(
    val index: Int,
    val name: String,
    val tomlMapper: TomlMapper,
    val kType: KType
)


class PackServerConfig(
    private val directory : File,
    private val tokenizedReplacement: Map<String,String> = emptyMap()
) : CacheTask(serverTaskOnly = true) {

    override val priority: TaskPriority
        get() = TaskPriority.END

    fun Map<String, Any?>.bool(key: String, defualt : Boolean) =
        this.containsKey(key) ?: defualt

    init {
        registerPackType("item",kType = typeOf<List<ItemServerType>>())
        registerPackType("object",kType = typeOf<List<ObjectServerType>>())
        registerPackType("npc", kType = typeOf<List<NpcServerType>>())
        registerPackType("health", kType = typeOf<List<HealthBarServerType>>())
        registerPackType("anims", kType = typeOf<List<HealthBarServerType>>())
        registerPackType("inventory", kType = typeOf<List<InventoryServerType>>(), tomlMapper = tomlMapper {
            addDecoder<InventoryServerType> { content, def: InventoryServerType ->
                def.apply {
                    flags = pack(
                        protect = content.bool("protect",true),
                        allStock = content.bool("allStock",false),
                        restock = content.bool("restock",false),
                        runWeight = content.bool("runWeight",false),
                        dummyInv = content.bool("dummyInv",false),
                        placeholders = content.bool("placeholders",false),
                    )
                }
            }
        })
    }


    private val logger = KotlinLogging.logger {}

    @OptIn(InternalAPI::class)
    override fun init(cache: Cache) {
        val parsedDefinitions = mutableMapOf<String, MutableList<Definition>>()
        CacheManager.init(OsrsCacheProvider(Cache.load(Path.of(getCacheLocation())), revision))

        ItemRenderDataManager.init()

        val files = getFiles(directory, "toml")

        for (file in files) {
            val document = TomlValue.from(processDocumentChanges(file.readText()))

            document.properties.forEach { prop ->
                val packType = packTypes[prop.key] ?: return@forEach

                val decodedDefs: List<Definition> = packType.tomlMapper.decode(packType.kType, prop.value)
                parsedDefinitions.getOrPut(packType.name) { mutableListOf() }.addAll(decodedDefs)
            }

        }

        val progress = progress("Packing Server Configs", parsedDefinitions.size)
        packTypes.forEach { (type) ->
            progress.extraMessage = type
            when(type) {
                "object" -> {
                    val codec = ObjectServerCodec(
                        CacheManager.getObjects(),
                        InfoBoxObject.load(File("../data/raw-cache/extra-dump/object-examines.csv").toPath())
                    )
                    CacheManager.getObjects().forEach {
                        cache.write(CONFIGS, 55, it.key, codec.encodeToBuffer(ObjectServerType(it.key)))
                    }
                }
                "npc" -> {
                    val codec = NpcServerCodec(CacheManager.getNpcs())
                    CacheManager.getNpcs().forEach {
                        cache.write(CONFIGS, 58, it.key, codec.encodeToBuffer(NpcServerType(it.key)))
                    }
                }
                "health" -> {
                    val codec = HealthBarServerCodec(CacheManager.getHealthBars())
                    CacheManager.getHealthBars().forEach {
                        cache.write(CONFIGS, 56, it.key, codec.encodeToBuffer(HealthBarServerType(it.key)))
                    }
                }
                "item" -> {
                    val codec = ItemServerCodec(
                        CacheManager.getItems(),
                        CacheManager.getEnums(),
                        InfoBoxItem.load(File("../data/raw-cache/extra-dump/item-data.json").toPath()))

                    CacheManager.getItems().forEach {
                        cache.write(CONFIGS, 59, it.key, codec.encodeToBuffer(ItemServerType(it.key)))
                    }
                }
                "anims" -> {
                    val codec3 = SequenceServerCodec(CacheManager.getAnims())
                    CacheManager.getAnims().forEach {
                        cache.write(CONFIGS, 57, it.key, codec3.encodeToBuffer(SequenceServerType(it.key)))
                    }
                }
                "inventory" -> {
                    val inventoryTypes = mutableMapOf<Int, InventoryType>().apply {
                        OsrsCacheProvider.InventoryDecoder().load(cache, this)
                    }

                    val serverTypes: Map<Int, InventoryServerType>? =
                        parsedDefinitions["inventory"]
                            ?.filterIsInstance<InventoryServerType>()
                            ?.associateBy { it.id }

                    val codec = InventoryServerCodec(
                        inventoryTypes,
                        serverTypes
                    )

                    inventoryTypes.forEach { (id, _) ->
                        cache.write(CONFIGS, 60, id, codec.encodeToBuffer(InventoryServerType(id)))
                    }
                }
                else -> println("Missing Type: $type")
            }
            progress.step()
        }
        progress.close()

    }

    private fun processDocumentChanges(content: String): String {
        val tokenMap = tokenizedReplacement.toMutableMap()
        val regex = Regex("""\[\[tokenizedReplacement]](.*?)(?=\n\[\[|\z)""", RegexOption.DOT_MATCHES_ALL)

        regex.find(content)?.groups?.get(1)?.value
            ?.lineSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.forEach { parseInlineTableString(it).forEach { (k, v) -> tokenMap[k] = v.removeSurrounding("\"") } }

        var updated = content
        tokenMap.forEach { (key, value) ->
            updated = updated.replace(Regex("%${Regex.escape(key)}%", RegexOption.IGNORE_CASE), value)
        }

        return processRSCMModifier(updated)
    }

    private fun parseInlineTableString(input: String): List<Pair<String, String>> =
        input.removePrefix("{").removeSuffix("}")
            .split(",")
            .map { it.split("=").map { it.trim() } }
            .map { it[0].lowercase() to it[1] }

    private fun processRSCMModifier(input: String): String {
        val allowedPrefixes = ConstantProvider.types
        val output = StringBuilder()
        var debugNameAdded = false

        val quotedStringRegex = Regex(""""([^"]+)"""")

        input.lines().forEach { line ->
            val trimmed = line.trim()

            if (trimmed.startsWith("[[")) {
                debugNameAdded = false
                output.appendLine(line)
                return@forEach
            }

            var modifiedLine = line
            val matches = quotedStringRegex.findAll(line)

            for (match in matches) {
                val fullValue = match.groupValues[1]
                if (allowedPrefixes.any { fullValue.startsWith(it) }) {
                    val resolved = ConstantProvider.getMapping(fullValue)
                    modifiedLine = modifiedLine.replace("\"$fullValue\"", resolved.toString())

                    if (!debugNameAdded && trimmed.startsWith("id") && fullValue == match.groupValues[1]) {
                        output.appendLine("debugName = \"${fullValue.substringAfter(".")}\"")
                        debugNameAdded = true
                    }
                }
            }

            output.appendLine(modifiedLine)
        }

        return output.toString()
    }

    companion object {

        private val tomlMapperDefault = tomlMapper {  }
        val packTypes = mutableMapOf<String, PackType>()

        fun registerPackType(
            name: String,
            index: Int = CONFIGS,
            tomlMapper: TomlMapper = tomlMapperDefault,
            kType: KType,
        ) {
            val packType = PackType(index, name,tomlMapper,kType)
            packTypes[packType.name] = packType
        }

    }

}