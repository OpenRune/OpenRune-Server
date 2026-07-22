package dev.openrune.packscript

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.cache.CONFIGS
import dev.openrune.cache.ITEM
import dev.openrune.definition.codec.ItemCodec
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.util.toArray
import dev.openrune.filesystem.Cache
import io.netty.buffer.Unpooled

class PackLangRuntime(
    private val cache: Cache,
    revision: Int,
    tables: List<DBTable>,
    private val failOnMissingItem: Boolean = true,
) {
    private val logger = InlineLogger()
    private val codec = ItemCodec(revision)
    private val tablesByName = tables.mapNotNull { table -> table.rscmName?.let { it to table } }.toMap()
    private val mutableVars = mutableSetOf<String>()

    fun run(script: PackScript) {
        logger.info { "RunePack: ${script.phase.annotation}/${script.name} (${script.source})" }
        val scope = mutableMapOf<String, Any?>()
        mutableVars.clear()
        for (stmt in script.body) {
            exec(stmt, scope)
        }
    }

    private fun exec(stmt: Stmt, scope: MutableMap<String, Any?>) {
        when (stmt) {
            is Stmt.ForIn -> {
                val values = asList(eval(stmt.iterable, scope), "for-in")
                for (value in values) {
                    scope[stmt.binding] = value
                    for (inner in stmt.body) {
                        exec(inner, scope)
                    }
                }
                scope.remove(stmt.binding)
            }
            is Stmt.While -> {
                while (asBool(eval(stmt.cond, scope))) {
                    for (inner in stmt.body) {
                        exec(inner, scope)
                    }
                }
            }
            is Stmt.If -> {
                val body = if (asBool(eval(stmt.cond, scope))) stmt.thenBody else stmt.elseBody
                for (inner in body) {
                    exec(inner, scope)
                }
            }
            is Stmt.Val -> {
                scope[stmt.name] = eval(stmt.value, scope)
                if (stmt.mutable) {
                    mutableVars += stmt.name
                } else {
                    mutableVars -= stmt.name
                }
            }
            is Stmt.Assign -> {
                if (stmt.name !in scope) {
                    error("Cannot assign to undeclared variable '${stmt.name}' (use var/val first)")
                }
                if (stmt.name !in mutableVars) {
                    error("Cannot reassign val '${stmt.name}' (use var)")
                }
                scope[stmt.name] = eval(stmt.value, scope)
            }
            is Stmt.Call -> {
                invoke(stmt.path, stmt.args.map { eval(it, scope) }, scope)
            }
        }
    }

    private fun eval(expr: Expr, scope: Map<String, Any?>): Any? =
        when (expr) {
            is Expr.StringLit -> expr.value
            is Expr.IntLit -> expr.value
            is Expr.BoolLit -> expr.value
            is Expr.NullLit -> null
            is Expr.Ident ->
                if (expr.name in scope) {
                    scope[expr.name]
                } else {
                    error("Unknown variable '${expr.name}'")
                }
            is Expr.Path -> resolvePath(expr.parts, scope)
            is Expr.Call -> invoke(expr.path, expr.args.map { eval(it, scope) }, scope)
            is Expr.Lambda -> LambdaValue(expr.param, expr.body, HashMap(scope))
            is Expr.Unary ->
                when (expr.op) {
                    UnaryOp.NOT -> !asBool(eval(expr.expr, scope))
                    UnaryOp.NEG -> -asInt(eval(expr.expr, scope))
                }
            is Expr.Binary -> evalBinary(expr, scope)
            is Expr.IfExpr -> {
                if (asBool(eval(expr.cond, scope))) {
                    eval(expr.thenExpr, scope)
                } else {
                    eval(expr.elseExpr, scope)
                }
            }
        }

    private fun evalBinary(expr: Expr.Binary, scope: Map<String, Any?>): Any {
        when (expr.op) {
            BinaryOp.AND -> return asBool(eval(expr.left, scope)) && asBool(eval(expr.right, scope))
            BinaryOp.OR -> return asBool(eval(expr.left, scope)) || asBool(eval(expr.right, scope))
            else -> Unit
        }
        val left = eval(expr.left, scope)
        val right = eval(expr.right, scope)
        return when (expr.op) {
            BinaryOp.EQ -> valuesEqual(left, right)
            BinaryOp.NEQ -> !valuesEqual(left, right)
            BinaryOp.LT -> asInt(left) < asInt(right)
            BinaryOp.LTE -> asInt(left) <= asInt(right)
            BinaryOp.GT -> asInt(left) > asInt(right)
            BinaryOp.GTE -> asInt(left) >= asInt(right)
            BinaryOp.ADD -> asInt(left) + asInt(right)
            BinaryOp.SUB -> asInt(left) - asInt(right)
            BinaryOp.AND, BinaryOp.OR -> error("unreachable")
        }
    }

    private fun valuesEqual(left: Any?, right: Any?): Boolean {
        if (left is Number && right is Number) {
            return left.toInt() == right.toInt()
        }
        if (left is String && right is String) {
            return left.equals(right, ignoreCase = true)
        }
        return left == right
    }

    private fun resolvePath(parts: List<String>, scope: Map<String, Any?>): Any {
        if (parts.size == 1) {
            return if (parts[0] in scope) {
                scope[parts[0]] as Any
            } else {
                error("Unknown variable '${parts[0]}'")
            }
        }
        return when (parts[0]) {
            "db" -> {
                require(parts.size == 3) {
                    "db path must be db.<table>.<column>, got db.${parts.drop(1).joinToString(".")}"
                }
                val table =
                    tablesByName[parts[1]]
                        ?: error("Unknown db table '${parts[1]}'")
                val columnId =
                    table.columns.entries
                        .firstOrNull { (_, col) -> col.rscmName == parts[2] }
                        ?.key
                        ?: error("Unknown column '${parts[2]}' on table '${parts[1]}'")
                table.rows.flatMap { row ->
                    row.columns[columnId]
                        ?.mapNotNull { value -> (value as? Number)?.toInt() }
                        .orEmpty()
                }
            }
            else -> error("Unknown path root '${parts[0]}'")
        }
    }

    private fun invoke(path: List<String>, args: List<Any?>, scope: Map<String, Any?>): Any? {
        // Receiver form: ops.indexOfFirst { ... }
        if (path.size == 2 && path[0] in scope) {
            return invokeCollection(scope[path[0]], path[1], args)
        }

        when {
            path == listOf("oc", "addiop") -> {
                require(args.size == 2 || args.size == 3) {
                    "oc.addiop(obj, option[, index]) expects 2 or 3 args"
                }
                val objId = asInt(args[0])
                val option = args[1] as? String ?: error("oc.addiop: option must be string")
                val index = if (args.size == 3) asInt(args[2]) else null
                addInterfaceOption(objId, option, index)
                return null
            }
            path == listOf("oc", "equipable") -> {
                require(args.size == 1) { "oc.equipable(obj) expects 1 arg" }
                return isEquipableItem(asInt(args[0]))
            }
            path == listOf("oc", "equipslot") -> {
                require(args.size == 1) { "oc.equipslot(obj) expects 1 arg" }
                return loadItem(asInt(args[0])).equipSlot
            }
            path == listOf("oc", "iop") -> {
                require(args.size == 2) { "oc.iop(obj, index) expects 2 args" }
                return loadItem(asInt(args[0])).interfaceOptions.getOrNull(asInt(args[1]))
            }
            path == listOf("oc", "iops") -> {
                require(args.size == 1) { "oc.iops(obj) expects 1 arg" }
                val opts = loadItem(asInt(args[0])).interfaceOptions
                return (0 until MAX_IOPS).map { opts.getOrNull(it) }
            }
            path == listOf("range") -> {
                require(args.size == 1 || args.size == 2) { "range(n) or range(start, end)" }
                return if (args.size == 1) {
                    (0 until asInt(args[0])).toList()
                } else {
                    (asInt(args[0]) until asInt(args[1])).toList()
                }
            }
            path == listOf("blank") -> {
                require(args.size == 1) { "blank(value) expects 1 arg" }
                return isBlank(args[0])
            }
            path == listOf("size") -> {
                require(args.size == 1) { "size(list) expects 1 arg" }
                return asList(args[0], "size").size
            }
            path == listOf("get") -> {
                require(args.size == 2) { "get(list, index) expects 2 args" }
                return asList(args[0], "get").getOrNull(asInt(args[1]))
            }
            path == listOf("indexOfFirst") -> return collectionTopLevel("indexOfFirst", args)
            path == listOf("indexOfFirstBlank") -> return collectionTopLevel("indexOfFirstBlank", args)
            path == listOf("indexOfFirstNull") -> return collectionTopLevel("indexOfFirstNull", args)
            path == listOf("first") -> return collectionTopLevel("first", args)
            path == listOf("filter") -> return collectionTopLevel("filter", args)
            path == listOf("any") -> return collectionTopLevel("any", args)
            path == listOf("all") -> return collectionTopLevel("all", args)
            path == listOf("map") -> return collectionTopLevel("map", args)
            path == listOf("print") || path == listOf("println") -> {
                logger.info { "[RunePack] ${args.joinToString(" ")}" }
                return null
            }
            else -> error("Unknown call '${path.joinToString(".")}'")
        }
    }

    private fun collectionTopLevel(method: String, args: List<Any?>): Any? {
        require(args.isNotEmpty()) { "$method(list, …) expects a list" }
        return invokeCollection(args[0], method, args.drop(1))
    }

    private fun invokeCollection(receiver: Any?, method: String, args: List<Any?>): Any? {
        val list = asList(receiver, method)
        return when (method) {
            "indexOfFirst" -> {
                val (from, pred) = fromAndLambda(args, "indexOfFirst")
                indexOfFirstFrom(list, from) { asBool(callLambda(pred, it)) }
            }
            "indexOfFirstBlank" -> {
                val from = if (args.isEmpty()) 0 else asInt(args[0])
                require(args.size <= 1) { "indexOfFirstBlank([from]) expects 0 or 1 arg" }
                indexOfFirstFrom(list, from) { isBlank(it) }
            }
            "indexOfFirstNull" -> {
                val from = if (args.isEmpty()) 0 else asInt(args[0])
                require(args.size <= 1) { "indexOfFirstNull([from]) expects 0 or 1 arg" }
                indexOfFirstFrom(list, from) { it == null }
            }
            "first" -> {
                when {
                    args.isEmpty() -> list.firstOrNull()
                    else -> {
                        val pred = asLambda(args, method)
                        list.firstOrNull { asBool(callLambda(pred, it)) }
                    }
                }
            }
            "filter" -> {
                val pred = asLambda(args, method)
                list.filter { asBool(callLambda(pred, it)) }
            }
            "any" -> {
                val pred = asLambda(args, method)
                list.any { asBool(callLambda(pred, it)) }
            }
            "all" -> {
                val pred = asLambda(args, method)
                list.all { asBool(callLambda(pred, it)) }
            }
            "map" -> {
                val pred = asLambda(args, method)
                list.map { callLambda(pred, it) }
            }
            "get" -> {
                require(args.size == 1) { "list.get(index) expects 1 arg" }
                list.getOrNull(asInt(args[0]))
            }
            "size" -> {
                require(args.isEmpty()) { "list.size() expects no args" }
                list.size
            }
            else -> error("Unknown collection method '$method'")
        }
    }

    private fun fromAndLambda(args: List<Any?>, method: String): Pair<Int, LambdaValue> =
        when (args.size) {
            1 -> 0 to asLambda(args, method)
            2 -> asInt(args[0]) to (args[1] as? LambdaValue
                ?: error("$method(from) { … } expects a lambda as last arg"))
            else -> error("$method([from]) { … } expects a lambda, optionally with start index")
        }

    private fun indexOfFirstFrom(list: List<*>, from: Int, predicate: (Any?) -> Boolean): Int {
        val start = from.coerceAtLeast(0)
        for (i in start until list.size) {
            if (predicate(list[i])) return i
        }
        return -1
    }

    private fun asLambda(args: List<Any?>, method: String): LambdaValue {
        require(args.size == 1) { "$method expects a lambda `{ … }`" }
        return args[0] as? LambdaValue
            ?: error("$method expects a lambda `{ … }`, got ${args[0]?.let { it::class.simpleName }}")
    }

    private fun callLambda(lambda: LambdaValue, arg: Any?): Any? {
        val child = lambda.closure.toMutableMap()
        child[lambda.param] = arg
        return eval(lambda.body, child)
    }

    private fun asList(value: Any?, label: String): List<*> {
        if (value is List<*>) return value
        if (value is Iterable<*>) return value.toList()
        error("$label: expected list, got ${value?.let { it::class.simpleName }}")
    }

    private fun isBlank(value: Any?): Boolean =
        value == null || (value is String && value.isBlank())

    private fun asBool(value: Any?): Boolean =
        when (value) {
            is Boolean -> value
            null -> false
            else -> error("Expected boolean, got ${value::class.simpleName}: $value")
        }

    private fun asInt(value: Any?): Int =
        (value as? Number)?.toInt() ?: error("Expected int, got ${value?.let { it::class.simpleName }}: $value")

    private fun loadItem(objId: Int): dev.openrune.definition.type.ItemType {
        val data = cache.data(CONFIGS, ITEM, objId)
        if (data == null) {
            error("[RunePack] Missing item $objId")
        }
        return codec.loadData(objId, data)
    }

    private fun isEquipableItem(objId: Int): Boolean {
        val item = loadItem(objId)
        return isEquipable(item.equipSlot, item.interfaceOptions)
    }

    private fun addInterfaceOption(objId: Int, option: String, index: Int? = null) {
        val item = loadItem(objId)
        val changed = applyAddOption(item.interfaceOptions, option, index)
        if (changed == null) {
            error(
                if (index != null) {
                    "[RunePack] Cannot set interface-option index $index on item $objId for '$option'"
                } else {
                    "[RunePack] No free interface-option slot on item $objId for '$option'"
                },
            )
        }
        val writer = Unpooled.buffer(4096)
        with(codec) { writer.encode(item) }
        cache.write(CONFIGS, ITEM, objId, writer.toArray())
    }

    companion object {
        private const val MAX_IOPS = 5

        fun isEquipable(equipSlot: Int, interfaceOptions: List<String?>): Boolean {
            if (equipSlot == -1) {
                return false
            }
            val op2 = interfaceOptions.getOrNull(1)
            return op2.equals("Wield", ignoreCase = true) || op2.equals("Wear", ignoreCase = true)
        }

        fun applyAddOption(
            interfaceOptions: MutableList<String?>,
            option: String,
            preferredIndex: Int? = null,
        ): Int? {
            val existing = interfaceOptions.indexOfFirst { it == option }
            if (existing >= 0) {
                return existing
            }
            val slot =
                if (preferredIndex != null) {
                    require(preferredIndex >= 0) { "oc.addiop index must be >= 0, got $preferredIndex" }
                    val current = interfaceOptions.getOrNull(preferredIndex)
                    if (!current.isNullOrBlank() && current != option) {
                        return null
                    }
                    preferredIndex
                } else {
                    (0 until MAX_IOPS).firstOrNull { interfaceOptions.getOrNull(it).isNullOrBlank() }
                        ?: return null
                }
            while (interfaceOptions.size <= slot) {
                interfaceOptions.add(null)
            }
            interfaceOptions[slot] = option
            return slot
        }
    }
}
