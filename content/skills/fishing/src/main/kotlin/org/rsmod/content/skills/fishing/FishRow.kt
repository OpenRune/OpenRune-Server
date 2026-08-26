package org.rsmod.content.skills.fishing

data class FishRow(
    val fishId: Int,
    val spot: Int,
    val method: Int,
    val level: Int,
    val xpTenths: Int,
    val low: Int,
    val high: Int,
    val strXpTenths: Int = 0,
    val agiXpTenths: Int = 0,
    val countMax: Int = 1,
    val strReq: Int = 0,
    val agiReq: Int = 0,
    val baitOverride: String? = null,
)
