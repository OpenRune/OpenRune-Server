package org.alter.mechanics.shops

import org.alter.rscm.RSCM.getRSCM


/**
 * @author Tom <rspsmods@gmail.com>
 */
class CoinCurrency : ItemCurrency(getRSCM("items.coins"), singularCurrency = "coin", pluralCurrency = "coins")
