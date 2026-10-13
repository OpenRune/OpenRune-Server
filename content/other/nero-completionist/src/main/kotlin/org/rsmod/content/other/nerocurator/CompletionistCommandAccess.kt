package org.rsmod.content.other.nerocurator

import dev.or2.central.account.Rights
import org.rsmod.api.cheat.CheatHandlerBuilder

internal fun CheatHandlerBuilder.completionistTestAccess() {
    requiredRights = Rights.ADMINISTRATOR
    invalidRights = "Only administrators can use Completionist test commands."
}
