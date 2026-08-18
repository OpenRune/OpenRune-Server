package dev.openrune.packscript

import java.io.File

/**
 * Parser for RunePack (Kotlin-lite):
 *
 * ```
 * [after_db.name]
 * val x = oc.equipable(obj)
 * if (x) { ... } else { ... }
 * for obj in db.table.col { oc.addiop(obj, "...") }
 * ```
 */
object PackLangParser {
    fun parseFile(file: File): PackScript {
        val text = file.readText()
        return parse(text, file.name)
    }

    fun parse(source: String, sourceName: String = "<script>"): PackScript {
        val tokens = Lexer(source).tokenize()
        return Parser(tokens, sourceName).parseScript()
    }

    /** Method names that may take a bare trailing `{ lambda }` (not statement blocks). */
    private val TRAILING_LAMBDA_METHODS =
        setOf(
            "indexOfFirst",
            "indexOfFirstBlank",
            "indexOfFirstNull",
            "first",
            "filter",
            "any",
            "all",
            "map",
        )

    private enum class Kind {
        LBRACK,
        RBRACK,
        IDENT,
        STRING,
        INT,
        LBRACE,
        RBRACE,
        LPAREN,
        RPAREN,
        DOT,
        COMMA,
        EQ,
        EQEQ,
        NEQ,
        LT,
        LTE,
        GT,
        GTE,
        ANDAND,
        OROR,
        BANG,
        MINUS,
        PLUS,
        ARROW,
        EOF,
    }

    private data class Token(val kind: Kind, val text: String, val line: Int, val column: Int)

    private class Lexer(private val source: String) {
        private var i = 0
        private var line = 1
        private var column = 1

        fun tokenize(): List<Token> {
            val out = mutableListOf<Token>()
            while (true) {
                skipTrivia()
                if (i >= source.length) {
                    out += Token(Kind.EOF, "", line, column)
                    return out
                }
                val startLine = line
                val startCol = column
                when (val c = peek()) {
                    '[' -> {
                        advance()
                        out += Token(Kind.LBRACK, "[", startLine, startCol)
                    }
                    ']' -> {
                        advance()
                        out += Token(Kind.RBRACK, "]", startLine, startCol)
                    }
                    '{' -> {
                        advance()
                        out += Token(Kind.LBRACE, "{", startLine, startCol)
                    }
                    '}' -> {
                        advance()
                        out += Token(Kind.RBRACE, "}", startLine, startCol)
                    }
                    '(' -> {
                        advance()
                        out += Token(Kind.LPAREN, "(", startLine, startCol)
                    }
                    ')' -> {
                        advance()
                        out += Token(Kind.RPAREN, ")", startLine, startCol)
                    }
                    '.' -> {
                        advance()
                        out += Token(Kind.DOT, ".", startLine, startCol)
                    }
                    ',' -> {
                        advance()
                        out += Token(Kind.COMMA, ",", startLine, startCol)
                    }
                    '!' -> {
                        advance()
                        if (peek() == '=') {
                            advance()
                            out += Token(Kind.NEQ, "!=", startLine, startCol)
                        } else {
                            out += Token(Kind.BANG, "!", startLine, startCol)
                        }
                    }
                    '=' -> {
                        advance()
                        if (peek() == '=') {
                            advance()
                            out += Token(Kind.EQEQ, "==", startLine, startCol)
                        } else {
                            out += Token(Kind.EQ, "=", startLine, startCol)
                        }
                    }
                    '<' -> {
                        advance()
                        if (peek() == '=') {
                            advance()
                            out += Token(Kind.LTE, "<=", startLine, startCol)
                        } else {
                            out += Token(Kind.LT, "<", startLine, startCol)
                        }
                    }
                    '>' -> {
                        advance()
                        if (peek() == '=') {
                            advance()
                            out += Token(Kind.GTE, ">=", startLine, startCol)
                        } else {
                            out += Token(Kind.GT, ">", startLine, startCol)
                        }
                    }
                    '&' -> {
                        advance()
                        if (peek() != '&') error("$startLine:$startCol: expected '&&'")
                        advance()
                        out += Token(Kind.ANDAND, "&&", startLine, startCol)
                    }
                    '|' -> {
                        advance()
                        if (peek() != '|') error("$startLine:$startCol: expected '||'")
                        advance()
                        out += Token(Kind.OROR, "||", startLine, startCol)
                    }
                    '-' -> {
                        advance()
                        if (peek() == '>') {
                            advance()
                            out += Token(Kind.ARROW, "->", startLine, startCol)
                        } else {
                            out += Token(Kind.MINUS, "-", startLine, startCol)
                        }
                    }
                    '+' -> {
                        advance()
                        out += Token(Kind.PLUS, "+", startLine, startCol)
                    }
                    '"' -> out += readString(startLine, startCol)
                    else -> {
                        when {
                            c.isDigit() -> out += readInt(startLine, startCol)
                            c.isLetter() || c == '_' -> out += readIdent(startLine, startCol)
                            else -> error("$startLine:$startCol: unexpected character '$c'")
                        }
                    }
                }
            }
        }

