package org.alter.plugin.dialogue

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo
import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.constants.use
import org.alter.game.model.DialogueExpression
import org.alter.gamevals.GameValProvider
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

sealed class DialogueBlock {
    abstract val id: String
    abstract val order: Int?
    
    abstract fun generateCode(
        funBuilder: FunSpec.Builder,
        generator: DialogueGenerator,
        visited: MutableSet<String>
    )
}

data class NpcResponseBlock(
    override val id: String,
    override val order: Int?,
    val text: String,
    val expression: String? = null,
    val npc: String? = null,
    val title: String? = null
) : DialogueBlock() {
    override fun generateCode(
        funBuilder: FunSpec.Builder,
        generator: DialogueGenerator,
        visited: MutableSet<String>
    ) {
        val formattedText = generator.formatText(text).replace("<br>", " ")

        if (npc != null || expression != null || title != null) {
            val formatParts = mutableListOf<String>()
            val args = mutableListOf<Any>()
            
            formatParts.add("chatNpc(player")
            if (npc != null) {
                formatParts.add(", npc = %L")
                args.add("npcs.$npc".asRSCM())
            }
            if (expression != null) {
                formatParts.add(", animation = %S")
                args.add(DialogueExpression.valueOf(expression).id)
            }
            if (title != null) {
                formatParts.add(", title = %S")
                args.add(title)
            }
            formatParts.add(", message = %S)")
            args.add(formattedText)
            
            funBuilder.addStatement(formatParts.joinToString(""), *args.toTypedArray())
        } else {
            funBuilder.addStatement("chatNpc(player, %S)", formattedText)
        }

        generator.continueToNextBlock(id, funBuilder, visited)
    }
}

data class PlayerResponseBlock(
    override val id: String,
    override val order: Int?,
    val text: String,
    val expression: String? = null,
    val title: String? = null
) : DialogueBlock() {
    override fun generateCode(
        funBuilder: FunSpec.Builder,
        generator: DialogueGenerator,
        visited: MutableSet<String>
    ) {
        val formattedText = generator.formatText(text).replace("<br>", " ")

        if (expression != null || title != null) {
            val formatParts = mutableListOf<String>()
            val args = mutableListOf<Any>()
            
            formatParts.add("chatPlayer(player")
            if (expression != null) {
                formatParts.add(", animation = %S")
                args.add(DialogueExpression.valueOf(expression).id)
            }
            if (title != null) {
                formatParts.add(", title = %S")
                args.add(title)
            }
            formatParts.add(", message = %S)")
            args.add(formattedText)
            
            funBuilder.addStatement(formatParts.joinToString(""), *args.toTypedArray())
        } else {
            funBuilder.addStatement("chatPlayer(player, %S)", formattedText)
        }

        generator.continueToNextBlock(id, funBuilder, visited)
    }
}

data class OptionResponseBlock(
    override val id: String,
    override val order: Int?,
    val title: String? = null,
    val options: List<DialogueOption> = emptyList()
) : DialogueBlock() {
    override fun generateCode(
        funBuilder: FunSpec.Builder,
        generator: DialogueGenerator,
        visited: MutableSet<String>
    ) {
        if (options.isEmpty()) return

        val optionsCode = CodeBlock.builder()
        options.forEachIndexed { index, option ->
            optionsCode.add("        %S,\n", option.title)
        }

        funBuilder.beginControlFlow(
            "when (options(\n        player,\n%L    ))",
            optionsCode.build()
        )

        val optionEdgesMap = generator.matchOptionsToEdges(options, id)

        options.forEachIndexed { index, option ->
            val optNum = index + 1
            val edge = optionEdgesMap[option.id]
            
            if (edge != null) {
                val nextBlock = generator.getBlock(edge.target)
                val nextEdges = generator.getNextBlocks(edge.target)

                // Single-line optimization: only for simple response blocks with no outgoing edges
                val isSingleStatement = when (nextBlock) {
                    is PlayerResponseBlock -> nextEdges.isEmpty()
                    is NpcResponseBlock -> nextEdges.isEmpty()
                    else -> false
                }

                if (isSingleStatement && nextBlock != null) {
                    val text = generator.formatText(
                        (nextBlock as? PlayerResponseBlock)?.text
                            ?: (nextBlock as? NpcResponseBlock)?.text
                            ?: ""
                    )
                    when (nextBlock) {
                        is PlayerResponseBlock -> funBuilder.addCode("%L -> chatPlayer(player, %S)\n", optNum, text)
                        is NpcResponseBlock -> funBuilder.addCode("%L -> chatNpc(player, %S)\n", optNum, text)
                        else -> {}
                    }
                    visited.add(edge.target)
                } else {
                    // All other block types (or blocks with outgoing edges) use full block syntax
                    funBuilder.beginControlFlow("%L ->", optNum)
                    generator.generateBlockCode(edge.target, funBuilder, visited)
                    funBuilder.endControlFlow()
                }
            } else {
                funBuilder.addCode("%L -> {}\n", optNum)
            }
        }

        funBuilder.endControlFlow()
    }
}

