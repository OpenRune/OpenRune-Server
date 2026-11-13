package org.rsmod.content.skills.farming.pack

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.rscm.RSCM.asRSCM

object FarmingCompostTable {
    fun write(row: DBRowBuilder, kind: String): Unit = with(row) {
        if (kind == "NONE") {
            column(FarmingTables.COMPOST_CAPACITY, 10_000)
            column(FarmingTables.COMPOST_CHARGES_PER_BUCKET, 2)
            return@with
        }
        val suffix = when (kind) {
            "NORMAL" -> "compost"
            "SUPER" -> "supercompost"
            "ULTRA" -> "ultracompost"
            else -> error("Unknown compost kind: $kind")
        }
        column(
            FarmingTables.COMPOST_BUCKETS,
            "obj.bucket_$suffix".asRSCM(), kind, "obj.bucket_empty".asRSCM(),
            "obj.cert_bucket_$suffix".asRSCM(), kind, "obj.cert_bucket_empty".asRSCM(),
        )
        val upgrades = mutableListOf<Any>()
        if (kind == "NORMAL") {
            for (dose in 1..4) {
                val remainder = if (dose == 1) "obj.vial_empty" else "obj.supercompost_potion_${dose - 1}"
                upgrades.addAll(listOf(
                    "obj.bucket_compost".asRSCM(), "obj.supercompost_potion_$dose".asRSCM(), 1,
                    "obj.bucket_supercompost".asRSCM(), remainder.asRSCM(), 0, 0, 0, "POTION",
                ))
            }
            upgrades.addAll(listOf(
                "obj.bucket_compost".asRSCM(), "obj.hosidius_saltpetre".asRSCM(), 3,
                "obj.bucket_supercompost".asRSCM(), "obj.hosidius_saltpetre".asRSCM(), 20, 1, 4, "SALTPETRE",
            ))
        }
        if (kind == "SUPER") {
            upgrades.addAll(listOf(
                "obj.bucket_supercompost".asRSCM(), "obj.fossil_volcanic_ash".asRSCM(), 2,
                "obj.bucket_ultracompost".asRSCM(), "obj.fossil_volcanic_ash".asRSCM(), 11, 2, 2, "ASH",
            ))
        }
        if (upgrades.isNotEmpty()) column(FarmingTables.COMPOST_UPGRADES, *upgrades.toTypedArray())
    }
}