        private fun skipTrivia() {
            while (i < source.length) {
                val c = peek()
                when {
                    c == '#' || (c == '/' && peek(1) == '/') -> {
                        while (i < source.length && peek() != '\n') advance()
                    }
                    c == '/' && peek(1) == '*' -> {
                        advance()
                        advance()
                        while (i < source.length && !(peek() == '*' && peek(1) == '/')) {
                            advance()
                        }
                        if (i < source.length) {
                            advance()
                            advance()
                        }
                    }
                    c.isWhitespace() -> advance()
                    else -> return
                }
            }
        }

        private fun readIdent(startLine: Int, startCol: Int): Token {
            val start = i
            while (i < source.length) {
                val c = peek()
                if (c.isLetterOrDigit() || c == '_') {
                    advance()
                } else {
                    break
                }
            }
            return Token(Kind.IDENT, source.substring(start, i), startLine, startCol)
        }

        private fun readInt(startLine: Int, startCol: Int): Token {
            val start = i
            while (i < source.length && peek().isDigit()) advance()
            return Token(Kind.INT, source.substring(start, i), startLine, startCol)
        }

        private fun readString(startLine: Int, startCol: Int): Token {
            advance()
            val sb = StringBuilder()
            while (i < source.length && peek() != '"') {
                val c = advance()
                if (c == '\\' && i < source.length) {
                    sb.append(advance())
                } else {
                    sb.append(c)
                }
            }
            if (i >= source.length || peek() != '"') {
                error("$startLine:$startCol: unterminated string")
            }
            advance()
            return Token(Kind.STRING, sb.toString(), startLine, startCol)
        }

        private fun peek(offset: Int = 0): Char = source.getOrElse(i + offset) { '\u0000' }

        private fun advance(): Char {
            val c = source[i++]
            if (c == '\n') {
                line++
                column = 1
            } else {
                column++
            }
            return c
        }
    }

    private class Parser(private val tokens: List<Token>, private val sourceName: String) {
        private var i = 0

        fun parseScript(): PackScript {
            expect(Kind.LBRACK, "expected '[phase.name]'")
            val phaseName = expect(Kind.IDENT, "expected phase name").text
            val phase =
                PackScriptPhase.fromAnnotation(phaseName)
                    ?: error("${here()}: unknown phase '$phaseName' (use after_db / after_config)")
            expect(Kind.DOT, "expected '.' between phase and script name")
            val name = expect(Kind.IDENT, "expected script name").text
            expect(Kind.RBRACK, "expected ']'")
            val body = mutableListOf<Stmt>()
            while (!check(Kind.EOF)) {
                body += parseStmt()
            }
            return PackScript(phase, name, body, sourceName)
        }

        private fun parseStmt(): Stmt {
            if (matchIdent("if")) return parseIfStmt()
            if (matchIdent("for")) return parseForStmt()
            if (matchIdent("while")) return parseWhileStmt()
            if (matchIdent("val")) return parseValStmt(mutable = false)
            if (matchIdent("var")) return parseValStmt(mutable = true)

            // assignment: name = expr
            if (check(Kind.IDENT) && peek(1).kind == Kind.EQ) {
                val name = advance().text
                expect(Kind.EQ, "expected '='")
                return Stmt.Assign(name, parseExpr())
            }

            val path = parsePathParts()
            val bareLambda = check(Kind.LBRACE) && path.lastOrNull() in TRAILING_LAMBDA_METHODS
            val args = parseCallArgs(allowBareLambda = bareLambda)
            return Stmt.Call(path, args)
        }

