package org.rsmod.api.player.ironman

import dev.openrune.types.InventoryServerType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.subjectPronoun
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

public object IronmanRestrictions {
    public const val UIM_BANK_MESSAGE: String =
        "As an Ultimate Ironman, you cannot use the bank."

    public fun block(
        player: Player,
        activity: IronmanActivity,
        message: String? = activity.defaultMessage,
    ): Boolean {
        if (!activity.appliesTo(player)) {
            return false
        }
        val text = message ?: activity.defaultMessage
        if (!text.isNullOrEmpty()) {
            player.mes(text)
        }
        return true
    }

    public fun blockTrade(source: Player, target: Player): Boolean {
        when {
            source.isSoloIronman -> {
                source.mes("You are an Ironman. You stand alone.")
                return true
            }
            target.isSoloIronman -> {
                val name = target.displayName.ifBlank { target.username }
                source.mes("$name is an Ironman. ${target.subjectPronoun()} stands alone.")
                return true
            }
            else -> return false
        }
    }

    public fun blockUimBank(player: Player): Boolean {
        if (!player.isUltimateIronman) {
            return false
        }
        player.mes(UIM_BANK_MESSAGE)
        return true
    }

    public fun blockUimInventory(
        player: Player,
        type: InventoryServerType,
        message: String = "As an Ultimate Ironman, you cannot use that.",
    ): Boolean {
        if (!player.isUltimateIronman || !type.uimBlocked) {
            return false
        }
        if (message.isNotEmpty()) {
            player.mes(message)
        }
        return true
    }

    public fun blockUimInventory(
        player: Player,
        inventory: Inventory,
        message: String = "As an Ultimate Ironman, you cannot use that.",
    ): Boolean = blockUimInventory(player, inventory.type, message)
}

public enum class IronmanActivity(
    public val defaultMessage: String?,
    private val scope: Scope = Scope.Solo,
) {
    TRADE("You are an Ironman. You stand alone."),
    FOREIGN_LOOT("As an Ironman, you cannot take items that belong to other players."),
    GROUP_BOSS("As an Ironman, you cannot take part in this group boss content."),
    MINIGAME(null),
    ACCEPT_AID("Ironmen do not accept aid.", Scope.Any),
    POH("As an Ironman, you cannot enter another player's house."),
    GRAND_EXCHANGE("As an Ironman, you cannot use the Grand Exchange."),

    UIM_GE_ITEM_SETS("As an Ultimate Ironman, you cannot create item sets.", Scope.Ultimate),
    UIM_MISCELLANIA(
        "As an Ultimate Ironman, you cannot receive resources from Managing Miscellania.",
        Scope.Ultimate,
    ),
    UIM_POH_SERVANT_BANK(
        "As an Ultimate Ironman, your servant cannot send items to the bank.",
        Scope.Ultimate,
    ),
    UIM_POH_SERVANT_SAWMILL(
        "As an Ultimate Ironman, your servant cannot take planks from the sawmill.",
        Scope.Ultimate,
    ),
    UIM_SEED_VAULT("As an Ultimate Ironman, you cannot use the seed vault.", Scope.Ultimate),
    UIM_BERT_SAND(
        "As an Ultimate Ironman, you cannot receive buckets of sand from Bert.",
        Scope.Ultimate,
    ),
    UIM_COSTUME_PARTIAL(
        "As an Ultimate Ironman, you cannot withdraw partial costume sets.",
        Scope.Ultimate,
    ),
    UIM_COSTUME_DUPLICATES(
        "As an Ultimate Ironman, you cannot store duplicate costume pieces.",
        Scope.Ultimate,
    ),
    UIM_COX_STORAGE(
        "As an Ultimate Ironman, you cannot use Chambers of Xeric storage units.",
        Scope.Ultimate,
    ),
    UIM_PROTECT_ITEM("As an Ultimate Ironman, you cannot keep items on death.", Scope.Ultimate),
    ;

    internal fun appliesTo(player: Player): Boolean =
        when (scope) {
            Scope.Any -> player.isAnyIronman
            Scope.Ultimate -> player.isUltimateIronman
            Scope.Solo -> player.isSoloIronman
        }

    private enum class Scope {
        Solo,
        Any,
        Ultimate,
    }
}
