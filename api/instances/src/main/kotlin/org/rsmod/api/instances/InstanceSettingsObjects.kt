package org.rsmod.api.instances

import dev.openrune.types.ObjectServerType
import org.rsmod.api.table.InstanceSettingsRow

public fun InstanceSettingsRow.enterLocObjects(): List<ObjectServerType> = listOf(enterObject)

public fun InstanceSettingsRow.exitLocObjects(): List<ObjectServerType> = listOf(exitObject)
