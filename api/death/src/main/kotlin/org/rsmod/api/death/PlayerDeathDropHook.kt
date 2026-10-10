package org.rsmod.api.death

/** Pure selection transforms also run in the Items Kept on Death preview. */
public interface PlayerDeathDropHook {
    /** Tradeable equivalent for ranking protected items; never changes the carried item. */
    public fun valueObj(item: org.rsmod.game.inv.InvObj): org.rsmod.game.inv.InvObj = item
    public fun resolve(context: PlayerDeathContext, result: PlayerDeathDrops.DeathDropResult): PlayerDeathDrops.DeathDropResult = result

    /** Called only after actual death drops have been applied. Never called by previews. */
    public fun complete(context: PlayerDeathContext) {}

    /** Capture worn-state inputs before inventories are cleared by actual death. */
    public fun prepareCompletion(context: PlayerDeathContext): () -> Unit = { complete(context) }
}