data class ContainerBlock(
    override val id: String,
    override val order: Int?,
    val containerType: String,
    val items: List<ContainerItem> = emptyList()
) : DialogueBlock() {
    override fun generateCode(
        funBuilder: FunSpec.Builder,
        generator: DialogueGenerator,
        visited: MutableSet<String>
    ) {
        if (items.isEmpty()) return

        val itemArgs = items.map { item ->
            if (item.amount > 1) {
                CodeBlock.of("Item(%S, %L)", "items.${item.itemName}", item.amount)
            } else {
                CodeBlock.of("Item(%S)", "items.${item.itemName}")
            }
        }.joinToCode(", ")

        val checkCode = CodeBlock.of("player.%L.contains(%L)", containerType, itemArgs)

        funBuilder.beginControlFlow("if (%L)", checkCode)
        
        val trueEdge = generator.getNextBlocks(id).find { it.sourceHandle == "true" }
        if (trueEdge != null) generator.generateBlockCode(trueEdge.target, funBuilder, visited)
        
        funBuilder.nextControlFlow("else")
        
        val falseEdge = generator.getNextBlocks(id).find { it.sourceHandle == "false" }
        if (falseEdge != null) generator.generateBlockCode(falseEdge.target, funBuilder, visited)
        
        funBuilder.endControlFlow()
    }
}

data class ContainerItem(
    val itemName: String,
    val amount: Int
)

data class ActionBlock(
    override val id: String,
    override val order: Int?,
    val actionType: String,
    val itemName: String,
    val amount: Int,
    val containerType: String
) : DialogueBlock() {
    override fun generateCode(
        funBuilder: FunSpec.Builder,
        generator: DialogueGenerator,
        visited: MutableSet<String>
    ) {
        val container = when (actionType) {
            "remove_equipment", "add_equipment" -> "equipment"
            else -> containerType
        }
        
        when (actionType) {
            "remove_item", "remove_equipment" ->
                funBuilder.addStatement("player.$container.remove(%S, %L)", "items.$itemName", amount)
            "add_item", "add_equipment" ->
                funBuilder.addStatement("player.$container.add(%S, %L)", "items.$itemName", amount)
        }

        val nextEdges = generator.getNextBlocks(id)
        if (nextEdges.isNotEmpty()) {
            generator.generateBlockCode(nextEdges[0].target, funBuilder, visited)
        }
    }
}

data class DialogueOption(
    val id: String,
    val title: String
)

data class Edge(
    val source: String,
    val target: String,
    val sourceHandle: String? = null
)

class DialogueGeneratorSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val logger = environment.logger
    private val generatedFiles = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val moduleDir = environment.options["moduleDir"]
            ?: throw IllegalArgumentException("moduleDir KSP option not provided!")

        GameValProvider.load(environment.options["rootDir"].toString())

        val resourcesDir = File(moduleDir, "src/main/resources/dialogue")
        if (!resourcesDir.exists()) {
            logger.warn("Dialogue directory does not exist: $resourcesDir")
            return emptyList()
        }

        resourcesDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() == "xml" }
            .forEach { file ->
                val objectName = file.nameWithoutExtension
                    .replace("-", "_")
                    .replace(Regex("[^a-zA-Z0-9_]"), "_")
                    .let { if (it.isEmpty() || !it[0].isLetter()) "_$it" else it }
                    .let { "${it.replaceFirstChar { char -> char.uppercase() }}Dialogue" }
                
                if (generatedFiles.add(objectName)) {
                    try {
                        generateDialogue(file)
                    } catch (e: Exception) {
                        logger.error("Error processing dialogue file ${file.name}: ${e.message}", null)
                        logger.error("Stack trace: ${e.stackTraceToString()}", null)
                        generatedFiles.remove(objectName) // Remove on error so it can be retried
                    }
                } else {
                    logger.warn("Skipping duplicate dialogue file: ${file.name} (object: $objectName)")
                }
            }

        return emptyList()
    }

    private fun generateDialogue(xmlFile: File) {
        val generator = DialogueGenerator()

        val functionName = xmlFile.nameWithoutExtension
            .replace("-", "_")
            .replace(Regex("[^a-zA-Z0-9_]"), "_")
            .let { if (it.isEmpty() || !it[0].isLetter()) "_$it" else it }

        val pkg = "org.alter.dialogue"
        val objectName = "${functionName.replaceFirstChar { it.uppercase() }}Dialogue"

        val dialogFun = FunSpec.builder("dialog")
            .addParameter("player", ClassName("org.alter.game.model.entity", "Player"))
            .addModifiers(KModifier.SUSPEND)
            .apply {
                generator.parseXml(xmlFile, this)
            }
            .build()

        val objectSpec = TypeSpec.objectBuilder(objectName)
            .addFunction(dialogFun)
            .build()

        val fileSpec = FileSpec.builder(pkg, objectName)
            .indent("    ")
            .addImport("org.alter.api.ext", "chatNpc", "chatPlayer", "options")
            .addImport("org.alter.game.model.entity", "Player")
            .addImport("org.alter.game.model.queue", "QueueTask")
            .addImport("org.alter.game.model.item", "Item")
            .addType(objectSpec)
            .build()

        fileSpec.writeTo(environment.codeGenerator, aggregating = false)
        logger.info("Generated dialogue code for ${xmlFile.name}")
    }
}

class DialogueGenerator {
    private val blocks = mutableMapOf<String, DialogueBlock>()
    private val edges = mutableListOf<Edge>()
    private val outgoingEdges = mutableMapOf<String, MutableList<Edge>>()

    fun parseXml(xmlFile: File, funBuilder: FunSpec.Builder) {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xmlFile)
        doc.documentElement.normalize()

        parseNodes(doc.getElementsByTagName("nodes").item(0)?.childNodes)
        parseEdges(doc.getElementsByTagName("edges").item(0)?.childNodes)

