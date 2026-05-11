package org.rsmod.game.entity

/**
 * Thread-local context for which [Player] is currently being mutated on this game thread, plus a
 * listener for persistence-related changes (autosave).
 */
public object PlayerPersistenceHints {
    private val activePlayer = ThreadLocal<Player?>()

    @Volatile
    private var listener: ((Player) -> Unit)? = null

    public fun bind(listener: (Player) -> Unit) {
        this.listener = listener
    }

    public fun enter(player: Player) {
        activePlayer.set(player)
    }

    public fun leave() {
        activePlayer.remove()
    }

    public fun activeOrNull(): Player? = activePlayer.get()

    public fun notify(player: Player) {
        listener?.invoke(player)
    }
}
