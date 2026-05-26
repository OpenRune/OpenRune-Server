package org.rsmod.content.slayer.core

import org.rsmod.api.player.protect.ProtectedAccess

object SlayerCapePerk {

    private val slayerCapes = listOf("obj.skillcape_slayer", "obj.skillcape_slayer_trimmed")

    fun hasSlayerCape(access: ProtectedAccess): Boolean =
        slayerCapes.any { it in access.inv || it in access.worn }

    fun rollPerkProc(): Boolean = kotlin.random.Random.nextInt(CAPE_PERK_DENOMINATOR) == 0

    private const val CAPE_PERK_DENOMINATOR = 10
}
