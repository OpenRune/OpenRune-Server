package dev.openrune.packscript

/**
 * Static checks for a parsed RunePack script.
 */
object RunePackValidator {
    private val knownTopLevel =
        setOf(
            listOf("oc", "addiop"),
            listOf("oc", "equipable"),
            listOf("oc", "equipslot"),
            listOf("oc", "iop"),
            listOf("oc", "iops"),
            listOf("range"),
            listOf("blank"),
            listOf("size"),
            listOf("get"),
            listOf("indexOfFirst"),
            listOf("indexOfFirstBlank"),
            listOf("indexOfFirstNull"),
            listOf("first"),
            listOf("filter"),
            listOf("any"),
            listOf("all"),
            listOf("map"),
            listOf("print"),
            listOf("println"),
        )

    private val collectionMethods =
        setOf(
            "indexOfFirst",
            "indexOfFirstBlank",
            "indexOfFirstNull",
            "first",
            "filter",
            "any",
            "all",
            "map",
            "get",
            "size",
        )

    fun validate(script: PackScript, tableNames: Set<String>): List<String> {
        val errors = mutableListOf<String>()
        validateStmts(script.body, tableNames, errors, script.source)
        return errors
    }

    private fun validateStmts(
        stmts: List<Stmt>,
        tableNames: Set<String>,
        errors: MutableList<String>,
        source: String,
    ) {
        for (stmt in stmts) {
            when (stmt) {
                is Stmt.ForIn -> {
                    validateExpr(stmt.iterable, tableNames, errors, source)
                    validateStmts(stmt.body, tableNames, errors, source)
                }
                is Stmt.While -> {
                    validateExpr(stmt.cond, tableNames, errors, source)
                    validateStmts(stmt.body, tableNames, errors, source)
                }
                is Stmt.If -> {
                    validateExpr(stmt.cond, tableNames, errors, source)
                    validateStmts(stmt.thenBody, tableNames, errors, source)
                    validateStmts(stmt.elseBody, tableNames, errors, source)
                }
                is Stmt.Val -> validateExpr(stmt.value, tableNames, errors, source)
                is Stmt.Assign -> validateExpr(stmt.value, tableNames, errors, source)
                is Stmt.Call -> validateCall(stmt.path, stmt.args, tableNames, errors, source)
            }
        }
    }

    private fun validateCall(
        path: List<String>,
        args: List<Expr>,
        tableNames: Set<String>,
        errors: MutableList<String>,
        source: String,
    ) {
        val ok =
            path in knownTopLevel ||
                (path.size == 2 && path[1] in collectionMethods)
        if (!ok) {
            errors += "$source: unknown call '${path.joinToString(".")}'"
        }
        if (path == listOf("oc", "addiop") && args.size !in 2..3) {
            errors += "$source: oc.addiop(obj, option[, index]) expects 2 or 3 arguments"
        }
        for (arg in args) {
            validateExpr(arg, tableNames, errors, source)
        }
    }

    private fun validateExpr(
        expr: Expr,
        tableNames: Set<String>,
        errors: MutableList<String>,
        source: String,
    ) {
        when (expr) {
            is Expr.StringLit, is Expr.IntLit, is Expr.BoolLit, is Expr.NullLit, is Expr.Ident -> Unit
            is Expr.Path -> {
                if (expr.parts.firstOrNull() == "db") {
                    if (expr.parts.size != 3) {
                        errors +=
                            "$source: db path must be db.<table>.<column>, got ${expr.parts.joinToString(".")}"
                    } else if (tableNames.isNotEmpty() && expr.parts[1] !in tableNames) {
                        errors += "$source: unknown db table '${expr.parts[1]}'"
                    }
                }
            }
            is Expr.Call -> validateCall(expr.path, expr.args, tableNames, errors, source)
            is Expr.Lambda -> validateExpr(expr.body, tableNames, errors, source)
            is Expr.Unary -> validateExpr(expr.expr, tableNames, errors, source)
            is Expr.Binary -> {
                validateExpr(expr.left, tableNames, errors, source)
                validateExpr(expr.right, tableNames, errors, source)
            }
            is Expr.IfExpr -> {
                validateExpr(expr.cond, tableNames, errors, source)
                validateExpr(expr.thenExpr, tableNames, errors, source)
                validateExpr(expr.elseExpr, tableNames, errors, source)
            }
        }
    }
}
