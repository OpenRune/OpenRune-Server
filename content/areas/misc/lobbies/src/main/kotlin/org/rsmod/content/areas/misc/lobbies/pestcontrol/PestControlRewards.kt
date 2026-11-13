package org.rsmod.content.areas.misc.lobbies.pestcontrol

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.enums.PestControlEnums.pest_control_reward_objects
import org.rsmod.api.enums.UnnamedEnums3.enum_2285
import org.rsmod.api.enums.UnnamedEnums3.enum_2286
import org.rsmod.api.enums.UnnamedEnums3.enum_2287
import org.rsmod.api.enums.UnnamedEnums3.enum_2288
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.table.PestControlPackContentsRow
import org.rsmod.api.table.PestControlTrainingRow
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PestControlRewards @Inject constructor(private val random: GameRandom) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfModalButton(CONFIRM) {
            if (it.op == IfButtonOp.Op1 && it.comsub in 0..31 &&
                player.ui.containsModal(REWARD_SHOP)) {
                buy(it.comsub)
            }
        }
    }

    private suspend fun ProtectedAccess.buy(selection: Int) {
        val cost = enum_2286.getValue(selection)
        if (player.pestControlPoints < cost) {
            refuse("You need $cost ${if (cost == 1) "point" else "points"} to claim that reward.")
            return
        }
        if (selection <= 20) {
            train(selection, cost)
            return
        }
        if (enum_2287[selection] == true && !meetsEquipmentRequirements()) {
            refuse(
                "You need level 42 in Attack, Strength, Defence, Ranged, Magic and Hitpoints, " +
                    "and level 22 Prayer, to purchase that item."
            )
            return
        }
        if (selection in 21..23) {
            val skill = enum_2288.getValue(selection)
            if (player.stat(skill.internalName) < 25) {
                val name = skill.displayName.replaceFirstChar(Char::uppercaseChar)
                refuse("You need level 25 $name to purchase that item.")
                return
            }
            val contents = PACK_CONTENTS.getValue(selection)
            if (selection == 23 && inv.freeSpace() < 3) {
                refuse("You need at least 3 empty inventory slots to take a seed pack.")
                return
            }
            if (!givePack(selection, contents)) {
                refuse("You didn't have enough free space to carry it.")
                return
            }
        } else {
            val item = pest_control_reward_objects.getValue(selection)
            if (invAdd(inv, item.internalName).failure) {
                refuse("You didn't have enough free space to carry it.")
                return
            }
            CollectionLog.grant(player, item.id)
        }
        player.pestControlPoints -= cost
        val name = enum_2285.getValue(selection)
        val reward = when (selection) {
            21, 22, 23 -> "a ${name.lowercase()}"
            31 -> "a $name"
            else -> name
        }
        complete("The Void Knight has given you $reward.")
    }

    private fun ProtectedAccess.meetsEquipmentRequirements(): Boolean {
        for (training in TRAINING.values) {
            val required = if (training.skill.internalName == "stat.prayer") 22 else 42
            if (player.statBase(training.skill.internalName) < required) return false
        }
        return true
    }

    private suspend fun ProtectedAccess.train(selection: Int, cost: Int) {
        val training = TRAINING.getValue(selection / 3)
        val stat = training.skill.internalName
        val level = player.statBase(stat)
        if (level < 25) {
            refuse("The Void Knights will not offer training in skills for which you have a level under 25.")
            return
        }
        val fineMultiplier = when (cost) {
            10 -> 101
            100 -> 1100
            else -> 10
        }
        val xp = (level * level / 600) * training.multiplier * fineMultiplier / 10.0
        player.statAdvance(stat, xp)
        player.pestControlPoints -= cost
        val amount = if (xp == xp.toInt().toDouble()) xp.toInt().toString() else xp.toString()
        complete("The Void Knight has granted you $amount ${enum_2285.getValue(training.selection)}.")
    }

    private fun ProtectedAccess.givePack(
        selection: Int,
        contents: List<PestControlPackContentsRow>,
    ): Boolean {
        return player.invTransaction(inv) {
            val inventory = select(inv)
            val rolls = if (selection == 23) 3 else contents.size
            repeat(rolls) { index ->
                val entry = if (selection == 23) seed(contents) else contents[index]
                insert {
                    into = inventory
                    obj = entry.item.id
                    // UNVERIFIED: inclusive herb counts use uniform independent draws.
                    strictCount = if (entry.minimum == entry.maximum) entry.minimum
                        else this@PestControlRewards.random.of(entry.minimum, entry.maximum)
                }
            }
        }.success
    }

    private fun seed(contents: List<PestControlPackContentsRow>): PestControlPackContentsRow {
        var roll = random.of(SEED_WEIGHT)
        for (entry in contents) {
            roll -= entry.weight
            if (roll < 0) return entry
        }
        error("Pest Control seed weights do not cover the roll")
    }

    private suspend fun ProtectedAccess.refuse(message: String) {
        ifCloseSub(REWARD_SHOP)
        mesbox(message)
        openPestControlRewards()
    }

    private suspend fun ProtectedAccess.complete(message: String) {
        vars["varp.if1"] = player.pestControlPoints
        soundSynth("synth.pest_control_reward")
        ifCloseSub(REWARD_SHOP)
        mesbox(
            "$message<br><col=7f0000>Remaining Void Knight Commendation Points: " +
                "${player.pestControlPoints}</col>"
        )
        openPestControlRewards()
    }

    private companion object {
        val TRAINING by lazy { PestControlTrainingRow.all().associateBy { it.selection / 3 } }
        val PACK_CONTENTS by lazy { PestControlPackContentsRow.all().groupBy { it.selection } }
        val SEED_WEIGHT by lazy { PACK_CONTENTS.getValue(23).sumOf { it.weight } }
    }
}

internal var Player.pestControlPoints by intVarp("varp.pest_control_points")

internal fun ProtectedAccess.openPestControlRewards() {
    vars["varp.if1"] = player.pestControlPoints
    ifOpenMainModal(REWARD_SHOP)
    ifSetEvents(CONFIRM, 0..31, IfEvent.Op1)
    ifCloseChat()
}

private const val REWARD_SHOP = "interface.pest_rewardshop"
private const val CONFIRM = "component.pest_rewardshop:confirm"