        /**
         * `(args)` with optional trailing `{ lambda }`, or just `{ lambda }` for
         * known collection methods (not plain values like `db.table.col { … }`).
         */
        private fun parseCallArgs(allowBareLambda: Boolean = true): List<Expr> {
            val args = mutableListOf<Expr>()
            if (match(Kind.LPAREN)) {
                args += parseArgList()
                expect(Kind.RPAREN, "expected ')'")
                if (check(Kind.LBRACE)) {
                    args += parseLambda()
                }
            } else if (allowBareLambda && check(Kind.LBRACE)) {
                args += parseLambda()
            } else {
                error("${here()}: expected '(' or '{' after call")
            }
            return args
        }

        private fun parseIfStmt(): Stmt.If {
            expect(Kind.LPAREN, "expected '(' after if")
            val cond = parseExpr()
            expect(Kind.RPAREN, "expected ')' after if condition")
            val thenBody = parseBlock()
            val elseBody =
                when {
                    matchIdent("else") -> {
                        if (matchIdent("if")) {
                            listOf(parseIfStmt())
                        } else {
                            parseBlock()
                        }
                    }
                    else -> emptyList()
                }
            return Stmt.If(cond, thenBody, elseBody)
        }

        private fun parseForStmt(): Stmt.ForIn {
            val binding = expect(Kind.IDENT, "expected loop variable").text
            expectIdent("in")
            val iterable = parseExpr()
            val body = parseBlock()
            return Stmt.ForIn(binding, iterable, body)
        }

        private fun parseWhileStmt(): Stmt.While {
            expect(Kind.LPAREN, "expected '(' after while")
            val cond = parseExpr()
            expect(Kind.RPAREN, "expected ')' after while condition")
            return Stmt.While(cond, parseBlock())
        }

        private fun parseValStmt(mutable: Boolean): Stmt.Val {
            val name = expect(Kind.IDENT, "expected variable name").text
            expect(Kind.EQ, "expected '=' in val/var")
            val value = parseExpr()
            return Stmt.Val(name, value, mutable)
        }

        private fun parseBlock(): List<Stmt> {
            expect(Kind.LBRACE, "expected '{'")
            val body = mutableListOf<Stmt>()
            while (!check(Kind.RBRACE) && !check(Kind.EOF)) {
                body += parseStmt()
            }
            expect(Kind.RBRACE, "expected '}'")
            return body
        }

        private fun parseArgList(): List<Expr> {
            if (check(Kind.RPAREN)) return emptyList()
            val args = mutableListOf(parseExpr())
            while (match(Kind.COMMA)) {
                args += parseExpr()
            }
            return args
        }

        // ---- expressions (precedence) ----

        private fun parseExpr(): Expr = parseOr()

        private fun parseOr(): Expr {
            var left = parseAnd()
            while (match(Kind.OROR)) {
                left = Expr.Binary(BinaryOp.OR, left, parseAnd())
            }
            return left
        }

        private fun parseAnd(): Expr {
            var left = parseEquality()
            while (match(Kind.ANDAND)) {
                left = Expr.Binary(BinaryOp.AND, left, parseEquality())
            }
            return left
        }

        private fun parseEquality(): Expr {
            var left = parseRelational()
            while (true) {
                left =
                    when {
                        match(Kind.EQEQ) -> Expr.Binary(BinaryOp.EQ, left, parseRelational())
                        match(Kind.NEQ) -> Expr.Binary(BinaryOp.NEQ, left, parseRelational())
                        else -> return left
                    }
            }
        }

        private fun parseRelational(): Expr {
            var left = parseAdditive()
            while (true) {
                left =
                    when {
                        match(Kind.LT) -> Expr.Binary(BinaryOp.LT, left, parseAdditive())
                        match(Kind.LTE) -> Expr.Binary(BinaryOp.LTE, left, parseAdditive())
                        match(Kind.GT) -> Expr.Binary(BinaryOp.GT, left, parseAdditive())
                        match(Kind.GTE) -> Expr.Binary(BinaryOp.GTE, left, parseAdditive())
                        else -> return left
                    }
            }
        }

