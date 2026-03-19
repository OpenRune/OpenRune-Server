package org.rsmod.api.type.refs.category

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.category.CategoryType
import org.rsmod.game.type.category.CategoryTypeBuilder

public abstract class CategoryReferences : TypeReferences<CategoryType>() {
    public fun category(internal: String): CategoryType {
        val type = CategoryTypeBuilder(internal).build(id = -1)
        cache += type
        return type
    }
}
