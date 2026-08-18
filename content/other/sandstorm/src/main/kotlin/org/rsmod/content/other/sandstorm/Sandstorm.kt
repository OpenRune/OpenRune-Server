package org.rsmod.content.other.sandstorm

import dev.openrune.types.ItemServerType
import org.rsmod.api.config.Constants
import org.rsmod.api.enums.NamedEnums.drew_sandstone_amounts
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** If true, Drew speaks when depositing at the Sandstorm loc just as it was before the 10 July 2024 update. */
private const val SHOW_DREW_DIALOGUE_ON_DEPOSIT = false

internal const val MAX_STORED = 25_000
internal const val SAND_PRICE = 50

var Player.sandstormBucketsSand by intVarBit("varbit.drew_sandstone_bucket_sand")
var Player.sandstormBucketsEmpty by intVarBit("varbit.drew_sandstone_empty_buckets")

class Sandstorm : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1("loc.grinder_machine") {
            depositAtGrinder()
        }
        onOpLocU("loc.grinder_machine") {
            useObjOnGrinder(it.objType)
        }
    }

    private suspend fun ProtectedAccess.useObjOnGrinder(objType: ItemServerType) {
        if (objType !in drew_sandstone_amounts.filterValuesNotNull().keys) {
            mes(Constants.dm_default)
            return
        }

        depositAtGrinder()
    }

    private suspend fun ProtectedAccess.depositAtGrinder() {
        val deposit = depositSandstone()

        if (deposit.deposited <= 0) {
            mes(if (deposit.overflowed) {
                "The grinder is full of sandstone."
            } else {
                "You do not have any sandstone to grind."
            })
            return
        }

        if (SHOW_DREW_DIALOGUE_ON_DEPOSIT) {
            val message = sandstoneDepositMessage(
                deposit.overflowed,
                player.sandstormBucketsSand,
            )
            startDialogue {
                chatNpcSpecific("Drew", "npc.grinder_drew", neutral, message)
            }
        } else {
            mes("The grinder is holding enough sandstone for " + "${player.sandstormBucketsSand.formatAmount} buckets of sand.")
        }
    }
}

internal fun sandstoneDepositMessage(
    overflowed: Boolean,
    bucketsSand: Int,
): String {
    if (overflowed) {
        return "The grinder is too full to hold all of that sandstone. It's holding enough " +
            "sandstone equivalent to ${bucketsSand.formatAmount} buckets of sand."
    }

    return "The grinder is now holding enough sandstone equivalent to " +
        "${bucketsSand.formatAmount} buckets of sand."
}

internal data class SandstoneDeposit(
    val deposited: Int,
    val overflowed: Boolean,
)

internal fun ProtectedAccess.depositSandstone(): SandstoneDeposit {
    var remaining = (MAX_STORED - player.sandstormBucketsSand).coerceAtLeast(0)
    var deposited = 0
    var overflowed = false

    drew_sandstone_amounts.filterValuesNotNull().forEach { (obj, sandPerItem) ->
        val held = inv.count(obj.internalName)
        if (held <= 0) return@forEach

        val deposit = minOf(held, remaining / sandPerItem)

        if (deposit < held) {
            overflowed = true
        }

        if (deposit <= 0) return@forEach

        if (invDel(inv, obj.internalName, deposit).failure) {
            return@forEach
        }

        val sandAmount = deposit * sandPerItem
        player.sandstormBucketsSand += sandAmount
        remaining -= sandAmount
        deposited += deposit
    }

    return SandstoneDeposit(deposited, overflowed)
}

internal fun ProtectedAccess.heldBuckets(): Int =
    bucketObjs().sumOf { inv.count(it) }

internal fun ProtectedAccess.storeBuckets(count: Int): Int {
    var remaining = minOf(
        count,
        MAX_STORED - player.sandstormBucketsEmpty,
    ).coerceAtLeast(0)

    var deposited = 0

    for (obj in bucketObjs()) {
        if (remaining <= 0) {
            break
        }

        val take = minOf(inv.count(obj), remaining)
        if (take <= 0) {
            continue
        }

        if (invDel(inv, obj, take).failure) {
            continue
        }

        remaining -= take
        deposited += take
    }

    player.sandstormBucketsEmpty += deposited
    return deposited
}

internal fun ProtectedAccess.withdrawableSand(): Int =
    minOf(player.sandstormBucketsEmpty, player.sandstormBucketsSand)

internal fun ProtectedAccess.withdrawSand(count: Int): Int {
    val affordable = inv.count("obj.coins") / SAND_PRICE
    val amount = minOf(count, withdrawableSand(), affordable)

    if (amount <= 0) {
        return 0
    }

    if (!inv.hasFreeSpace() && !inv.contains(ocCert("obj.bucket_sand"))) {
        mes(Constants.dm_invspace)
        return 0
    }

    val cost = amount * SAND_PRICE

    if (invDel(inv, "obj.coins", cost).failure) {
        return 0
    }

    if (invAdd(inv, "obj.bucket_sand", amount, cert = true).failure) {
        invAdd(inv, "obj.coins", cost)
        return 0
    }

    player.sandstormBucketsEmpty -= amount
    player.sandstormBucketsSand -= amount

    return amount
}

private fun ProtectedAccess.bucketObjs(): List<String> {
    val noted = ocCert("obj.bucket_empty").internalName

    if (noted == "obj.bucket_empty") {
        return listOf("obj.bucket_empty")
    }

    return listOf("obj.bucket_empty", noted)
}
