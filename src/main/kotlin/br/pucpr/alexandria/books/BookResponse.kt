package br.pucpr.alexandria.books

import java.math.BigDecimal

data class BookResponse(
    val id: Long,
    val title: String,
    val isbn: String,
    val price: BigDecimal,
    val publishedYear: Int,
    val stock: Int,
    val authorId: Long,
    val authorName: String,
    val categories: List<String>
) {
    constructor(book: Book) : this(
        id = book.id!!,
        title = book.title,
        isbn = book.isbn,
        price = book.price,
        publishedYear = book.publishedYear,
        stock = book.stock,
        authorId = book.author.id!!,
        authorName = book.author.name,
        categories = book.categories.map { it.name }
    )
}
