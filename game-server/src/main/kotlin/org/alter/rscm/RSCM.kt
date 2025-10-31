package org.alter.rscm

import dev.openrune.definition.constants.ConstantProvider
import org.alter.rscm.RSCMType.Companion.RSCM_PREFIXES
import kotlin.system.exitProcess

enum class RSCMType(val prefix: String) {
    OBJTYPES("items"),
    NPCTYPES("npcs"),
    INVTYPES("inv"),
    VARPTYPES("varp"),
    VARBITTYPES("varbits"),
    LOCTYPES("objects"),
    SEQTYPES("sequences"),
    SPOTTYPES("spotanims"),
    ROWTYPES("dbrows"),
    TABLETYPES("tables"),
    COMPONENTS("components"),
    ENUMS("enums"),
    COLUMNS("columns"),
    INTERFACES("interfaces");

    companion object {
        val RSCM_PREFIXES = RSCMType.entries.map { it.prefix }.toSet()
    }

}

object RSCM {

    val NONE = "NONE"

    fun getRSCM(entity: Array<String>): List<Int> = entity.map { getRSCM(it) }

    fun String.asRSCM(): Int = getRSCM(this)

    fun requireRSCM(type: RSCMType, entity: String) {
        if (!entity.startsWith(type.prefix)) {
            error("Invalid RSCM key. Expected prefix '${type.prefix}', got '${entity.substringBefore(".")}'")
        }
    }

    fun getReverseMapping(table: RSCMType, value: Int): String? {
        return ConstantProvider.mappings.entries
            .find { it.key.startsWith("$${table.prefix}.") && it.value == value }
            ?.key
    }

    fun getRSCM(entity: String): Int {
        if (entity == NONE) return -1
        require(RSCM_PREFIXES.any { entity.startsWith(it) }) { "Prefix not found for '${entity.substringBefore(".")}'" }
        return ConstantProvider.getMapping(entity)
    }
}