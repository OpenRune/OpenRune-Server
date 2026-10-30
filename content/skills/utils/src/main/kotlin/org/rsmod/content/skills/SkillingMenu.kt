package org.rsmod.content.skills

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfSubType
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.Inventory

private const val SKILLMULTI_SETUP_SCRIPT = 2046
private const val SKILLMULTI_ITEM_SLOTS = 18
private const val CHATBOX_UNCLAMP_VARBIT = "varbit.chatmodal_unclamp"

data class SkillMultiSelection(
    val entry: SkillMultiEntry,
    val amount: Int,
)

data class SkillMultiEntry(
    val internal: String,
    val materials: List<Material> = emptyList(),
) {

    val item: ItemServerType
        get() = ServerCacheManager
            .getItem(internal.asRSCM(RSCMType.OBJ))
            ?: error("Unable to resolve item: $internal")

    fun maxCount(inv: Inventory): Int {
        return materials.minOfOrNull {
            inv.count(it.obj.internalName) / it.count
        } ?: inv.count(item.internalName)
    }
}

data class Material(
    val internal: String,
    val count: Int = 1,
) {

    val obj: ItemServerType
        get() = ServerCacheManager
            .getItem(internal.asRSCM(RSCMType.OBJ))
            ?: error("Unable to resolve item: $internal")
}

data class SkillMultiConfig(
    val actionType: SkillingActionType = SkillingActionType.DEFAULT,
    val verb: String,
    val entries: List<SkillMultiEntry>,
    val maxCountProvider: ((Inventory, SkillMultiEntry) -> Int)? = null,
    val defaultAmount: Int? = null,
    val quantityVarp: String? = null,
    val titleOverride: String? = null,
    val showUnavailable: Boolean = false,
    val eventsEndAtMaxCount: Boolean = false,
    val eventsAfterSetup: Boolean = false,
    val padLabels: Boolean = false,
) {

    val title: String
        get() = titleOverride ?: if (entries.size == 1) {
            "How many would you like to $verb?"
        } else {
            "What would you like to $verb?"
        }
}

class SkillMultiBuilder {

    private var verb: String = "make"
    private var actionType: SkillingActionType = SkillingActionType.DEFAULT

    private val entries = mutableListOf<SkillMultiEntry>()

    fun verb(value: String) {
        verb = value
    }

    fun action(value: SkillingActionType) {
        actionType = value
    }

    fun entry(
        item: String,
        builder: SkillMultiEntryBuilder.() -> Unit = {},
    ) {
        val entryBuilder = SkillMultiEntryBuilder().apply(builder)

        entries += SkillMultiEntry(
            internal = item,
            materials = entryBuilder.materials,
        )
    }

    fun build(): SkillMultiConfig {
        return SkillMultiConfig(
            actionType = actionType,
            verb = verb,
            entries = entries,
        )
    }
}

class SkillMultiEntryBuilder {

    internal val materials = mutableListOf<Material>()

    fun material(
        item: String,
        count: Int = 1,
    ) {
        materials += Material(
            internal = item,
            count = count,
        )
    }
}

fun skillMulti(
    builder: SkillMultiBuilder.() -> Unit
): SkillMultiConfig {
    return SkillMultiBuilder().apply(builder).build()
}

suspend fun ProtectedAccess.openSkillMulti(
    config: SkillMultiConfig,
    onComplete: suspend (SkillMultiSelection) -> Unit = {},
) {

    val available = config.entries.mapNotNull { entry ->
        val amount = config.maxCountProvider?.invoke(inv, entry)
            ?: entry.maxCount(inv)

        if (amount <= 0 && !config.showUnavailable) {
            null
        } else {
            entry to amount
        }
    }

    if (available.isEmpty()) {
        return
    }

    vars[CHATBOX_UNCLAMP_VARBIT] = 1

    runClientScript(2379)

    ifOpenSub(
        "interface.skillmulti",
        "component.chatbox:chatmodal",
        IfSubType.Modal,
    )

    val maxCount = skillmultiMaxCount(config, available)
    val eventsEnd = if (config.eventsEndAtMaxCount) maxCount else 28

    val setupArgs = skillmultiSetupArgs(
        actionType = config.actionType,
        config,
        available,
        suggestedAmount(config, maxCount),
    ).toTypedArray()

    if (config.eventsAfterSetup) {
        runClientScript(SKILLMULTI_SETUP_SCRIPT, *setupArgs)
    }

    for (i in available.indices) {
        ifSetEvents(
            validButtons[i],
            0..eventsEnd,
            IfEvent.PauseButton
        )
    }

    if (!config.eventsAfterSetup) {
        runClientScript(SKILLMULTI_SETUP_SCRIPT, *setupArgs)
    }

    val input = coroutine.pause(ResumePauseButtonInput::class)

    val buttonIndex = validButtons.indexOf(input.component)

    if (buttonIndex == -1 || buttonIndex >= available.size) {
        return
    }

    val (entry, maxAmount) = available[buttonIndex]
    val selectedAmount = if (maxAmount <= 0) 0 else input.subcomponent.coerceIn(1, maxAmount)

    if (config.quantityVarp != null) {
        // 0 stores "All", so the next menu suggests its own maximum rather than this count.
        vars[config.quantityVarp] = if (input.subcomponent >= maxCount) 0 else selectedAmount
    }

    onComplete(
        SkillMultiSelection(
            entry = entry,
            amount = selectedAmount,
        )
    )
}

