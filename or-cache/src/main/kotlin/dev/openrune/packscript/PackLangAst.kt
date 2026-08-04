package dev.openrune.packscript

/**
 * RunePack — tiny Kotlin-flavoured pack-time scripting.
 *
 * ```
 * [after_db.toolbelt_items]
 * for obj in db.toolbelt_slots.allowed_items {
 *   val ops = oc.iops(obj)
 *   val anchor = ops.indexOfFirst { it == "Wear" || it == "Wield" }
 *   oc.addiop(obj, "Add-to-toolbelt", slot)
 * }
 * ```
 */
data class PackScript(
    val phase: PackScriptPhase,
    val name: String,
    val body: List<Stmt>,
    val source: String,
)

sealed interface Stmt {
    data class ForIn(
        val binding: String,
        val iterable: Expr,
        val body: List<Stmt>,
    ) : Stmt

    data class While(
        val cond: Expr,
        val body: List<Stmt>,
    ) : Stmt

    data class If(
        val cond: Expr,
        val thenBody: List<Stmt>,
        val elseBody: List<Stmt>,
    ) : Stmt

    data class Val(
        val name: String,
        val value: Expr,
        val mutable: Boolean,
    ) : Stmt

    data class Assign(
        val name: String,
        val value: Expr,
    ) : Stmt

    data class Call(val path: List<String>, val args: List<Expr>) : Stmt
}

sealed interface Expr {
    data class StringLit(val value: String) : Expr

    data class IntLit(val value: Int) : Expr

    data class BoolLit(val value: Boolean) : Expr

    data object NullLit : Expr

    data class Ident(val name: String) : Expr

    data class Path(val parts: List<String>) : Expr

    data class Call(val path: List<String>, val args: List<Expr>) : Expr

    /** `{ it -> expr }` or `{ expr }` (param defaults to `it`). */
    data class Lambda(val param: String, val body: Expr) : Expr

    data class Unary(val op: UnaryOp, val expr: Expr) : Expr

    data class Binary(val op: BinaryOp, val left: Expr, val right: Expr) : Expr

    data class IfExpr(val cond: Expr, val thenExpr: Expr, val elseExpr: Expr) : Expr
}

/** Runtime closure created when a lambda expression is evaluated. */
data class LambdaValue(
    val param: String,
    val body: Expr,
    val closure: Map<String, Any?>,
)

enum class UnaryOp { NOT, NEG }

enum class BinaryOp {
    EQ,
    NEQ,
    LT,
    LTE,
    GT,
    GTE,
    AND,
    OR,
    ADD,
    SUB,
}
