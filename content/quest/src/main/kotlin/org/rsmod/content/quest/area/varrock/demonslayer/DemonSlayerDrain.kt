package org.rsmod.content.quest.area.varrock.demonslayer

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.DRAIN_KEY
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.DRAIN_STATE
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.KEY_ZOOM
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.STAGE_KEYS
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.ownsAnywhere
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The palace kitchen drain and the rusty key it washes into the Varrock sewers. */
class DemonSlayerDrain @Inject constructor(private val quest: DemonSlayerQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        for (drain in DRAINS) {
            onOpLoc1(drain) { searchDrain() }
            onOpLocU(drain, BUCKET_OF_WATER) { pourWater() }
        }
        for (key in SEWER_KEYS) {
            onOpLoc1(key) { takeKey() }
        }
    }

    private fun ProtectedAccess.keyNeeded(): Boolean =
        quest.stage(player) == STAGE_KEYS &&
            !quest.hasCollectedSilverlight(player) &&
            !ownsAnywhere(this, DRAIN_KEY)

    private suspend fun ProtectedAccess.searchDrain() {
        arriveDelay()
        if (player.vars[DRAIN_STATE] == KEY_TAKEN && keyNeeded()) {
            VarPlayerIntMapSetter.set(player, DRAIN_STATE, KEY_IN_DRAIN)
        }
        if (player.vars[DRAIN_STATE] != KEY_IN_DRAIN || !keyNeeded()) {
            mes("Nothing interesting seems to have been dropped down here today.")
            return
        }
        startDialogue {
            chatPlayer(neutral, "That must be the key Sir Prysin dropped.")
            chatPlayer(confused, "I don't seem to be able to reach it. I wonder if I can " +
                "dislodge it somehow. That way it may go down into the sewers.")
        }
    }

    private suspend fun ProtectedAccess.pourWater() {
        arriveDelay()
        if (invReplace(inv, BUCKET_OF_WATER, 1, EMPTY_BUCKET).failure) {
            return
        }
        soundSynth("synth.ds_drain")
        if (player.vars[DRAIN_STATE] != KEY_IN_DRAIN || !keyNeeded()) {
            mes("You pour the liquid down the drain.")
            return
        }
        VarPlayerIntMapSetter.set(player, DRAIN_STATE, KEY_IN_SEWER)
        soundSynth("synth.ds_key_fall")
        startDialogue {
            chatPlayer(happy, "OK, I think I've washed the key down into the sewer. I'd better " +
                "go down and get it!")
        }
    }

    private suspend fun ProtectedAccess.takeKey() {
        arriveDelay()
        if (!pickUpSewerKey(this)) {
            return
        }
        soundSynth("synth.pick2")
        objbox(DRAIN_KEY, KEY_ZOOM, "You pick up an old rusty key.")
    }

    fun pickUpSewerKey(access: ProtectedAccess): Boolean {
        if (access.player.vars[DRAIN_STATE] != KEY_IN_SEWER || !access.keyNeeded()) {
            return false
        }
        if (access.invAdd(access.inv, DRAIN_KEY).failure) {
            access.mes("Not enough inventory space.")
            return false
        }
        VarPlayerIntMapSetter.set(access.player, DRAIN_STATE, KEY_TAKEN)
        return true
    }

    private companion object {
        const val KEY_IN_DRAIN = 0
        const val KEY_IN_SEWER = 1
        const val KEY_TAKEN = 2

        const val BUCKET_OF_WATER = "obj.bucket_water"
        const val EMPTY_BUCKET = "obj.bucket_empty"

        val DRAINS =
            listOf("loc.questdrain", "loc.qip_ds_questdrain_key", "loc.qip_ds_questdrain_nokey")
        val SEWER_KEYS = listOf("loc.qip_ds_sewer_key", "loc.qip_ds_rustykey_mud")
    }
}
