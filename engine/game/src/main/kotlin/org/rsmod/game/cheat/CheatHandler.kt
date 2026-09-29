package org.rsmod.game.cheat

import dev.or2.central.account.Rights

/**
 * @param registrant The classloader of the actual `cheat { ... }` lambda a caller passed to
 *   [org.rsmod.api.cheat.CheatHandlerBuilder], captured before it gets wrapped in validation
 *   logic. [action] (the wrapped closure) is always defined in `api.cheat`'s own classloader
 *   regardless of caller, so [CheatCommandMap.removeByClassLoader] must check this instead of
 *   `action.javaClass.classLoader` to correctly attribute a command to an external plugin.
 */
public class CheatHandler(
    public val desc: String?,
    public val action: Cheat.() -> Unit,
    public val registrant: ClassLoader?,
    public val requiredRights: Rights? = null,
)
