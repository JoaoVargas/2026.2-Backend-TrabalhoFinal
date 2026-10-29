package br.pucpr.alexandria.categories

data class CategoryResponse(
    val id: Long,
    val name: String
) {
    constructor(category: Category) : this(id = category.id!!, name = category.name)
}
