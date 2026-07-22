package dev.openrune.packscript

enum class PackScriptPhase(val annotation: String) {
    AFTER_CONFIG("after_config"),
    AFTER_DB("after_db"),
    ;

    companion object {
        fun fromAnnotation(name: String): PackScriptPhase? =
            entries.firstOrNull { it.annotation.equals(name, ignoreCase = true) }
    }
}
