package org.alter.game.model.container.key

import org.alter.game.model.container.ContainerStackType

/**
 * A decoupled file that holds [ContainerKey]s that are pre-defined in our core
 * game-module.
 *
 * @author Tom <rspsmods@gmail.com>
 */
val BOND_POUCH_KEY = ContainerKey("bonds", "inv.bonds_pouch",capacity = 200, stackType = ContainerStackType.NO_STACK)
val INVENTORY_KEY = ContainerKey("inventory", "inv.inv", capacity = 28, stackType = ContainerStackType.NORMAL)
val EQUIPMENT_KEY = ContainerKey("equipment", "inv.worn",capacity = 14, stackType = ContainerStackType.NORMAL)
val BANK_KEY = ContainerKey("bank", "inv.bank",capacity = 800, stackType = ContainerStackType.STACK)
