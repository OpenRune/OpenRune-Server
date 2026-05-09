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
    val verb: String,
    val entries: List<SkillMultiEntry>,
    val maxCountProvider: ((Inventory, SkillMultiEntry) -> Int)? = null,
) {

    val title: String
        get() = if (entries.size == 1) {
            "How many would you like to $verb?"
        } else {
            "What would you like to $verb?"
        }
}

class SkillMultiBuilder {

    private var verb: String = "make"

    private val entries = mutableListOf<SkillMultiEntry>()

    fun verb(value: String) {
        verb = value
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
    return SkillMultiBuilder()
        .apply(builder)
        .build()
}

suspend fun ProtectedAccess.openSkillMulti(
    config: SkillMultiConfig,
    onComplete: suspend (SkillMultiSelection) -> Unit = {},
) {


    val available = config.entries.mapNotNull { entry ->
        val amount = config.maxCountProvider?.invoke(inv, entry)
            ?: entry.maxCount(inv)

        if (amount <= 0) {
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

    validButtons.forEach { button ->
        ifSetEvents(
            button,
            0..90,
            IfEvent.PauseButton
        )
    }

    runClientScript(SKILLMULTI_SETUP_SCRIPT, *skillmultiSetupArgs(config, available).toTypedArray())

    val input = coroutine.pause(ResumePauseButtonInput::class)

    val buttonIndex = validButtons.indexOf(input.component)

    if (buttonIndex == -1 || buttonIndex >= available.size) {
        return
    }

    val (entry, maxAmount) = available[buttonIndex]

    onComplete(
        SkillMultiSelection(
            entry = entry,
            amount = maxAmount,
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

private fun ProtectedAccess.skillmultiSetupArgs(
    config: SkillMultiConfig,
    available: List<Pair<SkillMultiEntry, Int>>,
): List<Any> {

    val maxCount = available.maxOfOrNull { it.second } ?: 0

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
    }

    return buildList {
        add(0)
        add(maxCount)
        addAll(itemIds)
        add(maxCount)
        add(labels)
    }
}