val validButtons = listOf(
    "component.skillmulti:a",
    "component.skillmulti:b",
    "component.skillmulti:c",
    "component.skillmulti:d",
    "component.skillmulti:e",
    "component.skillmulti:f",
    "component.skillmulti:g",
    "component.skillmulti:h",
    "component.skillmulti:i",
    "component.skillmulti:j",
    "component.skillmulti:k",
    "component.skillmulti:l",
    "component.skillmulti:m",
    "component.skillmulti:n",
    "component.skillmulti:o",
    "component.skillmulti:p",
    "component.skillmulti:q",
    "component.skillmulti:r",
    "component.skillmulti:x",
)

private fun skillmultiMaxCount(
    config: SkillMultiConfig,
    available: List<Pair<SkillMultiEntry, Int>>,
): Int =
    (available.maxOfOrNull { it.second } ?: 0).let {
        if (config.showUnavailable) it.coerceAtLeast(1) else it
    }

private fun ProtectedAccess.suggestedAmount(config: SkillMultiConfig, maxCount: Int): Int {
    val varp = config.quantityVarp ?: return config.defaultAmount ?: maxCount
    val remembered = vars[varp]
    return if (remembered == 0) maxCount else remembered.coerceAtMost(maxCount)
}

private fun skillmultiSetupArgs(
    actionType: SkillingActionType,
    config: SkillMultiConfig,
    available: List<Pair<SkillMultiEntry, Int>>,
    suggestedAmount: Int,
): List<Any> {

    val maxCount = skillmultiMaxCount(config, available)

    val itemIds = buildList {
        addAll(available.map { it.first.item.id })

        repeat(SKILLMULTI_ITEM_SLOTS - size) {
            add(-1)
        }
    }

    val labels = buildString {
        append(config.title)

        available.forEach { (entry, _) ->
            append('|')
            append(entry.item.name)
        }

        if (config.padLabels) {
            repeat(SKILLMULTI_ITEM_SLOTS - available.size) {
                append('|')
            }
        }
    }

    return buildList {
        add(actionType.id)
        add(labels)
        add(maxCount)
        addAll(itemIds)
        add(suggestedAmount)
    }
}

enum class SkillingActionType(val id: Int) {
    MAKE_SETS(2),
    MAKE_SETS_ALT(3),
    BUY(4),
    BUY_SETS(5),
    COOK(6),
    COOK_ALT_1(7),
    COOK_ALT_2(8),
    FIRE(9),
    STRING(10),
    CHARGE(11),
    CUT(12),
    SMELT(13),
    USE(14),
    UPGRADE(15),
    SPIN(16),
    TAKE(17),
    REQUEST(18),
    ENCHANT(19),
    BUY_ALT(20),
    LIGHT(21),
    MAKE(22),
    TAKE_ALT_1(23),
    GIVE(24),
    SMITH(25),
    BURN(27),
    SELL(28),
    TAKE_ALT_2(29),
    USE_ALT(30),
    CARVE(31),
    PLANT(32),
    CHOOSE(33),
    CARVE_ALT(34),
    WEAVE(35),
    SMITH_ALT(37),

    DEFAULT(0);

    companion object {
        private val BY_ID = entries.associateBy(SkillingActionType::id)

        fun fromId(id: Int): SkillingActionType =
            BY_ID[id] ?: DEFAULT
    }
}