        buildGraph()
        generateKotlinCode(funBuilder)
    }

    private fun parseNodes(nodeList: org.w3c.dom.NodeList?) {
        nodeList?.let {
            for (i in 0 until it.length) {
                val node = it.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    parseBlock(node as Element)
                }
            }
        }
    }

    private fun parseEdges(edgeList: org.w3c.dom.NodeList?) {
        edgeList?.let {
            for (i in 0 until it.length) {
                val node = it.item(i)
                if (node.nodeType == Node.ELEMENT_NODE) {
                    parseEdge(node as Element)
                }
            }
        }
    }

    private fun parseBlock(element: Element) {
        val id = element.getAttribute("id")
        val type = element.tagName
        val order = element.getAttribute("order").toIntOrNull()

        val block = when (type) {
            "npc" -> {
                val text = element.getElementsByTagName("text").item(0)?.textContent ?: ""
                val expression = element.getAttribute("expression").takeIf { it.isNotEmpty() }
                val npc = element.getAttribute("npc").takeIf { it.isNotEmpty() }
                val title = element.getAttribute("title").takeIf { it.isNotEmpty() }

                NpcResponseBlock(id, order, text, expression, npc, title)
            }
            "player" -> {
                val text = element.getElementsByTagName("text").item(0)?.textContent ?: ""
                val expression = element.getAttribute("expression").takeIf { it.isNotEmpty() }
                val title = element.getAttribute("title").takeIf { it.isNotEmpty() }
                PlayerResponseBlock(id, order, text, expression, title)
            }
            "optionsNode" -> {
                val title = element.getAttribute("title").takeIf { it.isNotEmpty() }
                val options = element.getElementsByTagName("options").item(0)?.let { optionsElement ->
                    (optionsElement as Element).getElementsByTagName("option")
                        .let { optionList ->
                            (0 until optionList.length).map { j ->
                                val option = optionList.item(j) as Element
                                DialogueOption(option.getAttribute("id"), option.getAttribute("title"))
                            }
                        }
                } ?: emptyList()
                OptionResponseBlock(id, order, title, options)
            }
            "container" -> {
                val containerType = element.getAttribute("containerType").takeIf { it.isNotEmpty() } ?: "inventory"
                val items = element.getElementsByTagName("items").item(0)?.let { itemsElement ->
                    (itemsElement as Element).getElementsByTagName("item")
                        .let { itemList ->
                            (0 until itemList.length).map { j ->
                                val item = itemList.item(j) as Element
                                ContainerItem(
                                    item.getAttribute("itemName"),
                                    item.getAttribute("amount").toIntOrNull() ?: 1
                                )
                            }
                        }
                } ?: emptyList()
                ContainerBlock(id, order, containerType, items)
            }
            "action" -> {
                val actionType = element.getAttribute("actionType")
                val itemName = element.getAttribute("itemName")
                val amount = element.getAttribute("amount").toIntOrNull() ?: 1
                val containerType = element.getAttribute("containerType").takeIf { it.isNotEmpty() } ?: "inventory"
                ActionBlock(id, order, actionType, itemName, amount, containerType)
            }
            else -> return
        }

        blocks[id] = block
    }

    private fun parseEdge(element: Element) {
        val source = element.getAttribute("source")
        val target = element.getAttribute("target")
        val sourceHandle = element.getAttribute("sourceHandle").takeIf { it.isNotEmpty() }
        edges.add(Edge(source, target, sourceHandle))
    }

    private fun buildGraph() {
        outgoingEdges.clear()
        edges.forEach { edge ->
            outgoingEdges.getOrPut(edge.source) { mutableListOf() }.add(edge)
        }
    }

    fun getBlock(blockId: String): DialogueBlock? = blocks[blockId]
    
    fun getNextBlocks(blockId: String): List<Edge> {
        return outgoingEdges[blockId] ?: emptyList()
    }
    
    fun formatText(str: String): String {
        return str.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
    }
    
    fun matchOptionsToEdges(options: List<DialogueOption>, blockId: String): Map<String, Edge> {
        val optionEdgesMap = mutableMapOf<String, Edge>()
        val allEdges = getNextBlocks(blockId)

        fun extractOptionIdentifier(id: String) = id.split("option_").getOrNull(1) ?: ""

        val optionByIdentifier = options.associateBy { extractOptionIdentifier(it.id) }
            .filterKeys { it.isNotEmpty() }

        allEdges.forEach { edge ->
            val handle = edge.sourceHandle ?: ""
            val handleId = extractOptionIdentifier(handle)
            
            optionByIdentifier[handleId]?.let { option ->
                optionEdgesMap.putIfAbsent(option.id, edge)
            }
            
            if (handle.isNotEmpty()) {
                options.firstOrNull { it.id == handle }?.let { option ->
                    optionEdgesMap.putIfAbsent(option.id, edge)
                }
            }
        }

        // Fallback: match by position if not all options matched
        if (optionEdgesMap.size < options.size && allEdges.size == options.size) {
            options.forEachIndexed { index, option ->
                if (!optionEdgesMap.containsKey(option.id) && index < allEdges.size) {
                    optionEdgesMap[option.id] = allEdges[index]
                }
            }
        }

        return optionEdgesMap
    }

    private fun generateKotlinCode(funBuilder: FunSpec.Builder) {
        funBuilder.beginControlFlow("player.queue")
        
        val visited = mutableSetOf<String>()

        val startEdge = edges.find { it.source == "npc_meta_node" }
        val startBlockId = startEdge?.target
            ?: blocks.values.filterIsInstance<NpcResponseBlock>().minByOrNull { it.order ?: Int.MAX_VALUE }?.id

        if (startBlockId != null) {
            generateBlockCode(startBlockId, funBuilder, visited)
        }
        
        funBuilder.endControlFlow()
    }

    fun generateBlockCode(blockId: String, funBuilder: FunSpec.Builder, visited: MutableSet<String>) {
        if (blockId in visited) return
        val block = blocks[blockId] ?: return
        visited.add(blockId)
        block.generateCode(funBuilder, this, visited)
    }

    fun continueToNextBlock(blockId: String, funBuilder: FunSpec.Builder, visited: MutableSet<String>) {
        val nextEdges = getNextBlocks(blockId)
        if (nextEdges.size == 1 && nextEdges[0].sourceHandle == null) {
            generateBlockCode(nextEdges[0].target, funBuilder, visited)
        }
    }
}
