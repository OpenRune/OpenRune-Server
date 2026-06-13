package org.rsmod.content.skills.cooking

import org.rsmod.api.table.cooking.CookingAlesRow
import org.rsmod.api.table.cooking.CookingFoodsRow

val CookingFoodsRow.raw get() = input
val CookingFoodsRow.cooked get() = output
val CookingFoodsRow.level get() = statReq.first().t1

val CookingAlesRow.ingredient get() = input
val CookingAlesRow.ingredientCount get() = inputAmount
val CookingAlesRow.result get() = output.first()
val CookingAlesRow.matureResult get() = output[1]
val CookingAlesRow.level get() = statReq.first().t1
