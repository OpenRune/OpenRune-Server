package dev.openrune.rscm

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.rscm.RSCMType.Companion.RSCM_PREFIXES

enum class RSCMType(val prefix: String) {
    AREA("area"),
    BAS("bas"),
    CATEGORY("category"),
    CLIENTSCRIPT("clientscript"),
    COMPONENT("component"),
    CONTENT("content"),
    CONTROLLER("controller"),
    DROP_TRIGGER("droptrigger"),
    CURRENCY("currency"),
    DBCOL("dbcol"),
    DBROW("dbrow"),
    DBTABLE("dbtable"),
    ENUM("enum"),
    FONT("font"),
    HEADBAR("headbar"),
    HITMARK("hitmark"),
    INTERFACE("interface"),
    INV("inv"),
    JINGLE("jingle"),
    LOC("loc"),
    MESANIM("mesanim"),
    MIDI("midi"),
    MODLEVEL("modlevel"),
    NPC("npc"),
    OBJ("obj"),
    PARAM("param"),
    PROJANIM("projanim"),
    QUEUE("queue"),
    SEQ("seq"),
    SPOTANIM("spotanim"),
    STAT("stat"),
    SYNTH("synth"),
    TIMER("timer"),
    VARBIT("varbit"),
    VARCON("varcon"),
    VARN("varn"),
    VAROBJ("varobj"),
    VARP("varp"),
    WALKTRIGGER("walktrigger");

    companion object {
        val RSCM_PREFIXES = entries.map { it.prefix }.toSet()
    }

}


object RSCM {

    val NONE = "NONE"

    // O(1) prefix validation instead of scanning with startsWith
    private val PREFIX_SET = RSCM_PREFIXES.toHashSet()

    // Cache for resolved mappings
    private val cache = HashMap<String, Int>(1024)

    fun getRSCM(entity: Array<String>): List<Int> = entity.map { getRSCM(it) }

    fun String.asRSCM(): Int = getRSCM(this)

    fun String.asRSCM(type: RSCMType): Int {
        val prefix = extractPrefix(this)
        require(prefix == type.prefix) {
            "Invalid RSCM key. Expected prefix '${type.prefix}', got '$prefix'"
        }
        return getRSCM(this)
    }

    fun requireRSCM(type: RSCMType, vararg entities: String) {
        for (entity in entities) {
            if (entity != NONE) {
                val prefix = extractPrefix(entity)
                if (prefix != type.prefix) {
                    error("Invalid RSCM key. Expected '${type.prefix}', got '$prefix'")
                }
            }
        }
    }

    fun getReverseMapping(table: RSCMType, value: Int): String {
        if (value == -1) return "-1"
        return ConstantProvider.getReverseMapping(table.prefix, value)
    }

    fun getRSCM(entity: String): Int {
        if (entity == NONE) return -1

        return cache.getOrPut(entity) {
            val prefix = extractPrefix(entity)

            require(prefix in PREFIX_SET) {
                "Prefix not found for '$prefix'"
            }

            ConstantProvider.getMapping(entity)
        }
    }

    private fun extractPrefix(entity: String): String {
        val idx = entity.indexOf('.')
        return if (idx == -1) entity else entity.substring(0, idx)
    }
}
