package org.rsmod.api.mechanics.toxins

import org.rsmod.api.mechanics.toxins.impl.PlayerDisease
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

public object Toxin {

    public fun syncStatusOrbs(
        player: Player,
        restartAntipoisonBuff: Boolean = false,
        clock: Int =
            player.currentMapClock,
    ) {
        val envenomed =
            PlayerVenom.isEnvenomed(player)

        val poisoned =
            PlayerPoison.isPoisoned(player)

        val immunityValue =
            if (!envenomed && !poisoned) {

                ToxinImmunity.statusOrbValue(
                    player = player,
                    clock = clock,
                )
            } else {
                null
            }

        val poisonVenomOrb =
            when {
                envenomed ->
                    1_000_000

                poisoned ->
                    1

                else ->
                    immunityValue ?: 0
            }

        val restartImmunityDisplay =
            restartAntipoisonBuff &&
                immunityValue != null

        if (restartImmunityDisplay) {
            VarPlayerIntMapSetter.set(
                player,
                POISON_VARP,
                0,
            )
        }

        VarPlayerIntMapSetter.set(
            player,
            POISON_VARP,
            poisonVenomOrb,
        )

        if (restartImmunityDisplay) {
            val struct =
                if (
                    immunityValue <
                    VENOM_IMMUNITY_THRESHOLD
                ) {
                    ANTIVENOM_BUFF_STRUCT
                } else {
                    ANTIPOISON_BUFF_STRUCT
                }

            player.runClientScript(
                BUFF_BAR_START_CLIENTSCRIPT,
                struct,
                clock.coerceAtLeast(1),
            )
        }

        val diseaseOrb =
            if (PlayerDisease.isDiseased(player)) {
                1
            } else {
                0
            }

        VarPlayerIntMapSetter.set(
            player,
            DISEASE_VARP,
            diseaseOrb,
        )
    }

    public fun Player.cureAllToxins() {
        PlayerPoison.clear(this)
        PlayerVenom.clear(this)
        PlayerDisease.clear(this)
    }

    private const val POISON_VARP: String =
        "varp.poison"

    private const val DISEASE_VARP: String =
        "varp.disease"

    private const val BUFF_BAR_START_CLIENTSCRIPT: Int =
        5931

    private const val ANTIPOISON_BUFF_STRUCT: Int =
        3105

    private const val ANTIVENOM_BUFF_STRUCT: Int =
        3104

    private const val VENOM_IMMUNITY_THRESHOLD: Int =
        -38
}