        private fun parseAdditive(): Expr {
            var left = parseUnary()
            while (true) {
                left =
                    when {
                        match(Kind.PLUS) -> Expr.Binary(BinaryOp.ADD, left, parseUnary())
                        match(Kind.MINUS) -> Expr.Binary(BinaryOp.SUB, left, parseUnary())
                        else -> return left
                    }
            }
        }

        private fun parseUnary(): Expr =
            when {
                match(Kind.BANG) -> Expr.Unary(UnaryOp.NOT, parseUnary())
                match(Kind.MINUS) -> Expr.Unary(UnaryOp.NEG, parseUnary())
                else -> parsePrimary()
            }

        private fun parsePrimary(): Expr {
            if (matchIdent("if")) {
                expect(Kind.LPAREN, "expected '(' after if")
                val cond = parseExpr()
                expect(Kind.RPAREN, "expected ')'")
                val thenExpr = parseExpr()
                expectIdent("else")
                val elseExpr = parseExpr()
                return Expr.IfExpr(cond, thenExpr, elseExpr)
            }
            if (matchIdent("true")) return Expr.BoolLit(true)
            if (matchIdent("false")) return Expr.BoolLit(false)
            if (matchIdent("null")) return Expr.NullLit
            if (check(Kind.LBRACE)) return parseLambda()
            if (check(Kind.STRING)) return Expr.StringLit(advance().text)
            if (check(Kind.INT)) return Expr.IntLit(advance().text.toInt())
            if (match(Kind.LPAREN)) {
                val inner = parseExpr()
                expect(Kind.RPAREN, "expected ')'")
                return inner
            }
            if (check(Kind.IDENT)) {
                val parts = parsePathParts()
                val bareLambda = check(Kind.LBRACE) && parts.lastOrNull() in TRAILING_LAMBDA_METHODS
                if (check(Kind.LPAREN) || bareLambda) {
                    return Expr.Call(parts, parseCallArgs(allowBareLambda = bareLambda))
                }
                return if (parts.size == 1) Expr.Ident(parts[0]) else Expr.Path(parts)
            }
            error("${here()}: expected expression")
        }

        /** `{ expr }` or `{ name -> expr }` */
        private fun parseLambda(): Expr.Lambda {
            expect(Kind.LBRACE, "expected '{'")
            val param: String
            val body: Expr
            if (check(Kind.IDENT) && peek(1).kind == Kind.ARROW) {
                param = advance().text
                expect(Kind.ARROW, "expected '->'")
                body = parseExpr()
            } else {
                param = "it"
                body = parseExpr()
            }
            expect(Kind.RBRACE, "expected '}'")
            return Expr.Lambda(param, body)
        }

        private fun parsePathParts(): List<String> {
            val parts = mutableListOf(expect(Kind.IDENT, "expected identifier").text)
            while (match(Kind.DOT)) {
                parts += expect(Kind.IDENT, "expected identifier after '.'").text
            }
            return parts
        }

        private fun matchIdent(text: String): Boolean {
            if (check(Kind.IDENT) && peek().text == text) {
                advance()
                return true
            }
            return false
        }

        private fun expectIdent(text: String) {
            val tok = expect(Kind.IDENT, "expected '$text'")
            if (tok.text != text) {
                error("${tok.line}:${tok.column}: expected '$text', got '${tok.text}'")
            }
        }

        private fun match(kind: Kind): Boolean {
            if (check(kind)) {
                advance()
                return true
            }
            return false
        }

        private fun expect(kind: Kind, message: String): Token {
            if (!check(kind)) {
                error("${here()}: $message")
            }
            return advance()
        }

        private fun check(kind: Kind): Boolean = peek().kind == kind

        private fun peek(offset: Int = 0): Token = tokens.getOrElse(i + offset) { tokens.last() }

        private fun advance(): Token = tokens[i++]

        private fun here(): String {
            val t = peek()
            return "$sourceName:${t.line}:${t.column}"
        }
    }
}
